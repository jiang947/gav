package com.jsq.gav.sample.utils;

import android.Manifest;

/**
 * Created by jiang on 2019/5/13
 */

public interface Constants {

    interface PermissionGroup {

        String[] RECORD = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };

        String[] ALBUM = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };




    }

}
