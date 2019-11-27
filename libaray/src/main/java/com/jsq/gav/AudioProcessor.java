package com.jsq.gav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jiang on 2019/6/4
 */

public interface AudioProcessor {

    ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());

    void queueInput(ByteBuffer inputBuffer, long presentationTimeUs);

    int getOutputBuffer(ByteBuffer outputBuffer);

    int getOutputSize();

    void onInputFormatChange(RawAudioFormat rawAudioFormat);

    boolean isActive(long presentationTimeUs);

    default void release() {
    }


    interface Fac {

    }

}
