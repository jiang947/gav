package com.jsq.gav;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * Created by jiang on 2019/5/14
 */

public class AudioPlayer implements AudioDecoder.DecoderEvent {

    private static final int MSG_PREPARE = 1;
    private static final int MSG_START = 2;
    private static final int MSG_STOP = 3;

    private AudioTrack mAudioTrack;


    private HandlerThread mDecoderThread;
    private Handler mDecoderEventHandler;
    private MediaCodec mDecoder;
    private DataSourceChain mDataSourceChain;

    private volatile boolean mPlaying;

    public AudioPlayer() {
    }

    public void setDataSource(DataSourceChain dataSource) {
        mDataSourceChain = dataSource;
    }


    public void prepare() {
        mDecoderThread = new HandlerThread("audio-decoder");
        mDecoderThread.start();
        mDecoderEventHandler = new Handler(mDecoderThread.getLooper(), this::handlerDecoderMessage);
        mDecoderEventHandler.sendEmptyMessage(MSG_PREPARE);
    }


    private boolean handlerDecoderMessage(Message msg) {
        switch (msg.what) {
            case MSG_PREPARE:
                onPrepare();
                break;
            case MSG_START:
                startPlay();
                break;
            case MSG_STOP:
                stopPlay();
                break;
        }
        return true;
    }


    public void start() {
        mPlaying = true;
        mDecoderEventHandler.sendEmptyMessage(MSG_START);
    }

    public void pause() {
        mPlaying = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public void stop() {
        mPlaying = false;
        mDecoderEventHandler.sendEmptyMessage(MSG_START);
    }

    private void onPrepare() {
        mDataSourceChain.prepare();
//        mDecoder
    }

    private void startPlay() {

    }

    private void stopPlay() {
        mAudioTrack.stop();
        mAudioTrack.release();
        mAudioTrack = null;
    }


    @Override
    public void OnFormatChange(int sampleRateInHz, int channels, int audioFormat) {
        if (mAudioTrack == null) {
            int audioChannel = 2;
            if (channels == 1) {
                audioChannel = AudioFormat.CHANNEL_OUT_MONO;
            } else if (channels == 2) {
                audioChannel = AudioFormat.CHANNEL_OUT_STEREO;
            }
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, audioChannel, audioFormat);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, audioChannel,
                    audioFormat, minBufferSize, AudioTrack.MODE_STREAM);
            mAudioTrack.play();
        }
    }

    @Override
    public void OnRenderFrame(byte[] buffer, long presentationTimeUs) {
        mAudioTrack.write(buffer, 0, buffer.length);
    }

    @Override
    public void onEof() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

    }

}
