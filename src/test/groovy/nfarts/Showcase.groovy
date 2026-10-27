package nfarts

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Renders the README gallery: `./gradlew showcase`
 */
class Showcase {

    static void main(String[] args) {
        final dir = Paths.get(args ? args[0] : 'docs/images')
        Files.createDirectories(dir)
        final pieces = [
            [style: 'radial',  palette: 'viridis', seed: 1, samples: 24],
            [style: 'radial',  palette: 'magma',   seed: 2, samples: 48],
            [style: 'radial',  palette: 'nfcore',  seed: 3, samples: 12],
            [style: 'radial',  palette: 'mono',    seed: 4, samples: 24, resumed: true],
            [style: 'hilbert', palette: 'viridis', seed: 1, samples: 24],
            [style: 'hilbert', palette: 'ocean',   seed: 5, samples: 48],
            [style: 'hilbert', palette: 'sunset',  seed: 6, samples: 12],
            [style: 'hilbert', palette: 'mono',    seed: 4, samples: 24, resumed: true],
        ]
        for( Map p : pieces ) {
            final tiles = SyntheticRun.create(p.seed as long, p.samples as int, p.resumed as boolean)
            final config = new ArtsConfig(style: p.style, palette: p.palette, width: 800, height: 800)
            final name = "${p.style}-${p.palette}${p.resumed ? '-resumed' : ''}.png"
            Files.write(dir.resolve(name), Artist.paint(tiles, config, 'png', p.seed as long))
            println "wrote ${dir.resolve(name)} (${tiles.size()} tasks)"
        }
    }
}
