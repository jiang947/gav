package com.jsq.gav.sample.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jsq.gav.sample.R;
import com.jsq.gav.sample.utils.Glide4Engine;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

public class ExtractVideoFramesActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 934;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract_video_frames);


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
                .forResult(REQUEST_CODE);


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE && data != null) {

            }
        }
    }
}
