#!/bin/sh

SCRIPT_DIR=$(dirname "$0")
cd $SCRIPT_DIR
cd ../../
PROJECT_ROOT="$(pwd)"

FFMPEG_ROOT="$PROJECT_ROOT/thirdparty/ffmpeg"

HOST_PLATFORM="darwin-x86_64"

TOOLCHAIN=${NDK_HOME}/toolchains/llvm/prebuilt/${HOST_PLATFORM}/bin
SYSROOT=${NDK_HOME}/toolchains/llvm/prebuilt/${HOST_PLATFORM}/sysroot


COMMON_OPTIONS="--target-os=android \
                --disable-static \
                --enable-shared \
                --disable-doc \
                --disable-programs \
                --disable-everything \
                --disable-avdevice \
                --disable-avformat \
                --disable-swscale \
                --disable-postproc \
                --disable-avfilter \
                --disable-symver \
                --enable-swresample \
                --disable-avresample \
                --enable-encoder=gif \
                --disable-decoder=vorbis \
                --disable-decoder=opus \
                --disable-decoder=flac \
                "
#git://source.ffmpeg.org/ffmpeg


if [ ! -d $FFMPEG_ROOT ];then
    mkdir -p $FFMPEG_ROOT
    echo "make dir -p $FFMPEG_ROOT"
fi

cd "${FFMPEG_ROOT}"

(git -C source pull || git clone https://github.com/FFmpeg/FFmpeg.git source)
cd source && git checkout release/4.1

./configure \
    --prefix=$FFMPEG_ROOT \
    --libdir="$FFMPEG_ROOT/libs/armeabi-v7a" \
    --cc=$TOOLCHAIN/armv7a-linux-androideabi19-clang \
    --cxx=$TOOLCHAIN/armv7a-linux-androideabi19-clang++ \
    --ld=$TOOLCHAIN/armv7a-linux-androideabi19-clang \
    --arch=arm \
    --target-os=android \
    --cpu=armv7-a \
    --enable-cross-compile \
    --cross-prefix=$TOOLCHAIN/arm-linux-androideabi- \
    --sysroot="${SYSROOT}" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp" \
    --extra-ldflags="-Wl,--fix-cortex-a8" \
    --extra-ldexeflags=-pie \
    ${COMMON_OPTIONS}
make -j4 && make install
make clean

#./configure \
#  --prefix=$FFMPEG_ROOT \
#  --libdir="$FFMPEG_ROOT/libs/x86" \
#  --arch=x86 \
#  --target-os=darwin-x86_64 \
#  --cpu=i686 \
#  --cross-prefix="${NDK_HOME}/toolchains/x86-4.9/prebuilt/${HOST_PLATFORM}/bin/i686-linux-android-" \
#  --sysroot="${NDK_HOME}/platforms/android-9/arch-x86/" \
#  --extra-ldexeflags=-pie \
#  --disable-asm \
#  ${COMMON_OPTIONS}
#
#make -j4 && make install-libs
#make clean
#
#./configure \
#  --prefix=$FFMPEG_ROOT \
#  --libdir="$FFMPEG_ROOT/libs/arm64-v8a" \
#  --arch=aarch64 \
#  --target-os=darwin-x86_64 \
#  --cpu=armv8-a \
#  --cross-prefix="${NDK_HOME}/toolchains/aarch64-linux-android-4.9/prebuilt/${HOST_PLATFORM}/bin/aarch64-linux-android-" \
#  --sysroot="${NDK_HOME}/platforms/android-21/arch-arm64/" \
#  --extra-ldexeflags=-pie \
#  ${COMMON_OPTIONS}
#make -j4 && make install-libs
#make clean