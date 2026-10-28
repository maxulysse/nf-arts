package nfarts

import groovy.transform.CompileStatic

/**
 * A run ready to paint: its tasks, its caption and, when known, its session.
 */
@CompileStatic
record PaintedRun(List<TaskTile> tiles, Title title, UUID session) {

    /** the session gives the same art as the live plugin; without it, the same tasks give the same art */
    long seed() {
        session ? session.mostSignificantBits : (long) tiles.hashCode()
    }
}
