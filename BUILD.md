# Instruction how to build this project

first of all you need [android-sdk](https://developer.android.com/tools/releases/platform-tools) and [android-ndk](https://developer.android.com/ndk) install to get
 android-sdk:
 - d8
 - r8
 - aidl
 - cmake (optional if using cmake)
 - ninja (optional if using cmake)

 android-ndk:
 - clang and another toolchain
 - ndk-build (optional but still not supported so i recomended use cmake)

and change `ANDROID_SDK_PATH` and `ANDROID_NDK_PATH` variable in [config.sh](config.sh)
to your android-sdk and android-ndk path. since android-sdk have many version of
build tools you can override `BUILD_TOOLS_VER` variable in [config.sh](config,sh) to
your exsisting build-tools version and also you can change `ANDROID_ABI`, `ANDROID_API`
variable to change default android api and abi, and then simply run [build.sh](build.sh)
or you can andd --help flag to print all the flag.

```
usage: build.sh [OPTIONS]

options:
    -h/--help              print this help message
    -b/--build [java|cpp]  build only java or cpp, default java and cpp
    -r/--run <exe>         run the in the package app, try -l/--list to list them
    -l/--list-exe          list executable in then bin package app
```

example for `-r/--run` flag:
```
$ ./build.sh -r termux-daemon --version
$ termux-daemon --version
v1.0.4-debug
```

so basically everything after `-r/--run` will be exe flag
