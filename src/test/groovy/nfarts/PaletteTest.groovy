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
        Palette.of('plaid')
        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('viridis')

        when:
        Palette.of('x', ['#zzzzzz'])
        then:
        thrown(IllegalArgumentException)
    }

    def 'mondrian palette should cycle colors without blending' () {
        given:
        def palette = Palette.of('mondrian')
        def n = palette.colors.size()

        expect:
        palette.discrete
        palette.background == '#111111'
        palette.color(1, 20) == '#d62e24'
        palette.color(n + 1, 20) == '#d62e24'
        (0..<20).collect { palette.color(it, 20) }.toSet() == ['#f2efe6', '#d62e24', '#1d4f9c', '#f2c81f'] as Set
        !Palette.of('viridis').discrete
        !Palette.of('mondrian', ['#000000', '#ffffff']).discrete
        Palette.of('viridis').color(0, 3) == Palette.of('viridis').at(0)
    }
}
