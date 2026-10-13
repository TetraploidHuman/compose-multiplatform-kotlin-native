#!/usr/bin/env bash
# Run a Tetraploid linuxX64 .kexe with Wayland/EGL/GL shared libraries (NixOS-safe).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEXE="${1:-$ROOT/samples/wayland-demo/build/bin/linuxX64/releaseExecutable/wayland-demo.kexe}"
shift || true

if [[ ! -f "$KEXE" ]]; then
  echo "Binary not found: $KEXE" >&2
  echo "Build first: nix-shell ./shell.nix --run './gradlew :samples:wayland-demo:linkReleaseExecutableLinuxX64Stable'" >&2
  exit 1
fi

if command -v nix-shell >/dev/null 2>&1 && [[ -f "$ROOT/shell.nix" ]]; then
  # Embed argv into --run; trailing `-- "$@"` is parsed by nix-shell itself, not the kexe.
  RUN_CMD="export XDG_RUNTIME_DIR=\${XDG_RUNTIME_DIR:-/run/user/$(id -u)}; "
  RUN_CMD+="export WAYLAND_DISPLAY=\${WAYLAND_DISPLAY:-wayland-0}; "
  RUN_CMD+="exec $(printf '%q' "$KEXE")"
  for arg in "$@"; do
    RUN_CMD+=" $(printf '%q' "$arg")"
  done
  exec nix-shell "$ROOT/shell.nix" --run "$RUN_CMD"
fi

echo "On NixOS, use: ./scripts/run-linux-native.sh" >&2
echo "Or set LD_LIBRARY_PATH to include libxcrypt.so.1 (see shell.nix)." >&2
exit 1
