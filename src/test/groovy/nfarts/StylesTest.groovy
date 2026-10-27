package nfarts

import javax.imageio.ImageIO

import spock.lang.Specification

class StylesTest extends Specification {

    def 'hilbert curve should visit every cell once with unit steps' () {
        given:
        def n = 16
        def cells = (0..<n * n).collect { HilbertStyle.d2xy(n, it) as List }

        expect:
        HilbertStyle.d2xy(2, 0) == [0, 0] as int[]
        HilbertStyle.d2xy(2, 1) == [0, 1] as int[]
        HilbertStyle.d2xy(2, 2) == [1, 1] as int[]
        HilbertStyle.d2xy(2, 3) == [1, 0] as int[]
        cells.unique(false).size() == n * n
        (1..<cells.size()).every { i -> Math.abs(cells[i][0] - cells[i - 1][0]) + Math.abs(cells[i][1] - cells[i - 1][1]) == 1 }
    }

    def 'hilbert order should grow with the number of tasks' () {
        expect:
        HilbertStyle.order(1) == 3
        HilbertStyle.order(100) == 6
        HilbertStyle.order(100_000) == HilbertStyle.MAX_ORDER
    }

    def 'radial should draw one arc per task' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def svg = new String(Artist.paint(tiles, new ArtsConfig(width: 400, height: 300), 'svg', 1))

        expect:
        svg.startsWith('<svg')
        svg.contains('width="400" height="300"')
        svg.count('<path ') == tiles.size()
    }

    def 'hilbert should draw one line per task' () {
        given:
        def tiles = SyntheticRun.create(1, 4, true)
        def svg = new String(Artist.paint(tiles, new ArtsConfig(style: 'hilbert'), 'svg', 1))

        expect:
        svg.count('<polyline ') == tiles.size()
        svg.contains('stroke-opacity="0.30"')   // cached tasks
    }

    def 'should render a png of the requested size' () {
        when:
        def bytes = Artist.paint(SyntheticRun.create(1, 4), new ArtsConfig(style: style, width: 320, height: 240), 'png', 1)
        def image = ImageIO.read(new ByteArrayInputStream(bytes))
        then:
        image.width == 320
        image.height == 240
        where:
        style << ['radial', 'hilbert']
    }

    def 'same seed should give the same art, other seeds other art' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def config = new ArtsConfig(style: style)

        expect:
        Artist.paint(tiles, config, 'svg', 7) == Artist.paint(tiles, config, 'svg', 7)
        // hilbert only has the 8 symmetries of the square
        (1..8).collect { new String(Artist.paint(tiles, config, 'svg', it)) }.unique().size() > 1
        where:
        style << ['radial', 'hilbert']
    }

    def 'should handle a single task and an empty run' () {
        expect:
        new String(Artist.paint([new TaskTile('A', 0, 0, 0, 'COMPLETED')], new ArtsConfig(style: style), 'svg', 1)).contains('stroke=')
        new String(Artist.paint([], new ArtsConfig(style: style), 'svg', 1)).contains('<rect')
        where:
        style << ['radial', 'hilbert']
    }
}
