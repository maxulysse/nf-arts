package nfarts

import groovy.transform.CompileStatic

/**
 * Turns the tasks of a run into shapes on a canvas.
 */
@CompileStatic
interface Style {
    void draw(List<TaskTile> tiles, Canvas canvas, Palette palette, Random random)
}

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
        this.tiles = tiles.toSorted { TaskTile it -> it.start() }
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

    static double opacity(TaskTile tile) {
        switch( tile.status() ) {
            case 'CACHED': return 0.3d
            case 'FAILED': return 1d
            default: return 0.8d
        }
    }
}

/**
 * Each process is a ring, each task an arc on it: angle is time,
 * clockwise from 12 o'clock, thickness is memory.
 */
@CompileStatic
class RadialStyle implements Style {

    @Override
    void draw(List<TaskTile> tiles, Canvas canvas, Palette palette, Random random) {
        final run = new Run(tiles)
        final size = Math.min(canvas.width, canvas.height)
        final cx = canvas.width / 2d, cy = canvas.height / 2d
        final inner = size * 0.10d, outer = size * 0.46d
        final band = (outer - inner) / run.processes.size()

        for( TaskTile tile : run.tiles ) {
            final ring = run.processes.indexOf(tile.process())
            // jitter inside the band so parallel tasks of one process fan out
            final r = inner + band * (ring + 0.5d + (random.nextDouble() - 0.5d) * 0.6d)
            final start = -90d + 360d * (tile.start() - run.t0) / run.span
            final sweep = Math.max(0.6d, 360d * tile.duration() / run.span)
            double stroke = Math.max(1d, band * (0.4d + 0.8d * run.memory(tile)))
            if( tile.status() == 'FAILED' )
                stroke = Math.max(1d, stroke / 3)
            canvas.arc(cx, cy, r, start, sweep, stroke, run.color(tile, palette), Run.opacity(tile))
        }
    }
}

/**
 * Tasks end to end along a Hilbert curve in start order, each taking a
 * share proportional to its duration.
 */
@CompileStatic
class HilbertStyle implements Style {

    static final int MAX_ORDER = 7

    @Override
    void draw(List<TaskTile> tiles, Canvas canvas, Palette palette, Random random) {
        final run = new Run(tiles)
        final order = order(run.tiles.size())
        final n = 1 << order
        final cells = n * n
        final size = Math.min(canvas.width, canvas.height) * 0.9d
        final cell = size / n
        final ox = (canvas.width - size) / 2d + cell / 2, oy = (canvas.height - size) / 2d + cell / 2
        // the seed picks one of the 8 symmetries of the square
        final symmetry = random.nextInt(8)

        final total = (double) run.tiles.sum { TaskTile it -> Math.max(1L, it.duration()) }
        double acc = 0
        int from = 0
        for( TaskTile tile : run.tiles ) {
            acc += Math.max(1L, tile.duration())
            final to = Math.max(from + 1, Math.min(cells - 1, (int) Math.round(acc / total * (cells - 1))))
            final points = new double[(to - from + 1) * 2]
            for( int d = from; d <= to; d++ ) {
                final xy = transform(d2xy(n, Math.min(d, cells - 1)), n, symmetry)
                points[(d - from) * 2] = ox + xy[0] * cell
                points[(d - from) * 2 + 1] = oy + xy[1] * cell
            }
            double stroke = cell * (0.3d + 0.5d * run.memory(tile))
            if( tile.status() == 'FAILED' )
                stroke = stroke / 3
            canvas.polyline(points, stroke, run.color(tile, palette), Run.opacity(tile))
            from = Math.min(to, cells - 2)
        }
    }

    /** smallest order with about 16 cells per task, between 3 and MAX_ORDER */
    static int order(int tasks) {
        int k = 3
        while( k < MAX_ORDER && (1L << (2 * k)) < tasks * 16L )
            k++
        return k
    }

    /** cell coordinates of distance d along the Hilbert curve of side n */
    static int[] d2xy(int n, int d) {
        int x = 0, y = 0, t = d
        for( int s = 1; s < n; s *= 2 ) {
            final rx = 1 & (t / 2 as int)
            final ry = 1 & (t ^ rx)
            if( ry == 0 ) {
                if( rx == 1 ) {
                    x = s - 1 - x
                    y = s - 1 - y
                }
                final tmp = x; x = y; y = tmp
            }
            x += s * rx
            y += s * ry
            t = t / 4 as int
        }
        return [x, y] as int[]
    }

    static int[] transform(int[] xy, int n, int symmetry) {
        int x = xy[0], y = xy[1]
        if( symmetry & 1 ) { final tmp = x; x = y; y = tmp }
        if( symmetry & 2 ) x = n - 1 - x
        if( symmetry & 4 ) y = n - 1 - y
        return [x, y] as int[]
    }
}
