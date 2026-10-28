package nfarts

import java.nio.file.Files
import java.nio.file.Paths

import groovy.transform.CompileStatic
import nextflow.file.FileHelper
import nextflow.util.HistoryFile

/**
 * `nextflow plugin nf-arts:render`: paints a run that already finished.
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
  --style NAME        radial, hilbert, ridgeline, phyllotaxis, flowfield, mondrian or tree
  --palette NAME      viridis, magma, nfcore, ocean, sunset or mono
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
        final pipeline = opts.remove('pipeline') as String
        final revision = opts.remove('revision') as String
        final runName = opts.remove('run-name') as String
        final dir = Paths.get((opts.remove('dir') ?: '.') as String)
        final record = trace ? null : RunReader.record(new HistoryFile(dir.resolve('.nextflow/history')), run)
        final tracePath = trace ? FileHelper.asPath(trace) : null
        final report = tracePath ? RunReader.report(tracePath) : null
        final tiles = trace
            ? RunReader.fromTrace(tracePath)
            : RunReader.fromCache(dir, record)
        if( !tiles )
            throw new IllegalArgumentException('The run has no tasks to paint')

        final name = trace ? trace.tokenize('/').last().replaceFirst(/\.[^.]+$/, '') : record.runName
        final config = new ArtsConfig(opts)
        final out = Paths.get(config.file ?: "art-${name}.svg".toString())
        // same seed as the live plugin when the session is known, else same tasks same art
        final session = record?.sessionId ?: report?.session()
        final seed = session ? session.mostSignificantBits : (long) tiles.hashCode()
        final history = record ? Title.fromCommand(record.command, record.runName) : report?.title() ?: new Title(null, null, null)
        final title = new Title(pipeline ?: history.pipeline(), revision ?: history.revision(), runName ?: history.runName())
        final bytes = Artist.paint(tiles, config, ArtsConfig.format(out.toString()), seed, title)
        if( out.toAbsolutePath().parent )
            Files.createDirectories(out.toAbsolutePath().parent)
        Files.write(out, bytes)
        println "nf-arts: painted ${tiles.size()} tasks to ${out}"
        return 0
    }
}
