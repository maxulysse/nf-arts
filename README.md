# nf-arts

## Summary

`nf-arts` is a Nextflow plugin that turns each pipeline run into a piece of generative art.

The plugin records every task of the run: process, start time, duration, peak memory and status. When the run ends, it draws those tasks as an SVG or PNG image. The run's session ID is the random seed, so the same run always gives the same image.

| Radial | Hilbert |
| :---: | :---: |
| ![radial](docs/images/test-rnavar-radial.png) | ![hilbert](docs/images/test-rnavar-hilbert.png) |

Both images come from a real run of `nf-core/rnavar -profile test,docker` (43 tasks).

## Get Started

Enable the plugin in your pipeline's `nextflow.config`:

```groovy
plugins {
    id 'nf-arts@0.1.0'
}
```

When the run ends, the plugin writes `art-<runName>.svg` to the launch directory.

To add it to a pipeline you don't edit, such as an nf-core pipeline, repeat the pipeline's own plugins. Both `-plugins` and a `plugins {}` block in a `-c` config replace the pipeline's plugins instead of adding to them:

```bash
nextflow run nf-core/rnavar -profile test,docker --outdir results \
    -plugins nf-core-utils@0.4.0,nf-schema@2.6.1,nf-arts@0.1.0
```

### Paint a finished run

`render` paints a run that already finished, from the local history and cache (like [nf-diff](https://github.com/mribeirodantas/nf-diff)) or from an execution trace file:

```bash
# last run in this directory, or pass a run name or session id
nextflow plugin nf-arts:render --style hilbert --file art.png

# any trace file, e.g. an nf-core megatest on S3 (no AWS credentials needed for public buckets)
nextflow plugin nf-arts:render \
    --trace s3://nf-core-awsmegatests/rnavar/results-ea2c7b6286f5ae18f5b51dc0af6b5330ecd6a13a/pipeline_info/execution_trace_2025-12-05_11-13-40.txt \
    --file rnavar.png --palette nfcore
```

It takes the same options as the `arts` scope (`--style`, `--palette`, `--colors a,b,c`, `--background`, `--width`, `--height`, `--file`), plus `--dir` for another project directory. For a local run it uses the same seed as the live plugin, so it gives the same image.

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
| `enabled` | `true` | Create the artwork at the end of the run. |
| `file` | `art-<runName>.svg` | Output file. The `.svg` or `.png` extension sets the format. Cloud paths work too. An existing file is overwritten. |
| `style` | `radial` | `radial`, `hilbert`, `ridgeline`, `phyllotaxis`, `flowfield`, `mondrian` or `tree`. |
| `palette` | `viridis` | `viridis`, `magma`, `nfcore`, `ocean`, `sunset` or `mono`. |
| `colors` | | List of hex colors. Overrides `palette`. |
| `background` | from palette | Hex background color. |
| `width`, `height` | `1200` | Size in pixels (16 to 10000). |

### Styles

**radial**: each process is a ring and each task is an arc on its ring. The angle is time: 12 o'clock is the start of the run, and the run goes clockwise. Arc thickness is peak memory.

**hilbert**: tasks are laid end to end, in start order, along a [Hilbert curve](https://en.wikipedia.org/wiki/Hilbert_curve). Each task gets a share of the curve proportional to its duration. Line thickness is peak memory. The curve's resolution grows with the number of tasks. The seed picks one of the 8 orientations of the square.

**ridgeline**: one ridge per process, stacked top to bottom, with time running left to right. The height of a ridge is how many tasks of that process were running, relative to that process's busiest moment.

**phyllotaxis**: tasks are seeds of a sunflower, placed on a golden-angle spiral in start order. Dot size is peak memory.

**flowfield**: each task is a brush stroke drifting through a flow field seeded by the run. A stroke starts at the task's place in start order (x), in its process's row (y). Stroke length is duration and width is peak memory.

**mondrian**: the canvas is split into one block per process, then one block per task, with area proportional to duration. Opacity is peak memory and failed tasks are hollow.

**tree**: the run is the trunk, and each process is a branch, in start order from the bottom up. Branch length is how long the process was active. Each task is a twig with a leaf: twig length is duration and leaf size is peak memory. Failed tasks have no leaf.

In all styles, color is the process, cached tasks are faded and failed tasks are thin or empty.

### Gallery

nf-core megatests, painted with `render` from the trace files on `s3://nf-core-awsmegatests`:

| Pipeline | Radial | Hilbert |
| --- | :---: | :---: |
| rnavar, 553 tasks, `nfcore` | ![](docs/images/megatest-rnavar-radial.png) | ![](docs/images/megatest-rnavar-hilbert.png) |
| sarek, 4645 tasks (3273 cached), `magma` | ![](docs/images/megatest-sarek-radial.png) | ![](docs/images/megatest-sarek-hilbert.png) |
| rnaseq, 332 tasks, `viridis` | ![](docs/images/megatest-rnaseq-radial.png) | ![](docs/images/megatest-rnaseq-hilbert.png) |
| mag, 3207 tasks, `ocean` | ![](docs/images/megatest-mag-radial.png) | ![](docs/images/megatest-mag-hilbert.png) |
| raredisease, 990 tasks, `sunset` | ![](docs/images/megatest-raredisease-radial.png) | ![](docs/images/megatest-raredisease-hilbert.png) |
| viralrecon, 3239 tasks, `mono` | ![](docs/images/megatest-viralrecon-radial.png) | ![](docs/images/megatest-viralrecon-hilbert.png) |

The other styles on the rnavar test profile, the rnavar megatest and the sarek megatest:

| Style | rnavar test, 43 tasks | rnavar megatest, 553 tasks | sarek megatest, 4645 tasks |
| --- | :---: | :---: | :---: |
| ridgeline | ![](docs/images/test-rnavar-ridgeline.png) | ![](docs/images/megatest-rnavar-ridgeline.png) | ![](docs/images/megatest-sarek-ridgeline.png) |
| phyllotaxis | ![](docs/images/test-rnavar-phyllotaxis.png) | ![](docs/images/megatest-rnavar-phyllotaxis.png) | ![](docs/images/megatest-sarek-phyllotaxis.png) |
| flowfield | ![](docs/images/test-rnavar-flowfield.png) | ![](docs/images/megatest-rnavar-flowfield.png) | ![](docs/images/megatest-sarek-flowfield.png) |
| mondrian | ![](docs/images/test-rnavar-mondrian.png) | ![](docs/images/megatest-rnavar-mondrian.png) | ![](docs/images/megatest-sarek-mondrian.png) |
| tree | ![](docs/images/test-rnavar-tree.png) | ![](docs/images/megatest-rnavar-tree.png) | ![](docs/images/megatest-sarek-tree.png) |

## Plugin development

```bash
make test       # unit tests
make install    # install into ~/.nextflow/plugins
nextflow run validation/ -plugins nf-arts@0.1.0
```

`nextflow plugin` only finds a plugin by its bare id, and resolves it from the registry. To try `render` on an unpublished build:

```bash
make dev-repo
export NXF_PLUGINS_TEST_REPOSITORY="file://$PWD/build/plugin-repo/plugins.json"
nextflow plugin nf-arts:render
```

To publish to the Nextflow Registry, copy `.env.example` to `.env`, set `NPR_API_KEY`, then run `make release`.

## License

MIT. See the [`LICENSE`](LICENSE) file for details.
