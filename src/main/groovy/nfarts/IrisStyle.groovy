package nfarts

import groovy.transform.CompileStatic

/**
 * An eye's iris: each task is a fibre running out from the pupil, in start
 * order clockwise from 12 o'clock. Length is duration, width is memory.
 */
@CompileStatic
class IrisStyle implements Style {

    static final int STEPS = 24

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final cx = canvas.width / 2d, cy = canvas.height / 2d
        final pupil = size * 0.13d, outer = size * 0.46d
        final collarette = pupil + (outer - pupil) * 0.3d
        final n = run.tiles.size()
        // the width that would close the circle halfway out, so many tasks fill the iris
        final gap = 2 * Math.PI * (pupil + outer) / 2 / Math.max(1, n)

        run.tiles.eachWithIndex { TaskTile tile, int k ->
            final a0 = 2 * Math.PI * ((k + random.nextDouble() * 0.8d) / n - 0.25d)
            final end = collarette + (outer - collarette) * (0.3d + 0.7d * run.duration(tile))
            // each fibre wanders a little, so neighbours cross like iris stroma
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
            final width = Math.min(size * 0.012d, gap * (0.6d + 1.4d * run.memory(tile)))
            final stroke = tile.status().thin(Math.max(1d, width))
            canvas.polyline(points, stroke, run.color(tile, palette), tile.status().opacity)
        }

        // the collarette: a wavy ring where the inner and outer zones meet
        final ring = new double[(STEPS * 8 + 1) * 2]
        final waves = 12 + random.nextInt(8)
        for( int i = 0; i <= STEPS * 8; i++ ) {
            final a = 2 * Math.PI * i / (STEPS * 8)
            final r = collarette * (1 + 0.04d * Math.sin(waves * a))
            ring[2 * i] = cx + r * Math.cos(a)
            ring[2 * i + 1] = cy + r * Math.sin(a)
        }
        canvas.polyline(ring, Math.max(1d, size * 0.003d), palette.ink(), 0.25d)

        canvas.circle(cx, cy, pupil, palette.background, 1d)
        canvas.circle(cx + pupil * 0.35d, cy - pupil * 0.35d, pupil * 0.18d, palette.ink(), 0.8d)
    }
}
