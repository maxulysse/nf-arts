# nf-arts

## Summary

`nf-arts` is a Nextflow plugin that turns a run into generative art.

It records each task's process, start time, duration, peak memory and status, and draws them as an SVG or PNG when the run ends. The session ID seeds the randomness, so a run always gives the same image.

| Radial | Hilbert |
| :---: | :---: |
| ![radial](docs/images/test-rnavar-radial.png) | ![hilbert](docs/images/test-rnavar-hilbert.png) |

Both from `nf-core/rnavar -profile test,docker` (43 tasks).

## Get Started

Add the plugin to `nextflow.config`:

```groovy
plugins {
    id 'nf-arts@0.1.0'
}
```

At the end of the run it writes `art-<runName>.svg` to the launch directory.

`-plugins` and a `plugins {}` block in a `-c` config both replace a pipeline's plugins. To add nf-arts to a pipeline you don't edit, list the pipeline's plugins too:

```bash
nextflow run nf-core/rnavar -profile test,docker --outdir results \
    -plugins nf-core-utils@0.4.0,nf-schema@2.6.1,nf-arts@0.1.0
```

### Paint a finished run

`render` paints a finished run from the local history and cache, like [nf-diff](https://github.com/mribeirodantas/nf-diff), or from a trace file:

```bash
# last run here, or pass a run name or session ID
nextflow plugin nf-arts:render --style hilbert --file art.png

# a trace file, e.g. an nf-core megatest on S3 (public buckets need no AWS credentials)
nextflow plugin nf-arts:render \
    --trace s3://nf-core-awsmegatests/rnavar/results-ea2c7b6286f5ae18f5b51dc0af6b5330ecd6a13a/pipeline_info/execution_trace_2025-12-05_11-13-40.txt \
    --file rnavar.png --palette nfcore
```

It takes the `arts` options as flags (`--style`, `--palette`, `--colors a,b,c`, `--background`, `--width`, `--height`, `--file`), plus `--dir` for another project directory. A local run gets the same seed as the live plugin, so the same image.

The caption comes from the history, or from the `execution_report_*.html` next to the trace, which nf-core pipelines publish in `pipeline_info/`. Override it with `--pipeline`, `--revision` and `--run-name`, or drop it with `--title false`.

## Examples

```groovy
arts {
    file    = "${params.outdir}/pipeline_info/art.png" // .svg or .png
    style   = 'hilbert'                                 // see Styles below
    palette = 'magma'
    width   = 1600
    height  = 1600
}
```

Custom colors:

```groovy
arts {
    colors     = ['#ff006e', '#8338ec', '#3a86ff']
    background = '#ffffff'
}
```

<img src="docs/images/test-rnavar-custom.svg" width="300" alt="radial art of the rnavar test profile with custom colors, as SVG">

### Options

| Option | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Draw the artwork when the run ends. |
| `file` | `art-<runName>.svg` | Output file. `.svg` or `.png` sets the format. Cloud paths work. Overwrites an existing file. |
| `style` | `radial` | `radial`, `hilbert`, `ridgeline`, `phyllotaxis`, `flowfield`, `mondrian`, `tree` or `ginkgo`. |
| `palette` | `viridis` | `viridis`, `magma`, `nfcore`, `ocean`, `sunset`, `mono` or `mondrian` (default for the mondrian style). |
| `colors` | | List of hex colors. Overrides `palette`. |
| `background` | from palette | Hex background color. |
| `width`, `height` | `1200` | Size in pixels (16 to 10000). |
| `title` | `true` | Caption: pipeline (`manifest.name`), version, run name and nf-arts version. |

### Styles

**radial**: each process is a ring, each task an arc on it. Angle is time, clockwise from 12 o'clock. Thickness is peak memory.

**hilbert**: tasks run end to end along a [Hilbert curve](https://en.wikipedia.org/wiki/Hilbert_curve) in start order, each taking a share proportional to its duration. Thickness is peak memory. More tasks make a finer curve. The seed picks one of its 8 orientations.

**ridgeline**: one ridge per process, top to bottom, time left to right. Ridge height is how many of the process's tasks were running, relative to its busiest moment.

**phyllotaxis**: tasks are sunflower seeds on a golden-angle spiral, in start order. Dot size is peak memory.

**flowfield**: each task is a brush stroke drifting through a seeded flow field. It starts at the task's start order (x) in its process's row (y). Length is duration, width is peak memory.

**mondrian**: one block per process, split into one block per task, area proportional to duration. Opacity is peak memory; failed tasks are hollow. The default `mondrian` palette cycles off-white, red, blue and yellow on black lines, without blending.

**tree**: the run is the trunk, each process a branch, in start order from the bottom. Branch length is how long the process was active. Each task is a twig with a leaf: twig length is duration, leaf size is peak memory. Failed tasks have no leaf.

**ginkgo**: the run is one ginkgo leaf. Each task is a vein fanning out from the stem and forking near its tip. Angle is start time, left to right across the fan, length is duration, width is peak memory.

In every style, color is the process, cached tasks are faded and failed tasks are thin or empty.

### Gallery

nf-core megatests, rendered from their trace files on `s3://nf-core-awsmegatests`:

| Pipeline | Radial | Hilbert |
| --- | :---: | :---: |
| rnavar, 553 tasks, `nfcore` | ![](docs/images/megatest-rnavar-radial.png) | ![](docs/images/megatest-rnavar-hilbert.png) |
| sarek, 2314 tasks, `magma` | ![](docs/images/megatest-sarek-radial.png) | ![](docs/images/megatest-sarek-hilbert.png) |
| rnaseq, 332 tasks, `viridis` | ![](docs/images/megatest-rnaseq-radial.png) | ![](docs/images/megatest-rnaseq-hilbert.png) |
| mag, 3207 tasks, `ocean` | ![](docs/images/megatest-mag-radial.png) | ![](docs/images/megatest-mag-hilbert.png) |
| raredisease, 990 tasks, `sunset` | ![](docs/images/megatest-raredisease-radial.png) | ![](docs/images/megatest-raredisease-hilbert.png) |
| viralrecon, 3239 tasks, `mono` | ![](docs/images/megatest-viralrecon-radial.png) | ![](docs/images/megatest-viralrecon-hilbert.png) |

The other styles, on the rnavar test profile and the rnavar and sarek megatests:

| Style | rnavar test, 43 tasks | rnavar megatest, 553 tasks | sarek megatest, 2314 tasks |
| --- | :---: | :---: | :---: |
| ridgeline | ![](docs/images/test-rnavar-ridgeline.png) | ![](docs/images/megatest-rnavar-ridgeline.png) | ![](docs/images/megatest-sarek-ridgeline.png) |
| phyllotaxis | ![](docs/images/test-rnavar-phyllotaxis.png) | ![](docs/images/megatest-rnavar-phyllotaxis.png) | ![](docs/images/megatest-sarek-phyllotaxis.png) |
| flowfield | ![](docs/images/test-rnavar-flowfield.png) | ![](docs/images/megatest-rnavar-flowfield.png) | ![](docs/images/megatest-sarek-flowfield.png) |
| mondrian | ![](docs/images/test-rnavar-mondrian.png) | ![](docs/images/megatest-rnavar-mondrian.png) | ![](docs/images/megatest-sarek-mondrian.png) |
| tree | ![](docs/images/test-rnavar-tree.png) | ![](docs/images/megatest-rnavar-tree.png) | ![](docs/images/megatest-sarek-tree.png) |
| ginkgo | ![](docs/images/test-rnavar-ginkgo.png) | ![](docs/images/megatest-rnavar-ginkgo.png) | ![](docs/images/megatest-sarek-ginkgo.png) |

## Plugin development

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the workflow and how to add a style.

```bash
make test       # unit tests
make install    # install into ~/.nextflow/plugins
nextflow run validation/ -plugins nf-arts@0.1.0
```

`nextflow plugin` finds plugins by bare ID, in the registry. To try `render` on an unpublished build:

```bash
make dev-repo
export NXF_PLUGINS_TEST_REPOSITORY="file://$PWD/build/plugin-repo/plugins.json"
nextflow plugin nf-arts:render
```

To publish to the Nextflow Registry: copy `.env.example` to `.env`, set `NPR_API_KEY`, run `make release`.

## License

MIT, see [`LICENSE`](LICENSE).
