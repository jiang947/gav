package com.jsq.gav;

import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/7/11
 */

public interface AudioPipe extends AudioSink, AudioFeed {

    /**
     * 队列 input buffer
     * @param buffer
     * @param size
     * @param presentationTimeUs
     */
    default void queueInput(ByteBuffer buffer, int size, long presentationTimeUs){
    }

    default void queueEndOfStream(){}


    default void onFormatChange(RawAudioFormat rawAudioFormat){}

    default boolean isFull(){
        return false;
    }

    default void addAudioProcessor(AudioProcessor... audioProcessor){

    }


}
