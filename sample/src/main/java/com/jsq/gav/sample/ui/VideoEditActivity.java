package com.jsq.gav.sample.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jsq.gav.LogUtil;
import com.jsq.gav.MediaComposer;
import com.jsq.gav.MediaSourceInfo;
import com.jsq.gav.TimeScalePeriod;
import com.jsq.gav.gles.GlFilterConfig;
import com.jsq.gav.gles.filter.GlSubtitleFilter;
import com.jsq.gav.gles.filter.GlWatermarkFilter;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.sample.utils.ExoDataSourceProvider;
import com.jsq.gav.widget.VideoEditView;
import com.jsq.gav.widget.VideoFrameFormat;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class VideoEditActivity extends BaseActivity {

    private static final String KEY_TO_EDIT_INFO_LIST = "TO_EDIT_INFO__LIST";

    public static void start(Context context, ArrayList<MediaSourceInfo> fileList) {
        Intent starter = new Intent(context, VideoEditActivity.class);
        starter.putParcelableArrayListExtra(KEY_TO_EDIT_INFO_LIST, fileList);
        context.startActivity(starter);
    }

    private List<MediaSourceInfo> mVideoList;
    private VideoEditView mVideoEditView;
    private SimpleExoPlayer mPlayer;
    private VideoCoverAdapter mVideoCoverAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editer);
        mVideoList = getIntent().getParcelableArrayListExtra(KEY_TO_EDIT_INFO_LIST);
        mVideoEditView = findViewById(R.id.video_edit_view);
        mPlayer = ExoPlayerFactory.newSimpleInstance(this);

        mVideoEditView.setOnSurfaceAvailableListener(new VideoEditView.OnSurfaceAvailableListener() {
            @Override
            public void onSurfaceAvailable(Surface surface) {
                mPlayer.setVideoSurface(surface);
            }
        });

        mPlayer.setVideoFrameMetadataListener((presentationTimeUs, releaseTimeNs, format) -> {
            mVideoEditView.setVideoFrameFormat(new VideoFrameFormat(format.width, format.height, format.rotationDegrees));
            mVideoEditView.setPresentationTime(presentationTimeUs);
        });
        mPlayer.setPlaybackParameters(new PlaybackParameters(1));

//        Debug.startMethodTracing("extract");
//        Trace.beginSection();
        long start = System.currentTimeMillis();


        LogUtil.e("VideoEditActivity: " + (System.currentTimeMillis() - start));
//        Debug.stopMethodTracing();
        setupVideoRecyclerView();
        setupBottomPanel();
        setupSectionPanel();
        setupFilterPanel();
        findViewById(R.id.choose_effect_panel).setOnClickListener(v -> {
            VideoEffectActivity.start(this, mVideoList.get(mVideoCoverAdapter.getSelected()).source);
        });

