# Paint a finished run

`nextflow plugin nf-arts:render` paints a run that already finished, from the local history and cache, or from a trace file.

```bash
# the last run in this directory, or pass a run name or session ID
nextflow plugin nf-arts:render --style hilbert --file art.png

# a trace file: local, s3://, gs://, az:// or http(s)://
nextflow plugin nf-arts:render \
    --trace s3://nf-core-awsmegatests/rnavar/results-ea2c7b6286f5ae18f5b51dc0af6b5330ecd6a13a/pipeline_info/execution_trace_2025-12-05_11-13-40.txt \
    --file rnavar.png --palette nfcore
```

Public buckets such as `nf-core-awsmegatests` need no AWS credentials.

## Options

The [`arts` options](configuration.md#the-arts-scope) as flags, except `enabled`: `--file`, `--style`, `--palette`, `--colors a,b,c`, `--background`, `--width`, `--height` and `--title false`. Plus:

| Option | Description |
| --- | --- |
| `--trace FILE` | Read a trace file instead of the history. |
| `--dir DIR` | The project directory with the `.nextflow` history. Default: `.` |
| `--pipeline`, `--revision`, `--run-name` | Set the caption by hand. |

`--help` lists them all. An unknown option is an error, and so is any failure: `render` then exits with code 1.

## Caption and seed

From the history, the caption comes from the command line and the run name. From a trace, it comes from the `execution_report_*.html` next to it, which nf-core pipelines publish in `pipeline_info/`. Without a report, set it with `--pipeline`, `--revision` and `--run-name`, or drop it with `--title false`. [What goes into the art](data.md#the-caption) lists each source.

A run from the history, or a trace with its report, gets the same seed as the live plugin, so the same image.
