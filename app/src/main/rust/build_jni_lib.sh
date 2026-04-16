#!/usr/bin/env bash

#
# Build JNI turn_tunnel_lib and copy to jniLibs/ directory
#
PROJ_DIR=$(dirname "$(readlink -f $0)")
LIB_NAME="libturn_tunnel_lib.so"

cargo ndk build -t x86_64 --lib
mkdir -p "${PROJ_DIR}/jniLibs/x86_64/"
cp -r "${PROJ_DIR}/target/x86_64-linux-android/debug/${LIB_NAME}" "${PROJ_DIR}/jniLibs/x86_64/"
