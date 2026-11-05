package nfarts

import groovy.transform.CompileStatic

/**
 * Shared measures over the tasks of a run, which has at least one task.
 */
@CompileStatic
class Run {
    final List<TaskTile> tiles
    final List<String> processes
    final long t0
    final long span
    private final Map<String, List<TaskTile>> byProcess
    private final LogScale memoryScale
    private final LogScale durationScale

    Run(List<TaskTile> tiles) {
        // ties must not depend on arrival, which differs between a live run and its history
        this.tiles = replayCached(tiles).toSorted { TaskTile a, TaskTile b ->
            a.start() <=> b.start() ?: a.process() <=> b.process() ?: a.duration() <=> b.duration() ?: a.memory() <=> b.memory() ?: a.status() <=> b.status()
        }
        // processes in order of first appearance
        this.byProcess = this.tiles.groupBy { TaskTile it -> it.process() }
        this.processes = byProcess.keySet().toList()
        this.t0 = this.tiles*.start().min() as long
        final t1 = this.tiles.collect { TaskTile it -> it.start() + it.duration() }.max() as long
        this.span = Math.max(1L, t1 - t0)
        this.memoryScale = LogScale.of(this.tiles*.memory())
        this.durationScale = LogScale.of(this.tiles*.duration())
    }

    /** cached tasks carry the times of an earlier run, so move them to the start of this one */
    private static List<TaskTile> replayCached(List<TaskTile> tiles) {
        final fresh = tiles.findAll { TaskTile it -> it.status() != Status.CACHED }
        if( !fresh || fresh.size() == tiles.size() )
            return tiles
        final t0 = fresh*.start().min() as long
        return tiles.collect { TaskTile it ->
            it.status() == Status.CACHED ? new TaskTile(it.process(), t0, it.duration(), it.memory(), it.status()) : it
        }
    }

    /** the place of the task's process, in order of first appearance */
    int rank(TaskTile tile) {
        processes.indexOf(tile.process())
    }

    String color(TaskTile tile, Palette palette) {
        palette.color(rank(tile), processes.size())
    }

    /** memory in [0,1], log-scaled between the smallest and largest task so one big task does not flatten the rest */
    double memory(TaskTile tile) {
        memoryScale.at(tile.memory())
    }

    /** duration in [0,1], on the same log scale as memory */
    double duration(TaskTile tile) {
        durationScale.at(tile.duration())
    }

    List<TaskTile> tasksOf(String process) {
        byProcess[process]
    }

    /** the tasks of each process, in the order of `processes` */
    List<List<TaskTile>> getGroups() {
        byProcess.values().toList()
    }

    private static record LogScale(double min, double max) {
        static LogScale of(List<Long> values) {
            final positive = values.findAll { long v -> v > 0 }
            return positive ? new LogScale(Math.log(positive.min() as double), Math.log(positive.max() as double)) : new LogScale(0d, 0d)
        }

        double at(long value) {
            if( value <= 0 )
                return 0d
            if( max == min )
                return 1d
            (Math.log(value as double) - min) / (max - min)
        }
    }
}
