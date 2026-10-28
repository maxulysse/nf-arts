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

    static TaskTile tile(TraceRecord trace, boolean cached, long cachedStart) {
        final duration = (trace.get('realtime') ?: trace.get('duration') ?: 0L) as long
        final start = cached ? cachedStart : (trace.get('start') ?: trace.get('submit') ?: cachedStart) as long
        final status = cached ? 'CACHED' : (trace.get('status') == 'COMPLETED' ? 'COMPLETED' : 'FAILED')
        final memory = (trace.get('peak_rss') ?: 0L) as long
        return new TaskTile(trace.getProcessName() ?: trace.get('name') as String, start, duration, memory, status)
    }

    /**
     * Tasks of a run from the cache in `dir`/.nextflow
     */
    static List<TaskTile> fromCache(Path dir, HistoryFile.Record record) {
        final db = new CacheDB(new DefaultCacheStore(record.sessionId, record.runName, dir.resolve('.nextflow'))).openForRead()
        final tiles = new ArrayList<TaskTile>()
        try {
            db.eachRecord { TraceRecord trace -> tiles << tile(trace, trace.cached, 0L) }
        }
        finally {
            db.close()
        }
        return replayCached(tiles)
    }

    /**
     * The last run when `idOrName` is null, else the run with that name or session id prefix.
     */
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
     * Tasks from an execution trace file, e.g. `pipeline_info/execution_trace_*.txt`.
     */
    static List<TaskTile> fromTrace(Path file) {
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
            final status = row.status == 'CACHED' ? 'CACHED' : row.status == 'COMPLETED' ? 'COMPLETED' : 'FAILED'
            tiles << new TaskTile(
                row.name.replaceFirst(/ \(.*\)$/, ''),
                when ? dates.parse(when).time : 0L,
                time ? Duration.of(time).toMillis() : 0L,
                rss ? MemoryUnit.of(rss).toBytes() : 0L,
                status)
        }
        return replayCached(tiles)
    }

    /**
     * Title and session id from the execution report published next to a trace file,
     * e.g. `execution_report_<date>.html` for `execution_trace_<date>.txt`; null when there is none.
     */
    static Report report(Path trace) {
        final name = trace.fileName.toString().replace('execution_trace', 'execution_report').replaceFirst(/\.[^.]+$/, '.html')
        if( name == trace.fileName.toString() )
            return null
        // optional: a missing or unreadable report only loses the caption details
        try {
            // the run details sit after ~150 KB of inline CSS, the report can be megabytes long
            final head = Files.newInputStream(trace.resolveSibling(name)).withCloseable { InputStream it -> it.readNBytes(512 * 1024) }
            return Report.parse(new String(head, 'UTF-8'))
        }
        catch( Exception e ) {
            System.err.println "nf-arts: no execution report next to the trace (${e.message?.take(80)}), use --pipeline, --revision and --run-name for the caption"
            return null
        }
    }

    private static String value(Map<String, String> row, String key) {
        final v = row[key]
        return v && v != '-' ? v : null
    }

    /** cached tasks carry the times of an earlier run, so move them to the start of this one */
    static List<TaskTile> replayCached(List<TaskTile> tiles) {
        final fresh = tiles.findAll { TaskTile it -> it.status() != 'CACHED' }
        if( !fresh || fresh.size() == tiles.size() )
            return tiles
        final t0 = fresh*.start().min() as long
        return tiles.collect { TaskTile it ->
            it.status() == 'CACHED' ? new TaskTile(it.process(), t0, it.duration(), it.memory(), it.status()) : it
        }
    }
}
