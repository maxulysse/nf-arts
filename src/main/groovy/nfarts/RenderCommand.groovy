package nfarts

import java.nio.file.Paths

import groovy.transform.CompileStatic
import nextflow.file.FileHelper

/**
 * `nextflow plugin nf-arts:render`: paints a finished run.
 */
@CompileStatic
class RenderCommand {

    static final String USAGE = '''\
Usage: nextflow plugin nf-arts:render [RUN] [options]

Paint a finished run. RUN is a run name or session id from the local
history (default: the last run), or use --trace.

Options:
  --trace FILE        read an execution trace file (local, s3://, gs://, az://, http(s)://)
  --dir DIR           project directory with the .nextflow history (default: .)
  --file FILE         output .svg or .png (default: art-<run>.svg)
  --style NAME        radial, hilbert, ridgeline, phyllotaxis, flowfield, mondrian, tree or ginkgo
  --palette NAME      viridis, magma, nfcore, ocean, sunset, mono or mondrian
  --colors A,B,...    custom hex colors
  --background HEX    background color
  --width N           width in pixels
  --height N          height in pixels
  --title false       no caption
  --pipeline NAME     caption pipeline, e.g. nf-core/rnavar
  --revision REV      caption version
  --run-name NAME     caption run name
                      (default: from the history, or from the execution
                      report next to the trace file)'''

    static final List<String> INTS = ['width', 'height']
    static final List<String> BOOLEANS = ['title', 'enabled']

    int run(List<String> args) {
        String run = null
        final opts = [:] as Map<String, Object>
        for( int i = 0; i < args.size(); i++ ) {
            final arg = args[i]
            if( arg in ['-h', '--help'] ) {
                println USAGE
                return 0
            }
            if( !arg.startsWith('--') ) {
                run = arg
                continue
            }
            if( i + 1 >= args.size() )
                throw new IllegalArgumentException("Missing value for ${arg}")
            final key = arg.substring(2)
            final value = args[++i]
            opts[key] = key in INTS ? Integer.parseInt(value)
                : key in BOOLEANS ? Boolean.parseBoolean(value)
                : key == 'colors' ? value.tokenize(',')
                : value
        }

        final trace = opts.remove('trace') as String
        final dir = Paths.get((opts.remove('dir') ?: '.') as String)
        final overrides = new Title(opts.remove('pipeline') as String, opts.remove('revision') as String, opts.remove('run-name') as String)
        final config = new ArtsConfig(opts)

        final source = trace ? RunReader.fromTrace(FileHelper.asPath(trace)) : RunReader.fromHistory(dir, run)
        if( !source.tiles() )
            throw new IllegalArgumentException('The run has no tasks to paint')
        final painted = new PaintedRun(source.tiles(), source.title().overriddenBy(overrides), source.session())
        if( config.title && !painted.title().text() )
            System.err.println 'nf-arts: no execution report next to the trace, use --pipeline, --revision and --run-name for the caption'

        final out = Artist.save(painted, config)
        println "nf-arts: painted ${painted.tiles().size()} tasks to ${out}"
        return 0
    }
}
