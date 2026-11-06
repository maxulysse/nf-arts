# What goes into the art

nf-arts draws from a few numbers per task. Nothing about your data, samples, inputs or outputs reaches the image. The caption is the one place with text, and you can turn it off.

## Per task

| Field | Read from | Used for |
| --- | --- | --- |
| Process name | the process, without the task tag | Grouping tasks and picking colors, in start order. In `hive`, the order also sets each process's place on the spokes. In `butterfly`, a process's task count also sets its cell's width and its margin spot. Never written into the image. |
| Start time | `start`, else `submit` | Position in time, relative to the first task of the run. A task with neither sits at the start of the run. In `hive`, also which task a curve links to. |
| Duration | `realtime`, else `duration` | Length of the shape. In `iris`, also the ring's shape and the highlight's position. In `butterfly`, also wing size, outline and tails. In `hive`, also which task a curve links to. |
| Peak memory | `peak_rss` | Width or size of the shape, on a log scale. In `butterfly`, also where the eyespots sit. In `hive`, also the position along a spoke. |
| Status | `status`, or cached | Opacity; failed tasks are thinner, smaller, hollow or leafless, or bite a butterfly's wing. |

Everything else is ignored, in live runs, history renders and trace files alike: task names and tags (where sample IDs usually are), inputs, outputs, work directories, scripts, exit codes, CPU usage, I/O, hashes, containers and parameters.

## What someone can read from an image

- How many tasks ran (one shape per task; `butterfly` mirrors each, and a run with a single process fills both wings with it, so four shapes per task) and how many processes (one ring, row, branch, spoke segment or color each).
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

The pipeline shows only if it looks like `org/name` or an http(s) URL with no credentials, so that local paths such as `/home/me/secret-project/main.nf` stay out of the image. A `manifest.name` with no slash, such as `rnavar`, is dropped too. `--pipeline` sets it by hand, as is.

A relative folder such as `secret/project` looks like `org/name`, so it still shows. Before sharing an image, check its caption, or drop it:

```groovy
arts {
    title = false
}
```

or `--title false` with `render`.

## Randomness

The session ID seeds the random choices, such as jitter and orientation. The same tasks and the same nf-arts version give the same SVG, so `render` from the history, or from a trace with its report, matches the live run apart from the caption. A PNG can differ in its caption text, which uses a font from the host. A resumed run keeps its session but its tasks can differ, and styles can change between versions, so either can change the image. Without a session, for a trace file with no execution report, a hash of the task values above is the seed. Neither is written into the image.

## The files

- SVG: shapes and the caption text, nothing else.
- PNG: pixels only, with no metadata chunks.
