package nfarts

import javax.imageio.ImageIO

import spock.lang.Specification

class StylesTest extends Specification {

    // MD5 of each style's SVG. A change here is a visible change: compare the images, then update it
    static final Map<String, String> SVG_HASHES = [
        butterfly:   'aff652f4f713f1a9e4557c817067f792',
        flowfield:   'dad1317b5ca85dcfaa7fd90c9c6e701a',
        ginkgo:      'b4ca8de6322a7ad69b09af865a9fbc21',
        hilbert:     'c59360e2e3e88f8c96c6437e303a9aba',
        hive:        'c4f4068c2d2286f8faaf46ed19b74a43',
        iris:        'f616f33257bc4d6049ae5b5b5413dbc6',
        mondrian:    '7ff75d32b21744d94de05af637349a43',
        phyllotaxis: 'c6daf016a8878b211af49f242e1ba330',
        radial:      '9343dca8c3de9aad83235cc1e877804d',
        ridgeline:   'e3914331ece14c44ad3148f1d3c037b3',
        tree:        '51063372070b4ff54dad7b21309785d1',
    ]

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
        HilbertStyle.order(100_000) == 9
    }

    def 'radial should draw one arc per task' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def svg = SyntheticRun.svg(tiles, new ArtsConfig(width: 400, height: 300))

        expect:
        svg.startsWith('<svg')
        svg.contains('width="400" height="300"')
        svg.count('<path ') == tiles.size()
    }

    def 'hilbert should draw one line per task' () {
        given:
        def tiles = SyntheticRun.create(1, 4, true)
        def svg = SyntheticRun.svg(tiles, new ArtsConfig(style: 'hilbert'))

        expect:
        svg.count('<polyline ') == tiles.size()
        svg.contains('stroke-opacity="0.30"')   // cached tasks
    }

    def 'should render a png of the requested size' () {
        when:
        def bytes = Artist.paint(new PaintedRun(SyntheticRun.create(1, 4), new Title(null, null, null), null), new ArtsConfig(style: style, width: 320, height: 240), 'png')
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
        SyntheticRun.svg(tiles, config, 7) == SyntheticRun.svg(tiles, config, 7)
        // hilbert only has the 8 symmetries of the square
        (1..8).collect { SyntheticRun.svg(tiles, config, it) }.unique().size() > 1
        where:
        style << ArtsConfig.STYLES
    }

    def 'the same tasks should give the same svg' () {
        given:
        def tiles = SyntheticRun.create(1, 8, true)

        expect:
        SyntheticRun.svg(tiles, new ArtsConfig(style: style, title: false)).md5() == SVG_HASHES[style]
        where:
        style << ArtsConfig.STYLES
    }

    def 'the order tasks arrive in should not change the art' () {
        given:
        def tiles = SyntheticRun.create(1, 4, true)
        def shuffled = new ArrayList(tiles)
        Collections.shuffle(shuffled, new Random(1))
        def config = new ArtsConfig(style: style)

        expect:
        SyntheticRun.svg(shuffled, config) == SyntheticRun.svg(tiles, config)
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

    def 'tasks with no time should sit at the start of the run' () {
        given:
        def tile = { long start, Status status -> new TaskTile('A', start, 10, 1, status) }
        def run = new Run([tile(1000, Status.COMPLETED), tile(0, Status.FAILED), tile(1500, Status.COMPLETED)])
        def cached = new Run([tile(5, Status.CACHED), tile(0, Status.CACHED), tile(9, Status.CACHED)])

        expect:
        run.t0 == 1000
        run.tiles*.start() == [1000, 1000, 1500]
        // a run that is all cached keeps its times, and takes the first known one
        cached.tiles*.start() == [5, 5, 9]
        new Run([tile(0, Status.COMPLETED)]).t0 == 0
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
        run.tiles*.memory() == [0, 1_000_000, 1_000_000_000, 1_000_000_000_000]
        run.memory(run.tiles[0]) == 0d
        run.memory(run.tiles[1]) == 0d
        Math.abs(run.memory(run.tiles[2]) - 0.5d) < 1e-9
        run.memory(run.tiles[3]) == 1d
        new Run([single]).memory(single) == 1d
    }

    def 'should handle a single task' () {
        expect:
        SyntheticRun.svg([new TaskTile('A', 0, 0, 0, Status.COMPLETED)], new ArtsConfig(style: style)) =~ /<(path|polyline|polygon|circle) /
        where:
        style << ArtsConfig.STYLES
    }

    def 'new styles should draw their shapes' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def failed = tiles.count { it.status() == Status.FAILED }
        def processes = tiles*.process().unique().size()
        def svg = { String style -> SyntheticRun.svg(tiles, new ArtsConfig(style: style)) }

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
        // a fibre per task plus the collarette, then the pupil and its catchlight
        svg('iris').count('<polyline ') == tiles.size() + 1
        svg('iris').count('<circle ') == 2
        svg('hive').count('<circle ') == tiles.size()
    }

    def 'hive should give every process its own slot' () {
        given:
        def slots = (0..<count).collect { HiveStyle.slot(layout, it, count, axes) }

        expect:
        slots.unique(false).size() == count
        slots.every { it.spoke() in 0..<axes && it.level() in 0..<levels }
        slots[0..3].collect { [it.spoke(), it.level()] } == first
        where:
        layout   | count | axes | levels | first
        'spiral' | 29    | 5    | 6      | [[0, 0], [1, 0], [2, 0], [3, 0]]
        'spiral' | 4     | 4    | 1      | [[0, 0], [1, 0], [2, 0], [3, 0]]
        'phase'  | 29    | 5    | 6      | [[0, 0], [0, 1], [0, 2], [0, 3]]
        'phase'  | 4     | 4    | 1      | [[0, 0], [1, 0], [2, 0], [3, 0]]
    }

    def 'hive should link a task to the last one that finished on another spoke' () {
        given:
        def tiles = [
            new TaskTile('A', 1000, 100, 1000, Status.COMPLETED),
            new TaskTile('B', 2000, 100, 1000, Status.COMPLETED),
            new TaskTile('C', 3000, 100, 1000, Status.COMPLETED),
        ]
        def svg = SyntheticRun.svg(tiles, new ArtsConfig(style: 'hive', spokes: spokes, layout: layout))

        expect:
        // one segment per process, plus a curve for every task with a task before it on another spoke
        svg.count('<polyline ') == 3 + curves
        where:
        layout   | spokes | curves
        'spiral' | 3      | 2
        'phase'  | 3      | 2
        // A and B share the first spoke, so B has nothing to link to
        'phase'  | 2      | 1
    }

    def 'hive options should change the art, and spare spokes should not' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def svg = { Map opts -> SyntheticRun.svg(tiles, new ArtsConfig([style: 'hive'] + opts)) }
        def processes = tiles*.process().unique().size()

        expect:
        svg([:]) == svg(layout: 'spiral', spokes: 5)
        svg(layout: 'phase') != svg(layout: 'spiral')
        svg(spokes: 3) != svg(spokes: 5)
        svg(spokes: processes + 7) == svg(spokes: processes)
    }

    def 'butterfly should draw every task on both sides and stay on the canvas' () {
        given:
        def config = new ArtsConfig(style: 'butterfly', width: width, height: height, title: false)
        def svg = SyntheticRun.svg(tiles, config)
        def processes = tiles*.process().unique().size()
        // a lone process fills both wings
        def wings = processes == 1 ? 2 : 1
        def points = (svg =~ /(-?[\d.]+),(-?[\d.]+)/).collect { [it[1] as double, it[2] as double] }
        def circles = (svg =~ /cx="(-?[\d.]+)" cy="(-?[\d.]+)" r="([\d.]+)"/).collect { it[1..3].collect { v -> v as double } }
        def xs = points*.getAt(0) + circles.collectMany { [it[0] - it[2], it[0] + it[2]] }
        def ys = points*.getAt(1) + circles.collectMany { [it[1] - it[2], it[1] + it[2]] }

        expect:
        // a stroke per task and a line per cell, on both sides, plus the antennae
        svg.count('<polyline ') == 2 * wings * (tiles.size() + processes) + 4
        svg.count(String.format(Locale.ROOT, 'fill-opacity="%.2f"', ButterflyStyle.WASH)) == 2 * wings * processes
        xs.min() >= 0 && xs.max() <= width
        ys.min() >= 0 && ys.max() <= height

        where:
        tiles                                                       | width | height
        SyntheticRun.create(1, 4)                                   | 600   | 600
        SyntheticRun.create(1, 4)                                   | 900   | 300
        SyntheticRun.create(1, 4).findAll { it.process() == 'BQSR' } | 600   | 600
    }

    def 'butterfly should have no eyespots without memory' () {
        given:
        def tiles = SyntheticRun.create(1, 4).collect { new TaskTile(it.process(), it.start(), it.duration(), 0, it.status()) }
        def svg = SyntheticRun.svg(tiles, new ArtsConfig(style: 'butterfly'))

        expect:
        // a wash per cell on both sides, then the abdomen's underlay and 8 bands, the thorax and the head
        svg.count('<polygon ') == 2 * tiles*.process().unique().size() + 11
    }

    def 'butterfly should bite the wing for failed tasks' () {
        given:
        def tiles = SyntheticRun.create(1, 4)
        def failed = tiles.withIndex().collect { t, i -> i ? t : new TaskTile(t.process(), t.start(), t.duration(), t.memory(), Status.FAILED) }
        def circles = { list -> SyntheticRun.svg(list, new ArtsConfig(style: 'butterfly')).count('<circle ') }

        expect:
        circles(failed) == circles(tiles) + 1
    }

    def 'mondrian should draw every task of a dense run' () {
        given:
        def tiles = SyntheticRun.create(1, 2000)

        when:
        def svg = SyntheticRun.svg(tiles, new ArtsConfig(style: 'mondrian', title: false))
        then:
        svg.count('<polygon') + svg.count('<polyline') == tiles.size()
    }

    def 'mondrian partition should split area by weight' () {
        when:
        def blocks = MondrianStyle.partition([1d, 1d, 2d], [0, 0, 400, 100] as double[], new Random(1))
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

    def 'hilbert should keep two cells per task past the largest order' () {
        expect:
        HilbertStyle.order(8_192) == HilbertStyle.MAX_ORDER
        HilbertStyle.order(8_193) == HilbertStyle.MAX_ORDER + 1
        HilbertStyle.order(40_000) == 9
        (1L << (2 * HilbertStyle.order(40_000))) >= 80_000
    }
}
