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
