package nfarts

import groovy.transform.CompileStatic

/**
 * What the run was, for the caption under the artwork.
 */
@CompileStatic
record Title(String pipeline, String revision, String runName) {

    String text() {
        [pipeline, revision, runName].findAll { String it -> it }.join(' · ')
    }

    /** this title, with the parts that `other` has */
    Title overriddenBy(Title other) {
        new Title(other.pipeline ?: pipeline, other.revision ?: revision, other.runName ?: runName)
    }

    // `nextflow run` options that take a value; any other single-dash option is a flag
    static final Set<String> VALUE_OPTIONS = ['-r', '-revision', '-profile', '-c', '-C', '-config', '-params-file', '-name',
        '-w', '-work-dir', '-plugins', '-ansi-log', '-entry', '-main-script', '-hub', '-user', '-qs', '-queue-size',
        '-cache', '-bucket-dir', '-with-tower', '-with-weblog'] as Set<String>

    /** from a history command line, e.g. `nextflow run nf-core/rnavar -r 1.3.0 -profile test` */
    static Title fromCommand(String command, String runName) {
        final args = command?.tokenize(' ') ?: []
        final run = args.indexOf('run')
        String pipeline = null
        String revision = null
        for( int i = run + 1; run >= 0 && i < args.size(); i++ ) {
            final arg = args[i]
            final next = i + 1 < args.size() ? args[i + 1] : null
            if( arg in ['-r', '-revision'] && next )
                revision = args[++i]
            else if( arg.startsWith('--') ) {
                // a pipeline parameter: `--name value` or `--name=value`
                if( !arg.contains('=') && next && !next.startsWith('-') )
                    i++
            }
            else if( arg == '-resume' ) {
                // -resume takes a session ID or `last`, else stands alone
                if( next == 'last' || next ==~ /[0-9a-f]{8}-[0-9a-f-]{27}/ )
                    i++
            }
            else if( arg.startsWith('-') ) {
                if( arg in VALUE_OPTIONS && next )
                    i++
            }
            else if( pipeline == null )
                pipeline = arg
        }
        return new Title(pipeline, revision, runName)
    }
}
