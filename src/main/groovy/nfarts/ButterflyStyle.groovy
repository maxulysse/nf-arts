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

    static final int STEPS = 24, EYESPOTS = 3, ANTENNA = 16, CLUB = 3, ABDOMEN = 32
    static final double WASH = 0.3d
    // how far along the outline, in degrees, a cell's reach is felt
    static final double BLEND = 6d

    private static final Shape FOREWING = new Shape(12d, 95d, 0.75d)
    private static final Shape HINDWING = new Shape(95d, 170d, 1.2d)

    private static record Shape(double from, double to, double skew) {}

    private static record Cell(List<TaskTile> tasks, String color, double from, double to, double reach) {
        double at(double f) {
            from + (to - from) * f
        }

        double middle() {
            at(0.5d)
        }

        TaskTile peak() {
            tasks.max { TaskTile t -> t.memory() }
        }
    }

    private static record Wing(List<Cell> cells, double size, double skew, double tail, double cx, double cy) {
        double from() {
            cells[0].from()
        }

        double to() {
            cells[-1].to()
        }

        /** long cells pull the outline out, blended so it stays one curve */
        double reach(double angle) {
            double sum = 0, total = 0
            for( Cell cell : cells ) {
                final weight = Math.exp(-Math.pow((angle - cell.middle()) / BLEND, 2))
                sum += weight * cell.reach()
                total += weight
            }
            return sum / total
        }

        double edge(double angle) {
            final u = Math.min(1d, (angle - from()) / (to() - from()))
            final lobe = Math.pow(Math.sin(Math.PI * Math.pow(u, skew)), 0.5d)
            final swallowtail = tail * Math.exp(-Math.pow((u - 0.72d) / 0.05d, 2))
            return size * reach(angle) * (lobe + swallowtail)
        }

        double x(double r, double angle) {
            cx + r * Math.sin(Math.toRadians(angle))
        }

        double y(double r, double angle) {
            cy - r * Math.cos(Math.toRadians(angle))
        }

        /** a closed line from the body, around the edge between two angles and back */
        double[] outline(double from, double to, int steps) {
            final points = new double[(steps + 3) * 2]
            points[0] = points[points.length - 2] = cx
            points[1] = points[points.length - 1] = cy
            for( int i = 0; i <= steps; i++ ) {
                final angle = from + (to - from) * i / steps
                points[2 * i + 2] = x(edge(angle), angle)
                points[2 * i + 3] = y(edge(angle), angle)
            }
            return points
        }

        Wing placed(double factor, double x, double y) {
            new Wing(cells, size * factor, skew, tail, x, y)
        }
    }

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final wings = wings(run, canvas, palette)
        final mirrored = new MirrorCanvas(canvas, wings[0].cx())

        for( Wing wing : wings ) {
            for( Cell cell : wing.cells() ) {
                mirrored.polygon(wing.outline(cell.from(), cell.to(), STEPS), cell.color(), WASH)
                strokes(run, wing, cell, mirrored, random)
            }
        }
        eyespots(run, wings[1], mirrored, palette)
        for( Wing wing : wings ) {
            veins(wing, mirrored, palette)
            spots(run, wing, mirrored, palette)
        }
        // on the plain canvas: a wing wears on one side only
        for( Wing wing : wings )
            tears(wing, canvas, palette, random)
        body(run, canvas, wings[0].cx(), wings[0].cy(), palette)
    }

    /** the fore and hind wing, sized by the run and placed to fill the canvas */
    private static List<Wing> wings(Run run, Canvas canvas, Palette palette) {
        final half = run.t0 + run.span / 2
        final late = run.processes.findAll { String p -> run.tasksOf(p)[0].start() > half } ?: run.processes.takeRight(1)
        final early = (run.processes - late) ?: late

        // the wing holding more of the run's compute time grows at the other's expense
        final foreWork = work(run, early), hindWork = work(run, late)
        final share = foreWork / Math.max(1d, foreWork + hindWork)
        // a typical task far shorter than the longest one gives the hindwings tails
        final typical = run.duration(run.tiles.toSorted { TaskTile t -> t.duration() }[run.tiles.size().intdiv(2) as int])
        final fore = wing(run, palette, early, FOREWING, 0.46d + 0.2d * share, 0d)
        final hind = wing(run, palette, late, HINDWING, 0.3d + 0.2d * (1 - share), 0.9d * Math.max(0d, 0.6d - typical))

        double wide = 0, up = 0, down = 0
        for( Wing wing : [fore, hind] ) {
            for( double angle = wing.from(); angle <= wing.to(); angle++ ) {
                final edge = wing.edge(angle)
                wide = Math.max(wide, wing.x(edge, angle))
                up = Math.max(up, -wing.y(edge, angle))
                down = Math.max(down, wing.y(edge, angle))
            }
        }
        final fit = Math.min(canvas.width * 0.47d / wide, canvas.height * 0.94d / (up + down))
        final cx = canvas.width / 2d, cy = canvas.height / 2d + fit * (up - down) / 2
        return [fore.placed(fit, cx, cy), hind.placed(fit, cx, cy)]
    }

    private static double work(Run run, List<String> processes) {
        run.tiles.findAll { TaskTile t -> t.process() in processes }.sum(0d) { TaskTile t -> t.duration() as double } as double
    }

    private static Wing wing(Run run, Palette palette, List<String> processes, Shape shape, double size, double tail) {
        final tasks = processes.collect { String p -> run.tasksOf(p) }
        final weights = tasks.collect { List<TaskTile> list -> 1 + Math.log(list.size() as double) }
        final total = weights.sum() as double
        final cells = new ArrayList<Cell>()
        double angle = shape.from()
        tasks.eachWithIndex { List<TaskTile> list, int i ->
            final next = angle + (shape.to() - shape.from()) * weights[i] / total
            final mean = (list.sum(0d) { TaskTile t -> run.duration(t) } as double) / list.size()
            cells << new Cell(list, run.color(list[0], palette), angle, next, 0.75d + 0.25d * mean)
            angle = next
        }
        return new Wing(cells, size, shape.skew(), tail, 0d, 0d)
    }

    private static void strokes(Run run, Wing wing, Cell cell, Canvas canvas, Random random) {
        final size = canvas.size
        for( TaskTile tile : cell.tasks() ) {
            final angle = cell.at(0.06d + 0.88d * random.nextDouble())
            final edge = wing.edge(angle)
            final r0 = edge * (0.14d + 0.8d * (tile.start() - run.t0) / run.span)
            final r1 = Math.min(edge, r0 + Math.max(size * 0.006d, edge * tile.duration() / run.span))
            final width = tile.status().thin(Math.max(1d, size * (0.0015d + 0.006d * run.memory(tile))))
            final points = [wing.x(r0, angle), wing.y(r0, angle), wing.x(r1, angle), wing.y(r1, angle)] as double[]
            canvas.polyline(points, width, cell.color(), tile.status().opacity)
        }
    }

    /** eyespots on the cells of the processes with the largest tasks, sized by memory and kept between the veins */
    private static void eyespots(Run run, Wing wing, Canvas canvas, Palette palette) {
        final cells = wing.cells().findAll { Cell c -> c.peak().memory() > 0 }.toSorted { Cell c -> -c.peak().memory() }
        for( Cell cell : cells.take(EYESPOTS) ) {
            final angle = cell.middle()
            final tilt = Math.toRadians(angle)
            final distance = wing.edge(angle) * 0.78d
            final room = distance * Math.sin(Math.toRadians(cell.to() - cell.from()) / 2)
            final r = Math.min(room * 0.9d, canvas.size * (0.01d + 0.016d * run.memory(cell.peak())))
            final px = wing.x(distance, angle), py = wing.y(distance, angle)
            canvas.polygon(ellipse(px, py, r, r * 1.3d, tilt), palette.ink(), 0.45d)
            canvas.polygon(ellipse(px, py, r * 0.84d, r * 1.09d, tilt), palette.background, 0.95d)
            canvas.polygon(ellipse(px, py, r * 0.5d, r * 0.65d, tilt), cell.color(), 0.9d)
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

    /** the wing's outline, then one line for each vein between two cells */
    private static void veins(Wing wing, Canvas canvas, Palette palette) {
        final stroke = Math.max(1d, canvas.size * 0.002d)
        canvas.polyline(wing.outline(wing.from(), wing.to(), STEPS * wing.cells().size()), stroke, palette.background, 0.95d)
        for( Cell cell : wing.cells().drop(1) ) {
            final edge = wing.edge(cell.from())
            canvas.polyline([wing.cx(), wing.cy(), wing.x(edge, cell.from()), wing.y(edge, cell.from())] as double[], stroke, palette.background, 0.95d)
        }
    }

    /** a spot at the margin of each cell, larger for processes with more tasks */
    private static void spots(Run run, Wing wing, Canvas canvas, Palette palette) {
        for( Cell cell : wing.cells() ) {
            final r = canvas.size * Math.max(0.002d, 0.012d * Math.sqrt(cell.tasks().size() / (double) run.tiles.size()))
            final distance = wing.edge(cell.middle()) * 0.93d
            canvas.circle(wing.x(distance, cell.middle()), wing.y(distance, cell.middle()), r, palette.ink(), 0.7d)
        }
    }

    /** failed tasks bite the wing's edge, on one side only, the way real wings wear */
    private static void tears(Wing wing, Canvas canvas, Palette palette, Random random) {
        for( Cell cell : wing.cells() ) {
            for( TaskTile tile : cell.tasks().findAll { TaskTile t -> t.status() == Status.FAILED } ) {
                final angle = cell.at(random.nextDouble())
                final edge = wing.edge(angle)
                final px = wing.x(edge, angle)
                final side = random.nextBoolean() ? px : 2 * wing.cx() - px
                canvas.circle(side, wing.y(edge, angle), canvas.size * (0.006d + 0.01d * random.nextDouble()), palette.background, 1d)
            }
        }
    }

    /** the abdomen runs through the processes in start order, tip last */
    private static void body(Run run, Canvas canvas, double cx, double cy, Palette palette) {
        final size = canvas.size
        final n = run.processes.size()
        final head = cy - size * 0.05d

        for( int side : [-1, 1] )
            antenna(canvas, cx, head, side, palette.ink())

        // the underlay shows between the bands: three slices of colour, then one of underlay
        canvas.polygon(abdomen(0, ABDOMEN, cx, cy, size), palette.ink(), 0.45d)
        for( int i = 0; i < ABDOMEN; i += 4 )
            canvas.polygon(abdomen(i, i + 3, cx, cy, size), palette.color((i * n).intdiv(ABDOMEN) as int, n), 1d)

        canvas.polygon(ellipse(cx, cy - size * 0.018d, size * 0.012d, size * 0.03d, 0d), palette.color(0, n), 1d)
        canvas.polygon(ellipse(cx, head, size * 0.0075d, size * 0.007d, 0d), palette.color(0, n), 1d)
    }

    private static void antenna(Canvas canvas, double cx, double head, int side, String ink) {
        final size = canvas.size
        final line = new double[(ANTENNA + 1) * 2]
        for( int i = 0; i <= ANTENNA; i++ ) {
            final f = i / (double) ANTENNA
            line[2 * i] = cx + side * size * (0.012d * f + 0.075d * f * f * f)
            line[2 * i + 1] = head - size * 0.19d * (f - 0.08d * f * f * f)
        }
        canvas.polyline(line, Math.max(0.6d, size * 0.001d), ink, 0.6d)
        canvas.polyline(Arrays.copyOfRange(line, (ANTENNA - CLUB) * 2, line.length), Math.max(1d, size * 0.003d), ink, 0.6d)
    }

    /** the tapered abdomen between two slices, down the right side and back up the left */
    private static double[] abdomen(int from, int to, double cx, double cy, double size) {
        final waist = cy - size * 0.01d, tip = cy + size * 0.13d
        final points = new double[(to - from + 1) * 4]
        for( int i = 0; i <= to - from; i++ ) {
            final f = (from + i) / (double) ABDOMEN
            final height = waist + (tip - waist) * f
            final half = size * 0.0085d * Math.pow(Math.sin(Math.PI * (0.2d + 0.8d * f)), 0.7d)
            points[2 * i] = cx + half
            points[2 * i + 1] = height
            points[points.length - 2 * i - 2] = cx - half
            points[points.length - 2 * i - 1] = height
        }
        return points
    }
}
