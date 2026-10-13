#!/usr/bin/env bash
# 用 Release maven zip（或已有目录）跑 samples/consumer-smoke 端到端冒烟：
# fetch → assert → compile → link → 可执行文件 --compile-only（不弹 GUI）。
#
#   ./scripts/smoke-composekn-consumer.sh                # 干净目录拉 Release 再测
#   ./scripts/smoke-composekn-consumer.sh ~/composekn-m2  # 已有 maven-repo
#   COMPOSEKN_VERSION=0.5.68 ./scripts/smoke-composekn-consumer.sh
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VER="${COMPOSEKN_VERSION:-$(grep '^composekn.version=' "$REPO/gradle.properties" | cut -d= -f2)}"
M2="${1:-}"

info() { printf '\033[36m==>\033[0m %s\n' "$1"; }
die() { printf '\033[31m!!\033[0m %s\n' "$1" >&2; exit 1; }

if [ -z "$M2" ]; then
  M2="$REPO/build/consumer-m2"
  info "Clean fetch Tetraploid $VER → $M2"
  rm -rf "$M2"
  mkdir -p "$M2"
  COMPOSEKN_VERSION="$VER" "$REPO/scripts/fetch-composekn-maven.sh" "$M2"
fi
[ -d "$M2/cn/tetraploid" ] || die "not a maven-repo: $M2 (need cn/tetraploid/…)"

chmod +x "$REPO/scripts/assert-composekn-maven-repo.sh"
"$REPO/scripts/assert-composekn-maven-repo.sh" "$M2"

# 对齐 sample 内版本号（单版本：不再要求 composekn.compose.ui.version）
SMOKE="$REPO/samples/consumer-smoke"
sed -i "s/^composekn.version=.*/composekn.version=$VER/" "$SMOKE/gradle.properties"

GRADLE=(
  "$REPO/gradlew" --no-daemon
  -Pcomposekn.maven.local="$M2"
  -Pcomposekn.version="$VER"
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
"${GRADLE[@]}" tasks --group composekn | grep -q 'runLinuxX64' \
  || die "runLinuxX64 task not registered by cn.tetraploid.app"

info "consumer-smoke OK (fetch + assert + compile + link + --compile-only + runLinuxX64 task)"
