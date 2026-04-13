#!/usr/bin/env bash
# =============================================================================
#  Hello World Mod - Build Script (Unix / macOS)
#
#  使い方:
#    ./build.sh              # すべてのローダーをビルド
#    ./build.sh neoforge     # NeoForge のみ
#    ./build.sh fabric       # Fabric のみ
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALL_LOADERS=("neoforge" "fabric")
OUTPUT_DIR="$SCRIPT_DIR/dist"

# ---- カラー出力 -------------------------------------------------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }
divider() { echo -e "${YELLOW}----------------------------------------${NC}"; }

# ---- ターゲット決定 ---------------------------------------------------
if [ $# -eq 0 ]; then
    TARGETS=("${ALL_LOADERS[@]}")
else
    TARGETS=()
    for arg in "$@"; do
        arg_lower=$(echo "$arg" | tr '[:upper:]' '[:lower:]')
        valid=false
        for loader in "${ALL_LOADERS[@]}"; do
            if [ "$arg_lower" = "$loader" ]; then
                TARGETS+=("$loader")
                valid=true
                break
            fi
        done
        if [ "$valid" = false ]; then
            error "Unknown loader: '$arg'  (valid: neoforge, fabric)"
            exit 1
        fi
    done
fi

# ---- ビルド実行 -------------------------------------------------------
mkdir -p "$OUTPUT_DIR"
FAILED=()
SUCCEEDED=()

for loader in "${TARGETS[@]}"; do
    divider
    info "Building: $loader"
    divider

    LOADER_DIR="$SCRIPT_DIR/$loader"

    if [ ! -f "$LOADER_DIR/gradlew" ]; then
        warn "gradlew not found in $loader/ — run 'gradle wrapper' first."
        FAILED+=("$loader (no gradlew)")
        continue
    fi

    chmod +x "$LOADER_DIR/gradlew"

    if (cd "$LOADER_DIR" && ./gradlew build --console=plain); then
        # JAR を dist/ にコピー
        jar_file=$(find "$LOADER_DIR/build/libs" -name "*.jar" ! -name "*-sources*" ! -name "*-dev*" | head -1)
        if [ -n "$jar_file" ]; then
            cp "$jar_file" "$OUTPUT_DIR/"
            info "Output: dist/$(basename "$jar_file")"
        fi
        SUCCEEDED+=("$loader")
    else
        error "Build failed: $loader"
        FAILED+=("$loader")
    fi
done

# ---- サマリー ---------------------------------------------------------
divider
info "Build Summary"
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
