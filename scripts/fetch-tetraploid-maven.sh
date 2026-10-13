#!/usr/bin/env bash
# 从 GitHub Release 拉 compose-kn Maven 仓库（稳定 CDN，绕过 Packages Billing）。
#
#   ./scripts/fetch-tetraploid-maven.sh                 # → ~/.tetraploid/m2/<ver>
#   ./scripts/fetch-tetraploid-maven.sh /path/to/m2     # 自定义目录
#   TETRAPLOID_VERSION=0.5.66 ./scripts/fetch-tetraploid-maven.sh
#
# 与 com.tetraploid settings 插件默认缓存一致。
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VER="${TETRAPLOID_VERSION:-$(grep '^tetraploid.version=' "$REPO/gradle.properties" 2>/dev/null | cut -d= -f2 || echo 0.5.66)}"
DEST="${1:-$HOME/.tetraploid/m2/$VER}"
OWNER="${TETRAPLOID_GH_OWNER:-TetraploidHuman}"
REPO_NAME="${TETRAPLOID_GH_REPO:-compose-multiplatform-kotlin-native}"
ASSET="tetraploid-maven-${VER}.zip"
TAG="v${VER}"

info() { printf '\033[36m==>\033[0m %s\n' "$1"; }
die() { printf '\033[31m!!\033[0m %s\n' "$1" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

info "Download $OWNER/$REPO_NAME@$TAG /$ASSET"
AUTH=()
if [ -n "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ]; then
  AUTH=(-H "Authorization: Bearer ${GITHUB_TOKEN:-$GH_TOKEN}")
fi
# 私有仓 Release 资产需 API（带 Accept），不能直接拼 browser download URL
API="https://api.github.com/repos/$OWNER/$REPO_NAME/releases/tags/$TAG"
ASSET_ID="$(curl -fsSL "${AUTH[@]}" -H "Accept: application/vnd.github+json" "$API" \
  | python3 -c "import json,sys; r=json.load(sys.stdin); print(next(a['id'] for a in r.get('assets',[]) if a['name']==sys.argv[1]))" "$ASSET" \
  2>/dev/null || true)"
[ -n "$ASSET_ID" ] || die "Release $TAG 没有资产 $ASSET（先跑: ./scripts/publish-tetraploid-packages.sh --release）"

curl -fsSL "${AUTH[@]}" -H "Accept: application/octet-stream" \
  -o "$TMP/$ASSET" \
  "https://api.github.com/repos/$OWNER/$REPO_NAME/releases/assets/$ASSET_ID"

mkdir -p "$DEST"
# zip 内顶层是 com/ …
if command -v unzip >/dev/null 2>&1; then
  unzip -qo "$TMP/$ASSET" -d "$DEST"
else
  python3 - <<PY
import zipfile
with zipfile.ZipFile("$TMP/$ASSET") as z:
    z.extractall("$DEST")
PY
fi
echo "$VER" > "$DEST/.tetraploid-complete"
info "Maven repo ready: $DEST"
find "$DEST" -name '*.klib' 2>/dev/null | sort || true
