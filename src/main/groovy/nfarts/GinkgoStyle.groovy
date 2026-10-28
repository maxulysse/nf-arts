package nfarts

import groovy.transform.CompileStatic

/**
 * The run as one ginkgo leaf. Each task is a vein fanning out from the
 * stem and forking near its tip: angle is start time, left to right across
 * the fan, length is duration, width is memory.
 */
@CompileStatic
class GinkgoStyle implements Style {

    // half the opening of the fan
    static final double SPREAD = Math.toRadians(56)
    static final int STEPS = 12

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = Math.min(canvas.width, canvas.height)
        final cx = canvas.width / 2d
        final by = canvas.height * 0.82d
        final radius = size * 0.6d
        final wave = random.nextDouble() * 2 * Math.PI

        // outline: wavy outer edge with the notch in the middle
        final edge = 60
        final outline = new double[(edge + 3) * 2]
        outline[0] = cx
        outline[1] = by
        for( int k = 0; k <= edge; k++ ) {
            final a = -SPREAD + 2 * SPREAD * k / edge
            final r = rim(a, radius, wave)
            outline[(k + 1) * 2] = cx + r * Math.sin(a)
            outline[(k + 1) * 2 + 1] = by - r * Math.cos(a)
        }
        outline[(edge + 2) * 2] = cx
        outline[(edge + 2) * 2 + 1] = by
        final leaf = palette.color(run.processes.size().intdiv(2), run.processes.size())
        canvas.polygon(outline, leaf, 0.15d)

        for( TaskTile tile : run.tiles ) {
            final target = -SPREAD + 2 * SPREAD * (tile.start() - run.t0) / run.span + (random.nextDouble() - 0.5d) * 0.02d
            final length = rim(target, radius, wave) * (0.55d + 0.42d * run.duration(tile))
            final width = tile.status().thin(Math.max(0.5d, size * (0.0008d + 0.003d * run.memory(tile))))
            final color = run.color(tile, palette)
            final opacity = tile.status().opacity * 0.85d
            // veins leave the stem together and spread out, like ginkgo venation, then fork
            final fork = 0.55d + 0.2d * random.nextDouble()
            final trunk = vein(cx, by, target, length, 0, fork, 0)
            canvas.polyline(trunk, width, color, opacity)
            for( int side : [-1, 1] )
                canvas.polyline(vein(cx, by, target, length, fork, 1, side * 0.025d), width * 0.7d, color, opacity)
        }

        canvas.polyline(outline, Math.max(1d, size * 0.002d), leaf, 0.6d)
        canvas.polyline([cx, by, cx - size * 0.01d, by + size * 0.08d, cx + size * 0.005d, canvas.height * 0.97d] as double[],
            Math.max(1.5d, size * 0.012d), leaf, 0.9d)
    }

    /** distance from the stem to the leaf edge at angle a: wavy, notched in the middle, rounded at the shoulders */
    static double rim(double a, double radius, double wave) {
        final notch = 1 - 0.3d * Math.exp(-Math.pow(a / 0.09d, 2))
        final shoulder = 1 - 0.35d * Math.pow(Math.abs(a) / SPREAD, 5)
        return radius * (0.93d + 0.04d * Math.sin(a * 9 + wave)) * notch * shoulder
    }

    /** points of a vein from fraction `from` to `to` of its length, angled off by `split` after a fork */
    static double[] vein(double cx, double by, double target, double length, double from, double to, double split) {
        final points = new double[(STEPS + 1) * 2]
        for( int s = 0; s <= STEPS; s++ ) {
            final t = from + (to - from) * s / STEPS
            final a = target * (0.15d + 0.85d * Math.sqrt(t)) + split * (t - from)
            points[s * 2] = cx + length * t * Math.sin(a)
            points[s * 2 + 1] = by - length * t * Math.cos(a)
        }
        return points
    }
}
