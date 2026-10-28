package nfarts

import groovy.transform.CompileStatic

/**
 * What a Nextflow execution report says about its run.
 */
@CompileStatic
record Report(Title title, UUID session) {

    static Report parse(String html) {
        final runName = find(html, /<title>\[([^\]]+)\]/)
        final session = find(html, /Workflow session<\/dt>\s*<dd[^>]*><code>([0-9a-f-]{36})</)
        final repository = find(html, /Workflow repository<\/dt>\s*<dd[^>]*><code>([^<]+)<\/code>/)
        final revision = find(html, /Workflow repository<\/dt>\s*<dd[^>]*><code>[^<]+<\/code>, revision <code>([^<]+)</)
        // a repository URL ends with <org>/<name>, else fall back to the command line
        final pipeline = repository
            ? repository.replaceFirst(/\.git$/, '').tokenize('/').takeRight(2).join('/')
            : Title.fromCommand(find(html, /class="nfcommand"><code>([^<]+)</)?.replace('&#39;', '')?.replace("'", ''), null).pipeline()
        return new Report(new Title(pipeline, revision, runName), session ? UUID.fromString(session) : null)
    }

    private static String find(String html, String regex) {
        final m = html =~ regex
        return m.find() ? m.group(1).trim() : null
    }
}
