package nfarts

import groovy.transform.CompileStatic

/**
 * Each task is a stroke drifting through a flow field seeded by the run.
 * It starts at its place in start order (x) in the row of its process (y);
 * stroke length is duration, width is memory.
 */
@CompileStatic
class FlowFieldStyle implements Style {

    static final int WAVES = 4
    // keeps the SVG size reasonable on large runs
    static final int MAX_POINTS = 200_000

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = Math.min(canvas.width, canvas.height)
        final fx = new double[WAVES], fy = new double[WAVES], phase = new double[WAVES]
        for( int k = 0; k < WAVES; k++ ) {
            fx[k] = (2 + random.nextDouble() * 6) / size
            fy[k] = (2 + random.nextDouble() * 6) / size
            phase[k] = random.nextDouble() * 2 * Math.PI
        }
        final left = canvas.width * 0.08d, width = canvas.width * 0.84d
        final top = canvas.height * 0.08d, height = canvas.height * 0.84d
        final row = height / run.processes.size()
        final maxSteps = Math.min(40, Math.max(8, MAX_POINTS.intdiv(run.tiles.size()) as int))
        final step = size * 0.004d

        run.tiles.eachWithIndex { TaskTile tile, int i ->
            double x = left + width * ((i + 0.5d) / run.tiles.size() + (random.nextDouble() - 0.5d) * 0.06d)
            double y = top + row * (run.processes.indexOf(tile.process()) + 0.5d + (random.nextDouble() - 0.5d) * 0.8d)
            final steps = 2 + (int) (maxSteps * run.duration(tile))
            final points = new ArrayList<Double>()
            points << x << y
            for( int s = 1; s < steps; s++ ) {
                double angle = 0
                for( int k = 0; k < WAVES; k++ )
                    angle += Math.sin(fx[k] * x + fy[k] * y + phase[k])
                angle *= Math.PI / 2
                x += step * Math.cos(angle)
                y += step * Math.sin(angle)
                if( x < 0 || y < 0 || x > canvas.width || y > canvas.height )
                    break
                points << x << y
            }
            if( points.size() == 2 )
                points << x + 0.01d << y
            final stroke = Math.max(0.6d, tile.status().thin(Math.max(0.6d, size * (0.0015d + 0.006d * run.memory(tile)))))
            canvas.polyline(points as double[], stroke, run.color(tile, palette), tile.status().opacity * 0.7d)
        }
    }
}
