package nfarts

import spock.lang.Specification

class ArtsConfigTest extends Specification {

    def 'should use defaults' () {
        when:
        def config = new ArtsConfig([:])
        then:
        config.enabled
        config.style == 'radial'
        config.palette == 'viridis'
        config.width == 1200
        config.height == 1200
        config.file == null
    }

    def 'should read options' () {
        when:
        def config = new ArtsConfig(enabled: false, style: 'hilbert', palette: 'ocean', width: 300, height: 200, file: 'out/art.PNG')
        then:
        !config.enabled
        config.style == 'hilbert'
        config.width == 300
        config.height == 200
        ArtsConfig.format(config.file) == 'png'
    }

    def 'should reject invalid options' () {
        when:
        new ArtsConfig(opts)
        then:
        thrown(IllegalArgumentException)
        where:
        opts << [
            [style: 'cubist'],
            [width: 0],
            [height: 20000],
            [file: 'art.jpg'],
            [palette: 'rainbow'],
            [colors: ['red']],
        ]
    }

    def 'mondrian style should default to the mondrian palette' () {
        expect:
        new ArtsConfig(style: 'mondrian').palette == 'mondrian'
        new ArtsConfig(style: 'mondrian', palette: 'magma').palette == 'magma'
        new ArtsConfig(style: 'tree').palette == 'viridis'
    }
}
