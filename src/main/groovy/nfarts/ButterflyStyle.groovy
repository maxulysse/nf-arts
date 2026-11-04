package nfarts

import groovy.transform.CompileStatic

/**
 * A butterfly grown from the run. Processes that start in the first half of
 * the run fill the forewings, the rest the hindwings, and each is a cell
 * between two veins. Each task is a stroke in its cell, on both sides:
 * distance from the body is start time, length is duration, width is memory,
 * so bands appear where processes ran at the same time.
 */
@CompileStatic
class ButterflyStyle implements Style {

    static final int STEPS = 24, EYESPOTS = 3, ANTENNA = 16, ABDOMEN = 32
    static final double START = 12d, FOLD = 95d, END = 170d

    private static record Cell(String process, double from, double to, double reach) {
        double middle() {
            (from + to) / 2
        }
    }

    private static record Wing(List<Cell> cells, double size, double skew, double tail) {
        double from() {
            cells[0].from()
        }

        double to() {
            cells[-1].to()
        }

        /** long cells pull the outline out, blended between neighbours so it stays one curve */
        double reach(double angle) {
            if( angle <= cells[0].middle() )
                return cells[0].reach()
            for( int i = 1; i < cells.size(); i++ ) {
                if( angle <= cells[i].middle() ) {
                    final f = (angle - cells[i - 1].middle()) / (cells[i].middle() - cells[i - 1].middle())
                    return cells[i - 1].reach() + (cells[i].reach() - cells[i - 1].reach()) * f
                }
            }
            return cells[-1].reach()
        }

        double edge(double angle) {
            final u = (angle - from()) / (to() - from())
            final lobe = Math.pow(Math.sin(Math.PI * Math.pow(u, skew)), 0.5d)
            final swallowtail = tail * Math.exp(-Math.pow((u - 0.72d) / 0.05d, 2))
            return size * reach(angle) * (lobe + swallowtail)
        }

        Wing scaled(double factor) {
            new Wing(cells, size * factor, skew, tail)
        }
    }

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final half = run.t0 + run.span / 2
        final early = run.processes.count { String p -> run.tasksOf(p)[0].start() <= half } as int
        final split = Math.max(1, Math.min(early, run.processes.size() - 1))
        final front = run.processes.take(split)
        final back = run.processes.size() > 1 ? run.processes.drop(split) : front

        // the wing holding more of the run's compute time grows at the other's expense
        final share = work(run, front) / Math.max(1d, work(run, front) + work(run, back))
        // a typical task far shorter than the longest one gives the hindwings tails
        final typical = run.duration(run.tiles.toSorted { TaskTile t -> t.duration() }[run.tiles.size().intdiv(2) as int])
        final fore = wing(run, front, 0.46d + 0.2d * share, START, FOLD, 0.75d, 0d)
        final hind = wing(run, back, 0.3d + 0.2d * (1 - share), FOLD, END, 1.2d, 0.9d * Math.max(0d, 0.6d - typical))

        // measure the outline, then scale it to fill the canvas
        double wide = 0, up = 0, down = 0
        for( Wing wing : [fore, hind] ) {
            for( double angle = wing.from(); angle <= wing.to(); angle++ ) {
                final edge = wing.edge(angle)
                wide = Math.max(wide, x(0d, edge, angle))
                up = Math.max(up, -y(0d, edge, angle))
                down = Math.max(down, y(0d, edge, angle))
            }
        }
        final fit = Math.min(canvas.width * 0.47d / wide, canvas.height * 0.94d / (up + down))
        final wings = [fore.scaled(fit), hind.scaled(fit)]

        final cx = canvas.width / 2d
        final cy = canvas.height / 2d + fit * (up - down) / 2
        final both = { double[] points, Closure draw ->
            draw(points)
            final other = points.clone()
            for( int i = 0; i < other.length; i += 2 )
                other[i] = 2 * cx - other[i]
            draw(other)
        }

