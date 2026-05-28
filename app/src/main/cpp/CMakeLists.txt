
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("turnrelay")

# Map Android ABIs to Go Architectures (GOARCH)
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(GOARCH "arm64")
    set(GOOS "android")
    set(TARGET_HOST "aarch64-linux-android")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(GOARCH "arm")
    set(GOOS "android")
    set(TARGET_HOST "armv7a-linux-androideabi")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(GOARCH "amd64")
    set(GOOS "android")
    set(TARGET_HOST "x86_64-linux-android")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(GOARCH "386")
    set(GOOS "android")
    set(TARGET_HOST "i686-linux-android")
endif()

set(CGO_CC "${ANDROID_NDK}/toolchains/llvm/prebuilt/${ANDROID_HOST_TAG}/bin/${TARGET_HOST}${ANDROID_PLATFORM_LEVEL}-clang")

find_program(GO_EXECUTABLE go REQUIRED)


set(GO_SRC ${CMAKE_CURRENT_SOURCE_DIR}/main.go)
set(GO_LIB_PATH ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libturn_tunnel_lib.so)

set(MY_C_MODULE_FILES
        ${CMAKE_CURRENT_SOURCE_DIR}/main.go
        ${CMAKE_CURRENT_SOURCE_DIR}/turnrelay.c
)

add_custom_target(${CMAKE_PROJECT_NAME} ALL
        COMMAND ${CMAKE_COMMAND} -E env
        "CGO_ENABLED=1"
        GOOS=${GOOS}
        GOARCH=${GOARCH}
        CC=${CGO_CC}
        ${GO_EXECUTABLE} build -buildmode=c-shared "-ldflags=-checklinkname=0" -o ${GO_LIB_PATH}
        SOURCES ${MY_C_MODULE_FILES}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Build go lib"
        VERBATIM
)
