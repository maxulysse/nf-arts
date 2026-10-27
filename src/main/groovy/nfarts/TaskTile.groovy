package nfarts

import groovy.transform.CompileStatic

/**
 * One task of the run, reduced to what the styles draw.
 *
 * @param process process name
 * @param start start time, epoch milliseconds
 * @param duration wall time, milliseconds
 * @param memory peak RSS, bytes
 * @param status COMPLETED, FAILED or CACHED
 */
@CompileStatic
record TaskTile(String process, long start, long duration, long memory, String status) {}
