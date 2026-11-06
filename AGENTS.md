# AGENTS.md

Notes for AI coding agents working on nf-arts. Read `CONTRIBUTING.md` for the workflow, which applies to agents too. User docs live in `docs/`; the README keeps only the sections the Nextflow Registry requires.

## Map

- `ArtsPlugin`: entry point, and the `render` command through `PluginExecAware`.
- `ArtsFactory`, `ArtsObserver`: the live plugin. The observer collects `TaskTile`s and calls `Artist.save` when the run ends.
- `RunReader`: finished runs, from the history and cache (`fromHistory`) or a trace file (`fromTrace`). Both return a `PaintedRun`.
- `PaintedRun`: tasks, caption (`Title`) and session. Its seed is the session, else a hash of plain task values.
- `Artist`: the style registry (`STYLES`), `save` (output path, format, write) and `paint` (canvas, caption band).
- `Run`: the model styles draw from: sorted tasks, processes, time span, log-scaled memory and duration. It moves cached tasks, and tasks with no time, to the start of the run.
- `Status`: `COMPLETED`, `FAILED`, `CACHED`, with opacity and `thin()`.
- `Style` implementations, one per file, with `Wing` and `ButterflyBody` for `ButterflyStyle`. `Canvas` with `SvgCanvas`, `PngCanvas`, `InsetCanvas` (the area above the caption) and `MirrorCanvas` (draws both sides of a vertical axis).
- `Palette`, `ArtsConfig` (the `arts` scope), `Title`, `Report` (parses the execution report next to a trace).

## Commands

```bash
mise run images       # render docs/images from megatest traces
prek run --all-files  # the commit hooks, as in CI
```

## Rules

- A refactor must not visibly change images. `StylesTest.SVG_HASHES` pins each style's SVG; if a hash changes, render that style before and after, compare the images by eye, and update the hash only for an intended change.
- Images rendered with a `-dev` build show `-dev` in their caption. `test-rnavar-*` are not regenerated: their runs are gone.
- README links and images are absolute GitHub URLs: the Nextflow Registry shows the README outside the repository.
- The art only uses the task fields listed in `docs/data.md`. Anything new that reaches the image, including caption text, must be added there first.

## Pitfalls

- `nextflow plugin` resolves plugins by bare ID from the registry. `mise run install` lists the build in a local repo and `mise exec --` sets `NXF_PLUGINS_TEST_REPOSITORY`, so call `nf-arts:render`, not `nf-arts@x.y.z:render`.
- Nextflow's console only shows loggers under `nextflow`, so use `@Slf4j(category = 'nextflow.arts')`.
- `PluginExecAware.exec` lost its `Launcher` parameter in 26.10. `ArtsPlugin` implements both signatures, so do not use the `PluginAbstractExec` trait.
- The cache cannot be read inside Gradle tests: its serializer needs `--add-opens`, which the `nextflow` launcher passes. Test history renders through the CLI.
- Enums hash by identity, which changes between JVM runs. Never seed from a hash that includes one.
- In Groovy, `'false' as boolean` is `true`. Text values go through `ArtsConfig`, which parses them.
- Nextflow's `CmdRun` cannot parse a history command line on its own: the launcher rewrites the arguments first. `Title.fromCommand` keeps its own small parser.
- nf-amazon refuses to read a whole large S3 object anonymously. `RunReader.report` streams only the start of the execution report.
- In shell loops, give `nextflow` `</dev/null`, or it reads the loop's input. In zsh, do not name a variable `path`: it is tied to `PATH`.
