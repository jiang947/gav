package com.jsq.gav;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;


/**
 * Created by jiang on 2019/5/17
 */

public class MediaCodecExtractor implements Extractor {

    private MediaExtractor mMediaExtractor;

    public MediaCodecExtractor() {
        mMediaExtractor = new MediaExtractor();
    }

    public void setDataSource(@NonNull Context context, @NonNull Uri uri,  @Nullable Map<String, String> headers) throws IOException {
        mMediaExtractor.setDataSource(context, uri, headers);
    }

    public void setDataSource(@NonNull String path) throws IOException {
        mMediaExtractor.setDataSource(path);
    }

    public void release() {
        mMediaExtractor.release();
    }

    public int getTrackCount() {
        return mMediaExtractor.getTrackCount();
    }

    public MediaFormat getTrackFormat(int index) {
        return mMediaExtractor.getTrackFormat(index);
    }

    public void selectTrack(int index) {
        mMediaExtractor.selectTrack(index);
    }

    public void unselectTrack(int index) {
        mMediaExtractor.unselectTrack(index);
    }

    public void seekTo(long timeUs, int mode) {
        mMediaExtractor.seekTo(timeUs, mode);
    }

    public boolean advance() {
        return mMediaExtractor.advance();
    }

    public int readSampleData(@NonNull ByteBuffer byteBuf, int offset) {
        return mMediaExtractor.readSampleData(byteBuf, offset);
    }

    public int getSampleTrackIndex() {
        return mMediaExtractor.getSampleTrackIndex();
    }

    public long getSampleTime() {
        return mMediaExtractor.getSampleTime();
    }


    public long getSampleSize() {
        return mMediaExtractor.getSampleSize();
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
}
