# CLAUDE.md

Instructions for Claude Code when working on this repository.

## General rules

- Every code change that affects behavior, configuration, public API, environment variables, task names, or usage must be reflected in `README.md`. Keep the README in sync with the code at all times.
- Do not add comments, docstrings, or type annotations to code you did not change.
- Do not introduce abstractions or helpers for one-off operations.
- Do not add error handling for scenarios that cannot happen given the existing guarantees of SBT and the JVM standard library.

## Code style

- Format code with Scalafmt before committing: `sbt fmt`
- Lint with Scalafix: it runs as part of the build checks
- Max line length: 120 characters (enforced by Scalafmt)

## README sync requirement

When making any of the following changes, update `README.md` accordingly:

- Adding, removing, or renaming SBT tasks
- Changing environment variable names or their behavior
- Changing argument conventions for tasks
- Changing deployment directory naming format
- Adding or removing plugin dependencies
- Changing the minimum SBT or Java version
- Changing installation/enablement instructions

The README is the primary user-facing documentation. It must never describe behavior that no longer exists, and must never omit behavior that does exist.

## Build

- SBT version: 1.12.3 (see `project/build.properties`)
- The plugin requires `sbt-native-packager` to be present in consumer projects
- Version is resolved from the `APP_VERSION` environment variable, falling back to `<git-commit>-SNAPSHOT`

## Commit hygiene

- Use clear, descriptive commit messages
- Do not commit formatting-only changes mixed with logic changes