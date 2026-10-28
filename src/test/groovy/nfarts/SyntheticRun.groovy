package nfarts

/**
 * Fake runs for tests: samples flow through a chain of
 * processes, with a final aggregation step.
 */
class SyntheticRun {

    static final List<Map> PIPELINE = [
        [name: 'FASTQC',        min:   2, max:   6, mem:  300],
        [name: 'FASTP',         min:   5, max:  15, mem:  800],
        [name: 'BWAMEM2_MEM',   min:  40, max: 120, mem: 9000],
        [name: 'SAMTOOLS_SORT', min:  10, max:  30, mem: 3000],
        [name: 'MARKDUPLICATES',min:  20, max:  60, mem: 6000],
        [name: 'BQSR',          min:  15, max:  40, mem: 4000],
        [name: 'HAPLOTYPECALLER',min: 30, max:  90, mem: 5000],
        [name: 'BCFTOOLS_STATS',min:   1, max:   4, mem:  200],
    ]

    static List<TaskTile> create(long seed, int samples = 24, boolean resumed = false) {
        final rnd = new Random(seed)
        final tiles = []
        final min = 60_000L
        long last = 0
        samples.times { s ->
            long t = (long) (rnd.nextDouble() * 20 * min)
            PIPELINE.eachWithIndex { Map p, int i ->
                final cached = resumed && i < 5 && s < samples * 0.8
                t += (long) (rnd.nextDouble() * 5 * min)
                long duration = (long) ((p.min + rnd.nextDouble() * (p.max - p.min)) * min)
                final memory = (long) (p.mem * (0.5 + rnd.nextDouble()) * 1_000_000)
                if( !cached && rnd.nextDouble() < 0.04 ) {
                    // failed attempt, then a retry
                    tiles << new TaskTile(p.name, t, duration.intdiv(3), memory, Status.FAILED)
                    t += duration.intdiv(3)
                }
                tiles << new TaskTile(p.name, cached ? 0 : t, duration, memory, cached ? Status.CACHED : Status.COMPLETED)
                if( !cached ) t += duration
                last = Math.max(last, t)
            }
        }
        tiles << new TaskTile('MULTIQC', last, 8 * min, 2_000_000_000L, Status.COMPLETED)
        return tiles
    }
}
