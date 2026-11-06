# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions: [Semantic Versioning](https://semver.org/).

## v1.1.0-dev

### Added

- [#17](https://github.com/maxulysse/nf-arts/pull/17): A `rainbow` palette.
- [#18](https://github.com/maxulysse/nf-arts/pull/18): An `iris` style: each task is a fibre of an eye's iris.
- [#19](https://github.com/maxulysse/nf-arts/pull/19): A website with a gallery, built from `docs/` and deployed to GitHub Pages.
- [#21](https://github.com/maxulysse/nf-arts/pull/21): A `butterfly` style: each process is a wing cell and each task a stroke across it, with wing size, tails and eyespots grown from the run.
- [#45](https://github.com/maxulysse/nf-arts/pull/45): A `hive` style: tasks sit on spokes and curves link each task to the one that finished before it, with `layout` and `spokes` options.

### Changed

- [#22](https://github.com/maxulysse/nf-arts/pull/22): Internal restructuring of `Run`, `Artist.paint` and the `butterfly` style. Images are unchanged.
- [#27](https://github.com/maxulysse/nf-arts/pull/27), [#29](https://github.com/maxulysse/nf-arts/pull/29): Smaller cleanups in the `mondrian`, `tree` and `iris` styles, and internal constants and helpers are private. Images are unchanged.
- [#28](https://github.com/maxulysse/nf-arts/pull/28): The docs build leaves no scratch files behind when it fails, and the CI actions are pinned to commits.
- [#30](https://github.com/maxulysse/nf-arts/pull/30): `CHANGELOG.md` entries start with their PR number and are ordered by it.
- [#32](https://github.com/maxulysse/nf-arts/pull/32): The docs promise the same SVG for the same tasks and nf-arts version, instead of the same image for a run.
- [#34](https://github.com/maxulysse/nf-arts/pull/34): Tests pin the SVG of every style, so a change to an image cannot pass unnoticed.
- [#35](https://github.com/maxulysse/nf-arts/pull/35): The caption shows the pipeline only if it looks like `org/name` or a URL, so local paths stay out of the image. `--pipeline` still sets it by hand.
- [#36](https://github.com/maxulysse/nf-arts/pull/36): Without `file`, nf-arts writes both `art-<runName>.svg` and `art-<runName>.png`, and a failed file leaves the other one. `file` still writes one file.
- [#41](https://github.com/maxulysse/nf-arts/pull/41): `make images` renders the megatest images from a recorded list of traces. The rnavar test images stay as rendered with 1.0.0, as their run is gone.
- [#42](https://github.com/maxulysse/nf-arts/pull/42): `mise` replaces `make`, with Java 21 pinned in `mise.toml`, and CI runs on Java 21 only. `prek` hooks run Nextflow lint, whitespace and end-of-file checks on commit and unit tests on push, and CI runs them.
- [#46](https://github.com/maxulysse/nf-arts/pull/46): `AGENTS.md`, the docs and code comments drop repeated text.

### Fixed

- [#20](https://github.com/maxulysse/nf-arts/pull/20): The website landing page uses the same width and code highlighting as the docs pages.
- [#23](https://github.com/maxulysse/nf-arts/pull/23): CI has one required check, `CI pass`, so changing the build matrix no longer needs a branch protection update.
- [#24](https://github.com/maxulysse/nf-arts/pull/24): Returning visitors on the `auto` theme no longer see a flash of the light theme.
- [#25](https://github.com/maxulysse/nf-arts/pull/25): Tasks that start at the same time, such as cached tasks, are drawn in a fixed order, so a resumed run gives the same image live and from its history.
- [#26](https://github.com/maxulysse/nf-arts/pull/26): Tasks with no start or submit time sit at the start of the run in every source, and a task name without a process loses its tag.
- [#31](https://github.com/maxulysse/nf-arts/pull/31): A value given to `nextflow run` options such as `-with-trace` or `-output-dir` before the pipeline no longer ends up in the caption of images rendered from the history.
- [#33](https://github.com/maxulysse/nf-arts/pull/33): A render that runs out of memory logs the usual warning instead of failing silently.
- [#37](https://github.com/maxulysse/nf-arts/pull/37): Warnings and the saved artwork path show on the console during a live run, not only in `.nextflow.log`.
- [#38](https://github.com/maxulysse/nf-arts/pull/38): The `mondrian` style draws every task. Tasks too small for the gap were skipped: 65 of 553 on the rnavar megatest, and all of them on a 100k-task run. This changes `mondrian` images.
- [#40](https://github.com/maxulysse/nf-arts/pull/40): A run with no revision no longer shows `null` in the caption from its execution report.

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
