/*
 * Copyright 2026, Maxime U. Garcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nfarts

import java.nio.file.Files
import java.nio.file.Paths

import groovy.transform.CompileStatic
import nextflow.file.FileHelper
import nextflow.util.HistoryFile

/**
 * `nextflow plugin nf-arts:render`: paints a run that already finished.
 */
@CompileStatic
class RenderCommand {

    static final String USAGE = '''\
Usage: nextflow plugin nf-arts:render [RUN] [options]

Paint a finished run. RUN is a run name or session id from the local
history (default: the last run), or use --trace.

Options:
  --trace FILE        read an execution trace file (local, s3://, gs://, az://, http(s)://)
  --dir DIR           project directory with the .nextflow history (default: .)
  --file FILE         output .svg or .png (default: art-<run>.svg)
  --style NAME        radial or hilbert
  --palette NAME      viridis, magma, nfcore, ocean, sunset or mono
  --colors A,B,...    custom hex colors
  --background HEX    background color
  --width N           width in pixels
  --height N          height in pixels'''

    static final List<String> INTS = ['width', 'height']

    int run(List<String> args) {
        String run = null
        final opts = [:] as Map<String, Object>
        for( int i = 0; i < args.size(); i++ ) {
            final arg = args[i]
            if( arg in ['-h', '--help'] ) {
                println USAGE
                return 0
            }
            if( !arg.startsWith('--') ) {
                run = arg
                continue
            }
            if( i + 1 >= args.size() )
                throw new IllegalArgumentException("Missing value for ${arg}")
            final key = arg.substring(2)
            final value = args[++i]
            opts[key] = key in INTS ? Integer.parseInt(value) : key == 'colors' ? value.tokenize(',') : value
        }

        final trace = opts.remove('trace') as String
        final dir = Paths.get((opts.remove('dir') ?: '.') as String)
        final record = trace ? null : RunReader.record(new HistoryFile(dir.resolve('.nextflow/history')), run)
        final tiles = trace
            ? RunReader.fromTrace(FileHelper.asPath(trace))
            : RunReader.fromCache(dir, record)
        if( !tiles )
            throw new IllegalArgumentException('The run has no tasks to paint')

        final name = trace ? trace.tokenize('/').last().replaceFirst(/\.[^.]+$/, '') : record.runName
        final config = new ArtsConfig(opts)
        final out = Paths.get(config.file ?: "art-${name}.svg".toString())
        // same seed as the live plugin for a local run, else same tasks same art
        final seed = record ? record.sessionId.mostSignificantBits : (long) tiles.hashCode()
        final bytes = Artist.paint(tiles, config, ArtsConfig.format(out.toString()), seed)
        if( out.toAbsolutePath().parent )
            Files.createDirectories(out.toAbsolutePath().parent)
        Files.write(out, bytes)
        println "nf-arts: painted ${tiles.size()} tasks to ${out}"
        return 0
    }
}
