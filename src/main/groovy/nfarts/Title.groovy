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

    /** from a history command line, e.g. `nextflow run nf-core/rnavar -r 1.3.0 -profile test` */
    static Title fromCommand(String command, String runName) {
        final args = command?.tokenize(' ') ?: []
        final run = args.indexOf('run')
        String pipeline = null
        String revision = null
        for( int i = run + 1; run >= 0 && i < args.size(); i++ ) {
            if( args[i] == '-r' && i + 1 < args.size() )
                revision = args[++i]
            else if( args[i].startsWith('-') && !args[i].startsWith('--') && i + 1 < args.size() && !args[i + 1].startsWith('-') )
                i++ // value of another option
            else if( !args[i].startsWith('-') && pipeline == null )
                pipeline = args[i]
        }
        return new Title(pipeline, revision, runName)
    }
}
