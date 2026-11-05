package nfarts

import groovy.transform.CompileStatic

/**
 * One butterfly wing, in polar coordinates around the body: angles run
 * clockwise from up, and each process is a cell between two angles.
 */
@CompileStatic
record Wing(Shape shape, List<Cell> cells, double size, double tail, double cx, double cy) {

    // how far along the outline, in degrees, a cell's reach is felt
    static final double BLEND = 6d

    static record Shape(double from, double to, double skew) {}

    static record Cell(List<TaskTile> tasks, String color, double from, double to, double reach) {
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

    double from() {
        shape.from()
    }

    double to() {
        shape.to()
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
        final lobe = Math.pow(Math.sin(Math.PI * Math.pow(u, shape.skew())), 0.5d)
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
        new Wing(shape, cells, size * factor, tail, x, y)
    }
}
