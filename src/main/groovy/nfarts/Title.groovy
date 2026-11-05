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

    /** the pipeline if it is `org/name` or a URL without credentials, as anything else may be a local path */
    static String shareable(String pipeline) {
        pipeline ==~ /[\w-][\w.-]*\/[\w.-]+|https?:\/\/[^@]+/ ? pipeline : null
    }

    /** this title, with the parts that `other` has */
    Title overriddenBy(Title other) {
        new Title(other.pipeline ?: pipeline, other.revision ?: revision, other.runName ?: runName)
    }

    // `nextflow run` options that take a value; any other single-dash option is a flag
    private static final Set<String> VALUE_OPTIONS = ['-r', '-revision', '-profile', '-c', '-C', '-config', '-params-file', '-name',
        '-w', '-work-dir', '-plugins', '-entry', '-main-script', '-hub', '-user', '-qs', '-queue-size', '-ps', '-pool-size',
        '-pi', '-poll-interval', '-cache', '-bucket-dir', '-lib', '-o', '-output-dir', '-output-format', '-d', '-deep'] as Set<String>

    // options that take a value only when the next argument is not an option
    private static final Set<String> OPTIONAL_VALUE_OPTIONS = ['-ansi-log', '-test', '-dump-hashes', '-dump-channels', '-N', '-with-notification',
        '-with-tower', '-with-wave', '-with-fusion', '-with-weblog', '-with-cloudcache', '-with-trace', '-with-report', '-with-timeline',
        '-with-dag', '-with-docker', '-with-podman', '-with-singularity', '-with-apptainer', '-with-charliecloud', '-with-conda',
        '-with-spack'] as Set<String>

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
                final optional = arg in OPTIONAL_VALUE_OPTIONS || arg ==~ /-(process|cluster|executor)\.[^=]+/
                if( next && (arg in VALUE_OPTIONS || optional && !next.startsWith('-')) )
                    i++
            }
            else if( pipeline == null )
                pipeline = arg
        }
        return new Title(shareable(pipeline), revision, runName)
    }
}
