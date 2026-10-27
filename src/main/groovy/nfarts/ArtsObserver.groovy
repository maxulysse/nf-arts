/*
 * Copyright 2026, Maxime U. Garcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        if( trace == null )
            return
        final duration = (trace.get('realtime') ?: trace.get('duration') ?: 0L) as long
        // cached traces carry the times of the earlier run, so replay them now
        final start = cached ? System.currentTimeMillis() : (trace.get('start') ?: trace.get('submit') ?: System.currentTimeMillis()) as long
        final status = cached ? 'CACHED' : (trace.get('status') == 'COMPLETED' ? 'COMPLETED' : 'FAILED')
        final memory = (trace.get('peak_rss') ?: 0L) as long
        tiles << new TaskTile(trace.getProcessName() ?: trace.get('name') as String, start, duration, memory, status)
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
