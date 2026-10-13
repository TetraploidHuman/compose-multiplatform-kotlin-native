#!/usr/bin/env bash
# 发布 Gradle 插件（build-logic）+ version catalog → 本地 maven-repo / GitHub Packages。
#
#   GITHUB_TOKEN=… ./scripts/publish-tetraploid-plugins.sh
#   TETRAPLOID_PLUGINS_LOCAL_ONLY=1 ./scripts/publish-tetraploid-plugins.sh
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CKN_VER="$(grep '^tetraploid.version=' "$REPO/gradle.properties" | cut -d= -f2)"
LOCAL_ONLY="${TETRAPLOID_PLUGINS_LOCAL_ONLY:-0}"

info() { printf '\033[36m==>\033[0m %s\n' "$1"; }
die() { printf '\033[31m!!\033[0m %s\n' "$1" >&2; exit 1; }

GH_ARGS=()
TASKS_LOGIC=(
  publishAllPublicationsToTetraploidLocalRepository
)
TASKS_CATALOG=(
  :compose-kn-catalog:publishMavenPublicationToTetraploidLocalRepository
)
if [ "$LOCAL_ONLY" != 1 ]; then
  : "${GITHUB_TOKEN:?GITHUB_TOKEN required (or TETRAPLOID_PLUGINS_LOCAL_ONLY=1)}"
  GH_ARGS+=( -Ptetraploid.publish.github=true )
  TASKS_LOGIC+=( publishAllPublicationsToGitHubPackagesRepository )
  TASKS_CATALOG+=( :compose-kn-catalog:publishMavenPublicationToGitHubPackagesRepository )
fi

info "Publish build-logic plugins @ $CKN_VER"
cd "$REPO"
./gradlew -p build-logic --no-daemon \
  -Ptetraploid.version="$CKN_VER" \
  "${GH_ARGS[@]}" \
  "${TASKS_LOGIC[@]}"

info "Publish tetraploid-catalog @ $CKN_VER"
./gradlew --no-daemon \
  -Ptetraploid.version="$CKN_VER" \
  "${GH_ARGS[@]}" \
  "${TASKS_CATALOG[@]}"

# 插件 marker：统一入口 + 旧 settings
MARKER="$REPO/build/maven-repo/com/tetraploid/com.tetraploid.gradle.plugin/$CKN_VER"
[ -d "$MARKER" ] || die "missing com.tetraploid plugin marker at $MARKER"
MARKER_APP="$REPO/build/maven-repo/com/tetraploid/app/com.tetraploid.app.gradle.plugin/$CKN_VER"
[ -d "$MARKER_APP" ] || die "missing com.tetraploid.app plugin marker at $MARKER_APP"
MARKER_SETTINGS="$REPO/build/maven-repo/com/tetraploid/settings/com.tetraploid.settings.gradle.plugin/$CKN_VER"
[ -d "$MARKER_SETTINGS" ] || die "missing settings plugin marker at $MARKER_SETTINGS"

CATALOG_DIR="$REPO/build/maven-repo/com/tetraploid/tetraploid-catalog/$CKN_VER"
[ -d "$CATALOG_DIR" ] || die "missing catalog at $CATALOG_DIR"

info "done — plugins + catalog $CKN_VER"
ls -la "$MARKER" | head -10
ls -la "$CATALOG_DIR" | head -10
