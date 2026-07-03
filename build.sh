#!/usr/bin/env bash

. ./config.sh

set -e
[[ $DEBUG == true ]] && set -x

get_android_version() {
    case $1 in
        29) echo "10" ;;
        30) echo "11" ;;
        31) echo "12" ;;
        32|33) echo "13" ;;
        *) echo $1 ;;
    esac
}

if [[ ! -f "$ANDROID" ]]; then
    mkdir -p "$ANDROID_DIR"
    ZIP="${TMPDIR:-/tmp}/platform-${ANDROID_API}.zip"
    DL_URL=$(python fetch_android.py "$ANDROID_API")
    echo "[*] Downloading $ANDROID"
    # is there any way to just android.jar not whole android platforms
    curl -L -C - -o "$ZIP" "$DL_URL"
    ANDROID_VERSION=$(get_android_version $ANDROID_API)
    unzip -j "$ZIP" "android-${ANDROID_VERSION}/android.jar" -d "$ANDROID_DIR"
    rm "$ZIP"
fi

echo "[*] $ANDROID ready"

rm -rf "$BUILD"
rm -f classes.dex "$OUTPUT_APK"
mkdir -p "$BUILD"

echo "[*] Processing AIDL"
find src/java -name '*.aidl' | while read -r file; do
    out="${file%.aidl}.java"

    # Skip parcelable declarations
    grep -q '^parcelable ' "$file" && continue

    "$AIDL" \
        -I src/java \
        "$file" \
        "$out"
done

echo "[*] Compiling to class"
find src/java -name '*.java' > sources.txt
javac \
    -cp "$ANDROID" \
    -d "$BUILD" \
    --release 17 \
    @sources.txt

rm sources.txt

if [[ $MINIFIED == true ]]; then
    echo "[*] Compiling to dex (minify)"
    PG_CONF=""
    if [[ -f "$RULES" ]]; then
        PG_CONF="--pg-conf $RULES"
    else
        echo "[!] Minify dex need $RULES file"
        exit 1
    fi
    $R8 --release \
        --lib "$ANDROID" \
        --output . \
        $PG_CONF \
        "$BUILD/$PACKAGE_NAME"/*.class
else
    echo "[*] Compiling to dex (no minify)"
    bash "$D8" "$BUILD"/"$PACKAGE_NAME"/*.class \
         --lib "$ANDROID"
fi

echo "[*] Packaging dex into apk"
zip -j "$OUTPUT_APK" classes.dex
rm -f classes.dex

echo "[*] Build complete -+> $OUTPUT_APK"
