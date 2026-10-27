#!/usr/bin/env bash
set -euo pipefail

repo=$(cd "$(dirname "$0")/.." && pwd)
export TZ=Europe/Berlin
# Also applies to forked test JVMs; upstream numeric formatting tests expect a decimal point.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.language=en -Duser.country=US"

mvn -B -ntp -s "$repo/enna/maven-settings.xml" -f "$repo/pom.xml" verify "$@"
mkdir -p "$repo/enna/target"
cp "$repo/jitsi-media-transform/target/jitsi-media-transform-2.3-SNAPSHOT.jar" \
  "$repo/enna/target/jitsi-media-transform.jar"
printf 'upstream.revision=bf271b11f795d6546f8d78dee441c67d9ba2bf6b\nenna.revision=%s\nfix=vp9-temporal-bitrate\n' \
  "$(git -C "$repo" rev-parse HEAD)" > "$repo/enna/target/build-info.properties"
