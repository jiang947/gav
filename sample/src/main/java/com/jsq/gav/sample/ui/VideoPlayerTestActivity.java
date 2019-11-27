package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jsq.gav.CompositeMediaPlayer;
import com.jsq.gav.DataSourceChain;
import com.jsq.gav.LogUtil;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;

import java.util.ArrayList;

/**
 * Created by jiang on 2019/5/7
 */

public class VideoPlayerTestActivity extends BaseActivity implements SurfaceHolder.Callback {

    private static final String KEY_SOURCE_FILE = "SOURCE_FILE";

    private SurfaceView mSurfaceView;
    private CompositeMediaPlayer mMediaPlayer;

    private ArrayList<String> mSourceFile;
    private SeekBar mSeekBar;

    public static void start(Context context, ArrayList<String> file) {
        Intent starter = new Intent(context, VideoPlayerTestActivity.class);
        starter.putStringArrayListExtra(KEY_SOURCE_FILE, file);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_plater_test);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(this);
        mMediaPlayer = new CompositeMediaPlayer();
        mSourceFile = getIntent().getStringArrayListExtra(KEY_SOURCE_FILE);
        LogUtil.e("VideoPlayerTestActivity: onCreate" + mSourceFile);

        DataSourceChain dataSourceChain = new DataSourceChain.Builder()
                .addMediaDataSource(mSourceFile.get(0))
//                .addMediaDataSource(mSourceFile.get(1))
                .build();
        mMediaPlayer.setDataSource(dataSourceChain);
//        mVideoPlayer.setDataSource(mSourceFile.get(0));
        findViewById(R.id.ctrl_pause).setOnClickListener(v -> {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                ((TextView) v).setText("开始");
            } else {
                mMediaPlayer.start();
                ((TextView) v).setText("暂停");
            }
        });
        findViewById(R.id.ctrl_stop).setOnClickListener(v -> {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                ((TextView) v).setText("开始");
            } else {
                mMediaPlayer.start();
                ((TextView) v).setText("停止");
            }
        });
        ViewGroup ctrlSpeedViewGroup = findViewById(R.id.ctrl_speed);
        for (int i = 0; i < ctrlSpeedViewGroup.getChildCount(); i++) {
            ctrlSpeedViewGroup.getChildAt(i).setOnClickListener(v -> {
                float sc = Float.parseFloat(((TextView) v).getText().toString());
                mMediaPlayer.setSpeedScale(sc);
            });
        }
//        mMediaPlayer.setOnErrorListener(new VideoPlayer.OnErrorListener() {
//            @Override
//            public void onError(Throwable throwable) {
//                throwable.printStackTrace();
//            }
//        });
        String s = mSourceFile.get(0);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(s);
        long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        retriever.release();

        mSeekBar = findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long us = (long) (durationMs * 1000f * progress / 100);
                mMediaPlayer.seekTo(us);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mMediaPlayer.setSurface(holder.getSurface());
        mMediaPlayer.prepare();
//        mVideoPlayer.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        mVideoPlayer.stop();
        mMediaPlayer.release();
    }
}
