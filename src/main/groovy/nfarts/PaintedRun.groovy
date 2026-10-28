package nfarts

import groovy.transform.CompileStatic

/**
 * A run ready to paint: its tasks, its caption and, when known, its session.
 */
@CompileStatic
record PaintedRun(List<TaskTile> tiles, Title title, UUID session) {

    /** the session gives the same art as the live plugin; without it, the same tasks give the same art */
    long seed() {
        if( session )
            return session.mostSignificantBits
        // hash plain values: an enum hashes by identity, which changes between JVM runs
        return (long) tiles.collect { TaskTile t -> [t.process(), t.start(), t.duration(), t.memory(), t.status().name()] }.hashCode()
    }
}
