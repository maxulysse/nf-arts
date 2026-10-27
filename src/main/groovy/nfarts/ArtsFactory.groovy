package nfarts

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.trace.TraceObserverFactoryV2
import nextflow.trace.TraceObserverV2

/**
 * Creates the {@link ArtsObserver} when the plugin is enabled.
 */
@CompileStatic
class ArtsFactory implements TraceObserverFactoryV2 {

    @Override
    Collection<TraceObserverV2> create(Session session) {
        final config = new ArtsConfig(session.config.navigate('arts') as Map ?: [:])
        return config.enabled ? List.<TraceObserverV2>of(new ArtsObserver(config)) : List.<TraceObserverV2>of()
    }
}
