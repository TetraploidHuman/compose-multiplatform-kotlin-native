#!/usr/bin/env bash
# 用 Release maven zip（或已有目录）跑 samples/consumer-smoke 端到端冒烟：
# fetch → assert → compile → link → 可执行文件 --compile-only（不弹 GUI）。
#
#   ./scripts/smoke-tetraploid-consumer.sh                # 干净目录拉 Release 再测
#   ./scripts/smoke-tetraploid-consumer.sh ~/tetraploid-m2  # 已有 maven-repo
#   TETRAPLOID_VERSION=0.5.68 ./scripts/smoke-tetraploid-consumer.sh
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VER="${TETRAPLOID_VERSION:-$(grep '^tetraploid.version=' "$REPO/gradle.properties" | cut -d= -f2)}"
M2="${1:-}"

info() { printf '\033[36m==>\033[0m %s\n' "$1"; }
die() { printf '\033[31m!!\033[0m %s\n' "$1" >&2; exit 1; }

if [ -z "$M2" ]; then
  M2="$REPO/build/consumer-m2"
  info "Clean fetch Tetraploid $VER → $M2"
  rm -rf "$M2"
  mkdir -p "$M2"
  TETRAPLOID_VERSION="$VER" "$REPO/scripts/fetch-tetraploid-maven.sh" "$M2"
fi
[ -d "$M2/com/tetraploid" ] || die "not a maven-repo: $M2 (need com/tetraploid/…)"

chmod +x "$REPO/scripts/assert-tetraploid-maven-repo.sh"
"$REPO/scripts/assert-tetraploid-maven-repo.sh" "$M2"

# 对齐 sample 内版本号（单版本：不再要求 tetraploid.compose.ui.version）
SMOKE="$REPO/samples/consumer-smoke"
sed -i "s/^tetraploid.version=.*/tetraploid.version=$VER/" "$SMOKE/gradle.properties"

GRADLE=(
  "$REPO/gradlew" --no-daemon
  -Ptetraploid.maven.local="$M2"
  -Ptetraploid.version="$VER"
)

info "Compile + link consumer-smoke against $M2"
cd "$SMOKE"
"${GRADLE[@]}" compileKotlinLinuxX64
"${GRADLE[@]}" linkReleaseExecutableLinuxX64

EXE="$(find "$SMOKE/build/bin/linuxX64/releaseExecutable" -type f -name 'consumer-smoke.kexe' -print -quit 2>/dev/null || true)"
[ -n "$EXE" ] && [ -x "$EXE" ] || die "release executable missing under build/bin/linuxX64/releaseExecutable"

info "Run executable --compile-only (validates host/runtime load, no GUI)"
HELPER="$REPO/scripts/run-linux-native.sh"
if [ -f "$HELPER" ]; then
  bash "$HELPER" "$EXE" --compile-only
else
  "$EXE" --compile-only
fi

info "Verify runLinuxX64 task is registered"
"${GRADLE[@]}" tasks --group tetraploid | grep -q 'runLinuxX64' \
  || die "runLinuxX64 task not registered by com.tetraploid.app"

info "consumer-smoke OK (fetch + assert + compile + link + --compile-only + runLinuxX64 task)"
