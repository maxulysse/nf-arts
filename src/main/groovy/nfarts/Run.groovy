package nfarts

import groovy.transform.CompileStatic

/**
 * Shared measures over the tasks of a run.
 */
@CompileStatic
class Run {
    final List<TaskTile> tiles
    final List<String> processes
    final long t0
    final long span
    private final double memMin
    private final double memMax
    private final double durMin
    private final double durMax

    Run(List<TaskTile> tiles) {
        this.tiles = replayCached(tiles).toSorted { TaskTile it -> it.start() }
        // processes in order of first appearance
        this.processes = this.tiles.collect { TaskTile it -> it.process() }.unique()
        this.t0 = this.tiles*.start().min() as long
        final t1 = this.tiles.collect { TaskTile it -> it.start() + it.duration() }.max() as long
        this.span = Math.max(1L, t1 - t0)
        final memory = this.tiles*.memory().findAll { long m -> m > 0 }
        this.memMin = memory ? Math.log(memory.min() as double) : 0d
        this.memMax = memory ? Math.log(memory.max() as double) : 0d
        final duration = this.tiles*.duration().findAll { long d -> d > 0 }
        this.durMin = duration ? Math.log(duration.min() as double) : 0d
        this.durMax = duration ? Math.log(duration.max() as double) : 0d
    }

    /** cached tasks carry the times of an earlier run, so move them to the start of this one */
    static List<TaskTile> replayCached(List<TaskTile> tiles) {
        final fresh = tiles.findAll { TaskTile it -> it.status() != Status.CACHED }
        if( !fresh || fresh.size() == tiles.size() )
            return tiles
        final t0 = fresh*.start().min() as long
        return tiles.collect { TaskTile it ->
            it.status() == Status.CACHED ? new TaskTile(it.process(), t0, it.duration(), it.memory(), it.status()) : it
        }
    }

    String color(TaskTile tile, Palette palette) {
        palette.color(processes.indexOf(tile.process()), processes.size())
    }

    /** memory in [0,1], log-scaled between the smallest and largest task so one big task does not flatten the rest */
    double memory(TaskTile tile) {
        scale(tile.memory(), memMin, memMax)
    }

    /** duration in [0,1], on the same log scale as memory */
    double duration(TaskTile tile) {
        scale(tile.duration(), durMin, durMax)
    }

    private static double scale(long value, double min, double max) {
        if( value <= 0 )
            return 0d
        if( max == min )
            return 1d
        (Math.log(value as double) - min) / (max - min)
    }

    List<TaskTile> tasksOf(String process) {
        tiles.findAll { TaskTile it -> it.process() == process }
    }
}