//        float duration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000f;
//        TextView btnPlay = findViewById(R.id.btn_play);
//        btnPlay.setText(String.format("%.1fs", duration));
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        addDisposable(Observable.fromIterable(mVideoList)
                .map(info -> {
                    Bitmap bitmap;
                    retriever.setDataSource(this, info.source);
                    bitmap = retriever.getFrameAtTime();
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int max = Math.max(width, height);
                    if (max > 512) {
                        float scale = 512f / max;
                        int w = Math.round(scale * width);
                        int h = Math.round(scale * height);
                        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                    }
                    String path = new File(getExternalCacheDir(), System.currentTimeMillis() + ".jpg").getPath();

                    FileOutputStream outputStream = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    outputStream.flush();
                    outputStream.close();

                    info.videoCover = path;
                    return info;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaSourceInfo -> {
                    mVideoCoverAdapter.notifyItemChanged(mVideoList.indexOf(mediaSourceInfo));
                }, Throwable::printStackTrace, () -> {
                    retriever.release();
                    LogUtil.e("VideoEditActivity: Complete");
                }));

        findViewById(R.id.generate_video).setOnClickListener(v -> generateVideo());
        play(0);
    }

    int width = 720;
    int height = 1280;



    private void generateVideo() {
        mPlayer.setPlayWhenReady(false);
        String out = new File(getExternalCacheDir(), System.currentTimeMillis() + ".mp4").getPath();
        List<MediaSourceInfo> genList = new ArrayList<>(mVideoList);

//        for (MediaSourceInfo info : genList) {
//            info.isMute = true;
////            info.startTimeUs = 1000_000;
////            info.endTimeUs = 19000_000;
//        }


        ArrayList<GlFilterConfig> list = new ArrayList<>();
        list.add(genSubtitleFilter("先加一个字幕", 0, 2000));
        list.add(genSubtitleFilter("换个位置", 2000, 4000, width >> 1, (height >> 1) + 100));

        getExternalCacheDir();

        list.add(genSubtitleFilter("加个水印", 4000, 6000, width >> 1, (height >> 1) + 100));
        try (InputStream stream = getAssets().open("lookup.png")) {
            list.add(genWatermarkFilter(stream, 4000, 6000));
        } catch (IOException e) {
            e.printStackTrace();
        }
        list.add(genSubtitleFilter("速度 x2", 6000, 10_000, width >> 1, (height >> 1) + 100));
        list.add(genSubtitleFilter("速度 x0.5", 10_000, 14_000, width >> 1, (height >> 1) + 100));

//        filter.setBitmap(BitmapFactory.decodeStream(stream));
//        filter.setPosition(mVideoEditView.getWidth() >> 1, mVideooEditView.getHeight() >> 2);

        List<TimeScalePeriod> timeScalePeriodList = ImmutableList.of(
//                new TimeScalePeriod(0, 9000, 0.75f)
//                new TimeScalePeriod(10000, 20000, 0.5f)
//                new TimeScalePeriod(10_000, 18_000, 0.5f)
        );
        List<MediaSourceInfo> bgMusicList = new ArrayList<>();
//        bgMusicList.add(new MediaSourceInfo(Uri.parse("/storage/emulated/0/Music/1.aac"), 2000_000));
//        bgMusicList.add(new MediaSourceInfo(Uri.parse("/storage/emulated/0/Music/2.aac"), 5000_000));
        bgMusicList.add(new MediaSourceInfo(Uri.parse("/storage/emulated/0/Music/bg.mp3")));

        bgMusicList.get(0).paddingStartTimeUs = 1000_000;
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgress(0);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();
        MediaComposer.with(this)
                .width(width)
                .height(height)
                .frameRate(24)
                .sampleRate(44100)
                .mediaInfoList(genList)
                .backgroundMusic(bgMusicList)
//                .timeScalePeriodList(timeScalePeriodList)
//                .glFilterConfigs(list)
                .output(out)
                .listener(new MediaComposer.Listener() {
                    @Override
                    public void onProcess(double progress) {
                        LogUtil.e(progress + "===");
                        dialog.setProgress((int) (progress * 100));
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        dialog.dismiss();
                    }
                })
                .start();
    }


    private GlFilterConfig genSubtitleFilter(String text, long startMs, long endMs) {
        GlSubtitleFilter filter = new GlSubtitleFilter(this, text);
        filter.setPosition(width >> 1, height >> 1);
        return new GlFilterConfig(filter, startMs * 1000_000L, endMs * 1000_000L);
    }

    private GlFilterConfig genSubtitleFilter(String text, long startMs, long endMs, int x, int y) {
        GlSubtitleFilter filter = new GlSubtitleFilter(this, text);
        filter.setPosition(x, y);
        return new GlFilterConfig(filter, startMs * 1000_000L, endMs * 1000_000L);
    }

    private GlFilterConfig genWatermarkFilter(InputStream stream, long startMs, long endMs) {
//        InputStream stream = getAssets().open("lookup.png")
        GlWatermarkFilter filter = new GlWatermarkFilter(this);
        filter.setBitmap(BitmapFactory.decodeStream(stream));
        filter.setPosition(width >> 1, 0);
        return new GlFilterConfig(filter, startMs * 1000_000L, endMs * 1000_000L);
    }

    private void play(int position) {
        if (position == mVideoCoverAdapter.getSelected()) return;
        mVideoCoverAdapter.setSelected(position);
        MediaSourceInfo info = mVideoList.get(position);
        MediaSource mediaSource;
        mediaSource = ExoDataSourceProvider.createMergesDataSource(info.source);
        mediaSource = ExoDataSourceProvider.createLoopDataSource(mediaSource);
        mPlayer.prepare(mediaSource);
        mPlayer.setPlayWhenReady(true);
    }


    private void setupVideoRecyclerView() {
        RecyclerView videoRecyclerView = findViewById(R.id.video_recycler_view);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mVideoCoverAdapter = new VideoCoverAdapter();
        mVideoCoverAdapter.setOnItemClickListener((view, position) -> {
            play(position);
        });
        videoRecyclerView.setAdapter(mVideoCoverAdapter);
        mVideoCoverAdapter.submitList(mVideoList);
    }

    private void setupBottomPanel() {
        View sectionPanel = findViewById(R.id.section_panel);
        View filterPanel = findViewById(R.id.filter_panel);

        findViewById(R.id.choose_section_panel).setOnClickListener(v -> {
            filterPanel.setVisibility(View.GONE);
            sectionPanel.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.choose_filter_beauty_panel).setOnClickListener(v -> {
            filterPanel.setVisibility(View.VISIBLE);
            sectionPanel.setVisibility(View.GONE);
        });

    }

    private void setupSectionPanel() {
        findViewById(R.id.split_video).setOnClickListener(v -> {
            VideoSplitActivity.start(this, mVideoList.get(0).source);
        });

        findViewById(R.id.video_speed_scale).setOnClickListener(v -> {


        });

        findViewById(R.id.add_subtitle).setOnClickListener(v -> {
            GlSubtitleFilter filter = new GlSubtitleFilter(this, "你好");
            filter.setPosition(mVideoEditView.getWidth() >> 1, mVideoEditView.getHeight() >> 1);
            mVideoEditView.addFilter(filter);
        });

        findViewById(R.id.add_watermark).setOnClickListener(v -> {
            try (InputStream stream = getAssets().open("lookup.png")) {
                GlWatermarkFilter filter = new GlWatermarkFilter(this);
                filter.setBitmap(BitmapFactory.decodeStream(stream));
                filter.setPosition(mVideoEditView.getWidth() >> 1, mVideoEditView.getHeight() >> 2);

                mVideoEditView.addFilter(filter);
            } catch (IOException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void setupFilterPanel() {
        RecyclerView filterRecyclerView = findViewById(R.id.filter_recycler_view);
        filterRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        LookupAdapter adapter = new LookupAdapter((position, view, item) -> {
            mVideoEditView.setLutFilterIndex(position);
        });
//        mVideoEditView.setLutFilterDataList(item);
        filterRecyclerView.setAdapter(adapter);
        Observable.just(getAssets())
                .flatMap(assetManager -> Observable.fromArray(assetManager.list("FilterResources/filter")))
                .map(s -> String.format("file:///android_asset/FilterResources/filter/%s", s))
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(strings -> {
                    adapter.replace(strings);
                    mVideoEditView.setLutFilterImageList(strings);
                }, Throwable::printStackTrace);
        mVideoEditView.setLutFilterPagerChangeListener(item -> {
            LogUtil.e("setLutFilterPagerChangeListener:"+item);
        });
        RecyclerView beautyRecyclerView = findViewById(R.id.beauty_recycler_view);
        beautyRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        BeautyAdapter beautyAdapter = new BeautyAdapter((position, view, item) -> {
            mVideoEditView.setBeautyIntensity((position + 1) / 10f);
        });
        beautyRecyclerView.setAdapter(beautyAdapter);
        beautyAdapter.replace(ImmutableList.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));

        findViewById(R.id.choose_beauty_panel).setOnClickListener(v -> {
            filterRecyclerView.setVisibility(View.GONE);
            beautyRecyclerView.setVisibility(View.VISIBLE);
        });
        findViewById(R.id.choose_filter_panel).setOnClickListener(v -> {
            filterRecyclerView.setVisibility(View.VISIBLE);
            beautyRecyclerView.setVisibility(View.GONE);
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        mVideoEditView.onResume();
        mPlayer.setPlayWhenReady(true);

    }


    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }
}
