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

    static final int STEPS = 24, EYESPOTS = 3
    static final double WASH = 0.3d

    private static final Wing.Shape FOREWING = new Wing.Shape(12d, 95d, 0.75d)
    private static final Wing.Shape HINDWING = new Wing.Shape(95d, 170d, 1.2d)

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final wings = wings(run, canvas, palette)
        final fore = wings[0], hind = wings[1]
        final mirrored = new MirrorCanvas(canvas, fore.cx())

        for( Wing wing : wings ) {
            for( Wing.Cell cell : wing.cells() ) {
                mirrored.polygon(wing.outline(cell.from(), cell.to(), STEPS), cell.color(), WASH)
                strokes(run, wing, cell, mirrored, random)
            }
        }
        eyespots(run, hind, mirrored, palette)
        for( Wing wing : wings ) {
            veins(wing, mirrored, palette)
            spots(run, wing, mirrored, palette)
        }
        // on the plain canvas: a wing wears on one side only
        for( Wing wing : wings )
            tears(wing, canvas, palette, random)
        ButterflyBody.draw(canvas, fore.cx(), fore.cy(), run.processes.size(), palette)
    }

    /** the fore and hind wing, sized by the run and placed to fill the canvas */
    private static List<Wing> wings(Run run, Canvas canvas, Palette palette) {
        final half = run.t0 + run.span / 2
        final late = run.processes.findAll { String p -> run.tasksOf(p)[0].start() > half } ?: run.processes.takeRight(1)
        final early = (run.processes - late) ?: late

        // the wing holding more of the run's compute time grows at the other's expense
        final foreWork = work(run, early), hindWork = work(run, late)
        final share = foreWork / Math.max(1d, foreWork + hindWork)
        final fore = wing(run, palette, early, FOREWING, 0.46d + 0.2d * share, 0d)
        final hind = wing(run, palette, late, HINDWING, 0.3d + 0.2d * (1 - share), tail(run))
        return fit(fore, hind, canvas)
    }

    /** a typical task far shorter than the longest one gives the hindwings tails */
    private static double tail(Run run) {
        final typical = run.duration(run.tiles.toSorted { TaskTile t -> t.duration() }[run.tiles.size().intdiv(2) as int])
        return 0.9d * Math.max(0d, 0.6d - typical)
    }

    /** scale the wings to the canvas, and center what they reach */
    private static List<Wing> fit(Wing fore, Wing hind, Canvas canvas) {
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

    private static Wing wing(Run run, Palette palette, List<String> processes, Wing.Shape shape, double size, double tail) {
        final tasks = processes.collect { String p -> run.tasksOf(p) }
        final weights = tasks.collect { List<TaskTile> list -> 1 + Math.log(list.size() as double) }
        final total = weights.sum() as double
        final cells = new ArrayList<Wing.Cell>()
        double angle = shape.from()
        tasks.eachWithIndex { List<TaskTile> list, int i ->
            final next = angle + (shape.to() - shape.from()) * weights[i] / total
            final mean = (list.sum(0d) { TaskTile t -> run.duration(t) } as double) / list.size()
            cells << new Wing.Cell(list, run.color(list[0], palette), angle, next, 0.75d + 0.25d * mean)
            angle = next
        }
        return new Wing(shape, cells, size, tail, 0d, 0d)
    }

    private static void strokes(Run run, Wing wing, Wing.Cell cell, Canvas canvas, Random random) {
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
        final cells = wing.cells().findAll { Wing.Cell c -> c.peak().memory() > 0 }.toSorted { Wing.Cell c -> -c.peak().memory() }
        for( Wing.Cell cell : cells.take(EYESPOTS) ) {
            final angle = cell.middle()
            final tilt = Math.toRadians(angle)
            final distance = wing.edge(angle) * 0.78d
            final room = distance * Math.sin(Math.toRadians(cell.to() - cell.from()) / 2)
            final r = Math.min(room * 0.9d, canvas.size * (0.01d + 0.016d * run.memory(cell.peak())))
            final px = wing.x(distance, angle), py = wing.y(distance, angle)
            canvas.polygon(ButterflyBody.ellipse(px, py, r, r * 1.3d, tilt), palette.ink(), 0.45d)
            canvas.polygon(ButterflyBody.ellipse(px, py, r * 0.84d, r * 1.09d, tilt), palette.background, 0.95d)
            canvas.polygon(ButterflyBody.ellipse(px, py, r * 0.5d, r * 0.65d, tilt), cell.color(), 0.9d)
        }
    }

    /** the wing's outline, then one line for each vein between two cells */
    private static void veins(Wing wing, Canvas canvas, Palette palette) {
        final stroke = Math.max(1d, canvas.size * 0.002d)
        canvas.polyline(wing.outline(wing.from(), wing.to(), STEPS * wing.cells().size()), stroke, palette.background, 0.95d)
        for( Wing.Cell cell : wing.cells().drop(1) ) {
            final edge = wing.edge(cell.from())
            canvas.polyline([wing.cx(), wing.cy(), wing.x(edge, cell.from()), wing.y(edge, cell.from())] as double[], stroke, palette.background, 0.95d)
        }
    }

    /** a spot at the margin of each cell, larger for processes with more tasks */
    private static void spots(Run run, Wing wing, Canvas canvas, Palette palette) {
        for( Wing.Cell cell : wing.cells() ) {
            final r = canvas.size * Math.max(0.002d, 0.012d * Math.sqrt(cell.tasks().size() / (double) run.tiles.size()))
            final distance = wing.edge(cell.middle()) * 0.93d
            canvas.circle(wing.x(distance, cell.middle()), wing.y(distance, cell.middle()), r, palette.ink(), 0.7d)
        }
    }

    /** failed tasks bite the wing's edge, on one side only, the way real wings wear */
    private static void tears(Wing wing, Canvas canvas, Palette palette, Random random) {
        for( Wing.Cell cell : wing.cells() ) {
            for( TaskTile tile : cell.tasks().findAll { TaskTile t -> t.status() == Status.FAILED } ) {
                final angle = cell.at(random.nextDouble())
                final edge = wing.edge(angle)
                final px = wing.x(edge, angle)
                final side = random.nextBoolean() ? px : 2 * wing.cx() - px
                canvas.circle(side, wing.y(edge, angle), canvas.size * (0.006d + 0.01d * random.nextDouble()), palette.background, 1d)
            }
        }
    }
}
