package com.jsq.gav.sample.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import com.jsq.gav.sample.R;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

/**
 * Created by jiang on 2019/5/31
 */

public class MatisseUtil {



    public static void openVideo(Activity activity, int requestCode){
        Matisse.from(activity)
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
                .forResult(requestCode);
    }


}
