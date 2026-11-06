package nfarts

import groovy.transform.CompileStatic

/**
 * A hive plot. Spokes hold one segment per process, tasks are dots on them,
 * distance along a segment is memory. Each task has a curve from the last
 * task that finished before it started on another spoke: a handoff in time,
 * not a dependency, as runs carry no task graph.
 */
@CompileStatic
class HiveStyle implements Style {

    /** the first one is the default */
    static final List<String> LAYOUTS = ['spiral', 'phase']
    static final int DEFAULT_SPOKES = 5

    private static final int CURVE_STEPS = 24

    static record Slot(int spoke, int level) {}

    private static record Node(double angle, double r, int spoke, long end, TaskTile tile) {}

    private static record Handoff(Node from, Node to) {}

    private static record Hub(double cx, double cy, double inner, double band) {
        /** `along` runs from 0 to 1 over the segment of a process */
        double radius(Slot slot, double along) {
            inner + band * (slot.level() + 0.1d + 0.8d * along)
        }

        double[] at(double r, double angle) {
            [cx + r * Math.cos(angle), cy + r * Math.sin(angle)] as double[]
        }
    }

    private final String layout
    private final int spokes

    HiveStyle(String layout = LAYOUTS[0], int spokes = DEFAULT_SPOKES) {
        this.layout = layout
        this.spokes = spokes
    }

    @Override
    Style configured(ArtsConfig config) {
        new HiveStyle(config.layout, config.spokes)
    }

    @Override
    void draw(Run run, Canvas canvas, Palette palette, Random random) {
        final size = canvas.size
        final count = run.processes.size()
        // more spokes than processes would leave some empty
        final axes = Math.min(spokes, count)
        final inner = size * 0.06d, outer = size * 0.46d
        final hub = new Hub(canvas.width / 2d, canvas.height / 2d, inner, (outer - inner) / Math.floorDiv(count + axes - 1, axes))
        final turn = random.nextDouble() * 2 * Math.PI
        final angles = (0..<axes).collect { int i -> turn + i * 2 * Math.PI / axes }
        final slots = (0..<count).collect { int i -> slot(layout, i, count, axes) }

        for( Slot slot : slots ) {
            final angle = angles[slot.spoke()]
            final near = hub.at(hub.radius(slot, 0d), angle), far = hub.at(hub.radius(slot, 1d), angle)
            canvas.polyline([near[0], near[1], far[0], far[1]] as double[], 1d, palette.ink(), 0.25d)
        }

        final nodes = run.tiles.collect { TaskTile tile ->
            final slot = slots[run.rank(tile)]
            new Node(angles[slot.spoke()], hub.radius(slot, run.memory(tile)), slot.spoke(), tile.start() + tile.duration(), tile)
        }
        for( Handoff handoff : handoffs(nodes, axes) ) {
            final tile = handoff.to().tile()
            final stroke = tile.status().thin(size * (0.0008d + 0.004d * run.memory(tile)))
            canvas.polyline(curve(handoff, hub), Math.max(0.3d, stroke), run.color(tile, palette), tile.status().opacity * 0.7d)
        }
        // dots on top of every curve
        for( Node node : nodes ) {
            final dot = hub.at(node.r(), node.angle())
            canvas.circle(dot[0], dot[1], size * (0.002d + 0.006d * run.memory(node.tile())), run.color(node.tile(), palette), node.tile().status().opacity)
        }
    }

    /** where the i-th of `count` processes sits on `axes` spokes, with `axes` <= `count` */
    static Slot slot(String layout, int i, int count, int axes) {
        if( layout == 'spiral' )
            return new Slot(i % axes, Math.floorDiv(i, axes))
        final spoke = Math.floorDiv(i * axes, count)
        return new Slot(spoke, i - Math.floorDiv(spoke * count + axes - 1, axes))
    }

    /** `nodes` in start order, so the finished tasks only grow */
    private static List<Handoff> handoffs(List<Node> nodes, int axes) {
        final byEnd = nodes.toSorted { Node a, Node b -> a.end() <=> b.end() }
        final latest = new Node[axes]
        final handoffs = [] as List<Handoff>
        int finished = 0
        for( Node node : nodes ) {
            while( finished < byEnd.size() && byEnd[finished].end() <= node.tile().start() ) {
                final done = byEnd[finished++]
                latest[done.spoke()] = done
            }
            Node from = null
            for( Node other : latest ) {
                if( other != null && other.spoke() != node.spoke() && (from == null || other.end() > from.end()) )
                    from = other
            }
            if( from != null )
                handoffs << new Handoff(from, node)
        }
        return handoffs
    }

    /** a quadratic Bezier bending outward, the short way round */
    private static double[] curve(Handoff handoff, Hub hub) {
        final from = handoff.from(), to = handoff.to()
        final mid = from.angle() + Math.IEEEremainder(to.angle() - from.angle(), 2 * Math.PI) / 2
        final start = hub.at(from.r(), from.angle()), end = hub.at(to.r(), to.angle())
        final control = hub.at((from.r() + to.r()) / 2 * 1.1d, mid)
        final points = new double[(CURVE_STEPS + 1) * 2]
        for( int step = 0; step <= CURVE_STEPS; step++ ) {
            final u = step / (double) CURVE_STEPS
            final a = (1 - u) * (1 - u), b = 2 * u * (1 - u), c = u * u
            points[step * 2] = a * start[0] + b * control[0] + c * end[0]
            points[step * 2 + 1] = a * start[1] + b * control[1] + c * end[1]
        }
        return points
    }
}
