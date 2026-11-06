# Contributing to nf-arts

## Workflow

`main` is protected: every change goes through a pull request, and the CI jobs must pass before merging.

1. Branch from an up-to-date `main` (or fork the repo).
2. Make the change, with tests.
3. Add a line to `CHANGELOG.md` under the unreleased version, starting with a link to your PR, in PR number order.
4. Open a pull request against `main`. If it builds on another open PR, still target `main` and say "builds on #N" in the description.

CI runs on every pull request: the hooks, unit tests, a live run of the validation pipeline, and `render` checks, on Java 21 with Nextflow 25.10.0, the minimum version, and the latest release, edge included, where a failure of the validation checks does not block.

## Development

You need [mise](https://mise.jdx.dev), which installs the pinned Java from `mise.toml`, Nextflow 25.10 or later, and [prek](https://prek.j178.dev) for the Git hooks.

```bash
mise trust
mise install --locked
prek install        # format on commit, unit tests on push
mise run test       # unit tests
mise run validate   # the checks CI runs: the validation pipeline, then render
mise run install    # install this build, for Nextflow to use
mise exec -- nextflow plugin nf-arts:render --style ginkgo --file art.png
```

## Adding a style

1. Write a class implementing `Style` in its own file, e.g. `src/main/groovy/nfarts/SpiralStyle.groovy`. `draw` gets the `Run`, a `Canvas`, the `Palette` and a seeded `Random`. Use only that `Random`, so the same run gives the same image.
2. Register it in `Artist.STYLES`. The config check and `render --help` pick it up from there.
3. Add the name to the `style` description in `ArtsConfig`.
4. Add a test in `StylesTest`: the tests over `ArtsConfig.STYLES` cover PNG size, seeds, task order and single-task runs, so add one for the shapes your style draws, and a `StylesTest.SVG_HASHES` entry.
5. Add the style to `docs/styles.md` with its rnavar megatest image, and a row to the "Every style" table in `docs/gallery.md`. Images come from real runs only: add them to `mise-tasks/images`, which renders the runs already in those tables, not synthetic data.

## Releasing

Drop `-dev` from the version in `build.gradle` and `CHANGELOG.md`, add the codename to its `CHANGELOG.md` heading, run `mise run images`, copy `.env.example` to `.env`, set `NPR_API_KEY`, then run `mise run release`.

## Style

- Groovy with `@CompileStatic`, matching the existing code.
- No new dependencies: SVG is written as text, and PNG uses Java2D.
- Comments say why, not what.
- Keep prose short and concrete, in code, docs and pull requests.
