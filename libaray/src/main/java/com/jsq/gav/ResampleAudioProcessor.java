package com.jsq.gav;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jiang on 2019/7/3
 */

public class ResampleAudioProcessor implements AudioProcessor {

    private FfmpegResample mFfmpegResample;
    private ByteBuffer mByteBuffer;

    private int mOutputSizeBytes;
    private final RawAudioFormat mOutRawAudioFormat;
    private RawAudioFormat mInRawAudioFormat;

    public ResampleAudioProcessor(RawAudioFormat outRawAudioFormat) {
        mOutRawAudioFormat = outRawAudioFormat;
        mByteBuffer = ByteBuffer.allocateDirect(1024 * 32).order(ByteOrder.nativeOrder());
    }

    public void queueInput(ByteBuffer inBuffer, long presentationTimeUs) {
        mOutputSizeBytes = mFfmpegResample.process(inBuffer, mByteBuffer);
        mByteBuffer.limit(mOutputSizeBytes);
    }

    public int getOutputBuffer(ByteBuffer outputBuffer) {
        outputBuffer.put(mByteBuffer);
        mByteBuffer.compact();
        mOutputSizeBytes = 0;
        return mOutputSizeBytes;
    }

    public int getOutputSize() {
        return mOutputSizeBytes;
    }

    @Override
    public void onInputFormatChange(@NonNull RawAudioFormat inFormat) {
        if (mOutRawAudioFormat.equals(inFormat)) return;
        if (inFormat.equals(mInRawAudioFormat)) return;
        if (mInRawAudioFormat == null) mInRawAudioFormat = inFormat;
        mInRawAudioFormat = inFormat;
        if (mFfmpegResample != null) {
            mFfmpegResample.release();
            mFfmpegResample = null;
        }
        mFfmpegResample = new FfmpegResample(inFormat.channelCount, inFormat.sampleRate, inFormat.bitWidth,
                mOutRawAudioFormat.channelCount, mOutRawAudioFormat.sampleRate, mOutRawAudioFormat.bitWidth);
    }


    public int process(ByteBuffer inputBuffer, ByteBuffer outBuffer) {
        int size = mFfmpegResample.process(inputBuffer, mByteBuffer);
        return size;
    }

    /**
     * 音频处理器是否使用
     * @param presentationTimeUs
     * @return
     */
    @Override
    public boolean isActive(long presentationTimeUs) {
        return !mOutRawAudioFormat.equals(mInRawAudioFormat) && mFfmpegResample != null;
    }

    @Override
    public void release() {
        mFfmpegResample.release();
    }
}
