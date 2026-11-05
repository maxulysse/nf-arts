package nfarts

import java.nio.file.Files

import nextflow.Session
import nextflow.trace.TraceRecord
import nextflow.trace.event.TaskEvent
import spock.lang.Specification

class ArtsObserverTest extends Specification {

    def 'should create the observer only when enabled' () {
        given:
        def session = Mock(Session) { getConfig() >> [arts: [enabled: enabled]] }

        expect:
        new ArtsFactory().create(session).size() == count
        where:
        enabled | count
        true    | 1
        false   | 0
    }

    def 'should create the observer without config' () {
        given:
        def session = Mock(Session) { getConfig() >> [:] }

        expect:
        new ArtsFactory().create(session).first() instanceof ArtsObserver
    }

    private static TaskEvent event(Map fields) {
        def trace = new TraceRecord()
        fields.each { k, v -> trace.put(k as String, v) }
        new TaskEvent(null, trace)
    }

    def 'should collect tasks and write the artwork' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def file = dir.resolve('sub/art.png')
        def observer = new ArtsObserver(new ArtsConfig(file: file.toString(), width: 100, height: 100))
        observer.onFlowCreate(Mock(Session) { getUniqueId() >> UUID.randomUUID() })

        when:
        observer.onTaskComplete(event(process: 'FOO', start: 1000L, realtime: 500L, peak_rss: 10L, status: 'COMPLETED'))
        observer.onTaskComplete(event(process: 'BAR', start: 1200L, realtime: 100L, status: 'FAILED'))
        observer.onTaskCached(event(process: 'FOO', start: 1L, realtime: 300L, status: 'COMPLETED'))
        observer.onTaskComplete(new TaskEvent(null, null))
        observer.onFlowComplete()

        then:
        observer.tiles*.process() == ['FOO', 'BAR', 'FOO']
        observer.tiles*.status() == [Status.COMPLETED, Status.FAILED, Status.CACHED]
        observer.tiles[2].start() == 1L
        Files.size(file) > 0
        Files.newInputStream(file).withCloseable { it.readNBytes(4) } == [0x89, 0x50, 0x4e, 0x47] as byte[]

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'should survive a render that fails' () {
        given:
        def observer = new ArtsObserver(new ArtsConfig([:]))
        observer.onFlowCreate(Mock(Session) { getWorkflowMetadata() >> { throw failure } })
        observer.onTaskComplete(event(process: 'FOO', start: 1000L, realtime: 500L, status: 'COMPLETED'))

        when:
        observer.onFlowComplete()
        then:
        noExceptionThrown()

        where:
        failure << [new IllegalStateException('boom'), new OutOfMemoryError('boom')]
    }

    def 'should log where the Nextflow console shows it' () {
        expect:
        Artist.log.name.startsWith('nextflow')
        ArtsObserver.log.name.startsWith('nextflow')
    }

    def 'should name the file after the run' () {
        expect:
        Artist.outputPaths(new PaintedRun([], new Title(null, null, 'happy_turing'), null), new ArtsConfig([:]))*.toString() == ['art-happy_turing.svg', 'art-happy_turing.png']
        Artist.outputPaths(new PaintedRun([], new Title(null, null, null), null), new ArtsConfig([:]))*.toString() == ['art-run.svg', 'art-run.png']
        Artist.outputPaths(new PaintedRun([], new Title(null, null, 'x'), null), new ArtsConfig(file: 'out/a.png'))*.toString() == ['out/a.png']
    }

    def 'should keep the other file when one fails' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def run = new PaintedRun(SyntheticRun.create(1, 1), new Title(null, null, 'x'), null)
        def config = new ArtsConfig(width: 100, height: 100)
        def svg = dir.resolve('a.svg')
        def png = dir.resolve('a.png')
        def warnings = []

        when:
        Files.createDirectory(failing ? png : dir.resolve('other'))
        def written = Artist.write(run, config, [svg, png]) { warnings << it }
        then:
        written == (failing ? [svg] : [svg, png])
        warnings.size() == (failing ? 1 : 0)
        Files.size(svg) > 0

        cleanup:
        dir.toFile().deleteDir()

        where:
        failing << [true, false]
    }

    def 'should fail when no file is written' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def run = new PaintedRun(SyntheticRun.create(1, 1), new Title(null, null, 'x'), null)

        when:
        Artist.write(run, new ArtsConfig(width: 100, height: 100), [Files.createDirectory(dir.resolve('a.svg'))]) { }
        then:
        thrown(IOException)

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'should not write anything without tasks' () {
        given:
        def dir = Files.createTempDirectory('nf-arts')
        def file = dir.resolve('art.svg')
        def observer = new ArtsObserver(new ArtsConfig(file: file.toString()))
        observer.onFlowCreate(Mock(Session))

        when:
        observer.onFlowComplete()
        then:
        !Files.exists(file)

        cleanup:
        dir.toFile().deleteDir()
    }
}
