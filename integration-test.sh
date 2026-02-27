#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${1:-/tmp/sdp-deploy}"
LINKS_DIR="${2:-/tmp/sdp-links}"

sbt 'set version := "0.0.0-test"' publishLocal

(cd example && sbt "deploy $DEPLOY_DIR $LINKS_DIR")

cp example/conf/application.conf "$DEPLOY_DIR/sdp-example/conf/"
cp example/conf/logback.xml      "$DEPLOY_DIR/sdp-example/conf/"
cp example/conf/jvm-args         "$DEPLOY_DIR/sdp-example/conf/"

OUTPUT=$("$DEPLOY_DIR/sdp-example/current/bin/sdp-example" hello world 2>&1)
echo "--- App output ---"
echo "$OUTPUT"

echo "$OUTPUT" | grep -qF -- "-Xmx256m"            || { echo "FAIL: JVM arg -Xmx256m not found";    exit 1; }
echo "$OUTPUT" | grep -qF -- "hello"               || { echo "FAIL: CLI arg 'hello' not found";     exit 1; }
echo "$OUTPUT" | grep -qF -- "world"               || { echo "FAIL: CLI arg 'world' not found";     exit 1; }
echo "$OUTPUT" | grep -qF -- "ldp-example"         || { echo "FAIL: app.name not found in config"; exit 1; }
echo "$OUTPUT" | grep -qF -- "Application started" || { echo "FAIL: log output not found";         exit 1; }

echo "All checks passed."