        for( Wing wing : wings ) {
            for( Cell cell : wing.cells() ) {
                final color = run.color(run.tasksOf(cell.process())[0], palette)
                both(outline(wing, cell, cx, cy)) { double[] p -> canvas.polygon(p, color, 0.3d) }
                for( TaskTile tile : run.tasksOf(cell.process()) ) {
                    final angle = cell.from() + (cell.to() - cell.from()) * (0.06d + 0.88d * random.nextDouble())
                    final edge = wing.edge(angle)
                    final r0 = edge * (0.14d + 0.8d * (tile.start() - run.t0) / run.span)
                    final r1 = Math.min(edge, r0 + Math.max(size * 0.006d, edge * tile.duration() / run.span))
                    final width = tile.status().thin(Math.max(1d, size * (0.0015d + 0.006d * run.memory(tile))))
                    both([x(cx, r0, angle), y(cy, r0, angle), x(cx, r1, angle), y(cy, r1, angle)] as double[]) { double[] p ->
                        canvas.polyline(p, width, color, tile.status().opacity)
                    }
                }
            }
        }

        eyespots(run, wings[1], canvas, palette, cx, cy, both)

        for( Wing wing : wings ) {
            for( Cell cell : wing.cells() ) {
                both(outline(wing, cell, cx, cy)) { double[] p ->
                    canvas.polyline(p, Math.max(1d, size * 0.002d), palette.background, 0.8d)
                }
                // a spot at the margin of each cell, larger for processes with more tasks
                final r = size * 0.012d * Math.sqrt(run.tasksOf(cell.process()).size() / (double) run.tiles.size())
                final edge = wing.edge(cell.middle()) * 0.93d
                both([x(cx, edge, cell.middle()), y(cy, edge, cell.middle())] as double[]) { double[] p ->
                    canvas.circle(p[0], p[1], Math.max(size * 0.002d, r), palette.ink(), 0.7d)
                }
            }
            tears(run, wing, canvas, palette, cx, cy, random)
        }
        body(run, canvas, cx, cy, palette)
    }

    private static double work(Run run, List<String> processes) {
        processes.sum(0d) { String p -> run.tasksOf(p).sum(0d) { TaskTile t -> t.duration() as double } } as double
    }

    private static Wing wing(Run run, List<String> processes, double size, double from, double to, double skew, double tail) {
        final weights = processes.collect { String p -> 1 + Math.log(run.tasksOf(p).size() as double) }
        final total = weights.sum() as double
        final means = processes.collect { String p ->
            final tasks = run.tasksOf(p)
            (tasks.sum(0d) { TaskTile t -> run.duration(t) } as double) / tasks.size()
        }
        final cells = new ArrayList<Cell>()
        double angle = from
        processes.eachWithIndex { String p, int i ->
            final next = angle + (to - from) * weights[i] / total
            // averaged with the neighbours, or many small cells give a torn outline
            final near = means.subList(Math.max(0, i - 2), Math.min(means.size(), i + 3))
            cells << new Cell(p, angle, next, 0.75d + 0.25d * (near.sum() as double) / near.size())
            angle = next
        }
        return new Wing(cells, size, skew, tail)
    }

    private static double x(double cx, double r, double angle) {
        cx + r * Math.sin(Math.toRadians(angle))
    }

    private static double y(double cy, double r, double angle) {
        cy - r * Math.cos(Math.toRadians(angle))
    }

    /** the cell as a closed line from the body, around its edge and back */
    private static double[] outline(Wing wing, Cell cell, double cx, double cy) {
        final points = new double[(STEPS + 3) * 2]
        points[0] = points[points.length - 2] = cx
        points[1] = points[points.length - 1] = cy
        for( int i = 0; i <= STEPS; i++ ) {
            final angle = cell.from() + (cell.to() - cell.from()) * i / STEPS
            final edge = wing.edge(angle)
            points[2 * i + 2] = x(cx, edge, angle)
            points[2 * i + 3] = y(cy, edge, angle)
        }
        return points
    }

    /** eyespots on the hindwing cells of the processes with the largest tasks, sized by memory and kept between the veins */
    private static void eyespots(Run run, Wing wing, Canvas canvas, Palette palette, double cx, double cy, Closure both) {
        final peak = { Cell c -> run.tasksOf(c.process()).max { TaskTile t -> t.memory() } }
        for( Cell cell : wing.cells().toSorted { Cell c -> -peak(c).memory() }.take(EYESPOTS) ) {
            final top = peak(cell) as TaskTile
            final angle = cell.middle()
            final edge = wing.edge(angle) * 0.78d
            final room = edge * Math.sin(Math.toRadians(cell.to() - cell.from()) / 2)
            final r = Math.min(room * 0.9d, canvas.size * (0.01d + 0.016d * run.memory(top)))
            for( ring in [[1d, palette.ink(), 0.45d], [0.84d, palette.background, 0.95d], [0.5d, run.color(top, palette), 0.9d]] ) {
                final size = (ring[0] as double) * r
                both(ellipse(x(cx, edge, angle), y(cy, edge, angle), size, size * 1.3d, Math.toRadians(angle))) { double[] p ->
                    canvas.polygon(p, ring[1] as String, ring[2] as double)
                }
            }
        }
    }

    /** rx across, ry along the direction `tilt` points, clockwise from up */
    private static double[] ellipse(double cx, double cy, double rx, double ry, double tilt) {
        final points = new double[STEPS * 2]
        for( int i = 0; i < STEPS; i++ ) {
            final a = 2 * Math.PI * i / STEPS
            final across = rx * Math.cos(a), along = ry * Math.sin(a)
            points[2 * i] = cx + across * Math.cos(tilt) + along * Math.sin(tilt)
            points[2 * i + 1] = cy + across * Math.sin(tilt) - along * Math.cos(tilt)
        }
        return points
    }

    /** failed tasks bite the wing's edge, on one side only, the way real wings wear */
    private static void tears(Run run, Wing wing, Canvas canvas, Palette palette, double cx, double cy, Random random) {
        for( Cell cell : wing.cells() ) {
            for( TaskTile tile : run.tasksOf(cell.process()).findAll { TaskTile t -> t.status() == Status.FAILED } ) {
                final angle = cell.from() + (cell.to() - cell.from()) * random.nextDouble()
                final edge = wing.edge(angle)
                final px = x(cx, edge, angle)
                canvas.circle(random.nextBoolean() ? px : 2 * cx - px, y(cy, edge, angle), canvas.size * (0.006d + 0.01d * random.nextDouble()), palette.background, 1d)
            }
        }
    }

    /** the abdomen runs through the processes in start order, tip last */
    private static void body(Run run, Canvas canvas, double cx, double cy, Palette palette) {
        final size = canvas.size
        final n = run.processes.size()
        final head = cy - size * 0.05d, thorax = cy - size * 0.01d
        final tip = cy + size * 0.13d
        final ink = palette.ink()

        for( int side : [-1, 1] ) {
            final line = new double[(ANTENNA + 1) * 2]
            for( int i = 0; i <= ANTENNA; i++ ) {
                final f = i / (double) ANTENNA
                line[2 * i] = cx + side * size * (0.012d * f + 0.075d * f * f * f)
                line[2 * i + 1] = head - size * 0.19d * (f - 0.08d * f * f * f)
            }
            canvas.polyline(line, Math.max(0.6d, size * 0.001d), ink, 0.6d)
            canvas.polyline(Arrays.copyOfRange(line, line.length - 8, line.length), Math.max(1d, size * 0.003d), ink, 0.6d)
        }

        // the underlay shows between the bands
        final abdomen = new double[(ABDOMEN + 1) * 4]
        for( int i = 0; i <= ABDOMEN; i++ ) {
            final f = i / (double) ABDOMEN
            final y = thorax + (tip - thorax) * f
            final half = size * 0.0085d * Math.pow(Math.sin(Math.PI * (0.2d + 0.8d * f)), 0.7d)
            abdomen[2 * i] = cx + half
            abdomen[2 * i + 1] = y
            abdomen[abdomen.length - 2 * i - 2] = cx - half
            abdomen[abdomen.length - 2 * i - 1] = y
        }
        canvas.polygon(abdomen, ink, 0.45d)
        // three slices of colour, then one of underlay
        for( int i = 0; i < ABDOMEN; i += 4 ) {
            final a = 2 * i, b = 2 * (i + 3), last = abdomen.length - 2
            final band = [abdomen[a], abdomen[a + 1], abdomen[b], abdomen[b + 1], abdomen[last - b], abdomen[last - b + 1], abdomen[last - a], abdomen[last - a + 1]] as double[]
            canvas.polygon(band, palette.color((i * n / ABDOMEN) as int, n), 1d)
        }

        canvas.polygon(ellipse(cx, cy - size * 0.018d, size * 0.012d, size * 0.03d, 0d), palette.color(0, n), 1d)
        canvas.polygon(ellipse(cx, head, size * 0.0075d, size * 0.007d, 0d), palette.color(0, n), 1d)
    }
}
