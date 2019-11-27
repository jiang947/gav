package com.jsq.gav.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.UiThread;

import com.jsq.gav.CameraUtils;
import com.jsq.gav.LogUtil;
import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.GlFilterConfig;
import com.jsq.gav.gles.OutputSurface;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.CameraFilter;
import com.jsq.gav.gles.filter.GlImageBeautyFilter;
import com.jsq.gav.gles.filter.GlLutFilterPager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jiang on 2019/4/26
 */

class CameraRenderThread extends Thread implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraRenderThread";

    /**
     * 记录支持的 FOCUS_MODE 和优先级
     */
    private static final List<String> FOCUS_MODE_PRIORITY;

    static {
        FOCUS_MODE_PRIORITY = new ArrayList<>();
        FOCUS_MODE_PRIORITY.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        FOCUS_MODE_PRIORITY.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODE_PRIORITY.add(Camera.Parameters.FOCUS_MODE_FIXED);
    }

    private final Object mLock = new Object();
    private boolean mReady = false;
    private Looper mLooper;

    private CameraRenderHandler mRenderHandler;
    private CameraPreviewView.CameraPreviewHandler mPreviewHandler;
    private Context mContext;

    private volatile List<CameraPreviewView.OnDrawFrameListener> mOnDrawFrameListeners;

    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private OutputSurface mOutputSurface;

    private Camera mCamera;
    private int mCameraWidth;
    private int mCameraHeight;
    private int fps = 30;

    private CameraFilter mCameraFilter;

    private int mCameraId;
    private Camera.Parameters mCameraParams;
    private int mViewWidth;
    private int mViewHeight;
    private Camera.PreviewCallback mPreviewCallback;


    private GlLutFilterPager mImageLookupFilter;
    private List<String> mLookupImagePathList;
    private final Handler mExtEventHandler;
    private GlLutFilterPager.LookupFilterScroller mFilterScroller;
    private GlLutFilterPager.OnPageChangeListener mOnPageChangeListener;
    private GlImageBeautyFilter mBeautyFilter;

    public CameraRenderThread(Context context,
                              CameraPreviewView.CameraPreviewHandler previewHandler,
                              int cameraId, Handler extEventHandler, GlLutFilterPager.LookupFilterScroller filterScroller) {
        super(new ThreadGroup("GL Thread"), "GL Thread");
        mPreviewHandler = previewHandler;
        mCameraId = cameraId;
        mContext = context;
        mExtEventHandler = extEventHandler;
        mFilterScroller = filterScroller;
    }

    @Override
    public void run() {
        // super.run(); 不运行传进来的 run
        Looper.prepare();
        mRenderHandler = new CameraRenderHandler(this);
        mEglCore = new EglCore();
        openCamera(mCameraId, mCameraWidth, mCameraHeight);
        if (mPreviewCallback != null) {
            mCamera.setPreviewCallback(mPreviewCallback);
        }
        synchronized (mLock) {
            mReady = true;
            mLooper = Looper.myLooper();
            mLock.notifyAll();
        }
        Looper.loop();

        // release some one
        releaseCamera();
        if (mOutputSurface != null) {
            mOutputSurface.release();
            mOutputSurface = null;
        }
        if (mDisplaySurface != null) {
            mDisplaySurface.release();
            mDisplaySurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private void openCamera(int cameraId, int width, int height) {
        if (mCamera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int numCameras = Camera.getNumberOfCameras();
//            for (int i = 0; i < numCameras; i++) {
//                Camera.getCameraInfo(i, info);
//                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                    mCamera = Camera.open(i);
//                    mCameraId = i;
//                    break;
//                }
//            }
            mCamera = Camera.open(cameraId);
            if (mCamera == null) {
                mCamera = Camera.open(); // back
                mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            mCameraParams = mCamera.getParameters();

            CameraUtils.choosePreviewSize(mCameraParams, width, height);
            int thousandFps = CameraUtils.chooseFixedPreviewFps(mCameraParams, fps * 1000);
            mCameraParams.setRecordingHint(true);
            CameraUtils.setAutoFocusMode(mCameraParams);
            mCamera.setParameters(mCameraParams);

//            int[] fpsRange = new int[2];
//            Camera.Size mCameraPreviewSize = mCameraParams.getPreviewSize();
//            mCameraParams.getPreviewFpsRange(fpsRange);
        }
    }

    private void startPreview(SurfaceTexture texture) {
        try {
            mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        mDisplaySurface = new WindowSurface(mEglCore, surfaceTexture);
        mDisplaySurface.makeCurrent();

        Log.e(TAG, "surfaceAvailable: width:" + width + " height:" + height);


        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraFilter = new CameraFilter(mContext, GlConstants.UV_COORDS);
            mCameraFilter.setRotation(90);
        } else {
            mCameraFilter = new CameraFilter(mContext, GlConstants.UV_COORDS_ROTATION_90);
            mCameraFilter.setRotation(270);
        }
        // width 和 height 是从View上传过来的 ，此处默认手机为竖屏，而android摄像头默认是横屏
        // 所以此处 width和height 是反的
        mCameraFilter.setSize(height, width);
        mCameraFilter.setPosition(width >> 1, height >> 1);
        // mCameraFilter 上面进行了90或者270度选装, 此处width 和 height 是对的
        mOutputSurface = new OutputSurface(mContext, width, height, mCameraFilter);

        mImageLookupFilter = new GlLutFilterPager(mContext);
        mImageLookupFilter.setOnPageChangeListener(mOnPageChangeListener, mPreviewHandler);
//        if (mLookupImagePathList != null) {
//            mImageLookupFilter.setLookupImageList(mLookupImagePathList);
//        }
        mFilterScroller.setup(mImageLookupFilter, width);
        mOutputSurface.addFilter(mImageLookupFilter);

        mBeautyFilter = new GlImageBeautyFilter(mContext);
        mBeautyFilter.setSize(width, height);
        mOutputSurface.addFilter(mBeautyFilter);


        mOutputSurface.setOnFrameAvailableListener(this);
        startPreview(mOutputSurface.getSurfaceTexture());
    }

    public void surfaceChanged(int width, int height) {
        try {
            mCamera.setPreviewTexture(mOutputSurface.getSurfaceTexture());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    private Looper getLooper() {
        if (mLooper == null) {
            await();
        }
        return mLooper;
    }

    public void quit() {
        getLooper().quit();
    }

    private void startFocusing(int radius, int x, int y) {
        if (mCamera == null) return;
        List<String> focusModes = mCameraParams.getSupportedFocusModes();
        int areaRadiusX = (int) (radius * (2000f / mViewWidth));
        int areaRadiusY = (int) (radius * (2000f / mViewHeight));
        int areaLeft = (int) Math.max(-1000, (x - areaRadiusX - mViewWidth >> 1) * (2000f / mViewWidth));
        int areaTop = (int) Math.max(-1000, (y - areaRadiusY - mViewHeight >> 1) * (2000f / mViewHeight));
        int areaRight = (int) Math.min(1000, (x + areaRadiusX - (mViewWidth >> 1)) * (2000f / mViewWidth));
        int areaBottom = (int) Math.min(1000, (y + areaRadiusY - (mViewHeight >> 1)) * (2000f / mViewHeight));
        List<Camera.Area> areas = new ArrayList<>();
        Rect rect = new Rect(areaLeft, areaTop, areaRight, areaBottom);
        areas.add(new Camera.Area(rect, 1000));
        for (String mode : FOCUS_MODE_PRIORITY) {
            if (focusModes.contains(mode)) {
                if (mCameraParams.getMaxNumMeteringAreas() > 0) {
                    mCameraParams.setMeteringAreas(areas);
                }
                if (mCameraParams.getMaxNumFocusAreas() > 0) {
                    mCameraParams.setFocusAreas(areas);
                }
                mCamera.cancelAutoFocus();
                mCamera.setParameters(mCameraParams);
                mCameraParams.getMaxNumDetectedFaces();

                mCameraParams.setFocusMode(mode);
                mCamera.autoFocus((success, camera) -> {
                    LogUtil.d("" + success);
                });
                break;
            }
        }


    }

    private void addFilter(GlFilterConfig config) {
        mOutputSurface.addFilterConfig(config);
    }


    @UiThread
    public void await() {
        synchronized (mLock) {
            while (!mReady) {
                try {
                    mLock.wait(100);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    public CameraRenderHandler getHandler() {
        return mRenderHandler;
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mOutputSurface.getSurfaceTexture().updateTexImage();
//        drawFrame(surfaceTexture.getTimestamp());
        mRenderHandler.sendDrawFrame(surfaceTexture.getTimestamp());
    }

    private void onScreenshot(CameraPreviewView.OnScreenshotListener listener) {
        mOutputSurface.saveFrame(listener, mExtEventHandler);
    }


    private void drawFrame(long presentationTimeNs) {
        mDisplaySurface.makeCurrent();
        int texture = mOutputSurface.drawFrame(presentationTimeNs);
        mDisplaySurface.swapBuffers();

        if (mOnDrawFrameListeners != null) {
            for (CameraPreviewView.OnDrawFrameListener listener : mOnDrawFrameListeners) {
                LogUtil.e("call listener");
                listener.onDrawFrame(texture);
            }
        }
        GLES20.glFinish();
    }


    public void setCameraPreviewSize(int width, int height) {
        this.mCameraWidth = width;
        this.mCameraHeight = height;
    }

    public void addOnDrawFrameListener(CameraPreviewView.OnDrawFrameListener listener) {
        if (mOnDrawFrameListeners == null) {
            mOnDrawFrameListeners = new ArrayList<>();
            mOnDrawFrameListeners = Collections.synchronizedList(mOnDrawFrameListeners);
        }
        mOnDrawFrameListeners.add(listener);
    }

    public void removeOnDrawFrameListener() {
        for (int i = 0; i < mOnDrawFrameListeners.size(); i++) {
            mOnDrawFrameListeners.remove(i);
        }
        LogUtil.e("Listeners is removed");
    }

    public void addOnDrawFrameListener(List<CameraPreviewView.OnDrawFrameListener> listeners) {
        if (mOnDrawFrameListeners == null) {
            mOnDrawFrameListeners = new ArrayList<>();
            mOnDrawFrameListeners = Collections.synchronizedList(mOnDrawFrameListeners);
        }
        mOnDrawFrameListeners.addAll(listeners);
    }

    public void setLookupOnPageChangeListener(GlLutFilterPager.OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
        if (mImageLookupFilter == null) return;
        mImageLookupFilter.setOnPageChangeListener(listener, mPreviewHandler);
    }

    //CameraPreviewView.onSurfaceTextureAvailable 重设了此值
    public void setLookupFilterImageList(List<String> list) {
        if (mImageLookupFilter == null) return;
        mImageLookupFilter.setLookupImageList(list);
    }

    //CameraPreviewView.onSurfaceTextureAvailable 重设了此值
    public void setLookupFilterIndex(int index) {
        if (mImageLookupFilter == null) return;
        mImageLookupFilter.setCurrentItem(index);
    }

    //CameraPreviewView.onSurfaceTextureAvailable 重设了此值
    public void setBeautyIntensity(float intensity) {
        if (mBeautyFilter == null) return;
        mBeautyFilter.setIntensity(intensity);
    }


    public int CameraId() {
        return mCameraId;
    }


    static class CameraRenderHandler extends Handler {

        private static final int MSG_QUIT = 0;
        private static final int MSG_SURFACE_AVAILABLE = 1;
        private static final int MSG_PREPARE_SIZE = 2;

        private static final int MSG_FOCUSING = 5;
        private static final int MSG_ADD_FILTER = 6;
        private static final int MSG_STOP_PREVIEW = 7;
        private static final int MSG_SET_BEAUTY_INTENSITY = 8;
        private static final int MSG_SET_LOOKUP_FILTER_IMAGE_LIST = 9;
        private static final int MSG_SET_LOOKUP_FILTER_INDEX = 10;
        private static final int MSG_DRAW_FRAME = 12;
        private static final int MSG_SCREENSHOT = 13;

        private WeakReference<CameraRenderThread> mWeakRender;

        CameraRenderHandler(CameraRenderThread cameraRenderThread) {
            mWeakRender = new WeakReference<>(cameraRenderThread);
        }

        public void sendQuit() {
            sendMessage(obtainMessage(MSG_QUIT));
        }

        public void sendSurfaceAvailable(SurfaceTexture texture, int width, int height) {
            sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE, width, height, texture));
        }

        public void sendPrepareSize(int width, int height) {
            sendMessage(obtainMessage(MSG_PREPARE_SIZE, width, height));
        }

        public void sendSetFocusing(int radius, int x, int y) {
            sendMessage(obtainMessage(MSG_FOCUSING, radius, x, y));
        }

        public void sendAddFilter(GlFilterConfig config) {
            sendMessage(obtainMessage(MSG_ADD_FILTER, config));
        }


        public void sendSetBeautyIntensity(float intensity) {
            sendMessage(obtainMessage(MSG_SET_BEAUTY_INTENSITY, intensity));
        }

        public void sendSetLookupFilterImageList(List<String> list) {
            sendMessage(obtainMessage(MSG_SET_LOOKUP_FILTER_IMAGE_LIST, list));
        }

        public void sendSetLookupFilterIndex(int index) {
            sendMessage(obtainMessage(MSG_SET_LOOKUP_FILTER_INDEX, index));
        }

        public void sendStopPreview() {
            sendMessage(obtainMessage(MSG_STOP_PREVIEW));
        }


        public void sendDrawFrame(long time) {
            sendMessage(obtainMessage(MSG_DRAW_FRAME, time));
        }


        public void sendScreenshot(CameraPreviewView.OnScreenshotListener listener) {
            sendMessage(obtainMessage(MSG_SCREENSHOT, listener));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            if (mWeakRender.get() == null) return;
            switch (msg.what) {
                case MSG_QUIT:
                    mWeakRender.get().quit();
                    break;
                case MSG_SURFACE_AVAILABLE:
                    mWeakRender.get().surfaceAvailable((SurfaceTexture) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_PREPARE_SIZE:
                    mWeakRender.get().surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_FOCUSING:
                    mWeakRender.get().startFocusing(((int) msg.obj), msg.arg1, msg.arg2);
                    break;
                case MSG_ADD_FILTER:
                    mWeakRender.get().addFilter((GlFilterConfig) msg.obj);
                    break;
                case MSG_STOP_PREVIEW:
                    mWeakRender.get().stopPreview();
                    break;
                case MSG_SET_BEAUTY_INTENSITY:
                    mWeakRender.get().setBeautyIntensity(((float) msg.obj));
                    break;
                case MSG_SET_LOOKUP_FILTER_IMAGE_LIST:
                    mWeakRender.get().setLookupFilterImageList((List<String>) msg.obj);
                    break;
                case MSG_SET_LOOKUP_FILTER_INDEX:
                    mWeakRender.get().setLookupFilterIndex((int) msg.obj);
                    break;
                case MSG_DRAW_FRAME:
                    mWeakRender.get().drawFrame((Long) msg.obj);
                    break;
                case MSG_SCREENSHOT:
                    mWeakRender.get().onScreenshot((CameraPreviewView.OnScreenshotListener) msg.obj);
                    break;

            }
        }
    }

    private void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
}
