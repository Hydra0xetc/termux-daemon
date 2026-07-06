# Change this to your own configuration

DEBUG=false
MINIFIED=false
ANDROID_API=29
ANDROID_DIR=lib/android-$ANDROID_API
ANDROID=$ANDROID_DIR/android.jar

# i don't know why but if i use d8 or r8 from termux repo i keep getting a error
# so i use d8 from my android-sdk
D8=$HOME/opt/android-sdk/build-tools/35.0.0/d8
AIDL=$HOME/opt/android-sdk/build-tools/35.0.0/aidl
R8=$HOME/opt/r8/8.8.46/r8
RULES=src/proguard-rules.pro
BUILD=build/
PACKAGE_NAME=org/termux/daemon
OUTPUT_APK=termux-daemon.apk
