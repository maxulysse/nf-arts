package nfarts

import groovy.transform.CompileStatic

/**
 * How a task ended, and how that shows in the art.
 */
@CompileStatic
enum Status {
    COMPLETED(0.8d),
    FAILED(1d),
    CACHED(0.3d)

    final double opacity

    Status(double opacity) {
        this.opacity = opacity
    }

    /** failed tasks are drawn a third as wide */
    double thin(double width) {
        this == FAILED ? width / 3 : width
    }

    /** from a trace `status` field */
    static Status of(Object status, boolean cached) {
        cached || status == 'CACHED' ? CACHED : status == 'COMPLETED' ? COMPLETED : FAILED
    }
}
