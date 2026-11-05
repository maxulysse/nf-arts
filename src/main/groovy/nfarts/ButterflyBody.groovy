package nfarts

import groovy.transform.CompileStatic

/**
 * The butterfly's antennae, abdomen, thorax and head. The abdomen runs
 * through the process colors in start order, tip last.
 */
@CompileStatic
class ButterflyBody {

    static final int ELLIPSE = 24, ANTENNA = 16, CLUB = 3, ABDOMEN = 32

    static void draw(Canvas canvas, double cx, double cy, int processes, Palette palette) {
        final size = canvas.size
        final head = cy - size * 0.05d

        for( int side : [-1, 1] )
            antenna(canvas, cx, head, side, palette.ink())

        // the underlay shows between the bands: three slices of colour, then one of underlay
        canvas.polygon(abdomen(0, ABDOMEN, cx, cy, size), palette.ink(), 0.45d)
        for( int i = 0; i < ABDOMEN; i += 4 )
            canvas.polygon(abdomen(i, i + 3, cx, cy, size), palette.color((i * processes).intdiv(ABDOMEN) as int, processes), 1d)

        canvas.polygon(ellipse(cx, cy - size * 0.018d, size * 0.012d, size * 0.03d, 0d), palette.color(0, processes), 1d)
        canvas.polygon(ellipse(cx, head, size * 0.0075d, size * 0.007d, 0d), palette.color(0, processes), 1d)
    }

    /** rx across, ry along the direction `tilt` points, clockwise from up */
    static double[] ellipse(double cx, double cy, double rx, double ry, double tilt) {
        final points = new double[ELLIPSE * 2]
        for( int i = 0; i < ELLIPSE; i++ ) {
            final a = 2 * Math.PI * i / ELLIPSE
            final across = rx * Math.cos(a), along = ry * Math.sin(a)
            points[2 * i] = cx + across * Math.cos(tilt) + along * Math.sin(tilt)
            points[2 * i + 1] = cy + across * Math.sin(tilt) - along * Math.cos(tilt)
        }
        return points
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
