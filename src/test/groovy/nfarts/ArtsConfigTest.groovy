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

    def 'should read options given as text' () {
        when:
        def config = new ArtsConfig(enabled: 'false', title: 'false', width: '800', height: '600', colors: '#000000,#ffffff')
        then:
        !config.enabled
        !config.title
        config.width == 800
        config.height == 600
        config.colors == ['#000000', '#ffffff']
    }

    def 'should reject options that are not what they claim' () {
        when:
        new ArtsConfig(opts)
        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(message)
        where:
        opts                  | message
        [title: 'no']         | 'arts.title must be true or false'
        [width: '12.5']       | 'arts.width must be a whole number'
        [colors: ['#fff']]    | "Invalid arts color '#fff'"
        [background: 'red']   | "Invalid arts color 'red'"
    }

    def 'should list the arts options' () {
        expect:
        ArtsConfig.OPTIONS.toSet() == ['enabled', 'file', 'style', 'palette', 'colors', 'background', 'width', 'height', 'title'] as Set
    }
}
