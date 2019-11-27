package com.jsq.gav.sample.utils;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by jiang on 2019/4/22
 */

public class CameraUtils {


    public static void choosePreviewSize(Camera.Parameters params, int width, int height) {
        Camera.Size ppsfv = params.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
//            Log.d(TAG, "Camera preferred preview size for video is " +
//                    ppsfv.width + "x" + ppsfv.height);
        }


        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                params.setPreviewSize(width, height);
                return;
            }
        }

//        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            params.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }


    public static int chooseFixedPreviewFps(Camera.Parameters params, int desiredThousandFps) {
        List<int[]> supported = params.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                params.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        params.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;
        }

//        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }

}
