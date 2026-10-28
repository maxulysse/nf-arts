package nfarts

import groovy.transform.CompileStatic

/**
 * Turns the tasks of a run into shapes on a canvas.
 */
@CompileStatic
interface Style {
    void draw(Run run, Canvas canvas, Palette palette, Random random)

    /** the palette when `arts.palette` is not set */
    default String getDefaultPalette() {
        'viridis'
    }
}
