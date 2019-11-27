package com.jsq.gav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jiang on 2019/7/11
 */

public class MixedAudioPipe implements AudioPipe {

    public final AudioPipe primaryPipe;

    public final AudioPipe secondaryPipe;

    private ByteBuffer mPrimaryOutBuffer;
    private ByteBuffer mSecondaryOutBuffer;
    private float mPrimaryVolume = 0.5f;
    private float mSecondaryVolume = 0.5f;

    public MixedAudioPipe(RawAudioFormat format) {
        primaryPipe = new SingleAudioPipe(format);
        secondaryPipe = new SingleAudioPipe(format);
        int bufferSize = 1024 * AvUtil.getAudioBitDepthByWidth(format.bitWidth) * format.channelCount;
        mPrimaryOutBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        mSecondaryOutBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
    }

    /**
     * 返回buffer 给编码器编码
     * @param buffer
     * @param maxSizeOfBytes
     * @return
     */
    @Override
    public int getOutputBuffer(ByteBuffer buffer, int maxSizeOfBytes) {
        int size = Math.min(getOutputSize(), maxSizeOfBytes);
        if (secondaryPipe.isEnded()) {
            primaryPipe.getOutputBuffer(buffer, size);
        } else {
            primaryPipe.getOutputBuffer(mPrimaryOutBuffer, size);
            mPrimaryOutBuffer.flip();
            secondaryPipe.getOutputBuffer(mSecondaryOutBuffer, size);
            mSecondaryOutBuffer.flip();
            for (int i = 0; i < size / 2; i++) {
                buffer.putShort((short) (mPrimaryOutBuffer.getShort() * mPrimaryVolume + mSecondaryOutBuffer.getShort() * mSecondaryVolume));
            }
            mSecondaryOutBuffer.compact();
            mPrimaryOutBuffer.compact();
        }
        return size;
    }

    /**
     * 获取可以编码的 buffer size
     * @return
     */
    @Override
    public int getOutputSize() {
        if (secondaryPipe.isEnded()) {// 这里 背景音乐结束了 停止合成背景音乐
            return primaryPipe.getOutputSize();
        } else {
            return Math.min(primaryPipe.getOutputSize(), secondaryPipe.getOutputSize());
        }
    }

    /**
     * 是否 eof
     */
    @Override
    public void queueEndOfStream() {
        primaryPipe.queueEndOfStream();
    }


    /**
     * @return 是否时间结束
     */
    @Override
    public boolean isEnded() {
        return primaryPipe.isEnded();
    }

    /**
     * 设置声音大小
     * @param primaryVolume
     * @param secondaryVolume
     */
    public void setVolume(float primaryVolume, float secondaryVolume) {
        mPrimaryVolume = primaryVolume;
        mSecondaryVolume = secondaryVolume;
    }
}
