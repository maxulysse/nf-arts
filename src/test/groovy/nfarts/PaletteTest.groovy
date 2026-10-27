package nfarts

import spock.lang.Specification

class PaletteTest extends Specification {

    def 'should load a named palette' () {
        when:
        def palette = Palette.of('magma')
        then:
        palette.background == '#000004'
        palette.colors.size() == 5
        palette.at(0) == '#5c1a8f'
        palette.at(1) == '#fcfdbf'
    }

    def 'should interpolate between colors' () {
        given:
        def palette = Palette.of('x', ['#000000', '#ffffff'], '#123456')
        expect:
        palette.background == '#123456'
        palette.at(0.5) == '#808080'
        palette.at(-1) == '#000000'
        palette.at(2) == '#ffffff'
    }

    def 'should reject unknown palette and bad colors' () {
        when:
        Palette.of('rainbow')
        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('viridis')

        when:
        Palette.of('x', ['#zzzzzz'])
        then:
        thrown(IllegalArgumentException)
    }
}
