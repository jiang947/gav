package com.jsq.gav;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/17
 */

public class MediaCodecMediaSource implements MediaDataSource {


    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;

    private String mFilename;

    public MediaCodecMediaSource() {
        mMediaExtractor = new MediaExtractor();
    }

    public void setFile(String filename) {
        mFilename = filename;
    }

    public void release() {
        mMediaExtractor.release();
    }

    public int getTrackCount() {
        return mMediaExtractor.getTrackCount();
    }

    @Override
    public void selectTrack(int index) {
        mMediaExtractor.selectTrack(index);
    }

    public MediaFormat getTrackFormat(int index) {
//        format.getLong(MediaFormat.KEY_DURATION);
        return mMediaExtractor.getTrackFormat(index);
    }

    public void seekTo(long timeUs, int mode) {
        mMediaExtractor.seekTo(timeUs, mode);
    }

    public boolean advance() {
        return mMediaExtractor.advance();
    }


    @Override
    public void prepare() throws IOException {
        mMediaExtractor.setDataSource(mFilename);
        int trackCount = mMediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {

        }
    }

    public int readSampleData(@NonNull ByteBuffer byteBuf, int offset) {
        return mMediaExtractor.readSampleData(byteBuf, offset);
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getEndTime() {
        return 0;
    }

    public int getSampleTrackIndex() {
        return mMediaExtractor.getSampleTrackIndex();
    }

    public long getSampleTime() {
        return mMediaExtractor.getSampleTime();
    }

    @Override
    public void getMediaFormat() {

    }

    @Override
    public void seekTo(long time) {
        long start = System.currentTimeMillis();
        mMediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        LogUtil.e(" "+(System.currentTimeMillis() - start));
    }


    public long getSampleSize() {
        return 0;
//        return mMediaExtractor.getSampleSize();
    }


    public int getSampleFlags() {
        return mMediaExtractor.getSampleFlags();
    }

    public long getCachedDuration() {
        return mMediaExtractor.getCachedDuration();
    }

    public boolean hasCacheReachedEndOfStream() {
        return mMediaExtractor.hasCacheReachedEndOfStream();
    }

    public int findMediaFormat(String type, MediaFormat mediaFormat) {
        int trackCount = mMediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mMediaExtractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).contains("type")) {
                mediaFormat = format;
                return i;
            }
        }
        return -1;
    }

}
