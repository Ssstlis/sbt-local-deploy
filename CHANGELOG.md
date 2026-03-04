# Changelog

## Unreleased

### Build / Tooling

- **scalafix fixed** (`build.sbt`, `.scalafix.conf`): `sbt scalafixAll` was failing with "No rules requested to run".
  - Root cause: `.scalafix.conf` lacked `rules = [OrganizeImports]` — scalafix never inferred rules from bare config blocks.
  - Secondary causes: `semanticdbEnabled` not set and `-Ywarn-unused-import` missing (required for `removeUnused = true`).
  - Fixed in: this session (2026-03-04); prompted by upgrade `sbt-scalafix` 0.14.5 → 0.14.6 (commit `e86a48a`) which surfaced the error explicitly.
- **SBT updated** (commit `1b31aa5`): build tool bumped from 1.12.4 to 1.12.5.
- **Plugin file moved** (staged): `src/main/scala/LocalDeployPlugin.scala` → `src/main/scala/io/github/ssstlis/LocalDeployPlugin.scala`.
- **`build.sbt` restructured**: publication settings extracted to `publish.sbt`; remaining settings reorganised into `inThisBuild(List(...))` + `lazy val root = project.in(file(".")).settings(...)`.

### Documentation

- `CLAUDE.md`: corrected SBT version (was 1.12.3, now 1.12.5), removed incorrect `APP_VERSION` reference (version comes from `sbt-dynver`/git tags), added plugin file path, added scalafix config note, added documentation tracking instruction.
- `CHANGELOG.md`: created (this file).
- `MEMORY.md`: updated scalafix fix details and plugin file path.

## 1.0.4 and earlier

See git log for history.