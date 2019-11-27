#!/usr/bin/env bash

SCRIPT_DIR=$(dirname "$0")
cd $SCRIPT_DIR
cd ../../../
PROJECT_ROOT="$(pwd)"

FFMPEG_ROOT="$PROJECT_ROOT/thirdparty/ffmpeg"

rm -rf $FFMPEG_ROOT/libs/*

rm -rf $FFMPEG_ROOT/include/*