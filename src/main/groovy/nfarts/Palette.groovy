package nfarts

import java.awt.Color

import groovy.transform.CompileStatic

/**
 * A background color plus a gradient of colors.
 */
@CompileStatic
class Palette {

    // first entry is the background
    static final Map<String, List<String>> NAMED = [
        viridis: ['#0b0b12', '#5b2a86', '#3b528b', '#21918c', '#5ec962', '#fde725'],
        magma  : ['#000004', '#5c1a8f', '#8c2981', '#de4968', '#fe9f6d', '#fcfdbf'],
        nfcore : ['#1b1d1e', '#24b064', '#9fd36a', '#ecdc86', '#f9a440', '#e35a2c'],
        ocean  : ['#03111f', '#155e8b', '#2a8fc4', '#4fb9e0', '#93dcec', '#e0f7fa'],
        sunset : ['#1a0b1e', '#5c2a9d', '#b5179e', '#f72585', '#ff8c42', '#ffd166'],
        mono   : ['#f6f4ef', '#111111', '#3a3a3a', '#6b6b6b', '#9c9c9c'],
    ]

    final String background
    final List<Color> colors

    Palette(String background, List<Color> colors) {
        this.background = background
        this.colors = colors
    }

    static Palette of(String name, List<String> custom = null, String background = null) {
        List<String> hex
        if( custom ) {
            hex = [background ?: '#ffffff'] + custom
        }
        else {
            if( !NAMED.containsKey(name) )
                throw new IllegalArgumentException("Unknown arts.palette '${name}'. Use one of: ${NAMED.keySet().join(', ')}")
            hex = NAMED[name]
        }
        final bg = background ?: hex[0]
        decode(bg)
        return new Palette(bg, hex.drop(1).collect { decode(it) })
    }

    private static Color decode(String hex) {
        try {
            return Color.decode(hex)
        }
        catch( NumberFormatException e ) {
            throw new IllegalArgumentException("Invalid arts color '${hex}'. Use #rrggbb")
        }
    }

    /**
     * Color at position t in [0,1] along the gradient, as #rrggbb.
     */
    String at(double t) {
        if( colors.size() == 1 )
            return hex(colors[0])
        final x = Math.min(1d, Math.max(0d, t)) * (colors.size() - 1)
        final i = Math.min((int) x, colors.size() - 2)
        final f = x - i
        final a = colors[i], b = colors[i + 1]
        return hex(new Color(
            (int) Math.round(a.red + (b.red - a.red) * f),
            (int) Math.round(a.green + (b.green - a.green) * f),
            (int) Math.round(a.blue + (b.blue - a.blue) * f)))
    }

    private static String hex(Color c) {
        String.format('#%02x%02x%02x', c.red, c.green, c.blue)
    }
}
