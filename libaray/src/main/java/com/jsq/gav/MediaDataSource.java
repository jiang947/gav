package com.jsq.gav;

import android.media.MediaFormat;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/17
 */

public interface MediaDataSource {


    int readSampleData(@NonNull ByteBuffer byteBuf, int offset);

    long getStartTime();

    long getEndTime();

    boolean advance();

    void prepare() throws IOException;

    MediaFormat getTrackFormat(int index);

    int getTrackCount();

    void selectTrack(int index);

    int getSampleTrackIndex();

    long getSampleTime();


    void getMediaFormat();

    void seekTo(long time);

}
