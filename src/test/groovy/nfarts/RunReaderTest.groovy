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
        def tiles = RunReader.fromTrace(TRACE)
        def fresh = tiles.findAll { it.status() != 'CACHED' }
        then:
        tiles.size() == 5
        tiles[0].process() == 'NFCORE_RNAVAR:PREPARE_GENOME:STAR_INDEXVERSION'
        tiles[1].process() == 'NFCORE_RNAVAR:PREPARE_GENOME:SAMTOOLS_FAIDX'
        tiles[1].duration() == 23_000
        tiles[1].memory() == 5 * 1024 * 1024
        tiles*.status() == ['COMPLETED', 'COMPLETED', 'COMPLETED', 'CACHED', 'FAILED']
        // '-' values
        tiles[4].duration() == 0
        tiles[4].memory() == 0
        // cached tasks move to the start of this run
        tiles[3].start() == fresh*.start().min()
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
        RunReader.tile(trace, false, 1) == new TaskTile('FOO', 100, 50, 7, 'COMPLETED')
        RunReader.tile(trace, true, 1) == new TaskTile('FOO', 1, 50, 7, 'CACHED')
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
}
