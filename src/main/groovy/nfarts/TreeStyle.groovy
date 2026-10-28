package nfarts

import groovy.transform.CompileStatic

/**
 * The run is the trunk, each process a branch in start order, each task a
 * twig with a leaf. Branch length is how long the process was active,
 * twig length is duration, leaf size is memory. Failed tasks have no leaf.
 */
@CompileStatic
class TreeStyle implements Style {

    @Override
    void draw(List<TaskTile> tiles, Canvas canvas, Palette palette, Random random) {
        final run = new Run(tiles)
        final size = Math.min(canvas.width, canvas.height)
        final cx = canvas.width / 2d
        final base = canvas.height * 0.95d, crown = canvas.height * 0.12d
        final trunk = size * 0.02d
        canvas.polyline([cx, base, cx + (random.nextDouble() - 0.5d) * size * 0.04d, crown] as double[], trunk, palette.at(0), 1d)

        final groups = run.processes.collect { String p -> run.tasksOf(p) }
        final most = groups*.size().max() as int
        final List<double[]> leaves = []
        final List<TaskTile> leafTiles = []

        groups.eachWithIndex { List<TaskTile> group, int i ->
            final first = group*.start().min() as long
            final last = group.collect { TaskTile t -> t.start() + t.duration() }.max() as long
            final active = Math.max(1L, last - first)
            final y0 = base - (base - crown) * (0.1d + 0.85d * (groups.size() > 1 ? i / (double) (groups.size() - 1) : 0.5d))
            final side = i % 2 == 0 ? -1 : 1
            final angle = Math.toRadians(35 + 30 * random.nextDouble())
            final length = size * (0.1d + 0.3d * Math.sqrt(active / (double) run.span))
            final dx = side * Math.sin(angle), dy = -Math.cos(angle)
            final x1 = cx + dx * length, y1 = y0 + dy * length
            canvas.polyline([cx, y0, x1, y1] as double[], Math.max(1d, trunk * 0.5d * Math.sqrt(group.size() / (double) most)), palette.at(0), 1d)

            group.eachWithIndex { TaskTile tile, int j ->
                final t = (tile.start() - first) / (double) active
                final px = cx + dx * length * t, py = y0 + dy * length * t
                final twig = Math.toRadians(20 + 30 * random.nextDouble()) * (j % 2 == 0 ? 1 : -1)
                final tx = dx * Math.cos(twig) - dy * Math.sin(twig), ty = dx * Math.sin(twig) + dy * Math.cos(twig)
                final len = size * (0.015d + 0.06d * run.duration(tile))
                final qx = px + tx * len, qy = py + ty * len
                canvas.polyline([px, py, qx, qy] as double[], Math.max(0.6d, size * 0.002d * (0.5d + run.memory(tile))), palette.at(0), 0.8d)
                if( tile.status() != 'FAILED' ) {
                    leaves << ([qx, qy, size * (0.004d + 0.012d * run.memory(tile))] as double[])
                    leafTiles << tile
                }
            }
        }
        // leaves on top of every branch
        leaves.eachWithIndex { double[] leaf, int k ->
            canvas.circle(leaf[0], leaf[1], leaf[2], run.color(leafTiles[k], palette), Run.opacity(leafTiles[k]))
        }
    }
}
