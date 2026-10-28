# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions: [Semantic Versioning](https://semver.org/).

## v0.1.0 - unreleased

### Added

- Draw each run as an SVG or PNG when it ends, configured in the `arts` scope: `enabled`, `file`, `style`, `palette`, `colors`, `background`, `width`, `height`, `title`.
- Styles: `radial`, `hilbert`, `ridgeline`, `phyllotaxis`, `flowfield`, `mondrian`, `tree` and `ginkgo` ([#1](https://github.com/maxulysse/nf-arts/pull/1)).
- Palettes: `viridis`, `magma`, `nfcore`, `ocean`, `sunset`, `mono`, and the non-blending `mondrian`, default for the mondrian style.
- `nextflow plugin nf-arts:render` paints a finished run from the local history and cache, or from a trace file on a local or cloud path.
- Caption under the artwork: pipeline, version, run name and nf-arts version. It comes from the workflow metadata, the history, or the execution report next to a trace file.
- Line widths follow peak memory on a log scale, so one large task does not flatten the rest.
- `render --file` accepts cloud paths, like `arts.file` ([#4](https://github.com/maxulysse/nf-arts/pull/4)).
- CI: unit tests, a validation run and a `render` of it, on Java 17 and 21 with Nextflow 25.10 and 26.04. It runs on pushes to `main` and on pull requests ([#2](https://github.com/maxulysse/nf-arts/pull/2)).

### Changed

- Live, history and trace runs share one path to the image file and one rule for cached tasks ([#4](https://github.com/maxulysse/nf-arts/pull/4)).
- Task status is a `Status` enum that sets opacity and thinning; styles get one `Run` built by `Artist`, and choose their own default palette. `render --help` lists styles and palettes from the code ([#5](https://github.com/maxulysse/nf-arts/pull/5)).
- A trace without an execution report gets a new, stable seed ([#5](https://github.com/maxulysse/nf-arts/pull/5)).
