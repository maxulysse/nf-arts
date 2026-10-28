package nfarts

import java.nio.file.Files
import java.nio.file.Paths

import nextflow.trace.TraceRecord
import nextflow.util.HistoryFile
import spock.lang.Specification

class RunReaderTest extends Specification {

    static final TRACE = Paths.get(RunReaderTest.getResource('/execution_trace.txt').toURI())

    def 'should read an nf-core execution trace' () {
        when:
        def run = RunReader.fromTrace(TRACE)
        def tiles = run.tiles()
        then:
        // caption and session from the execution report next to the trace
        run.title() == new Title('nf-core/rnavar', '1.2.2', 'exotic_kimura')
        run.session() == UUID.fromString('1ba77276-0c91-46e0-862d-e017d222de42')
        tiles.size() == 5
        tiles[0].process() == 'NFCORE_RNAVAR:PREPARE_GENOME:STAR_INDEXVERSION'
        tiles[1].process() == 'NFCORE_RNAVAR:PREPARE_GENOME:SAMTOOLS_FAIDX'
        tiles[1].duration() == 23_000
        tiles[1].memory() == 5 * 1024 * 1024
        tiles*.status() == [Status.COMPLETED, Status.COMPLETED, Status.COMPLETED, Status.CACHED, Status.FAILED]
        // '-' values
        tiles[4].duration() == 0
        tiles[4].memory() == 0
        // cached tasks keep the times of the run they come from
        tiles[3].start() < tiles[0].start()
        tiles[3].duration() == 60_000
    }

    def 'should reject a file that is not a trace' () {
        given:
        def file = Files.createTempFile('trace', '.txt')
        file.text = 'foo\tbar\n1\t2\n'
        when:
        RunReader.fromTrace(file)
        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("no 'name' column")
        cleanup:
        Files.delete(file)
    }

    def 'should convert a trace record' () {
        given:
        def trace = new TraceRecord()
        trace.putAll(process: 'FOO', start: 100L, realtime: 50L, peak_rss: 7L, status: 'COMPLETED')

        expect:
        RunReader.tile(trace, false, 1) == new TaskTile('FOO', 100, 50, 7, Status.COMPLETED)
        RunReader.tile(trace, true, 1) == new TaskTile('FOO', 100, 50, 7, Status.CACHED)
        RunReader.tile(new TraceRecord(), false, 1).start() == 1
    }

    def 'should find runs in the history' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def file = dir.resolve('history')
        def history = new HistoryFile(file)
        history.write('happy_turing', UUID.fromString('11111111-0000-0000-0000-000000000000'), 'abc', ['nextflow', 'run'])
        history.write('sad_curie', UUID.fromString('22222222-0000-0000-0000-000000000000'), 'abc', ['nextflow', 'run'])

        expect:
        RunReader.record(history, null).runName == 'sad_curie'
        RunReader.record(history, 'happy_turing').runName == 'happy_turing'
        RunReader.record(history, '2222').runName == 'sad_curie'

        when:
        RunReader.record(history, 'nope')
        then:
        thrown(IllegalArgumentException)

        when:
        RunReader.record(new HistoryFile(dir.resolve('missing')), null)
        then:
        thrown(IllegalArgumentException)

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'render command should paint a trace file' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def out = dir.resolve('art.png')

        when:
        def code = new RenderCommand().run(['--trace', TRACE.toString(), '--file', out.toString(), '--style', 'hilbert', '--width', '200', '--height', '100', '--colors', '#000000,#ffffff'])
        then:
        code == 0
        javax.imageio.ImageIO.read(out.toFile()).width == 200

        when:
        new RenderCommand().run(['--style'])
        then:
        thrown(IllegalArgumentException)

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'render help should list every style and palette' () {
        expect:
        Artist.STYLES.keySet().every { RenderCommand.USAGE.contains(it) }
        Palette.NAMED.keySet().every { RenderCommand.USAGE.contains(it) }
    }

    def 'seed should come from the session, else from the tasks, the same in every JVM' () {
        given:
        def tiles = [new TaskTile('A', 1, 2, 3, Status.COMPLETED), new TaskTile('B', 4, 5, 6, Status.FAILED)]

        expect:
        new PaintedRun(tiles, new Title(null, null, null), new UUID(42, 7)).seed() == 42
        // a fixed value: an identity hash would differ from run to run
        new PaintedRun(tiles, new Title(null, null, null), null).seed() == SEED
    }

    static final long SEED = [['A', 1L, 2L, 3L, 'COMPLETED'], ['B', 4L, 5L, 6L, 'FAILED']].hashCode()
}
