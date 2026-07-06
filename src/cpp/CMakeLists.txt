cmake_minimum_required(VERSION 3.10)

project(android-daemon LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type" FORCE)
endif()

add_executable(android-daemon android-daemon.c)

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
  message(STATUS "[*] Build DEBUG selected")
  target_compile_options(android-daemon PRIVATE -g -O0)

elseif(CMAKE_BUILD_TYPE STREQUAL "Release")
  message(STATUS "[*] Build RELEASE selected")
  target_compile_options(android-daemon PRIVATE -O2 -flto -DNDEBUG -g0)
  target_link_options(android-daemon PRIVATE -flto -s)
endif()

install(TARGETS android-daemon DESTINATION bin)
