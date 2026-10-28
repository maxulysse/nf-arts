package nfarts

import groovy.transform.CompileStatic

/**
 * Each process is a ring, each task an arc on it: angle is time,
 * clockwise from 12 o'clock, thickness is memory.
 */
@CompileStatic
class RadialStyle implements Style {

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final cx = canvas.width / 2d, cy = canvas.height / 2d
        final inner = size * 0.10d, outer = size * 0.46d
        final band = (outer - inner) / run.processes.size()

        for( TaskTile tile : run.tiles ) {
            final ring = run.processes.indexOf(tile.process())
            // jitter inside the band so parallel tasks of one process fan out
            final r = inner + band * (ring + 0.5d + (random.nextDouble() - 0.5d) * 0.6d)
            final start = -90d + 360d * (tile.start() - run.t0) / run.span
            final sweep = Math.max(0.6d, 360d * tile.duration() / run.span)
            final stroke = Math.max(1d, tile.status().thin(Math.max(1d, band * (0.4d + 0.8d * run.memory(tile)))))
            canvas.arc(cx, cy, r, start, sweep, stroke, run.color(tile, palette), tile.status().opacity)
        }
    }
}
