package nfarts

import java.util.concurrent.ConcurrentLinkedQueue

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceRecord
import nextflow.trace.event.TaskEvent

/**
 * Collects every task of the run and paints them when the run completes.
 */
// Nextflow's console only shows loggers under `nextflow`
@Slf4j(category = 'nextflow.arts')
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
        if( trace != null )
            tiles << RunReader.tile(trace, cached)
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
            final meta = session?.workflowMetadata
            final title = new Title(Title.shareable(meta?.manifest?.name ?: meta?.projectName), meta?.manifest?.version ?: meta?.revision, session?.runName)
            final paths = Artist.save(new PaintedRun(getTiles(), title, session?.uniqueId), config)
            log.info "nf-arts: artwork saved to ${paths.join(', ')}"
        }
        catch( Throwable e ) {
            // art must never fail a pipeline, and Nextflow logs what reaches it only at debug level
            log.warn "nf-arts: unable to create artwork -- ${e.message}", e
        }
    }
}
