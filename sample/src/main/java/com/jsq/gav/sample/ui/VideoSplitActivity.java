package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jsq.gav.LogUtil;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.sample.utils.ExoDataSourceProvider;
import com.jsq.gav.widget.VideoEditView;
import com.jsq.gav.widget.VideoFrameFormat;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class VideoSplitActivity extends BaseActivity {

    private static final String KEY_FILE = "FILE";
    private SimpleExoPlayer mPlayer;
    private RecyclerView mRecyclerView;
    private Disposable mDisposable;


    public static void start(Context context, Uri file) {
        Intent starter = new Intent(context, VideoSplitActivity.class);
        starter.putExtra(KEY_FILE, file);
        context.startActivity(starter);
    }

    long currentPlayTime;
    long lastTimeMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_split);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        int padding = getResources().getDisplayMetrics().widthPixels >> 1;
        mRecyclerView.setPaddingRelative(padding, 0, 0, 0);
        mRecyclerView.setClipToPadding(false);
        mPlayer = ExoPlayerFactory.newSimpleInstance(this);
        Uri fileUri = getIntent().getParcelableExtra(KEY_FILE);

        mPlayer = ExoPlayerFactory.newSimpleInstance(this);

        mPlayer.prepare(ExoDataSourceProvider.createSingleDataSource(fileUri));

        mPlayer.setPlayWhenReady(true);
        VideoEditView videoEditView = findViewById(R.id.video_edit_view);
        videoEditView.setOnSurfaceAvailableListener(mPlayer::setVideoSurface);
        mPlayer.setVideoFrameMetadataListener((presentationTimeUs, releaseTimeNs, format) -> {
            videoEditView.setVideoFrameFormat(new VideoFrameFormat(format.width, format.height, format.rotationDegrees));
        });


        mPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    mDisposable.dispose();
                }
            }
        });
        List<File> fileList = genFileList(fileUri);

        float total = fileList.size() * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());

        mDisposable = Observable.interval(0, 16, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    long diff = mPlayer.getCurrentPosition() - lastTimeMs;
                    float rate = diff * 1f / mPlayer.getDuration();
                    float widthDiff = total * 1f * rate;

                    mRecyclerView.scrollBy((int) widthDiff, 0);
                    LogUtil.e("VideoSplitActivity: onCreate:" + widthDiff);
                    currentPlayTime = mPlayer.getCurrentPosition();
                    lastTimeMs = mPlayer.getCurrentPosition();
                });
//        ClippingMediaSource clippingMediaSource = new ClippingMediaSource()

        mRecyclerView.setAdapter(new InnerAdapter(fileList));
        extractImage(fileUri);

    }

    private List<File> genFileList(Uri fileUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        File dir = new File(getExternalCacheDir(), String.valueOf(fileUri.hashCode()));
        if (!dir.exists()) dir.mkdirs();
        retriever.setDataSource(this, fileUri);
        long duration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        retriever.release();
        List<File> fileList = new ArrayList<>();
        for (long i = 0; i < duration; i += 500) {
            fileList.add(new File(dir, String.format("%s.png", i)));
        }
        return fileList;
    }

    private void extractImage(Uri fileUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Disposable disposable = Observable.just(retriever)
                .map(mediaMetadataRetriever -> {
                    File dir = new File(getExternalCacheDir(), String.valueOf(fileUri.hashCode()));
                    if (!dir.exists()) dir.mkdirs();
                    retriever.setDataSource(this, fileUri);
                    long duration = Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    List<File> fileList = new ArrayList<>();
                    for (long i = 0; i < duration; i += 500) {
                        Bitmap bitmap = retriever.getFrameAtTime(i * 1000);
                        File file = new File(dir, String.format("%s.png", i));
                        if (!file.exists()) saveBitmap(bitmap, file);
                        bitmap.recycle();
                        fileList.add(file);
                    }
                    return fileList;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filepath -> {
                    LogUtil.e("VideoSplitActivity: extractImage" + filepath);

                }, Throwable::printStackTrace);


    }

    private void saveBitmap(Bitmap bitmap, File file) {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            outputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //bitmap.compress()

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
        if (!mDisposable.isDisposed()) mDisposable.dispose();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image_view);
        }
    }

    private static class InnerAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<File> mDataList;

        private InnerAdapter(List<File> dataList) {
            mDataList = dataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_image_40dp, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            Glide.with(viewHolder.itemView.getContext())
                    .load(mDataList.get(i))
                    .into(viewHolder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mDataList == null ? 0 : mDataList.size();
        }
    }


}
