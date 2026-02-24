# sbt-local-deploy

An SBT plugin for local deployment of Scala applications. It packages your application into a versioned directory, manages symlinks for binary scripts, and helps track stale (old) installations.

## Features

- **Per-app subdirectory** — Each application gets its own folder (`<deployPath>/<name>/`); all releases live inside it
- **Versioned deployments** — Each deploy creates a timestamped and commit-tagged directory (`<name>-<version>-<timestamp>-<commit>`)
- **`current` symlink** — A `current` symlink inside the app folder always points to the latest release
- **Shared `logs` and `conf`** — `<deployPath>/<name>/logs` and `<deployPath>/<name>/conf` are created once and symlinked into every release directory, so all releases share the same config and log location
- **Binary symlink management** — Automatically creates symlinks for binary scripts in a configurable link directory
- **Stale installation tracking** — Lists deployments older than 30 days to help clean up disk space
- **Dry-run preview** — Inspect planned deployment paths before executing
- **CI/CD friendly** — All paths configurable via environment variables

## Requirements

- SBT 1.x
- [sbt-native-packager](https://github.com/sbt/sbt-native-packager) — the plugin expects a staged universal distribution

## Installation

Add the plugin to your `project/plugins.sbt`:

```scala
addSbtPlugin("io.github.ssstlis" % "sbt-local-deploy" % "<version>")
```

Enable the plugin in your `build.sbt`:

```scala
enablePlugins(LocalDeployPlugin)
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
    ├── conf/                                                  ← shared, created once
    ├── logs/                                                  ← shared, created once
    ├── current -> foo-1.0.0-2024-06-01_12-00-00-abcd1234/   ← symlink, updated on each deploy
    ├── foo-1.0.0-2024-06-01_12-00-00-abcd1234/
    │   ├── bin/
    │   ├── lib/
    │   ├── conf -> /opt/releases/foo/conf/                   ← symlink
    │   └── logs -> /opt/releases/foo/logs/                   ← symlink
    └── foo-0.9.0-2024-05-01_09-00-00-deadbeef/
        ├── ...
        ├── conf -> /opt/releases/foo/conf/
        └── logs -> /opt/releases/foo/logs/
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
4. `<deployPath>/<name>/logs` and `<deployPath>/<name>/conf` are created if absent, then symlinked into the release as `<release>/logs` and `<release>/conf`
5. Binary symlinks are created (or updated) in the link path pointing to scripts inside the release
6. A warning is printed if any existing installations in `<deployPath>/<name>/` are older than 30 days

## Configuration

All configuration is done via environment variables or task arguments — no extra `build.sbt` keys required beyond enabling the plugin.

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