cmake_minimum_required(VERSION 3.10)

project(android-daemon LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED PROGRAM_NAME)
  message(STATUS "[*] PROGRAM_NAME is not set using default one")
  set(PROGRAM_NAME "android-daemon")
endif()


configure_file(
  ${CMAKE_SOURCE_DIR}/config.h.in
  ${CMAKE_BINARY_DIR}/config.h
)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type" FORCE)
endif()

add_executable(${PROGRAM_NAME} main.c)
target_include_directories(${PROGRAM_NAME} PRIVATE ${CMAKE_BINARY_DIR})

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
  message(STATUS "[*] Build DEBUG selected")
  target_compile_options(${PROGRAM_NAME} PRIVATE -g -O0)

elseif(CMAKE_BUILD_TYPE STREQUAL "Release")
  message(STATUS "[*] Build RELEASE selected")
  target_compile_options(${PROGRAM_NAME} PRIVATE -O2 -flto -DNDEBUG -g0)
  target_link_options(${PROGRAM_NAME} PRIVATE -flto -s)
endif()

install(TARGETS ${PROGRAM_NAME} DESTINATION bin)
