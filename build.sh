#!/usr/bin/env bash
# =============================================================================
#  SBSShot - Multi-Version Build Script
#
#  使い方:
#    ./build.sh              # versions.json にある全バージョン、全ローダーをビルド
#    ./build.sh 26.1.2       # 特定の MC バージョンのみ
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSIONS_JSON="$SCRIPT_DIR/versions.json"
OUTPUT_DIR="$SCRIPT_DIR/dist"

# ---- カラー出力 -------------------------------------------------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
divider() { echo -e "${YELLOW}----------------------------------------${NC}"; }

# ---- ターゲットMCバージョン決定 ---------------------------------------
if [ $# -eq 0 ]; then
    MC_VERSIONS=$(jq -r 'keys[]' "$VERSIONS_JSON")
else
    MC_VERSIONS=("$@")
fi

LOADERS=("fabric" "neoforge")

# ---- ビルド実行 -------------------------------------------------------
mkdir -p "$OUTPUT_DIR"
FAILED=()
SUCCEEDED=()

for mc_ver in $MC_VERSIONS; do
    divider
    info "Target Minecraft: $mc_ver"
    divider

    # バージョンごとの出力ディレクトリ
    VER_DIST="$OUTPUT_DIR/$mc_ver"
    mkdir -p "$VER_DIST"

    for loader in "${LOADERS[@]}"; do
        info "Building: $loader for MC $mc_ver"
        
        LOADER_DIR="$SCRIPT_DIR/$loader"
        if [ ! -d "$LOADER_DIR" ]; then
            warn "Loader directory $loader not found, skipping."
            continue
        fi

        # Gradle 実行 (-Pmc_ver を渡す)
        if (cd "$LOADER_DIR" && ./gradlew build -Pmc_ver="$mc_ver" --console=plain); then
            # JAR を整理してコピー
            # build.gradle で archivesName を sbsshot-loader-mcver にしているので、それを探す
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
info "Multi-Version Build Summary"
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
