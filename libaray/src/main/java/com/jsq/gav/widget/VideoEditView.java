package com.jsq.gav.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.GlFilterConfig;
import com.jsq.gav.gles.OutputSurface;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.GlFilter;
import com.jsq.gav.gles.filter.GlImageBeautyFilter;
import com.jsq.gav.gles.filter.GlLutFilterPager;
import com.jsq.gav.gles.filter.VideoFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/5/5
 */

public class VideoEditView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private static final int textureViewId = generateViewId();
    private static final int MSG_CALLBACK_SURFACE = 851;

    private static final int MSG_PREPARE_SURFACE = 64;
    private static final int MSG_FRAME_INFO = 523;
    private static final int MSG_SET_LOOKUP_IMAGE = 449;
    private static final int MSG_SET_LOOKUP_IMAGE_INDEX = 500;
    private static final int MSG_SET_BEAUTY_INTENSITY = 370;
    private static final int MSG_ADD_FILTER = 510;
    private static final int MSG_RELEASE = 574;
    private static final int MSG_RENDERING = 576;

    private OnSurfaceAvailableListener mOnSurfaceAvailableListener;

    private HandlerThread mRenderThread;
    private Handler mRenderEventHandler;
    private Handler mCallbackEventHandler;

    private TextureView mTextureView;

    private WindowSurface mWindowSurface;
    private EglCore mEglCore;
    private OutputSurface mOutputSurface;
    private VideoFilter mVideoFilter;
    private volatile long mPresentationTimeNs;
    private volatile int mVideoWidth;
    private volatile int mVideoHeight;

    private boolean isReady = false;

    private GlLutFilterPager mImageLookupFilter;
    private List<String> mLutFilterImageList;
    private int mLutIndex = -1;
    private GlLutFilterPager.LookupFilterScroller mFilterScroller;
    private GlLutFilterPager.OnPageChangeListener mOnPageChangeListener;
    private GlImageBeautyFilter mBeautyFilter;
    private float mBeautyIntensity = -1;


    public VideoEditView(Context context) {
        super(context);
        init();
    }

    public VideoEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoEditView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode()) return;
        mCallbackEventHandler = new Handler(Looper.getMainLooper(), this::handleCallbackMessage);
        mFilterScroller = new GlLutFilterPager.LookupFilterScroller(getContext());

        mRenderThread = new HandlerThread("GL HandlerThread");
        mRenderThread.start();
        mRenderEventHandler = new Handler(mRenderThread.getLooper(), this::handleRenderMessage);
        mFilterScroller.setRenderEventHandler(mRenderEventHandler);

        mTextureView = new TextureView(getContext());
        mTextureView.setId(textureViewId);
        addView(mTextureView);
        mTextureView.setSurfaceTextureListener(this);
    }

    private void addTextureView() {
        if (findViewById(textureViewId) != null) {
            throw new RuntimeException("CameraPreviewView上已经有一个TextureView了");
        }
        addView(mTextureView, 0);
    }


    public void onResume() {
//        if (findViewById(textureViewId) != null) {
//            removeView(mTextureView);
//            mTextureView.setSurfaceTextureListener(null);
//        }
//        mTextureView = new TextureView(getContext());
//        addTextureView();
//        mTextureView.setSurfaceTextureListener(this);
    }

    private boolean handleCallbackMessage(Message msg) {
        switch (msg.what) {
            case MSG_CALLBACK_SURFACE:
                isReady = true;
                if (mOnSurfaceAvailableListener != null) {
                    mOnSurfaceAvailableListener.onSurfaceAvailable((Surface) msg.obj);
                }
                break;
        }
        return true;
    }


    public void setOnSurfaceAvailableListener(OnSurfaceAvailableListener onSurfaceAvailableListener) {
        mOnSurfaceAvailableListener = onSurfaceAvailableListener;
    }

    public void setVideoFrameFormat(VideoFrameFormat frameFormat) {
        if (isReady) {
            mRenderEventHandler.obtainMessage(MSG_FRAME_INFO, frameFormat)
                    .sendToTarget();
        }
    }


    /**
     * lutFilter PageChangeListener
     *
     * @param listener
     */
    public void setLutFilterPagerChangeListener(GlLutFilterPager.OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
        if (mImageLookupFilter != null) {
            mImageLookupFilter.setOnPageChangeListener(mOnPageChangeListener, mCallbackEventHandler);
        }
    }

    public void setLutFilterImageList(List<String> list) {
        mLutFilterImageList = list;
        if (mImageLookupFilter != null) {
            mRenderEventHandler.obtainMessage(MSG_SET_LOOKUP_IMAGE, list).sendToTarget();
        }
    }

    public List<String> getLutFilterImageList() {
        return mLutFilterImageList;
    }

    public void setLutFilterIndex(int index) {
        mLutIndex = index;
        if (mImageLookupFilter != null) {
            mRenderEventHandler.obtainMessage(MSG_SET_LOOKUP_IMAGE_INDEX, index).sendToTarget();
        }
    }

    public void setBeautyIntensity(float intensity) {
        mBeautyIntensity = intensity;
        if (mBeautyFilter != null) {
            mRenderEventHandler.obtainMessage(MSG_SET_BEAUTY_INTENSITY, intensity).sendToTarget();
        }
    }

    public GlFilterConfig addFilter(GlFilter glFilter) {
        GlFilterConfig config = new GlFilterConfig(glFilter);
        mRenderEventHandler.obtainMessage(MSG_ADD_FILTER, config).sendToTarget();
        return config;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mFilterScroller.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mFilterScroller.onTouchEvent(event) || super.onTouchEvent(event);
    }


    public GlFilterConfig addFilter(GlFilter glFilter, long start) {
        GlFilterConfig config = new GlFilterConfig(glFilter, start, GlFilterConfig.DEFAULT_END_VALUE);
        mRenderEventHandler.obtainMessage(MSG_ADD_FILTER, config).sendToTarget();
        return config;
    }

    public GlFilterConfig addFilter(GlFilter glFilter, long start, long end) {
        GlFilterConfig config = new GlFilterConfig(glFilter, start, end);
        mRenderEventHandler.obtainMessage(MSG_ADD_FILTER, config).sendToTarget();
        return config;
    }

    public void setPresentationTime(long presentationTimeUs) {
        this.mPresentationTimeNs = presentationTimeUs * 1000;
    }

    @SuppressWarnings("unchecked")
    private boolean handleRenderMessage(Message msg) {
        switch (msg.what) {
            case MSG_PREPARE_SURFACE:
                prepareSurface((SurfaceTexture) msg.obj, msg.arg1, msg.arg2);
                break;
            case MSG_FRAME_INFO:
                onFrameFormat((VideoFrameFormat) msg.obj);
                break;
            case MSG_SET_LOOKUP_IMAGE:
                onSetLookupFilterImageList((List<String>) msg.obj);
                break;
            case MSG_SET_LOOKUP_IMAGE_INDEX:
                onSetLookupFilterIndex((int) msg.obj);
                break;
            case MSG_SET_BEAUTY_INTENSITY:
                onSetBeautyIntensity((float) msg.obj);
                break;
            case MSG_ADD_FILTER:
                onAddFilterConfig((GlFilterConfig) msg.obj);
                break;
            case MSG_RELEASE:
                onRelease();
                break;
            case MSG_RENDERING:
                onRendering((SurfaceTexture) msg.obj);
                break;
        }
        return true;
    }


    private void onRelease() {
        if (mOutputSurface != null) {
            mOutputSurface.release();
            mOutputSurface = null;
        }
//        mWindowSurface.release();
//        mWindowSurface = null;
//        mEglCore.release();
//        mEglCore = null;


//        mRenderThread.quit();
//        try {
//            mRenderThread.join();
//        } catch (InterruptedException ignore) {
//
//        }
    }

    private void onAddFilterConfig(GlFilterConfig glFilterConfig) {
        mOutputSurface.addFilterConfig(glFilterConfig);
    }

    private void onSetBeautyIntensity(float intensity) {
        if (mBeautyFilter != null) {
            mBeautyFilter.setIntensity(intensity);
        }
    }


    private void onSetLookupFilterImageList(List<String> imagePathList) {
        if (mImageLookupFilter != null) {
            mImageLookupFilter.setLookupImageList(imagePathList);
        }
    }

    private void onSetLookupFilterIndex(int index) {
        if (mImageLookupFilter != null) {
            mImageLookupFilter.setCurrentItem(index);
        }
    }

    private void prepareSurface(SurfaceTexture surfaceTexture, int width, int height) {
        Context context = getContext();
        if (mEglCore == null) {
            mEglCore = new EglCore();
        }

        if (mWindowSurface == null) {
            mWindowSurface = new WindowSurface(mEglCore, new Surface(surfaceTexture), true);
        }
        mWindowSurface.makeCurrent();
        mVideoFilter = new VideoFilter(context);
        mVideoFilter.setPosition(width >> 1, height >> 1);

        mOutputSurface = new OutputSurface(context, width, height, mVideoFilter);
        mImageLookupFilter = new GlLutFilterPager(context);
        mImageLookupFilter.setOnPageChangeListener(mOnPageChangeListener, mCallbackEventHandler);


        if (mLutFilterImageList != null) {
            mImageLookupFilter.setLookupImageList(mLutFilterImageList);
            if (mLutIndex >= 0) {
                mImageLookupFilter.setCurrentItem(mLutIndex);
            }
        }
        mFilterScroller.setup(mImageLookupFilter, getWidth());
        mOutputSurface.addFilter(mImageLookupFilter);

        mBeautyFilter = new GlImageBeautyFilter(context);
        mBeautyFilter.setSize(width, height);
        if (mBeautyIntensity >= 0) {
            mBeautyFilter.setIntensity(mBeautyIntensity);
        }
        mOutputSurface.addFilter(mBeautyFilter);

        mOutputSurface.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mRenderEventHandler.obtainMessage(MSG_RENDERING, surfaceTexture)
                        .sendToTarget();
            }
        });
        mCallbackEventHandler.obtainMessage(MSG_CALLBACK_SURFACE, mOutputSurface.getSurface())
                .sendToTarget();
    }

    private VideoFrameFormat mLastFrameFormat;

    private void onFrameFormat(VideoFrameFormat format) {

        mLastFrameFormat = format;
        mVideoFilter.setRotation(-format.degrees);
        mVideoWidth = format.width;
        mVideoHeight = format.height;


        if (getWidth() < getHeight() * format.width / format.height) {
            if (format.degrees == 90 || format.degrees == 180) { //ok
                mVideoFilter.setSize(getHeight(), 1f * getHeight() * format.height / format.width);
            } else { //ok
                mVideoFilter.setSize(getWidth(), 1f * getWidth() / format.width * format.height);
            }
        } else {
            if (format.degrees == 90 || format.degrees == 180) {
                mVideoFilter.setSize(1f * getWidth() / format.height * format.width, getWidth());
            } else { //ok
                mVideoFilter.setSize(1f * getHeight() * format.width / format.height, getHeight());
            }
        }


    }


    private void onRendering(SurfaceTexture surfaceTexture) {
        surfaceTexture.updateTexImage();
        mWindowSurface.makeCurrent();
        mOutputSurface.drawFrame(mPresentationTimeNs);
        mWindowSurface.swapBuffers();
        GLES20.glFinish();
    }

    private SurfaceTexture textureSurface;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (textureSurface == null) {
            textureSurface = surface;
            mRenderEventHandler.obtainMessage(MSG_PREPARE_SURFACE, width, height, textureSurface)
                    .sendToTarget();
        } else {
            mTextureView.setSurfaceTexture(textureSurface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public ArrayList<GlFilterConfig> dumpConfig() {
        return mOutputSurface.dumpConfig();
    }


    public interface OnSurfaceAvailableListener {
        void onSurfaceAvailable(Surface surface);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRenderEventHandler.sendEmptyMessage(MSG_RELEASE);
        mRenderThread.quitSafely();
        isReady = false;
        try {
            mRenderThread.join();
        } catch (InterruptedException ignore) {
        }
        mRenderEventHandler.removeCallbacksAndMessages(null);
        mRenderEventHandler = null;
        mRenderThread = null;
    }

}
