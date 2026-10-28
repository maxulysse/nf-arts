package nfarts

import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat

import groovy.transform.CompileStatic
import nextflow.cache.CacheDB
import nextflow.cache.DefaultCacheStore
import nextflow.trace.TraceRecord
import nextflow.util.Duration
import nextflow.util.HistoryFile
import nextflow.util.MemoryUnit

/**
 * Reads the tasks of a finished run, from the local cache or from a trace file.
 */
@CompileStatic
class RunReader {

    /** `fallbackStart` is for tasks with neither a start nor a submit time */
    static TaskTile tile(TraceRecord trace, boolean cached, long fallbackStart) {
        final duration = (trace.get('realtime') ?: trace.get('duration') ?: 0L) as long
        final start = (trace.get('start') ?: trace.get('submit') ?: fallbackStart) as long
        final memory = (trace.get('peak_rss') ?: 0L) as long
        return new TaskTile(trace.getProcessName() ?: trace.get('name') as String, start, duration, memory, Status.of(trace.get('status'), cached))
    }

    /**
     * A run from the history and cache in `dir`/.nextflow: the last run when `idOrName` is null,
     * else the run with that name or session ID prefix.
     */
    static PaintedRun fromHistory(Path dir, String idOrName) {
        final record = record(new HistoryFile(dir.resolve('.nextflow/history')), idOrName)
        final db = new CacheDB(new DefaultCacheStore(record.sessionId, record.runName, dir.resolve('.nextflow'))).openForRead()
        final tiles = new ArrayList<TaskTile>()
        try {
            db.eachRecord { TraceRecord trace -> tiles << tile(trace, trace.cached, 0L) }
        }
        finally {
            db.close()
        }
        return new PaintedRun(tiles, Title.fromCommand(record.command, record.runName), record.sessionId)
    }

    static HistoryFile.Record record(HistoryFile history, String idOrName) {
        if( !history.exists() )
            throw new IllegalArgumentException("No run history found: ${history}")
        if( !idOrName )
            return history.getLast()
        final matches = history.findByIdOrName(idOrName)
        if( !matches )
            throw new IllegalArgumentException("No run named or with session id '${idOrName}' in ${history}")
        if( matches.size() > 1 )
            throw new IllegalArgumentException("'${idOrName}' matches ${matches.size()} runs: ${matches*.runName.join(', ')}")
        return matches.first()
    }

    /**
     * A run from an execution trace file, e.g. `pipeline_info/execution_trace_*.txt`,
     * with the caption and session from the execution report next to it, if any.
     */
    static PaintedRun fromTrace(Path file) {
        final report = report(file)
        return new PaintedRun(tasks(file), report?.title() ?: new Title(null, null, null), report?.session())
    }

    static List<TaskTile> tasks(Path file) {
        final lines = Files.readAllLines(file)
        if( !lines )
            throw new IllegalArgumentException("Empty trace file: ${file}")
        final header = lines[0].split('\t').toList()
        for( String col : ['name', 'status'] )
            if( col !in header )
                throw new IllegalArgumentException("Trace file ${file} has no '${col}' column")
        final dates = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.SSS')
        final tiles = new ArrayList<TaskTile>()
        for( String line : lines.drop(1) ) {
            if( !line.trim() )
                continue
            final values = line.split('\t', -1)
            final row = [header, values.toList()].transpose().collectEntries() as Map<String, String>
            final when = value(row, 'start') ?: value(row, 'submit')
            final time = value(row, 'realtime') ?: value(row, 'duration')
            final rss = value(row, 'peak_rss')
            tiles << new TaskTile(
                row.name.replaceFirst(/ \(.*\)$/, ''),
                when ? dates.parse(when).time : 0L,
                time ? Duration.of(time).toMillis() : 0L,
                rss ? MemoryUnit.of(rss).toBytes() : 0L,
                Status.of(row.status, false))
        }
        return tiles
    }

    /**
     * Caption and session ID from the execution report next to a trace file
     * (`execution_report_<date>.html` for `execution_trace_<date>.txt`), or null.
     */
    static Report report(Path trace) {
        final name = trace.fileName.toString().replace('execution_trace', 'execution_report').replaceFirst(/\.[^.]+$/, '.html')
        if( name == trace.fileName.toString() )
            return null
        // optional: a missing or unreadable report only loses the caption details
        try {
            // the run details sit after ~150 KB of inline CSS; the whole report can be megabytes
            final head = Files.newInputStream(trace.resolveSibling(name)).withCloseable { InputStream it -> it.readNBytes(512 * 1024) }
            return Report.parse(new String(head, 'UTF-8'))
        }
        catch( Exception ignored ) {
            return null
        }
    }

    private static String value(Map<String, String> row, String key) {
        final v = row[key]
        return v && v != '-' ? v : null
    }
}
