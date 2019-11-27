cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

project(ffresample LANGUAGES C CXX)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -Wunused-variable -fno-exceptions -fno-rtti")


if (NOT EXISTS ${PROJECT_ROOT}/thirdparty/ffmpeg)
    execute_process(COMMAND echo "Hello")
endif ()

add_library(${PROJECT_NAME}
        SHARED
        ffmpeg_jni.cpp
        )

include_directories(${PROJECT_ROOT}/thirdparty/ffmpeg/include)

message("CMAKE_CURRENT_SOURCE_DIR:${CMAKE_CURRENT_SOURCE_DIR}")
message("PROJECT_SOURCE_DIR:${PROJECT_SOURCE_DIR}")


execute_process(COMMAND echo ${CMAKE_SOURCE_DIR})


add_library(avcodec
        SHARED
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${PROJECT_ROOT}/thirdparty/ffmpeg/libs/${ANDROID_ABI}/libavcodec.so
        )

add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${PROJECT_ROOT}/thirdparty/ffmpeg/libs/${ANDROID_ABI}/libavutil.so
        )

add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${PROJECT_ROOT}/thirdparty/ffmpeg/libs/${ANDROID_ABI}/libswresample.so
        )


target_link_libraries(${PROJECT_NAME}
        avcodec
        avutil
        swresample
        log
        )