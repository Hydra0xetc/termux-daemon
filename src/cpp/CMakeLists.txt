cmake_minimum_required(VERSION 3.10)

project(android-daemon LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type" FORCE)
endif()

if(NOT CMAKE_OUTPUT_NAME)
  message(STATUS "[*] CMAKE_OUTPUT_NAME is not set using default one")
  set(CMAKE_OUTPUT_NAME "android-daemon")
endif()

add_executable(${CMAKE_OUTPUT_NAME} main.c)

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
  message(STATUS "[*] Build DEBUG selected")
  target_compile_options(${CMAKE_OUTPUT_NAME} PRIVATE -g -O0)

elseif(CMAKE_BUILD_TYPE STREQUAL "Release")
  message(STATUS "[*] Build RELEASE selected")
  target_compile_options(${CMAKE_OUTPUT_NAME} PRIVATE -O2 -flto -DNDEBUG -g0)
  target_link_options(${CMAKE_OUTPUT_NAME} PRIVATE -flto -s)
endif()

install(TARGETS ${CMAKE_OUTPUT_NAME} DESTINATION bin)
