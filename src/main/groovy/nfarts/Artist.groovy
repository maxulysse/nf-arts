package nfarts

import groovy.transform.CompileStatic

/**
 * Paints a run with the configured style, palette and size.
 */
@CompileStatic
class Artist {

    static final Map<String, Style> STYLES = [
        radial     : new RadialStyle(),
        hilbert    : new HilbertStyle(),
        ridgeline  : new RidgelineStyle(),
        phyllotaxis: new PhyllotaxisStyle(),
        flowfield  : new FlowFieldStyle(),
        mondrian   : new MondrianStyle(),
        tree       : new TreeStyle(),
    ] as Map<String, Style>

    static byte[] paint(List<TaskTile> tiles, ArtsConfig config, String format, long seed) {
        final palette = Palette.of(config.palette, config.colors, config.background)
        final Canvas canvas = format == 'png'
            ? new PngCanvas(config.width, config.height, palette.background)
            : new SvgCanvas(config.width, config.height, palette.background)
        final style = STYLES[config.style]
        // mix the seed: java.util.Random gives near-identical first draws for nearby seeds
        if( tiles )
            style.draw(tiles, canvas, palette, new Random(seed * 0x9E3779B97F4A7C15L))
        return canvas.bytes()
    }
}
