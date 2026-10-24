#!/bin/bash
set -euo pipefail

# Gradle passes the same JDK used to compile the game. No extra package manager is needed.
if [[ "$(uname -s)" != Darwin || $# -ne 4 ]]; then
    echo 'Run ./gradlew packageMacos on macOS.' >&2
    exit 1
fi
tetris_jdk="$1"
tetris_input="$2"
tetris_jar="$3"
tetris_output="$4"
tetris_script_directory="$(cd "$(dirname "$0")" && pwd)"
# iCloud can attach FinderInfo to app bundles, which prevents ad-hoc signing.
# Assemble outside the synced directory, then copy the completed app and archive.
tetris_work="$(mktemp -d "${TMPDIR:-/tmp}/tetris-macos.XXXXXX")"
trap 'rm -rf "$tetris_work"' EXIT
tetris_iconset="$tetris_work/Tetris.iconset"
tetris_architecture="$(uname -m)"
tetris_java_architecture="$("$tetris_jdk/bin/java" -XshowSettings:properties -version 2>&1 \
    | /usr/bin/awk '$1 == "os.arch" {print $3}')"
case "$tetris_java_architecture" in
    aarch64) tetris_java_architecture=arm64 ;;
    amd64) tetris_java_architecture=x86_64 ;;
esac
if [[ "$tetris_java_architecture" != "$tetris_architecture" ]]; then
    echo "Use a JDK matching this Mac: $tetris_architecture (selected: $tetris_java_architecture)." >&2
    exit 1
fi

mkdir -p "$tetris_iconset"
"$tetris_jdk/bin/java" -Djava.awt.headless=true \
    "$tetris_script_directory/CreateIcon.java" "$tetris_iconset"
/usr/bin/iconutil -c icns "$tetris_iconset" -o "$tetris_work/Tetris.icns"

"$tetris_jdk/bin/jpackage" \
    --type app-image \
    --name Tetris \
    --app-version 1.0.0 \
    --vendor 'Tetris Team' \
    --description 'SeoulTech Software Engineering Tetris Team Project' \
    --mac-package-identifier com.github.darkran.tetris \
    --input "$tetris_input" \
    --main-jar "$tetris_jar" \
    --main-class tetris.Main \
    --icon "$tetris_work/Tetris.icns" \
    --add-modules java.desktop,jdk.random \
    --jlink-options '--strip-debug --no-man-pages --no-header-files' \
    --java-options '-Xmx256m' \
    --dest "$tetris_work"
/usr/bin/codesign --verify --deep --strict "$tetris_work/Tetris.app"

# ditto preserves the app bundle's permissions and symlinks for Finder extraction.
mkdir -p "$tetris_output"
/usr/bin/ditto "$tetris_work/Tetris.app" "$tetris_output/Tetris.app"
/usr/bin/ditto -c -k --sequesterRsrc --keepParent \
    "$tetris_work/Tetris.app" "$tetris_output/Tetris-macos-$tetris_architecture.zip"
(
    cd "$tetris_output"
    /usr/bin/shasum -a 256 "Tetris-macos-$tetris_architecture.zip" \
        > "Tetris-macos-$tetris_architecture.zip.sha256"
)
# A synced destination can attach non-security Finder metadata after copying.
# Only the generated bundle's FinderInfo is removed; the archive comes from staging.
/usr/bin/xattr -dr com.apple.FinderInfo "$tetris_output/Tetris.app" 2>/dev/null || true
/usr/bin/codesign --verify --deep --strict "$tetris_output/Tetris.app"
