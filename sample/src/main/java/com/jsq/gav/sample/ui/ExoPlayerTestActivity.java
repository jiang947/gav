package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.Nullable;

import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;

public class ExoPlayerTestActivity extends BaseActivity implements TextureView.SurfaceTextureListener {

    private static final String KEY_FILES = "FILES";
    private SimpleExoPlayer mPlayer;
    //    private ExoPlayer mPlayer;
    private PlayerView playerView;
    private Surface mSurface;
    private TextureView mSurfaceView;
    private SurfaceTexture mSurfaceTexture;

    public static void start(Context context, ArrayList<Uri> selected) {
        Intent starter = new Intent(context, ExoPlayerTestActivity.class);
        starter.putParcelableArrayListExtra(KEY_FILES, selected);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exo_player_test);
        ArrayList<Uri> files = getIntent().getParcelableArrayListExtra(KEY_FILES);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "yourApplicationName"));

        ConcatenatingMediaSource concatenatingMediaSource = Observable.fromIterable(files)
                .map(new ExtractorMediaSource.Factory(dataSourceFactory)::createMediaSource)
                .collect(ConcatenatingMediaSource::new, ConcatenatingMediaSource::addMediaSource)
                .blockingGet();
        mSurfaceView = findViewById(R.id.preview_view);

        MediaCodecSelector selector = new MediaCodecSelector() {
            @Override
            public List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
                List<MediaCodecInfo> decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder);
                return decoderInfos.isEmpty() ? Collections.emptyList() : Collections.singletonList(decoderInfos.get(0));
            }

            @Nullable
            @Override
            public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                return MediaCodecUtil.getPassthroughDecoderInfo();
            }
        };

        DefaultTrackSelector defaultTrackSelector = new DefaultTrackSelector();

//        mPlayer = ExoPlayerFactory.newInstance(new Renderer[]{new MediaCodecAudioRenderer(this, selector)}, defaultTrackSelector);

        mPlayer = ExoPlayerFactory.newSimpleInstance(this);


        PlaybackParameters param = new PlaybackParameters(2f);

//        mPlayer.setVideoTextureView(findViewById(R.id.texture_view));
        Uri videoUri = Uri.parse("storage/emulated/0/Music/1.aac");
        Uri videoUri2 = Uri.parse("storage/emulated/0/Music/2.aac");
//        mPlayer.prepare(concatenatingMediaSource);
        mPlayer.prepare(concatenatingMediaSource);


        mPlayer.setPlayWhenReady(true);
//        mPlayer.setPlaybackParameters(param);
        findViewById(R.id.s2).setOnClickListener(v -> mPlayer.setPlaybackParameters(new PlaybackParameters(0.5f)));
//        mPlayer.setVideoTextureView(surfaceView);
        mSurfaceView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //new Handler().postDelayed(() -> mPlayer.setPlayWhenReady(true),200);
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTexture != null) {
            mSurfaceView.setSurfaceTexture(mSurfaceTexture);
        }else {
            mSurfaceTexture = surface;
            mSurface = new Surface(surface);
            mPlayer.setVideoSurface(mSurface);
        }

//        if (mSurface == null) {
//            mSurface = new Surface(surface);
//            mPlayer.setVideoSurface(mSurface);
//        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
