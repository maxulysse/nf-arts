package nfarts

import groovy.transform.CompileStatic

/**
 * Tasks as seeds of a sunflower: dots on a golden-angle spiral in start
 * order, dot size is memory.
 */
@CompileStatic
class PhyllotaxisStyle implements Style {

    static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5))

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final n = run.tiles.size()
        final cx = canvas.width / 2d, cy = canvas.height / 2d
        // spacing so the outermost seed lands on the radius
        final c = canvas.size * 0.45d / Math.sqrt(n + 0.5d)
        final offset = random.nextDouble() * 2 * Math.PI

        run.tiles.eachWithIndex { TaskTile tile, int i ->
            final r = c * Math.sqrt(i + 0.5d)
            final a = offset + i * GOLDEN_ANGLE
            // a dot shrinks less than a line, so failed tasks stay visible
            final dot = c * (0.2d + 0.45d * run.memory(tile)) * (tile.status() == Status.FAILED ? 0.4d : 1d)
            canvas.circle(cx + r * Math.cos(a), cy + r * Math.sin(a), dot, run.color(tile, palette), tile.status().opacity)
        }
    }
}
