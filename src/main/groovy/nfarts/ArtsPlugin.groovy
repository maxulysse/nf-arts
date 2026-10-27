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
import nextflow.cli.Launcher
import nextflow.cli.PluginExecAware
import nextflow.plugin.BasePlugin
import org.pf4j.PluginWrapper

/**
 * The plugin entry point, with the `render` command for finished runs
 */
@CompileStatic
class ArtsPlugin extends BasePlugin implements PluginExecAware {

    ArtsPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    // Nextflow 25.x signature
    int exec(Launcher launcher, String pluginId, String cmd, List<String> args) {
        exec(pluginId, cmd, args)
    }

    // Nextflow 26.x signature, not in the 25.x interface
    int exec(String pluginId, String cmd, List<String> args) {
        if( cmd != 'render' ) {
            System.err.println "nf-arts: unknown command '${cmd}', use 'render'"
            return 1
        }
        try {
            return new RenderCommand().run(args)
        }
        catch( Exception e ) {
            System.err.println "nf-arts: ${e.message ?: e.class.simpleName}"
            return 1
        }
    }
}
