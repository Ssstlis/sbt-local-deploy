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
- **Tasks renamed and scoped exclusively to `LocalDeploy`**: `localDeployDeploy` → `deploy`, `localDeployDeployInfo` → `deployInfo`, `localDeployStaleInstallations` → `staleInstallations`. Tasks are now accessible only via `LocalDeploy/deploy`, `LocalDeploy/deployInfo`, `LocalDeploy/staleInstallations`. Unscoped aliases removed.

### Documentation

- `CLAUDE.md`: corrected SBT version (was 1.12.3, now 1.12.5), removed incorrect `APP_VERSION` reference (version comes from `sbt-dynver`/git tags), added plugin file path, added scalafix config note, added documentation tracking instruction.
- `CHANGELOG.md`: created (this file).
- `MEMORY.md`: updated scalafix fix details and plugin file path.
- `README.md`: corrected task names (`deploy` → `localDeployDeploy`, etc.), corrected env variable names (`SDP_SCALA_APP_DEPLOY_PATH` → `LDP_APP_DEPLOY_PATH`, `SDP_SCALA_APP_DEPLOY_LINK_PATH` → `LDP_APP_DEPLOY_LINK_PATH`), added `LocalDeploy` scope usage.

## 1.0.4 and earlier

See git log for history.