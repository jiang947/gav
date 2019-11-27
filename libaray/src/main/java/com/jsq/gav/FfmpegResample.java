package com.jsq.gav;

import androidx.annotation.IntRange;

import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/14
 */


public class FfmpegResample {

    private static boolean soLoadSuccessfully;

    static {
        try {
            System.loadLibrary("avcodec");
            System.loadLibrary("swresample");
            System.loadLibrary("avutil");
            System.loadLibrary("ffresample");
            soLoadSuccessfully = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private long mContext;

    public FfmpegResample(@IntRange(from = 1, to = 2) int inChannelCount, int inSampleRate, int inBitWidth,
                          int outChannelCount, int outSampleRate, int outBitWidth) {

        mContext = init(getChannel(inChannelCount), inSampleRate, getFormat(inBitWidth),
                getChannel(outChannelCount), outSampleRate, getFormat(outBitWidth));
    }

    public int process(ByteBuffer inputData, ByteBuffer outputData) {
        return process(mContext, inputData, inputData.limit(), outputData);
    }

    public void release() {
        release(mContext);
    }

    /**
     * 获取 ffmpeg 声道
     *
     * @param channelCount 声道数量
     * @return ffmpeg 声道常量
     */
    private int getChannel(int channelCount) {
        int channel = 4; //channel_layout.h AV_CH_LAYOUT_MONO
        if (channelCount == 2) {
            channel = 3; // AV_CH_LAYOUT_STEREO
        }
        return channel;
    }

    /**
     * 获取ffmpeg pcm format
     * 目前支持 AVSampleFormat#AV_SAMPLE_FMT_S16 ，AV_SAMPLE_FMT_U8
     *
     * @param bitWidth 音频数据位宽
     * @return samplefmt.h AVSampleFormat
     */
    private int getFormat(int bitWidth) {
        if (bitWidth == 16) {
            return 1; // AV_SAMPLE_FMT_S16
        } else {
            return 0; // AV_SAMPLE_FMT_U8
        }
    }

    native long init(int inChannel, int inSampleRate, int inFormat,
                     int outChannel, int outSampleRate, int outFormat);

    native int process(long context, ByteBuffer inputData, int inputSize, ByteBuffer outputData);


    native void release(long context);


}
