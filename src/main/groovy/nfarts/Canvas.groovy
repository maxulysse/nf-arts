package nfarts

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import groovy.transform.CompileStatic

/**
 * Drawing surface shared by the styles. Angles are in degrees,
 * clockwise from 3 o'clock (y axis points down).
 */
@CompileStatic
interface Canvas {
    int getWidth()
    int getHeight()
    void arc(double cx, double cy, double r, double startDeg, double sweepDeg, double stroke, String color, double opacity)
    /** points are interleaved x,y pairs */
    void polyline(double[] points, double stroke, String color, double opacity)
    /** filled, no outline; points are interleaved x,y pairs */
    void polygon(double[] points, String color, double opacity)
    void circle(double cx, double cy, double r, String color, double opacity)
    byte[] bytes()
}

@CompileStatic
class SvgCanvas implements Canvas {

    final int width
    final int height
    private final String background
    private final StringBuilder body = new StringBuilder()

    SvgCanvas(int width, int height, String background) {
        this.width = width
        this.height = height
        this.background = background
    }

    @Override
    void arc(double cx, double cy, double r, double startDeg, double sweepDeg, double stroke, String color, double opacity) {
        // a full circle collapses to an empty SVG arc
        final sweep = Math.min(sweepDeg, 359.99d)
        final a0 = Math.toRadians(startDeg), a1 = Math.toRadians(startDeg + sweep)
        body.append(String.format(Locale.ROOT,
            '<path d="M%.2f %.2f A%.2f %.2f 0 %d 1 %.2f %.2f" stroke="%s" stroke-width="%.2f" stroke-opacity="%.2f"/>\n',
            cx + r * Math.cos(a0), cy + r * Math.sin(a0), r, r, sweep > 180 ? 1 : 0,
            cx + r * Math.cos(a1), cy + r * Math.sin(a1), color, stroke, opacity))
    }

    private static String join(double[] points) {
        final pts = new StringBuilder()
        for( int i = 0; i < points.length; i += 2 )
            pts.append(String.format(Locale.ROOT, '%s%.2f,%.2f', i ? ' ' : '', points[i], points[i + 1]))
        return pts.toString()
    }

    @Override
    void polyline(double[] points, double stroke, String color, double opacity) {
        body.append(String.format(Locale.ROOT,
            '<polyline points="%s" stroke="%s" stroke-width="%.2f" stroke-opacity="%.2f"/>\n',
            join(points), color, stroke, opacity))
    }

    @Override
    void polygon(double[] points, String color, double opacity) {
        body.append(String.format(Locale.ROOT,
            '<polygon points="%s" fill="%s" fill-opacity="%.2f"/>\n', join(points), color, opacity))
    }

    @Override
    void circle(double cx, double cy, double r, String color, double opacity) {
        body.append(String.format(Locale.ROOT,
            '<circle cx="%.2f" cy="%.2f" r="%.2f" fill="%s" fill-opacity="%.2f"/>\n', cx, cy, r, color, opacity))
    }

    @Override
    byte[] bytes() {
        final svg = """<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
<rect width="100%" height="100%" fill="${background}"/>
<g fill="none" stroke-linecap="round" stroke-linejoin="round">
${body}</g>
</svg>
"""
        return svg.getBytes('UTF-8')
    }
}

@CompileStatic
class PngCanvas implements Canvas {

    final int width
    final int height
    private final BufferedImage image
    private final Graphics2D g

    PngCanvas(int width, int height, String background) {
        this.width = width
        this.height = height
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = Color.decode(background)
        g.fillRect(0, 0, width, height)
    }

    private void pen(double stroke, String color, double opacity) {
        g.stroke = new BasicStroke((float) stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color.decode(color)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity)
    }

    @Override
    void arc(double cx, double cy, double r, double startDeg, double sweepDeg, double stroke, String color, double opacity) {
        pen(stroke, color, opacity)
        // Java2D angles run counterclockwise
        g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, -startDeg, -sweepDeg, Arc2D.OPEN))
    }

    private static Path2D path(double[] points) {
        final path = new Path2D.Double()
        path.moveTo(points[0], points[1])
        for( int i = 2; i < points.length; i += 2 )
            path.lineTo(points[i], points[i + 1])
        return path
    }

    @Override
    void polyline(double[] points, double stroke, String color, double opacity) {
        pen(stroke, color, opacity)
        g.draw(path(points))
    }

    @Override
    void polygon(double[] points, String color, double opacity) {
        pen(1, color, opacity)
        final shape = path(points)
        shape.closePath()
        g.fill(shape)
    }

    @Override
    void circle(double cx, double cy, double r, String color, double opacity) {
        pen(1, color, opacity)
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r))
    }

    @Override
    byte[] bytes() {
        final out = new ByteArrayOutputStream()
        ImageIO.write(image, 'png', out)
        return out.toByteArray()
    }
}
