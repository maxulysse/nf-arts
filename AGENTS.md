# AGENTS.md

Notes for AI coding agents working on nf-arts. Read `CONTRIBUTING.md` for the workflow, which applies to agents too. User docs live in `docs/`; the README keeps only the sections the Nextflow Registry requires.

## Map

- `ArtsPlugin`: entry point, and the `render` command through `PluginExecAware`.
- `ArtsFactory`, `ArtsObserver`: the live plugin. The observer collects `TaskTile`s and calls `Artist.save` when the run ends.
- `RunReader`: finished runs, from the history and cache (`fromHistory`) or a trace file (`fromTrace`). Both return a `PaintedRun`.
- `PaintedRun`: tasks, caption (`Title`) and session. Its seed is the session, else a hash of plain task values.
- `Artist`: the style registry (`STYLES`), `save` (output path, format, write) and `paint` (canvas, caption band).
- `Run`: the model styles draw from: sorted tasks, processes, time span, log-scaled memory and duration. It moves cached tasks to the start of the run.
- `Status`: `COMPLETED`, `FAILED`, `CACHED`, with opacity and `thin()`.
- `Style` implementations, one per file. `Canvas` with `SvgCanvas`, `PngCanvas` and `InsetCanvas` (the area above the caption).
- `Palette`, `ArtsConfig` (the `arts` scope), `Title`, `Report` (parses the execution report next to a trace).

## Commands

```bash
make test                    # unit tests
make install                 # install into ~/.nextflow/plugins
make dev-repo                # local plugin repo for `nextflow plugin`
make -s version              # the version from build.gradle
```

## Rules

- Never push to `main`. Branch, open a PR against `main`, and let a maintainer merge. Stacked PRs also target `main`.
- Every PR adds a `CHANGELOG.md` line.
- A refactor must not change images. Render every style from fixed sources before and after, then compare the files byte for byte.
- Images in `docs/` come from real runs only.
- The art only uses the task fields listed in `docs/data.md`. Anything new that reaches the image, including caption text, must be added there first.
- The required checks in branch protection list the CI jobs by name. If you change the matrix, the protection must change too, after the PR merges.

## Pitfalls

- `nextflow plugin` resolves plugins by bare ID from the registry. Use `make dev-repo` plus `NXF_PLUGINS_TEST_REPOSITORY`, and call `nf-arts:render`, not `nf-arts@x.y.z:render`.
- `nextflow plugin` ignores the returned exit code, so `ArtsPlugin.exec` calls `System.exit` on failure.
- `PluginExecAware.exec` lost its `Launcher` parameter in 26.10. `ArtsPlugin` implements both signatures, so do not use the `PluginAbstractExec` trait.
- The cache cannot be read inside Gradle tests: its serializer needs `--add-opens`, which the `nextflow` launcher passes. Test history renders through the CLI.
- `-plugins` and a `plugins {}` block in a `-c` config replace a pipeline's plugins. Repeat the pipeline's own plugins when adding nf-arts.
- Enums hash by identity, which changes between JVM runs. Never seed from a hash that includes one.
- In Groovy, `'false' as boolean` is `true`. Text values go through `ArtsConfig`, which parses them.
- Nextflow's `CmdRun` cannot parse a history command line on its own: the launcher rewrites the arguments first. `Title.fromCommand` keeps its own small parser.
- nf-amazon refuses to read a whole large S3 object anonymously. `RunReader.report` streams only the start of the execution report.
- In shell loops, give `nextflow` `</dev/null`, or it reads the loop's input. In zsh, do not name a variable `path`: it is tied to `PATH`.
