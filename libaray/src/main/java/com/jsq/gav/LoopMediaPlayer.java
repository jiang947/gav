package com.jsq.gav;

import android.media.MediaPlayer;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by jiang on 2019/5/6
 */

public class LoopMediaPlayer {

    private MediaPlayer mCurrentPlayer = null;
    private MediaPlayer mNextPlayer = null;

    private final List<String> mFilepathList;
    private int mNextIndex = 0;
    private Surface mSurface;

    public LoopMediaPlayer(List<String> filepathList) {
        this.mFilepathList = filepathList;
        mCurrentPlayer = new MediaPlayer();

        try {
            mCurrentPlayer.setDataSource(getNextPath());
            mCurrentPlayer.setOnPreparedListener(mediaPlayer -> mCurrentPlayer.start());
            mCurrentPlayer.prepareAsync();
            createNextPlayer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNextPlayer() {
        mNextPlayer = new MediaPlayer();
        try {
            mNextPlayer.setDataSource(getNextPath());
            mNextPlayer.setOnPreparedListener(mp -> {
                mNextPlayer.seekTo(0);
                mCurrentPlayer.setNextMediaPlayer(mNextPlayer);
                mCurrentPlayer.setOnCompletionListener(onCompletionListener);
            });
            mNextPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final MediaPlayer.OnCompletionListener onCompletionListener =
            mediaPlayer -> {
                mCurrentPlayer = mNextPlayer;
                mediaPlayer.release();
                mCurrentPlayer.setSurface(mSurface);
                createNextPlayer();
            };


    private String getNextPath() {
        String filepath = mFilepathList.get(mNextIndex % mFilepathList.size());
        mNextIndex++;
        return filepath;
    }


    public void setDisplay(SurfaceHolder sh) {
        mCurrentPlayer.setDisplay(sh);
    }

    public void start() throws IllegalStateException {
        mCurrentPlayer.start();
    }

    public void stop() throws IllegalStateException {
        mCurrentPlayer.stop();
    }

    public void pause() throws IllegalStateException {
        mCurrentPlayer.pause();
    }

    public void release() {
        if (mCurrentPlayer != null)
            mCurrentPlayer.release();
        if (mNextPlayer != null)
            mNextPlayer.release();
    }

    public void reset() {
        mCurrentPlayer.reset();
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
        mCurrentPlayer.setSurface(surface);
    }
}
