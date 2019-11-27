package com.jsq.gav.gles.filter;

import android.content.Context;

import com.jsq.gav.gles.GlConstants;

/**
 * Created by jiang on 2019/5/8
 */

public class VideoFilter extends CameraFilter {


    public VideoFilter(Context context) {
        super(context, GlConstants.UV_COORDS_ROTATION_90);
    }
}
