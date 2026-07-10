# Change this to your own configuration

DEBUG=false

VERSION=v1.0.8
BUILD_TYPE=debug

# android tools
BUILD_TOOLS_VER=35.0.0
CMAKE_VERSION=4.3.4
ANDROID_API=29
# NOTE: for now lets just focused only two arch
# arm64-v8a and armeabi-v7a
ANDROID_ABI=arm64-v8a
ANDROID_SDK_PATH=$ANDROID_SDK_ROOT
ANDROID_NDK_PATH=$ANDROID_NDK_HOME

# other
RULES=src/proguard-rules.pro
JAVA_SRC_DIR=src/java
CPP_SRC_DIR=src/cpp
BUILD=build
JAVA_BUILD_DIR=$BUILD/java
CPP_BUILD_DIR=$BUILD/cpp/$ANDROID_ABI
PACKAGE_NAME=org/termux/daemon
OUTPUT_APK=termux-daemon.apk
OUTPUT_NATIVE=android-daemon
