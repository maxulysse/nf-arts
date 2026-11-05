package nfarts

import groovy.transform.CompileStatic

/**
 * One block per process, split into one block per task, area proportional
 * to duration. Opacity is memory; failed tasks are hollow. Pairs with the
 * `mondrian` palette.
 */
@CompileStatic
class MondrianStyle implements Style {

    @Override
    String getDefaultPalette() {
        'mondrian'
    }

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final gap = Math.max(1.5d, size * 0.008d)
        final groups = run.groups
        final weights = groups.collect { List<TaskTile> g -> (double) g.sum { TaskTile t -> weight(t) } }
        final blocks = partition(weights, [size * 0.05d + (canvas.width - size) / 2d, size * 0.05d + (canvas.height - size) / 2d, size * 0.9d, size * 0.9d] as double[], random)

        groups.eachWithIndex { List<TaskTile> group, int i ->
            final b = blocks[i]
            final cells = partition(group.collect { TaskTile t -> weight(t) }, [b[0] + gap / 2, b[1] + gap / 2, b[2] - gap, b[3] - gap] as double[], random)
            group.eachWithIndex { TaskTile tile, int j ->
                final c = cells[j]
                final x0 = c[0] + gap / 2, y0 = c[1] + gap / 2, x1 = c[0] + c[2] - gap / 2, y1 = c[1] + c[3] - gap / 2
                if( x1 - x0 < 0.5d || y1 - y0 < 0.5d )
                    return
                final rect = [x0, y0, x1, y0, x1, y1, x0, y1] as double[]
                final color = run.color(tile, palette)
                if( tile.status() == Status.FAILED )
                    canvas.polyline([x0, y0, x1, y0, x1, y1, x0, y1, x0, y0] as double[], Math.max(1d, gap / 2), color, 1d)
                else
                    canvas.polygon(rect, color, tile.status() == Status.CACHED ? 0.25d : 0.75d + 0.25d * run.memory(tile))
            }
        }
    }

    private static double weight(TaskTile tile) {
        Math.max(1L, tile.duration()) as double
    }

    /**
     * Split a [x, y, w, h] block in two along its longer side, in proportion to
     * the weights on each side, until each weight has its own block.
     * Returns one block per weight, in order.
     */
    static List<double[]> partition(List<Double> weights, double[] block, Random random) {
        if( weights.size() == 1 )
            return [block]
        final total = (double) weights.sum()
        double acc = 0
        int cut = 0
        while( cut < weights.size() - 1 && acc < total / 2 )
            acc += weights[cut++]
        final f = acc / total
        final x = block[0], y = block[1], w = block[2], h = block[3]
        // near square blocks split either way, the seed decides
        final vertical = w > h * 1.2d || (h <= w * 1.2d && random.nextBoolean())
        final first = vertical ? [x, y, w * f, h] : [x, y, w, h * f]
        final second = vertical ? [x + w * f, y, w * (1 - f), h] : [x, y + h * f, w, h * (1 - f)]
        final List<double[]> result = partition(weights.subList(0, cut), first as double[], random)
        result.addAll(partition(weights.subList(cut, weights.size()), second as double[], random))
        return result
    }
}
