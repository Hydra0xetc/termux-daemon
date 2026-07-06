#!/usr/bin/env bash

. ./config.sh

ANDROID_LIB="$ANDROID_SDK_PATH/platforms/android-$ANDROID_API/android.jar"
TOOLS="$ANDROID_SDK_PATH/build-tools/$BUILD_TOOLS_VER/lib/d8.jar"
R8=(java -cp "$TOOLS" com.android.tools.r8.R8)
D8=(java -cp "$TOOLS" com.android.tools.r8.D8)
AIDL="$ANDROID_SDK_PATH/build-tools/$BUILD_TOOLS_VER/aidl"
CMAKE="$ANDROID_SDK_PATH/cmake/$CMAKE_VERSION/bin/cmake"
NINJA="$ANDROID_SDK_PATH/cmake/$CMAKE_VERSION/bin/ninja"
CMAKE_TOOLCHAIN="$ANDROID_NDK_PATH/build/cmake/android.toolchain.cmake"
RESULT_DIR="$BUILD/termux-daemon-$ANDROID_ABI-$BUILD_TYPE"
SCRIPT_NAME=$(basename "$OUTPUT_APK" .apk)

set -e
[[ $DEBUG == true ]] && set -x

resolve_deps ()
{
  if [[ -f $ANDROID_LIB ]]; then
    echo "[*] found android.jar with api '$ANDROID_API'"
  else
    echo "[!] cannot found android.jar with api '$ANDROID_API' are you sure you have installed it?"
    exit 127
  fi

  if [[ -f $AIDL ]]; then
    echo "[*] found aidl"
  else
    echo "[!] cannot found aidl in $AIDL"
    exit 127
  fi

  if [[ -f $TOOLS ]]; then
    echo "[*] found d8 and r8"
  else
    echo "[!] cannot found d8 and r8 in $TOOLS"
    exit 127
  fi

}

clean_and_make ()
{
  echo "[*] cleaning build output"
  # NOTE: since CMakeLists or ndk-build know if source is change
  # we dont need to remove cpp build dir
  rm -rf "$JAVA_BUILD_DIR" "$OUTPUT_APK"
  mkdir -p "$JAVA_BUILD_DIR"
}

process_aidl ()
{
  echo "[*] processing aidl"

  find "$JAVA_SRC_DIR" -name "*.aidl" | while read -r file; do
    out="${file%.aidl}.java"

    # Skip parcelable declarations
    grep -q "^parcelable " "$file" && continue
    "$AIDL" -I "$JAVA_SRC_DIR" "$file" "$out"
  done

}

process_java ()
{
  echo "[*] Compiling to class"
  SOURCE_FILE="$JAVA_BUILD_DIR/sources.txt"
  find "$JAVA_SRC_DIR" -name "*.java" > "$SOURCE_FILE"
  javac -cp "$ANDROID_LIB" -d "$JAVA_BUILD_DIR" --release 17 "@$SOURCE_FILE"
}

process_dex ()
{
  if [[ $BUILD_TYPE == release ]]; then
    echo "[*] Compiling to dex (minify)"

    PG_CONF=""
    if [[ -f "$RULES" ]]; then
      PG_CONF="--pg-conf $RULES"
    else
      echo "[!] build minify need $RULES file"
      exit 127
    fi

    find "$JAVA_BUILD_DIR" -name "*.class" -print0 |
      xargs -0 "${R8[@]}" --release --lib "$ANDROID_LIB" --output \
      "$JAVA_BUILD_DIR" $PG_CONF
  elif [[ $BUILD_TYPE == debug ]]; then
    echo "[*] Compiling to dex (no minify)"
    find "$JAVA_BUILD_DIR" -name "*.class" -print0 |
      xargs -0 "${D8[@]}" --lib "$ANDROID_LIB" --output \
      "$JAVA_BUILD_DIR"
  else
    echo "[*] Unknow build type: $BUILD_TYPE"
    exit 1
  fi
}

__create_script_runner ()
{
  ENTRY_CLASS=$(echo "$PACKAGE_NAME" | tr / .).Main
  content=$(cat << EOF
#!/system/bin/env sh

HERE="\$(dirname "\$(readlink -f "\$0")")"

export PATH=/system/bin/
# TODO: Handle symlinked bin directories.
# NOTE: If the bin directory is a symlink, "../share"
# is resolved relative to the symlink target rather than
# the original path, so the APK cannot be found.
export CLASSPATH=\$HERE/../share/$SCRIPT_NAME/$OUTPUT_APK
exec app_process64 -Xmx10m -Xnoimage-dex2oat / "$ENTRY_CLASS" "\$@"
EOF
  )

  echo "$content" > "$1"
  chmod +x "$1"
}

package_all ()
{
  echo "[*] Packaging..."
  SHARE_DIR="$RESULT_DIR/share/$SCRIPT_NAME"
  BIN_DIR="$RESULT_DIR/bin"
  mkdir -p "$SHARE_DIR"
  mkdir -p "$BIN_DIR"
  zip -jq "$SHARE_DIR/$OUTPUT_APK" "$JAVA_BUILD_DIR/classes.dex"
  "$CMAKE" --install "$CPP_BUILD_DIR"
  __create_script_runner "$BIN_DIR/$SCRIPT_NAME"
  cp LICENSE "$SHARE_DIR"

  echo "[*] Done build termux-daemon-$ANDROID_ABI-$BUILD_TYPE"
}

__resolve_native_tools ()
{
  if [[ $1 == "cmake" ]]; then
    if [[ -f $CMAKE ]]; then
      echo "[*] found cmake with version $CMAKE_VERSION"
    else
      echo "[!] cannot found cmake with version $CMAKE_VERSION"
      exit 127
    fi
  elif [[ $1 == "ndk-build" ]]; then
    if [[ -f $ANDROID_NDK_PATH/ndk-build ]]; then
      echo "[*] found ndk-build"
    else
      echo "[!] cannot found ndk-build at $ANDROID_NDK_PATH"
    fi
  else
    echo "[*] usage __resolve_native_tools [ndk-build|cmake]"
    exit 1
  fi

}

process_cpp ()
{
  if [[ -d $CPP_SRC_DIR ]]; then
    echo "[*] found cpp dir"
    if [[ -f $CPP_SRC_DIR/CMakeLists.txt ]]; then
      __resolve_native_tools cmake
      echo "[*] Using cmake build system"
       "$CMAKE" "$CPP_SRC_DIR" -B "$CPP_BUILD_DIR" \
         -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN" \
         -DANDROID_ABI="$ANDROID_ABI" \
         -DANDROID_PLATFORM="$ANDROID_API" \
         -DCMAKE_BUILD_TYPE="${BUILD_TYPE^}" \
         -DCMAKE_INSTALL_PREFIX="$RESULT_DIR/" \
         -GNinja

       # NOTE: "string^" means uppercase first char

       "$NINJA" -C "$CPP_BUILD_DIR"
    elif [[ -f $CPP_SRC_DIR/Android.mk ]]; then
      echo "[*] Using ndk build system"
      __resolve_native_tools ndk-build
      echo "TODO: handle ndk build system"
    else
      echo "[!] cannot detect the build system are used"
      exit 1
    fi
  fi

}

build ()
{
  clean_and_make
  process_cpp
  resolve_deps
  process_aidl
  process_java
  process_dex
  package_all
}

main ()
{
  build
}

main "$@"
