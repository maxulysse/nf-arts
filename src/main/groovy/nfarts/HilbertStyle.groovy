package nfarts

import groovy.transform.CompileStatic

/**
 * Tasks end to end along a Hilbert curve in start order, each taking a
 * share proportional to its duration.
 */
@CompileStatic
class HilbertStyle implements Style {

    static final int MAX_ORDER = 7

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
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
            final stroke = tile.status().thin(cell * (0.3d + 0.5d * run.memory(tile)))
            canvas.polyline(points, stroke, run.color(tile, palette), tile.status().opacity)
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
