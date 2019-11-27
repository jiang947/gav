package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.jsq.gav.FrameSaver;
import com.jsq.gav.LogUtil;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;
import com.jsq.gav.widget.CameraPreviewView;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jiang on 2019-08-23
 */


public class TakePictureActivity extends BaseActivity {


    private CameraPreviewView mCameraPreviewView;

    public static void start(Context context) {
        Intent starter = new Intent(context, TakePictureActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        mCameraPreviewView = findViewById(R.id.camera_preview_view);

        FrameSaver saver = new FrameSaver(this);
        saver.setSize(720, 1280);
        mCameraPreviewView.addOnDrawFrameListener(saver);
        findViewById(R.id.take_picture)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saver.save(bmp -> {
                            BufferedOutputStream bos = null;
                            try {
                                bos = new BufferedOutputStream(new FileOutputStream("/storage/emulated/0/Android/data/com.jsq.video.simple/cache/1.jpg"));
                                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                                bmp.recycle();
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
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraPreviewView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mCameraPreviewView.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
