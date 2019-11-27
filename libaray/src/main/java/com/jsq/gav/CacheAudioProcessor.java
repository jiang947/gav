package com.jsq.gav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jiang on 2019/7/14
 */

public class CacheAudioProcessor implements AudioProcessor {

    public ByteBuffer mBuffer;


    public CacheAudioProcessor(RawAudioFormat format) {
        mBuffer = ByteBuffer.allocateDirect((int) (1024 * format.channelCount * 2 * 2.5f)).order(ByteOrder.nativeOrder());
    }


    @Override
    public void queueInput(ByteBuffer inputBuffer,long presentationTimeUs) {
        mBuffer.put(inputBuffer);
    }

    @Override
    public int getOutputBuffer(ByteBuffer outputBuffer) {
        mBuffer.flip();
        int length = mBuffer.remaining();
        outputBuffer.put(mBuffer);
        mBuffer.compact();
        outputBuffer.flip();
        return length;
    }

    @Override
    public int getOutputSize() {
        return mBuffer.position();
    }

    @Override
    public void onInputFormatChange(RawAudioFormat rawAudioFormat) {

    }

    @Override
    public boolean isActive(long presentationTimeUs) {
        return true;
    }
}
