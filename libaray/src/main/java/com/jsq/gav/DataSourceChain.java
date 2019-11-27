package com.jsq.gav;

import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/5/17
 */

public class DataSourceChain {


    private final List<MediaDataSource> mMediaSource;
    private int currentIndex = 0;
    private boolean loop = false;


    private long currentSampleTime;

    private String type;

    private DataSourceChain(Builder builder) {
        mMediaSource = builder.mediaSource;
    }

    void prepare() {
        try {
            for (MediaDataSource source : mMediaSource) {
                source.prepare();
            }
        } catch (IOException e) {
            // listener exception
        }
    }

    int readSampleData(ByteBuffer inputBuffer, int offset) {
        return mMediaSource.get(currentIndex).readSampleData(inputBuffer, offset);
    }

    long getSampleTime() {
        currentSampleTime = mMediaSource.get(currentIndex).getSampleTime();
        return currentSampleTime;
    }


    void seekTo(long timeUs, int seekToNextSync) {
        mMediaSource.get(currentIndex).seekTo(timeUs);
    }

    public void release() {

    }


    MediaFormat getTrackFormat(String type) {
        int trackCount = mMediaSource.get(currentIndex).getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mMediaSource.get(currentIndex).getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).contains(type)) {
                return format;
            }
        }
        return null;
    }

    boolean hasNext() {
        return currentIndex < mMediaSource.size() - 1;
    }

    void changeNext() {
        currentIndex++;
    }

    MediaFormat getNextTrackFormat(String type) {
        int trackCount = mMediaSource.get(currentIndex + 1).getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mMediaSource.get(currentIndex + 1).getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).contains(type)) {
                return format;
            }
        }
        return null; //没有音频或者视频 ...
    }

    public int getTrackCount() {
        return mMediaSource.get(currentIndex).getTrackCount();
    }

    public void selectTrack(String type) {
//        mMediaSource.get(currentIndex).selectTrack(index);
        for (MediaDataSource source : mMediaSource) {
            int trackCount = source.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = source.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).contains(type)) {
                    source.selectTrack(i);
                    break;
                }
            }
        }
    }

    public void advance() {
        mMediaSource.get(currentIndex).advance();
    }

    public int getSampleTrackIndex() {
        return mMediaSource.get(currentIndex).getSampleTrackIndex();
    }

    public boolean isEof() {
        return !hasNext() && mMediaSource.get(currentIndex).getSampleTime() == -1;
    }


    public static class Builder {

        private List<MediaDataSource> mediaSource = new ArrayList<>();


        public Builder addMediaDataSource(String filename) {
            MediaCodecMediaSource defaultMediaSource = new MediaCodecMediaSource();
            defaultMediaSource.setFile(filename);
            mediaSource.add(defaultMediaSource);
            return this;
        }


        public DataSourceChain build() {
            return new DataSourceChain(this);
        }

    }


}
