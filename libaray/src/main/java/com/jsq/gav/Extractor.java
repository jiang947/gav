package com.jsq.gav;

import android.content.Context;
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

public interface Extractor {


    void setDataSource(@NonNull Context context, @NonNull Uri uri, @Nullable Map<String, String> headers) throws IOException;

    void setDataSource(@NonNull String path) throws IOException;

    void release();

    int getTrackCount();

    MediaFormat getTrackFormat(int index);

    void selectTrack(int index);

    void unselectTrack(int index);

    void seekTo(long timeUs, int mode);

    boolean advance();

    int readSampleData(@NonNull ByteBuffer byteBuf, int offset);

    int getSampleTrackIndex();

    long getSampleTime();

    long getSampleSize();

    int getSampleFlags();

    long getCachedDuration();

    boolean hasCacheReachedEndOfStream();


}
