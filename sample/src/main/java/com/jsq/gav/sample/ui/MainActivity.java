package com.jsq.gav.sample.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.jsq.gav.BitmapExtractor;
import com.jsq.gav.LogUtil;
import com.jsq.gav.MediaSourceInfo;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.sample.utils.Constants;
import com.jsq.gav.sample.utils.Glide4Engine;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jiang on 2019/4/22
 */

/**
 * 1.图片合成视频
 * 2.ffmpeg gif encoder
 */
public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_EXTRACTOR_IMAGE = 2;
    private static final int REQUEST_SEEK_TEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogUtil.e(getExternalCacheDir().getAbsolutePath());
    }

    public void toRecordActivity(View view) {
        addDisposable(new RxPermissions(this).request(Constants.PermissionGroup.RECORD)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        RecordActivity.start(this);
                    } else {
                        Toast.makeText(this, "需要权限!", Toast.LENGTH_SHORT).show();
                    }
                }, Throwable::printStackTrace));
    }

    public void importVideo(View view) {
        RxPermissions rxPermissions = new RxPermissions(this);
        addDisposable(rxPermissions.request(Constants.PermissionGroup.ALBUM)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        Matisse.from(this)
                                .choose(MimeType.ofVideo())
                                .countable(true)
                                .capture(false)
                                .maxSelectable(9)
                                .showSingleMediaType(true)
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                .thumbnailScale(0.85f)
                                .imageEngine(new Glide4Engine())
                                .theme(R.style.Matisse_Dracula)
                                .originalEnable(true)
                                .forResult(REQUEST_CODE_EDIT);
                    }
                }, Throwable::printStackTrace));
    }

    public void startSimpleListActivity(View view) {
        SimpleListActivity.start(this);
    }

    public void onChangeClick(View view) {
        //            getSupportColorFormat();
//        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
//        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
//        for (MediaCodecInfo info : codecInfos) {
//            if (info.isEncoder()) continue;
//
//            if (info.getName().equals("OMX.google.mp3.decoder")) {
////                    for (String type : info.getSupportedTypes()) {
////                        LogUtil.e(type);
////                    }
//                info.getCapabilitiesForType("audio/mpeg");
//            }
//        }
        TakePictureActivity.start(this);
    }


    public void extractorImage(View view) {
        RxPermissions rxPermissions = new RxPermissions(this);
        addDisposable(rxPermissions.request(Constants.PermissionGroup.ALBUM)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        Matisse.from(this)
                                .choose(MimeType.ofVideo())
                                .countable(true)
                                .capture(false)
                                .maxSelectable(9)
                                .showSingleMediaType(true)
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                .thumbnailScale(0.85f)
                                .imageEngine(new Glide4Engine())
                                .theme(R.style.Matisse_Dracula)
                                .originalEnable(true)
                                .forResult(REQUEST_EXTRACTOR_IMAGE);
                    }
                }, Throwable::printStackTrace));
    }


    private void ex(List<String> filepath) {
        File dir = new File(getExternalCacheDir(), "" + System.currentTimeMillis());
        dir.mkdirs();
        imageIndex = 0;
        List<MediaSourceInfo> uris = new ArrayList<>();
        for (String s : filepath) {
            Uri path = Uri.parse(s);
            MediaSourceInfo info = new MediaSourceInfo(path);
            info.startTimeUs = 1000_000;
            uris.add(info);
        }
        LogUtil.e(uris.toString());
        long start = System.currentTimeMillis();
        extract(uris)
                .observeOn(Schedulers.io())
                .map(result -> saveBitmap(result, dir))
                .toList()
                .toObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(System.out::println, Throwable::printStackTrace, new Action() {
                    @Override
                    public void run() throws Exception {
                        LogUtil.e("consume:" + (System.currentTimeMillis() - start));
                    }
                });
    }

    private Observable<Bitmap> extract(List<MediaSourceInfo> uris) {
        return Observable.defer(() -> Observable.create(emitter -> new BitmapExtractor.Builder()
                .setContext(this)
                .setSourceInfoList(uris)
                .setExtractFrames(10)
                .setUseVirtualCut(true)
                .setSize(256)
                .setBitmapAvailableListener(emitter::onNext)
                .setOnExtractCompleteListener(emitter::onComplete)
                .setOnExtractErrorListener(emitter::onError)
                .build()
                .start()));
    }


    int imageIndex = 0;

    private String saveBitmap(Bitmap bitmap, File dir) throws IOException {
        imageIndex++;
        File file = new File(dir, String.format(Locale.US, "frame%02d.jpg", imageIndex));
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            return file.getPath();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void finish() {
        super.finish();
        System.exit(0);
    }

    public void seek(View view) {

        RxPermissions rxPermissions = new RxPermissions(this);
        addDisposable(rxPermissions.request(Constants.PermissionGroup.ALBUM)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        Matisse.from(this)
                                .choose(MimeType.ofVideo())
                                .countable(true)
                                .capture(false)
                                .maxSelectable(9)
                                .showSingleMediaType(true)
                                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                .thumbnailScale(0.85f)
                                .imageEngine(new Glide4Engine())
                                .theme(R.style.Matisse_Dracula)
                                .originalEnable(true)
                                .forResult(REQUEST_SEEK_TEST);
                    }
                }, Throwable::printStackTrace));
    }

    private void seekTest(List<String> strings) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(strings.get(0));
            mediaExtractor.selectTrack(0);
            LogUtil.e("first:" + mediaExtractor.getSampleTime() + "");
            mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            LogUtil.e("seek to 0:" + mediaExtractor.getSampleTime());

            mediaExtractor.advance();
            mediaExtractor.seekTo(mediaExtractor.getSampleTime(), MediaExtractor.SEEK_TO_NEXT_SYNC);
            LogUtil.e(":" + mediaExtractor.getSampleTime());

            mediaExtractor.advance();
            mediaExtractor.seekTo(mediaExtractor.getSampleTime(), MediaExtractor.SEEK_TO_NEXT_SYNC);
            LogUtil.e(":" + mediaExtractor.getSampleTime());

            mediaExtractor.advance();
            mediaExtractor.seekTo(mediaExtractor.getSampleTime(), MediaExtractor.SEEK_TO_NEXT_SYNC);
            LogUtil.e(":" + mediaExtractor.getSampleTime());

            mediaExtractor.advance();
            mediaExtractor.seekTo(mediaExtractor.getSampleTime(), MediaExtractor.SEEK_TO_NEXT_SYNC);
            LogUtil.e(":" + mediaExtractor.getSampleTime());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaExtractor.release();
    }

    static class Test implements Runnable {
        private String name;
        private Semaphore mSemaphore;

        Test(String name, Semaphore semaphore) {
            this.name = name;
            mSemaphore = semaphore;
        }

        @Override
        public void run() {
            try {
                mSemaphore.acquire(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(name);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_EDIT) {
                ArrayList<Uri> selected = (ArrayList<Uri>) Matisse.obtainResult(data);
                if (selected != null) {
                    ArrayList<MediaSourceInfo> infos = new ArrayList<>();
                    for (Uri uri : selected) {

                        infos.add(new MediaSourceInfo(uri));
                    }
                    VideoEditActivity.start(this, infos);
                }
            }
            if (requestCode == REQUEST_EXTRACTOR_IMAGE) {
                List<String> strings = Matisse.obtainPathResult(data);
                if (strings != null && !strings.isEmpty()) {
                    ex(strings);
                }
            }
            if (requestCode == REQUEST_SEEK_TEST) {
                List<String> strings = Matisse.obtainPathResult(data);
                if (strings != null && !strings.isEmpty()) {
                    seekTest(strings);
                }
            }

        }
    }


    private int getSupportColorFormat() {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            for (int j = 0; j < types.length && !found; j++) {
                if (types[j].equals("video/avc")) {
                    System.out.println("found");
                    found = true;
                }
            }
            if (!found)
                continue;
            codecInfo = info;
        }

        Log.e("AvcEncoder", "Found " + codecInfo.getName() + " supporting " + "video/avc");

        // Find a color profile that the codec supports
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
        Log.e("AvcEncoder",
                "length-" + capabilities.colorFormats.length + "==" + Arrays.toString(capabilities.colorFormats));

        for (int i = 0; i < capabilities.colorFormats.length; i++) {

            switch (capabilities.colorFormats[i]) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                    Log.e("AvcEncoder", "supported color format::" + capabilities.colorFormats[i]);
                    break;

                default:
                    Log.e("AvcEncoder", "other color format " + capabilities.colorFormats[i]);
                    break;
            }
        }
        //return capabilities.colorFormats[i];
        return 0;
    }
}
