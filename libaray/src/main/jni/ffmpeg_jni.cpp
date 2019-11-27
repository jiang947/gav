//
// Created by jiang on 2019/5/14.
//
#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include <string.h>

extern "C" {
#ifdef __cplusplus
#define __STDC_CONSTANT_MACROS
#ifdef _STDINT_H
#undef _STDINT_H
#endif
#include <stdint.h>
#endif

#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>
//AV_CODEC_ID_GIF

#define LOG_TAG "ffmpeg-jni"
#ifndef LOGI
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#endif
#ifndef LOGE
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#endif

typedef struct ResampleContext {

    struct SwrContext *swr_ctx;

    int src_sample_rate;
    int src_channel_count;
    AVSampleFormat src_fmt;
    int src_bytes_per_sample;
    int src_samples_in_frame;
    uint8_t **src_buffer;

    int dst_sample_rate;
    int dst_channel_count;
    AVSampleFormat dst_fmt;
    int dst_samples_in_frame;
    uint8_t **dst_buffer;


} ResampleContext;


JNIEXPORT jlong JNICALL
Java_com_dealuck_video_FfmpegResample_init(JNIEnv *env, jobject instance, jint srcChannel,
                                                  jint srcSampleRate, jint srcFormat,
                                                  jint dstChannel,
                                                  jint dstSampleRate, jint dstFormat) {
    struct ResampleContext *context = (ResampleContext *) malloc(sizeof(struct ResampleContext));
    context->swr_ctx = swr_alloc();
    av_opt_set_int(context->swr_ctx, "in_channel_layout", srcChannel, 0);
    av_opt_set_int(context->swr_ctx, "in_sample_rate", srcSampleRate, 0);
    av_opt_set_sample_fmt(context->swr_ctx, "in_sample_fmt", static_cast<AVSampleFormat>(srcFormat),
                          0);
    context->src_sample_rate = srcSampleRate;
    context->src_channel_count = av_get_channel_layout_nb_channels(
            static_cast<uint64_t>(srcChannel));
    context->src_fmt = static_cast<AVSampleFormat>(srcFormat);
    context->src_bytes_per_sample = av_get_bytes_per_sample(context->src_fmt);
    context->src_samples_in_frame = 0;
    context->src_buffer = NULL;

    av_opt_set_int(context->swr_ctx, "out_channel_layout", dstChannel, 0);
    av_opt_set_int(context->swr_ctx, "out_sample_rate", dstSampleRate, 0);
    av_opt_set_sample_fmt(context->swr_ctx, "out_sample_fmt",
                          static_cast<AVSampleFormat>(dstFormat), 0);
    context->dst_sample_rate = dstSampleRate;
    context->dst_channel_count = av_get_channel_layout_nb_channels(
            static_cast<uint64_t>(dstChannel));
    context->dst_fmt = static_cast<AVSampleFormat>(dstFormat);
    context->dst_samples_in_frame = 0;
    context->dst_buffer = NULL;
    swr_init(context->swr_ctx);

    return reinterpret_cast<jlong>(context);
}

JNIEXPORT jint JNICALL
Java_com_dealuck_video_FfmpegResample_process(JNIEnv *env, jobject instance,
                                                     jlong swrContext, jobject inputData,
                                                     jint inputSize, jobject outputData) {
    struct ResampleContext *context = reinterpret_cast<ResampleContext *>(swrContext);
    uint8_t *inputBuffer = static_cast<uint8_t *>(env->GetDirectBufferAddress(inputData));
    uint8_t *outBuffer = static_cast<uint8_t *>(env->GetDirectBufferAddress(outputData));

    int in_linesize = 0, out_linesize = 0;
    int src_samples_in_frame, dst_samples_in_frame; // 一个音音频帧的sample数量
    int ret;

    if (inputSize == 0) {
        return 0;
    }

    src_samples_in_frame = inputSize / context->src_channel_count / context->src_bytes_per_sample;

    if (!context->src_buffer) {
        av_samples_alloc_array_and_samples(&context->src_buffer, &in_linesize,
                                           context->src_channel_count, src_samples_in_frame,
                                           context->src_fmt, 0);
        context->src_samples_in_frame = src_samples_in_frame;
    }
    if (context->src_samples_in_frame < src_samples_in_frame) {
        av_freep(&context->src_buffer[0]);
        av_samples_alloc_array_and_samples(&context->src_buffer, &in_linesize,
                                           context->src_channel_count, src_samples_in_frame,
                                           context->src_fmt, 0);
        context->src_samples_in_frame = src_samples_in_frame;
    }
    //context->src_buffer[0] = inputBuffer;
    memcpy(context->src_buffer[0], inputBuffer, static_cast<size_t>(inputSize));

    dst_samples_in_frame = (int) av_rescale_rnd(src_samples_in_frame, context->dst_sample_rate,
                                                context->src_sample_rate, AV_ROUND_UP);
    if (!context->dst_buffer) {
        av_samples_alloc_array_and_samples(&context->dst_buffer, &out_linesize,
                                           context->dst_channel_count, dst_samples_in_frame,
                                           context->dst_fmt, 0);
        context->dst_samples_in_frame = dst_samples_in_frame;
    }

    if (context->dst_samples_in_frame < dst_samples_in_frame) {
        av_freep(&context->dst_buffer[0]);
        av_samples_alloc_array_and_samples(&context->dst_buffer, &out_linesize,
                                           context->dst_channel_count, dst_samples_in_frame,
                                           context->dst_fmt, 0);
        context->dst_samples_in_frame = dst_samples_in_frame;
    }

    ret = swr_convert(context->swr_ctx, context->dst_buffer, dst_samples_in_frame,
                      (const uint8_t **) context->src_buffer, src_samples_in_frame);
    if (ret < 0) {
        LOGE("swr_convert err %d", ret);
    }
    int out_size = av_samples_get_buffer_size(&out_linesize, context->dst_channel_count, ret,
                                              context->dst_fmt, 1);
    memcpy(outBuffer, context->dst_buffer[0], static_cast<size_t>(out_size));
    return out_size;
}


JNIEXPORT void JNICALL
Java_com_dealuck_video_FfmpegResample_release(JNIEnv *env, jobject instance,
                                                     jlong swrContext) {
    struct ResampleContext *context = reinterpret_cast<ResampleContext *>(swrContext);
    if (context->src_buffer) {
        av_freep(&context->src_buffer[0]);
    }
    av_freep(&context->src_buffer);

    if (context->dst_buffer) {
        av_freep(&context->dst_buffer[0]);
    }
    av_freep(&context->dst_buffer);

    swr_free(&context->swr_ctx);
    free(context);
}


}
