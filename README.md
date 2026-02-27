# sbt-local-deploy

An SBT plugin for local deployment of Scala applications. It packages your application into a versioned directory, manages symlinks for binary scripts, and helps track stale (old) installations.

[![Continuous Integration](https://github.com/Ssstlis/sbt-local-deploy/actions/workflows/ci.yml/badge.svg)](https://github.com/Ssstlis/sbt-local-deploy/actions/workflows/ci.yml)
[![Semgrep](https://github.com/Ssstlis/sbt-local-deploy/actions/workflows/semgrep.yml/badge.svg)](https://github.com/Ssstlis/sbt-local-deploy/actions/workflows/semgrep.yml)
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/io.github.ssstlis/sbt-local-deploy_2.12_1.0/badge.svg)](https://maven-badges.sml.io/sonatype-central/io.github.ssstlis/sbt-local-deploy_2.12_1.0)

## Features

- **Per-app subdirectory** — Each application gets its own folder (`<deployPath>/<name>/`); all releases live inside it
- **Versioned deployments** — Each deploy creates a timestamped and commit-tagged directory (`<name>-<version>-<timestamp>-<commit>`)
- **`current` symlink** — A `current` symlink inside the app folder always points to the latest release
- **Shared `logs` and `conf`** — `<deployPath>/<name>/logs` and `<deployPath>/<name>/conf` are created once and symlinked (as absolute paths) into every release directory; place `application.conf`, `logback.xml`, and similar files there — they will be picked up by every release automatically
- **`conf/` on the classpath** — The shared `conf/` directory is automatically prepended to the application's JVM classpath; Typesafe Config and Logback find `application.conf` and `logback.xml` there without any extra `-D` flags
- **JVM args from file** — If `<deployPath>/<name>/conf/jvm-args` exists, each non-empty, non-comment line is passed to the JVM at startup; update heap size or GC flags without rebuilding the application
- **Binary symlink management** — Automatically creates symlinks for binary scripts in a configurable link directory
- **Stale installation tracking** — Lists deployments older than 30 days to help clean up disk space
- **Dry-run preview** — Inspect planned deployment paths before executing
- **CI/CD friendly** — All paths configurable via environment variables

## Requirements

- SBT 1.x
- [sbt-native-packager](https://github.com/sbt/sbt-native-packager) with `JavaAppPackaging` enabled in the consumer project — the plugin requires `JavaAppPackaging` and uses its bash start script infrastructure

## Installation

Add the plugin to your `project/plugins.sbt`:

```scala
addSbtPlugin("io.github.ssstlis" % "sbt-local-deploy" % "<version>")
```

Enable the plugin in your `build.sbt`:

```scala
enablePlugins(JavaAppPackaging, LocalDeployPlugin)
```

## Usage

### Deploy

Stage and deploy your application:

```
sbt deploy
```

Optionally pass paths as arguments:

```
sbt "deploy /opt/myapp/releases /usr/local/bin"
```

Or set them via environment variables:

| Variable                      | Description                              |
|-------------------------------|------------------------------------------|
| `SDP_SCALA_APP_DEPLOY_PATH`   | Root directory where releases are placed |
| `SDP_SCALA_APP_DEPLOY_LINK_PATH` | Directory where symlinks are created  |

The resulting on-disk layout for an app called `foo` deployed to `/opt/releases`:

```
/opt/releases/
└── foo/
    ├── conf/                                                  ← shared, created once; put application.conf, logback.xml, etc. here
    ├── logs/                                                  ← shared, created once
    ├── current -> foo-1.0.0-2024-06-01_12-00-00-abcd1234/   ← symlink, updated on each deploy
    ├── foo-1.0.0-2024-06-01_12-00-00-abcd1234/
    │   ├── bin/
    │   ├── lib/
    │   ├── conf -> /opt/releases/foo/conf/                   ← absolute symlink
    │   └── logs -> /opt/releases/foo/logs/                   ← absolute symlink
    └── foo-0.9.0-2024-05-01_09-00-00-deadbeef/
        ├── ...
        ├── conf -> /opt/releases/foo/conf/                   ← absolute symlink
        └── logs -> /opt/releases/foo/logs/                   ← absolute symlink
```

Each release directory is named:

```
<name>-<version>-<yyyy-MM-dd_HH-mm-ss>-<git-commit>
```

### Preview deployment info

Show where files will be deployed without making any changes:

```
sbt deployInfo
```

### List stale installations

List deployments older than 30 days:

```
sbt staleInstallations
```

## How it works

1. `stage` (from sbt-native-packager) builds a universal distribution locally
2. `deploy` copies the staged output to `<deployPath>/<name>/<name>-<version>-<time>-<commit>/`
3. The `current` symlink at `<deployPath>/<name>/current` is updated to point to the new release directory
4. `<deployPath>/<name>/logs` and `<deployPath>/<name>/conf` are created if absent, then symlinked (with absolute paths) into the release as `<release>/logs` and `<release>/conf`; the `conf/` directory is the right place for `application.conf`, `logback.xml`, and any other per-environment configuration files
5. Binary symlinks are created (or updated) in the link path pointing to scripts inside the release
6. A warning is printed if any existing installations in `<deployPath>/<name>/` are older than 30 days

## Configuration

All deployment paths are configured via environment variables or task arguments — no extra `build.sbt` keys required beyond enabling the plugin.

### JVM arguments

Place a `jvm-args` file in the shared `conf/` directory to pass extra arguments to the JVM at startup. Each line is one argument; lines starting with `#` and blank lines are ignored:

```
# Heap
-Xmx4g
-Xms4g

# GC
-XX:+UseG1GC
-XX:+UseStringDeduplication
```

The file is optional — if absent, startup proceeds normally.

### Application configuration and logging

Because `conf/` is prepended to the classpath, Typesafe Config picks up `conf/application.conf` automatically (it takes priority over any embedded `reference.conf` in the JARs), and Logback picks up `conf/logback.xml` without any extra system properties.

## Development

Format code:

```
sbt fmt
```

Check formatting without changes:

```
sbt fmtCheck
```

Linting is powered by [Scalafix](https://scalacenter.github.io/scalafix/) and formatting by [Scalafmt](https://scalameta.org/scalafmt/).

## License

Apache-2.0