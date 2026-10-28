# Contributing to nf-arts

## Workflow

`main` is protected: every change goes through a pull request, and the CI jobs must pass before merging.

1. Branch from an up-to-date `main` (or fork the repo).
2. Make the change, with tests.
3. Add a line to `CHANGELOG.md` under the unreleased version, with a link to your PR.
4. Open a pull request against `main`. If it builds on another open PR, still target `main` and say "builds on #N" in the description.

CI runs on every pull request: unit tests, a live run of the validation pipeline, and `render` checks, on Java 17 and 21 with Nextflow 25.10, 26.04 and 26.10.

## Development

You need Java 17 or later and Nextflow 25.10 or later.

```bash
make test       # unit tests
make install    # install the plugin into ~/.nextflow/plugins
nextflow run validation/ -plugins nf-arts@$(make -s version)
```

`nextflow plugin` only finds plugins in the registry. To try `render` on your build:

```bash
make dev-repo
export NXF_PLUGINS_TEST_REPOSITORY="file://$PWD/build/plugin-repo/plugins.json"
nextflow plugin nf-arts:render --style ginkgo --file art.png
```

## Adding a style

1. Write a class implementing `Style` in its own file, e.g. `src/main/groovy/nfarts/SpiralStyle.groovy`. `draw` gets the `Run`, a `Canvas`, the `Palette` and a seeded `Random`. Use only that `Random`, so the same run gives the same image.
2. Register it in `Artist.STYLES`. The config check and `render --help` pick it up from there.
3. Add the name to the `style` description in `ArtsConfig`.
4. Add a test in `StylesTest`: the tests over `ArtsConfig.STYLES` cover PNG size, seeds and empty runs, so add one for the shapes your style draws.
5. Add the style to `docs/styles.md` with its rnavar megatest image, and a row to the "Every style" table in `docs/gallery.md`. Images come from real runs only: render them with `render` from the runs already in those tables, not from synthetic data.

## Releasing

Set the date on the version in `CHANGELOG.md`, copy `.env.example` to `.env`, set `NPR_API_KEY`, then run `make release`.

## Style

- Groovy with `@CompileStatic`, matching the existing code.
- No new dependencies: SVG is written as text, and PNG uses Java2D.
- Comments say why, not what.
- Keep prose short and concrete, in code, docs and pull requests.
