package com.jsq.gav;

import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/7/11
 */

public interface AudioFeed {

    int getOutputBuffer(ByteBuffer buffer,int maxSizeOfBytes);

    int getOutputSize();

    void queueEndOfStream();

    boolean isEnded();

}
