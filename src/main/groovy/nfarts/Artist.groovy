/*
 * Copyright 2026, Maxime U. Garcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nfarts

import groovy.transform.CompileStatic

/**
 * Paints a run with the configured style, palette and size.
 */
@CompileStatic
class Artist {

    static byte[] paint(List<TaskTile> tiles, ArtsConfig config, String format, long seed) {
        final palette = Palette.of(config.palette, config.colors, config.background)
        final Canvas canvas = format == 'png'
            ? new PngCanvas(config.width, config.height, palette.background)
            : new SvgCanvas(config.width, config.height, palette.background)
        final Style style = config.style == 'hilbert' ? new HilbertStyle() : new RadialStyle()
        // mix the seed: java.util.Random gives near-identical first draws for nearby seeds
        if( tiles )
            style.draw(tiles, canvas, palette, new Random(seed * 0x9E3779B97F4A7C15L))
        return canvas.bytes()
    }
}
