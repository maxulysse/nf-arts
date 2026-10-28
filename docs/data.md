# What goes into the art

nf-arts draws from a few numbers per task. Nothing about your data, samples, inputs or outputs reaches the image. The caption is the one place with text, and you can turn it off.

## Per task

| Field | Read from | Used for |
| --- | --- | --- |
| Process name | the process, without the task tag | Grouping tasks and picking colors, in order of first start. Never written into the image. |
| Start time | `start`, else `submit` | Position in time, relative to the first task of the run. |
| Duration | `realtime`, else `duration` | Length of the shape. In `iris`, also the ring's shape and the highlight's position. |
| Peak memory | `peak_rss` | Width or size of the shape, on a log scale. |
| Status | `status`, or cached | Opacity; failed tasks are thinner, smaller, hollow or leafless. |

Everything else is ignored, in live runs, history renders and trace files alike: task names and tags (where sample IDs usually are), inputs, outputs, work directories, scripts, exit codes, CPU usage, I/O, hashes, containers and parameters.

## What someone can read from an image

- How many tasks ran (one shape per task) and how many processes (one ring, row, branch or color each).
- The order processes started in, relative timing, durations and peak memory, roughly. The SVG keeps coordinates to 0.01 px, so these are more precise in an SVG than in a PNG.
- Which tasks failed or were cached.

Not process names, absolute dates or anything about the data.

## The caption

The caption shows the pipeline name, its version, the run name and the nf-arts version:

| Source | Pipeline | Version | Run name |
| --- | --- | --- | --- |
| Live run | `manifest.name`, else the project name | `manifest.version`, else the revision | run name |
| `render` from the history | what followed `nextflow run` | `-r` | run name |
| `render --trace` | repository in the execution report, else its command line | revision in the report | run name in the report |

From the history, "what followed `nextflow run`" can be a local path, such as `/home/me/secret-project/main.nf`. Before sharing an image, check its caption, or drop it:

```groovy
arts {
    title = false
}
```

or `--title false` with `render`. `--pipeline`, `--revision` and `--run-name` set the caption by hand.

## Randomness

The session ID seeds the random choices, such as jitter and orientation, so a run always gives the same image. Without a session, for a trace file with no execution report, a hash of the task values above is the seed. Neither is written into the image.

## The files

- SVG: shapes and the caption text, nothing else.
- PNG: pixels only, with no metadata chunks.
