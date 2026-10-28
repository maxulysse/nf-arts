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
        style << ArtsConfig.STYLES
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
        style << ArtsConfig.STYLES
    }

    def 'cached tasks should move to the start of the run' () {
        given:
        def run = new Run([
            new TaskTile('A', 1000, 10, 1, Status.COMPLETED),
            new TaskTile('A', 1, 10, 1, Status.CACHED),
            new TaskTile('B', 1500, 10, 1, Status.FAILED),
        ])

        expect:
        run.t0 == 1000
        run.tiles*.start() == [1000, 1000, 1500]
        // a run that is all cached keeps its times
        new Run([new TaskTile('A', 5, 1, 1, Status.CACHED)]).t0 == 5
    }

    def 'memory should be on a log scale between the smallest and largest task' () {
        given:
        def run = new Run([
            new TaskTile('A', 0, 1, 1_000_000, Status.COMPLETED),
            new TaskTile('A', 0, 1, 1_000_000_000, Status.COMPLETED),
            new TaskTile('A', 0, 1, 1_000_000_000_000, Status.COMPLETED),
            new TaskTile('A', 0, 1, 0, Status.COMPLETED),
        ])
        def single = new TaskTile('A', 0, 1, 5, Status.COMPLETED)

        expect:
        run.memory(run.tiles[0]) == 0d
        Math.abs(run.memory(run.tiles[1]) - 0.5d) < 1e-9
        run.memory(run.tiles[2]) == 1d
        run.memory(run.tiles[3]) == 0d
        new Run([single]).memory(single) == 1d
    }

    def 'should handle a single task and an empty run' () {
        expect:
        new String(Artist.paint([new TaskTile('A', 0, 0, 0, Status.COMPLETED)], new ArtsConfig(style: style), 'svg', 1)) =~ /<(path|polyline|polygon|circle) /
        new String(Artist.paint([], new ArtsConfig(style: style), 'svg', 1)).contains('<rect')
        where:
        style << ArtsConfig.STYLES
    }

    def 'new styles should draw their shapes' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def failed = tiles.count { it.status() == Status.FAILED }
        def processes = tiles*.process().unique().size()
        def svg = { String style -> new String(Artist.paint(tiles, new ArtsConfig(style: style), 'svg', 1)) }

        expect:
        svg('phyllotaxis').count('<circle ') == tiles.size()
        svg('flowfield').count('<polyline ') == tiles.size()
        svg('ridgeline').count('<polyline ') == processes
        svg('ridgeline').count('<polygon ') == processes
        svg('mondrian').count('<polygon ') + svg('mondrian').count('<polyline ') == tiles.size()
        svg('mondrian').count('<polyline ') == failed
        svg('tree').count('<circle ') == tiles.size() - failed
        // a vein and its two forks per task, plus the outline and the stem
        svg('ginkgo').count('<polyline ') == tiles.size() * 3 + 2
        svg('ginkgo').count('<polygon ') == 1
    }

    def 'mondrian partition should split area by weight' () {
        when:
        def blocks = MondrianStyle.partition([1d, 1d, 2d], 0, 0, 400, 100, new Random(1))
        then:
        blocks.size() == 3
        blocks.collect { it[2] * it[3] } == [10_000d, 10_000d, 20_000d]
        blocks.every { it[0] >= 0 && it[1] >= 0 && it[0] + it[2] <= 400 + 1e-9 && it[1] + it[3] <= 100 + 1e-9 }
    }

    def 'status should come from the trace and set how a task is drawn' () {
        expect:
        Status.of('COMPLETED', false) == Status.COMPLETED
        Status.of('COMPLETED', true) == Status.CACHED
        Status.of('CACHED', false) == Status.CACHED
        Status.of('ABORTED', false) == Status.FAILED
        Status.of(null, false) == Status.FAILED
        Status.FAILED.thin(9) == 3
        Status.COMPLETED.thin(9) == 9
        Status.CACHED.opacity < Status.COMPLETED.opacity
    }

    def 'inset canvas should scale by its own shorter side' () {
        given:
        def canvas = new SvgCanvas(400, 300, '#000000')

        expect:
        canvas.size == 300
        new InsetCanvas(canvas, 200).size == 200
        new InsetCanvas(new SvgCanvas(100, 300, '#000000'), 250).size == 100
    }
}
