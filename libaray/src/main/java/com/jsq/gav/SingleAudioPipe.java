package com.jsq.gav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by jiang on 2019/7/4
 */

public class SingleAudioPipe implements AudioPipe {


    private ByteBuffer mBuffer;
    private ArrayList<AudioProcessor> mProcessors;

    private boolean mInputEnded;

    private ByteBuffer mOutBuffer;
    private int bufferSizePreFrame = 4096; //1152*4


    public SingleAudioPipe(RawAudioFormat audioFormat) {
        int bufferSize = 1024 * AvUtil.getAudioBitDepthByWidth(audioFormat.bitWidth)
                * audioFormat.channelCount * 5; // 10 frame size
        mBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        mOutBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        mProcessors = new ArrayList<>();
    }


    public void queueInput(ByteBuffer buffer, int size, long presentationTimeUs) {
        process(buffer, 0, size, presentationTimeUs);
        bufferSizePreFrame = (int) Math.max(bufferSizePreFrame, mBuffer.remaining() * 1.5f);
        mOutBuffer.put(mBuffer);
    }

    @Override
    public void queueEndOfStream() {
        mInputEnded = true;
    }


    /**
     * 输入是否已经结束
     * 已经判断 getOutputSize 所以不需要写flush
     *
     * @return true , 已经结束
     */
    @Override
    public boolean isEnded() {
        return mInputEnded && getOutputSize() == 0;
    }

    /**
     * 处理音频数据
     * @param buffer
     * @param index
     * @param size
     * @param presentationTimeUs
     */
    private void process(ByteBuffer buffer, int index, int size, long presentationTimeUs) {
        if (index >= mProcessors.size()) {
            return;
        }
        AudioProcessor processor = mProcessors.get(index);
        int outSize = size;

        if (processor.isActive(presentationTimeUs)) {
            processor.queueInput(buffer, presentationTimeUs);
            outSize = processor.getOutputSize();
            if (outSize == 0) {
                return;
            }
            mBuffer.clear();
            processor.getOutputBuffer(mBuffer);
            if (mBuffer.position() > 0) {
                mBuffer.flip();
            }
            process(mBuffer, index + 1, outSize, presentationTimeUs);
        } else {
            process(buffer, index + 1, outSize, presentationTimeUs);
        }

    }


    public int getOutputBuffer(ByteBuffer buffer, int maxSizeOfBytes) {
        mOutBuffer.flip();
        int mark = mOutBuffer.limit();
        if (mOutBuffer.remaining() > maxSizeOfBytes) {
            mOutBuffer.limit(mOutBuffer.position() + maxSizeOfBytes);
        }
        buffer.put(mOutBuffer);
        mOutBuffer.limit(mark);
        mOutBuffer.compact();
        return buffer.position();
    }

    @Override
    public void addAudioProcessor(AudioProcessor... audioProcessor) {
        Collections.addAll(mProcessors, audioProcessor);
    }


    /**
     * input format change
     */
    public void onFormatChange(RawAudioFormat rawAudioFormat) {
        for (AudioProcessor processor : mProcessors) {
            processor.onInputFormatChange(rawAudioFormat);
        }
    }

    public boolean isFull() {
        return mOutBuffer.remaining() < bufferSizePreFrame;
    }


    public void release() {

    }

    public int getOutputSize() {
        return mOutBuffer.position();
    }


}
