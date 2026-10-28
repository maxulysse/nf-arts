package nfarts

import groovy.transform.CompileStatic

/**
 * One ridge per process, stacked top to bottom: the height of the ridge
 * is how many tasks of the process were running at that time, relative
 * to the busiest moment of that process.
 */
@CompileStatic
class RidgelineStyle implements Style {

    static final int BINS = 240

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final processes = run.processes.size()
        final size = Math.min(canvas.width, canvas.height)
        final left = canvas.width * 0.08d, right = canvas.width * 0.92d
        final top = canvas.height * 0.15d, bottom = canvas.height * 0.9d
        final row = (bottom - top) / processes
        final amplitude = Math.min(row * 2.5d, (bottom - top) * 0.35d)

        // running tasks per process and time bin, cached tasks count less
        final load = new double[processes][BINS]
        for( TaskTile tile : run.tiles ) {
            final p = run.processes.indexOf(tile.process())
            final a = (tile.start() - run.t0) / (double) run.span * BINS
            final b = (tile.start() + Math.max(1L, tile.duration()) - run.t0) / (double) run.span * BINS
            final weight = tile.status() == Status.CACHED ? 0.3d : 1d
            for( int k = (int) a; k < Math.min(BINS, (int) Math.ceil(b)); k++ )
                load[p][k] += weight * (Math.min(b, k + 1d) - Math.max(a, (double) k))
        }
        final stroke = Math.max(1.2d, size * 0.0025d)
        // back to front, so lower ridges hide the ridges above them
        for( int p = 0; p < processes; p++ ) {
            final base = top + row * (p + 1)
            final peak = Math.max(1e-9d, load[p].max())
            final line = new double[BINS * 2]
            for( int k = 0; k < BINS; k++ ) {
                final v = (load[p][Math.max(0, k - 1)] + 2 * load[p][k] + load[p][Math.min(BINS - 1, k + 1)]) / 4
                line[k * 2] = left + (right - left) * (k + 0.5d) / BINS
                line[k * 2 + 1] = base - amplitude * Math.sqrt(v / peak) - random.nextDouble() * row * 0.04d
            }
            final fill = Arrays.copyOf(line, line.length + 4)
            fill[line.length] = right
            fill[line.length + 1] = base + stroke
            fill[line.length + 2] = left
            fill[line.length + 3] = base + stroke
            canvas.polygon(fill, palette.background, 1d)
            canvas.polyline(line, stroke, palette.color(p, processes), 1d)
        }
    }
}
