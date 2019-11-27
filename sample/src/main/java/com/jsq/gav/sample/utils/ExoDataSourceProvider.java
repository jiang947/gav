package com.jsq.gav.sample.utils;

import android.net.Uri;

import com.jsq.gav.sample.App;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

/**
 * Created by jiang on 2019/5/9
 */

public class ExoDataSourceProvider {

    private static final DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(App.get(),
            Util.getUserAgent(App.get(), "yourApplicationName"));

    public static MediaSource createSingleDataSource(Uri uri) {
        return new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
    }


    public static MediaSource createLoopDataSource(Uri uri, int loopCount) {
        return new LoopingMediaSource(createSingleDataSource(uri), loopCount);
    }

    public static MediaSource createLoopDataSource(MediaSource mediaSource){
        return new LoopingMediaSource(mediaSource);
    }

    public static MediaSource createClipDataSource(Uri uri, long startPositionUs, long endPositionUs) {
        return new ClippingMediaSource(createSingleDataSource(uri), startPositionUs, endPositionUs);
    }

    public static MediaSource createMergesDataSource(Uri... uri) {
        ArrayList<MediaSource> sources = new ArrayList<>();
        for (Uri u :uri){
            sources.add(createSingleDataSource(u));
        }
        return new MergingMediaSource(sources.toArray(new MediaSource[0]));
    }


}
