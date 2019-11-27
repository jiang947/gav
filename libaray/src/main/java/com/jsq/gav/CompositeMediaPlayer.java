package com.jsq.gav;

import android.os.Handler;
import android.view.Surface;

/**
 * Created by jiang on 2019/5/24
 */

public class CompositeMediaPlayer {

    private AudioPlayer mAudioPlayer;

    private boolean mIsPlaying = false;

    private VideoPlayer mVideoPlayer;

    private DataSourceChain mSourceChain;

    private Handler mCallbackHandler = new Handler();
    private VideoPlayer.OnErrorListener mOnErrorListener;


    public CompositeMediaPlayer() {
        mVideoPlayer = new VideoPlayer();
    }

    public void setDataSource(DataSourceChain processor) {
        mSourceChain = processor;
    }


    public void prepare() {
        mSourceChain.prepare();

        mVideoPlayer.setOnErrorListener(mOnErrorListener);
        mVideoPlayer.setDataSource(mSourceChain);
        mVideoPlayer.prepare();

        mAudioPlayer = new AudioPlayer();
//        mAudioPlayer.setDataSource(mSourceChain);
//        mAudioPlayer.prepare();
    }

    public void start() {
        mVideoPlayer.start();
//        mAudioPlayer.start();
    }

    public void stop() {
        mVideoPlayer.stop();
    }

    public void pause() {
        mVideoPlayer.pause();
    }

    public void release() {
        mVideoPlayer.release();
        mCallbackHandler.removeCallbacksAndMessages(null);
    }

    public void setSurface(Surface surface) {
        mVideoPlayer.setSurface(surface);
    }

    public void setSpeedScale(float sc) {
        mVideoPlayer.setSpeedScale(sc);
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }


    public void seekTo(long timeUs){
        mVideoPlayer.seekTo(timeUs);
    }

    public void setOnErrorListener(VideoPlayer.OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
        if (mVideoPlayer != null) {
            mVideoPlayer.setOnErrorListener(onErrorListener);
        }
    }




}
