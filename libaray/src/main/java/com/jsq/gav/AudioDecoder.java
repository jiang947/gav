package com.jsq.gav;

import android.os.Handler;

/**
 * Created by jiang on 2019/5/20
 */

public interface AudioDecoder {


    void setDecoderEvent(DecoderEvent event);

    default void setDecoderEvent(DecoderEvent event, Handler handler) {
    }

    void drainDecoder();


    public interface DecoderEvent {


        void OnFormatChange(int sampleRateInHz, int channels, int audioFormat);

        void OnRenderFrame(byte[] buffer, long presentationTimeUs);

        void onEof();

    }


}
