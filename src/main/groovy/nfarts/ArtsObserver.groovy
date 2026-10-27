package nfarts

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.file.FileHelper
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceRecord
import nextflow.trace.event.TaskEvent

/**
 * Collects every task of the run and paints them when the run completes.
 */
@Slf4j
@CompileStatic
class ArtsObserver implements TraceObserverV2 {

    private final ArtsConfig config
    private final Queue<TaskTile> tiles = new ConcurrentLinkedQueue<>()
    private Session session

    ArtsObserver(ArtsConfig config) {
        this.config = config
    }

    @Override
    void onFlowCreate(Session session) {
        this.session = session
    }

    @Override
    void onTaskComplete(TaskEvent event) {
        add(event.trace, false)
    }

    @Override
    void onTaskCached(TaskEvent event) {
        add(event.trace, true)
    }

    private void add(TraceRecord trace, boolean cached) {
        // cached traces carry the times of the earlier run, so replay them now
        if( trace != null )
            tiles << RunReader.tile(trace, cached, System.currentTimeMillis())
    }

    List<TaskTile> getTiles() {
        new ArrayList<TaskTile>(tiles)
    }

    @Override
    void onFlowComplete() {
        if( tiles.isEmpty() ) {
            log.debug "nf-arts: no tasks, no artwork"
            return
        }
        try {
            final path = outputPath()
            final seed = session?.uniqueId?.mostSignificantBits ?: 0L
            final bytes = Artist.paint(getTiles(), config, ArtsConfig.format(path.toString()), seed)
            if( path.parent )
                Files.createDirectories(path.parent)
            Files.write(path, bytes)
            log.info "nf-arts: artwork saved to ${path}"
        }
        catch( Exception e ) {
            // art must never fail a pipeline
            log.warn "nf-arts: unable to create artwork -- ${e.message}", e
        }
    }

    Path outputPath() {
        FileHelper.asPath(config.file ?: "art-${session?.runName ?: 'run'}.svg".toString())
    }
}
