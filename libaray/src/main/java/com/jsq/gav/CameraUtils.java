package com.jsq.gav;

import android.hardware.Camera;
import android.util.Log;

import java.util.Collections;
import java.util.List;

/**
 * Created by jiang on 2019/4/22
 */

public class CameraUtils {


    private static final String TAG = "CameraUtils";

    public static void choosePreviewSize(Camera.Parameters params, int width, int height) {
        Camera.Size ppsfv = params.getPreferredPreviewSizeForVideo();
        List<Camera.Size> previewSizeList = params.getSupportedPreviewSizes();
        Collections.sort(previewSizeList, (o1, o2) -> o1.width * o1.height - o2.width * o2.height);
        for (Camera.Size size : previewSizeList) {
            Log.e(TAG, "choosePreviewSize: width:" + size.width + " height:" + size.height);
            if (size.width == width && size.height == height) {
                params.setPreviewSize(width, height);
                return;
            }
        }
        float scale = 1f * width / height;
        int listSize = previewSizeList.size();
        Camera.Size bestSize = null;
        for (int i = listSize / 2; i < listSize; i++) {
            Camera.Size size = previewSizeList.get(i);
            if (1f * size.width / size.height == scale) {
                bestSize = size;
                break;
            }
        }
        if (bestSize == null) {
            for (int i = listSize / 2; i >= 0; i--) {
                Camera.Size size = previewSizeList.get(i);
                if (1f * size.width / size.height == scale) {
                    bestSize = size;
                    break;
                }
            }
        }
        if (bestSize != null) {
            LogUtil.e("choose preview size:" + bestSize);
            params.setPreviewSize(bestSize.width, bestSize.height);
            return;
        }

        if (ppsfv != null) {
            params.setPreviewSize(ppsfv.width, ppsfv.height);
        }

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


    public static boolean setAutoFocusMode(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            return true;
        } else {
            return false;
        }

    }

}
