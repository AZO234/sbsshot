#!/usr/bin/env bash
# =============================================================================
#  SBSShot - Multi-Version Build Script
#
#  使い方:
#    ./build.sh <mc_ver> <mod_version>
#    例: ./build.sh 26.1.2 1.0.0
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSIONS_JSON="$SCRIPT_DIR/versions.json"
OUTPUT_DIR="$SCRIPT_DIR/dist"

# 環境変数 MOD_VERSION を最優先、なければ第2引数。それもなければ '1.0.0-dev'
RAW_VERSION="${MOD_VERSION:-${2:-1.0.0-dev}}"
# 'v' プレフィックスを除去 (v1.0.2 -> 1.0.2)
MOD_VERSION="${RAW_VERSION#v}"

# ---- カラー出力 -------------------------------------------------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
divider() { echo -e "${YELLOW}----------------------------------------${NC}"; }

# ---- ターゲットMCバージョン決定 ---------------------------------------
if [ "${1:-}" == "" ]; then
    MC_VERSIONS=$(jq -r 'keys[]' "$VERSIONS_JSON")
else
    MC_VERSIONS=("$1")
fi

LOADERS=("fabric" "neoforge")

# ---- ビルド実行 -------------------------------------------------------
mkdir -p "$OUTPUT_DIR"
FAILED=()
SUCCEEDED=()

for mc_ver in $MC_VERSIONS; do
    divider
    info "Target Minecraft: $mc_ver (Mod Version: $MOD_VERSION)"
    divider

    VER_DIST="$OUTPUT_DIR/$mc_ver"
    mkdir -p "$VER_DIST"

    for loader in "${LOADERS[@]}"; do
        info "Building: $loader for MC $mc_ver"
        
        LOADER_DIR="$SCRIPT_DIR/$loader"
        if [ ! -d "$LOADER_DIR" ]; then
            warn "Loader directory $loader not found, skipping."
            continue
        fi

        # Gradle 実行 (-Pmc_ver, -Pmod_version を渡す)
        if (cd "$LOADER_DIR" && ./gradlew build -Pmc_ver="$mc_ver" -Pmod_version="$MOD_VERSION" --console=plain); then
            # JAR を整理してコピー (build.gradle でアーカイブ名に含めているため、それを見つける)
            find "$LOADER_DIR/build/libs" -name "*.jar" ! -name "*-sources*" ! -name "*-dev*" ! -name "*-all*" | while read -r jar_file; do
                cp "$jar_file" "$VER_DIST/"
                info "Output: dist/$mc_ver/$(basename "$jar_file")"
            done
            SUCCEEDED+=("$loader-mc$mc_ver")
        else
            error "Build failed: $loader for MC $mc_ver"
            FAILED+=("$loader-mc$mc_ver")
        fi
    done
done

# ---- サマリー ---------------------------------------------------------
divider
info "Build Summary for Mod Version: $MOD_VERSION"
divider
for s in "${SUCCEEDED[@]:-}"; do
    [ -n "$s" ] && echo -e "  ${GREEN}✓${NC}  $s"
done
for f in "${FAILED[@]:-}"; do
    [ -n "$f" ] && echo -e "  ${RED}✗${NC}  $f"
done
divider

if [ ${#FAILED[@]} -gt 0 ]; then
    exit 1
fi
