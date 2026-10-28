package nfarts

import spock.lang.Specification

class ArtsPluginTest extends Specification {

    def 'should return 0 on success and 1 on any error' () {
        expect:
        ArtsPlugin.run('render', ['--help']) == 0
        ArtsPlugin.run('paint', []) == 1
        ArtsPlugin.run('render', ['--style']) == 1
        ArtsPlugin.run('render', ['--trace', 'missing_trace.txt']) == 1
    }
}
