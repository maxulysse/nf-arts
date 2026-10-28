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
        final code = run(cmd, args)
        // `nextflow plugin` ignores the returned code, so exit with it for scripts and CI
        if( code != 0 ) {
            System.out.flush()
            System.err.flush()
            System.exit(code)
        }
        return code
    }

    static int run(String cmd, List<String> args) {
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
