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
        observer.tiles*.status() == ['COMPLETED', 'FAILED', 'CACHED']
        observer.tiles[2].start() == 1L
        Files.size(file) > 0
        Files.newInputStream(file).withCloseable { it.readNBytes(4) } == [0x89, 0x50, 0x4e, 0x47] as byte[]

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'should name the file after the run' () {
        expect:
        Artist.outputPath(new PaintedRun([], new Title(null, null, 'happy_turing'), null), new ArtsConfig([:])).toString() == 'art-happy_turing.svg'
        Artist.outputPath(new PaintedRun([], new Title(null, null, null), null), new ArtsConfig([:])).toString() == 'art-run.svg'
        Artist.outputPath(new PaintedRun([], new Title(null, null, 'x'), null), new ArtsConfig(file: 'out/a.png')).toString() == 'out/a.png'
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
