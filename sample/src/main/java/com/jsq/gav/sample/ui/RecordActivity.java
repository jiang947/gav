package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import com.jsq.gav.LogUtil;
import com.jsq.gav.MediaRecorder;
import com.jsq.gav.MediaSourceInfo;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.widget.CameraPreviewView;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RecordActivity extends BaseActivity implements MediaRecorder.RecordEventListener {


    private int index = 1;

    private final static Map<String, String> sizeMap = ImmutableMap.of(
            "9:16", "720x1280",
            "3:4", "720x960",
            "1:1", "720x720");
    private final static List<String> sizeKeys = Arrays.asList(sizeMap.keySet().toArray(new String[0]));

    private int currentSizeIndex = 0;

    private ArrayList<String> videoPathList = new ArrayList<>();

    private MediaRecorder mMediaRecorder;
    private Disposable mRecordProcessDisposable;
    private TextView mBtnRecord;
    private CameraPreviewView cameraPreviewView;
    private TextView changeAspectRatioView;

    public static void start(Context context) {
        Intent starter = new Intent(context, RecordActivity.class);
        context.startActivity(starter);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mMediaRecorder = new MediaRecorder(this);
        mMediaRecorder.setRecordEventListener(this);
        Size size = Size.parseSize(sizeMap.get(sizeKeys.get(currentSizeIndex)));
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        cameraPreviewView = findViewById(R.id.camera_preview_view);
        cameraPreviewView.setIsFullScreen(true);
        mMediaRecorder.setPreviewDisplay(cameraPreviewView);
        mMediaRecorder.setVideoFrameRate(30);
        cameraPreviewView.setLutFilterPagerChangeListener(item -> {
            LogUtil.e("setLutFilterPagerChangeListener:" + item);
        });

        findViewById(R.id.take_picture).setOnClickListener(v -> {
            long start = System.currentTimeMillis();
            cameraPreviewView.screenshot(bitmap -> {
                LogUtil.e("save " + Thread.currentThread().getName());
                FileOutputStream bos = null;
                LogUtil.e("" + (System.currentTimeMillis() - start));
                try {
                    bos = new FileOutputStream("/storage/emulated/0/Android/data/com.jsq.video.simple/cache/1.jpg");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    bitmap.recycle();
                    LogUtil.e("/storage/emulated/0/Android/data/com.jsq.video.simple/cache/1.jpg");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null) {
                        try {
                            bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });


        Observable.just(getAssets())
                .flatMap(assetManager -> Observable.fromArray(assetManager.list("FilterResources/filter")))
                .map(s -> String.format("file:///android_asset/FilterResources/filter/%s", s))
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(strings -> {
                    cameraPreviewView.setLutFilterDataList(strings);
                }, Throwable::printStackTrace);


//        mMediaRecorder.setOnRecordListener(d -> LogUtil.e("RecordActivity: onRecordStopped" + Thread.currentThread().getName()));
//        mMediaRecorder.prepare();
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnRecord.setOnClickListener(v -> {
            if (!mMediaRecorder.isRecording()) {
                String filepath = new File(getExternalCacheDir(), System.currentTimeMillis() + ".mp4").getPath();
                LogUtil.e(filepath);
                try {
                    mMediaRecorder.start(filepath);
                } catch (IOException e) {
                    Log.e("RecordActivity", "start record", e);
                }
                videoPathList.add(filepath);
                mBtnRecord.setText("停止");
//                mRecordProcessDisposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(aLong -> {
//                            System.out.println(mMediaRecorder.getWrittenPresentationTimeMs());
//                        });
            } else {
                mMediaRecorder.stop();
                mBtnRecord.setText("开始");
                if (mRecordProcessDisposable != null && !mRecordProcessDisposable.isDisposed()) {
                    mRecordProcessDisposable.dispose();
                }
            }
        });
        findViewById(R.id.close).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btn_del).setOnClickListener(v -> {
            // del
            if (!videoPathList.isEmpty()) {
                int last = videoPathList.size() - 1;
                String filepath = videoPathList.get(last);
                File file = new File(filepath);
                if (file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    videoPathList.remove(last);

                }
            }
        });
        findViewById(R.id.btn_done).setOnClickListener(v -> {
            ArrayList<MediaSourceInfo> infoList = (ArrayList<MediaSourceInfo>) Observable
                    .fromIterable(videoPathList)
                    .map(Uri::parse)
                    .map(MediaSourceInfo::new)
                    .toList()
                    .blockingGet();
            VideoEditActivity.start(this, infoList);
        });
        changeAspectRatioView = findViewById(R.id.change_aspect_ratio);
        changeAspectRatioView.setText(sizeKeys.get(currentSizeIndex));
        changeAspectRatioView.setOnClickListener(v -> {
            change();
        });
        findViewById(R.id.toggle_camera).setOnClickListener(v -> cameraPreviewView.toggleCamera());

        findViewById(R.id.test).setOnClickListener(v -> {
            Handler handler = new Handler();
            for (int i = 0; i < 20; i++) {
                toggleRecorder();
                LogUtil.e(mMediaRecorder.isRecording()+"");
            }
        });
//        change();
    }


    private void toggleRecorder(){
        if (!mMediaRecorder.isRecording()) {
            String filepath = new File(getExternalCacheDir(), System.currentTimeMillis() + ".mp4").getPath();
            LogUtil.e(filepath);
            try {
                mMediaRecorder.start(filepath);
            } catch (IOException e) {
                Log.e("RecordActivity", "start record", e);
            }
            videoPathList.add(filepath);
            mBtnRecord.setText("停止");
        } else {
            mMediaRecorder.stop();
            mBtnRecord.setText("开始");
            if (mRecordProcessDisposable != null && !mRecordProcessDisposable.isDisposed()) {
                mRecordProcessDisposable.dispose();
            }
        }
    }

    @Override
    public void onRecordProgress(long duration) {
        LogUtil.d("录制进度:" + duration);
    }

    @Override
    public void onRecordComplete(String filename) {
        Toast.makeText(this, "录制完成", Toast.LENGTH_SHORT).show();
        LogUtil.d("录制完成:" + filename);
    }

    private void change() {
        currentSizeIndex++;
        String s1 = sizeKeys.get(currentSizeIndex % sizeKeys.size());
        cameraPreviewView.setIsFullScreen(sizeKeys.get(0).equals(s1));
        changeAspectRatioView.setText(s1);
        Size s = Size.parseSize(sizeMap.get(s1));
        mMediaRecorder.setVideoSize(s.getWidth(), s.getHeight());
        cameraPreviewView.refresh();
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraPreviewView.onResume();
        if (mMediaRecorder.isRecording()) {
            mBtnRecord.setText("停止");
        } else {
            mBtnRecord.setText("开始");
        }

//        mMediaRecorder.startPreview();
    }


    @Override
    protected void onPause() {
        super.onPause();
        cameraPreviewView.onPause();
//        mMediaRecorder.stopPreview();
        if (mMediaRecorder.isRecording()) {
            mMediaRecorder.stop();
        }

    }


}
