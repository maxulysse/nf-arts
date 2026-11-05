# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions: [Semantic Versioning](https://semver.org/).

## v1.1.0-dev

### Added

- A `rainbow` palette ([#17](https://github.com/maxulysse/nf-arts/pull/17)).
- An `iris` style: each task is a fibre of an eye's iris ([#18](https://github.com/maxulysse/nf-arts/pull/18)).
- A website with a gallery, built from `docs/` and deployed to GitHub Pages ([#19](https://github.com/maxulysse/nf-arts/pull/19)).
- A `butterfly` style: each process is a wing cell and each task a stroke across it, with wing size, tails and eyespots grown from the run ([#21](https://github.com/maxulysse/nf-arts/pull/21)).

### Changed

- The docs build leaves no scratch files behind when it fails, and the CI actions are pinned to commits ([#28](https://github.com/maxulysse/nf-arts/pull/28)).
- Internal restructuring of `Run`, `Artist.paint` and the `butterfly` style. Images are unchanged ([#22](https://github.com/maxulysse/nf-arts/pull/22)).
- Smaller cleanups in the `mondrian`, `tree` and `iris` styles, and internal constants and helpers are private. Images are unchanged ([#27](https://github.com/maxulysse/nf-arts/pull/27)).

### Fixed

- Returning visitors on the `auto` theme no longer see a flash of the light theme ([#24](https://github.com/maxulysse/nf-arts/pull/24)).
- The website landing page uses the same width and code highlighting as the docs pages ([#20](https://github.com/maxulysse/nf-arts/pull/20)).
- Tasks that start at the same time, such as cached tasks, are drawn in a fixed order, so a resumed run gives the same image live and from its history ([#25](https://github.com/maxulysse/nf-arts/pull/25)).
- CI has one required check, `CI pass`, so changing the build matrix no longer needs a branch protection update ([#23](https://github.com/maxulysse/nf-arts/pull/23)).
- Tasks with no start or submit time sit at the start of the run in every source, and a task name without a process loses its tag ([#26](https://github.com/maxulysse/nf-arts/pull/26)).

## v1.0.0 - Tournesols

First release.

### Added

- Draws each run as an SVG or PNG when it ends, configured in the `arts` scope: `enabled`, `file`, `style`, `palette`, `colors`, `background`, `width`, `height`, `title`.
- Eight styles: `radial`, `hilbert`, `ridgeline`, `phyllotaxis`, `flowfield`, `mondrian`, `tree` and `ginkgo` ([#1](https://github.com/maxulysse/nf-arts/pull/1)).
- Seven palettes: `viridis`, `magma`, `nfcore`, `ocean`, `sunset`, `mono`, and the non-blending `mondrian`, default for the mondrian style.
- Line widths follow peak memory on a log scale, so one large task does not flatten the rest.
- A caption under the artwork: pipeline, version, run name and nf-arts version. `title = false` turns it off.
- `nextflow plugin nf-arts:render` paints a finished run from the local history and cache, or from a trace file on a local or cloud path, with the caption from the execution report next to it. It writes to cloud paths, rejects unknown options and exits with code 1 on errors ([#4](https://github.com/maxulysse/nf-arts/pull/4), [#8](https://github.com/maxulysse/nf-arts/pull/8), [#13](https://github.com/maxulysse/nf-arts/pull/13)).
- Clear errors for bad `arts` values at the start of a run, empty histories and cleaned caches; partial trace rows are skipped ([#13](https://github.com/maxulysse/nf-arts/pull/13)).
- Logo ([#11](https://github.com/maxulysse/nf-arts/pull/11)).
- Docs in `docs/`, including what data goes into the art ([#12](https://github.com/maxulysse/nf-arts/pull/12), [#14](https://github.com/maxulysse/nf-arts/pull/14)), `CONTRIBUTING.md` and `AGENTS.md` ([#10](https://github.com/maxulysse/nf-arts/pull/10)).
- CI on Java 17 and 21 with Nextflow 25.10, 26.04 and 26.10: unit tests, a live validation run, and `render` from the history, a trace file and an S3 megatest ([#2](https://github.com/maxulysse/nf-arts/pull/2), [#9](https://github.com/maxulysse/nf-arts/pull/9)).
