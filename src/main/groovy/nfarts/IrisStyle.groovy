package nfarts

import groovy.transform.CompileStatic

/**
 * The run as an eye's iris, one fibre per task. See `docs/styles.md`.
 */
@CompileStatic
class IrisStyle implements Style {

    final String defaultPalette = 'ocean'

    private static final int STEPS = 24

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final cx = canvas.width / 2d, cy = canvas.height / 2d
        final pupil = size * 0.13d, outer = size * 0.46d
        final collarette = pupil + (outer - pupil) * 0.3d
        final n = run.tiles.size()
        // the width that would close the circle halfway out, so many tasks fill the iris
        final gap = 2 * Math.PI * (pupil + outer) / 2 / n
        final angles = new double[n], ends = new double[n]

        run.tiles.eachWithIndex { TaskTile tile, int k ->
            final a0 = 2 * Math.PI * ((k + random.nextDouble() * 0.8d) / n - 0.25d)
            final end = collarette + (outer - collarette) * (0.3d + 0.7d * run.duration(tile))
            angles[k] = a0
            ends[k] = end
            final width = Math.min(size * 0.012d, gap * (0.6d + 1.4d * run.memory(tile)))
            final stroke = tile.status().thin(Math.max(1d, width))
            canvas.polyline(fibre(cx, cy, pupil, end, a0, n, random), stroke, run.color(tile, palette), tile.status().opacity)
        }

        canvas.polyline(ring(angles, ends, cx, cy, pupil, collarette), Math.max(1d, size * 0.003d), palette.ink(), 0.25d)

        canvas.circle(cx, cy, pupil, palette.background, 1d)
        // the catchlight sits on the side of the longest task
        final longest = angles[(0..<n).max { int k -> ends[k] } as int]
        canvas.circle(cx + pupil * 0.5d * Math.cos(longest), cy + pupil * 0.5d * Math.sin(longest), pupil * 0.18d, palette.ink(), 0.8d)
    }

    /** each fibre wanders a little, so neighbors cross like iris stroma */
    private static double[] fibre(double cx, double cy, double pupil, double end, double a0, int n, Random random) {
        final wobble = (0.3d + random.nextDouble()) * Math.min(0.03d, 6d / n)
        final phase = random.nextDouble() * 2 * Math.PI
        final points = new double[(STEPS + 1) * 2]
        for( int i = 0; i <= STEPS; i++ ) {
            final f = i / (double) STEPS
            final r = pupil + (end - pupil) * f
            final a = a0 + wobble * Math.sin(phase + f * 3 * Math.PI)
            points[2 * i] = cx + r * Math.cos(a)
            points[2 * i + 1] = cy + r * Math.sin(a)
        }
        return points
    }

    /**
     * The collarette: a ring where the inner and outer zones meet, pushed out
     * where nearby fibres are long, so its shape follows the run's durations.
     */
    private static double[] ring(double[] angles, double[] ends, double cx, double cy, double pupil, double collarette) {
        final n = ends.length
        final window = Math.max(1, n.intdiv(60) as int)
        final mean = ends.sum() / n
        final points = new double[(n + 1) * 2]
        for( int k = 0; k <= n; k++ ) {
            double sum = 0
            for( int j = -window; j <= window; j++ )
                sum += ends[Math.floorMod(k + j, n)]
            final r = Math.max(pupil * 1.1d, collarette + (sum / (2 * window + 1) - mean) * 0.6d)
            points[2 * k] = cx + r * Math.cos(angles[k % n])
            points[2 * k + 1] = cy + r * Math.sin(angles[k % n])
        }
        return points
    }
}
