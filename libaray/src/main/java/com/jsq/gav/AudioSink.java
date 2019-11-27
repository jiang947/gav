package com.jsq.gav;

import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/6/4
 * 音频接收器
 * 从audio decoder 接收 pcm数据
 */

public interface AudioSink {

    void queueInput(ByteBuffer buffer, int size, long presentationTimeUs);

    void queueEndOfStream();

    void onFormatChange(RawAudioFormat rawAudioFormat);

    boolean isFull();

}
