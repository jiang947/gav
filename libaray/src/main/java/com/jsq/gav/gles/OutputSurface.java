package com.jsq.gav.gles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.Size;

import com.jsq.gav.gles.filter.GlFilter;
import com.jsq.gav.gles.filter.GlSpriteFilter;
import com.jsq.gav.gles.filter.PreviewFilter;
import com.jsq.gav.gles.filter.SaveFrameFilter;
import com.jsq.gav.widget.CameraPreviewView;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;

// Refer : https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/OutputSurface.java

/**
 * Created by jiang on 2019/4/23
 */

public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {

    private final Object mFrameSyncObject = new Object();

    private boolean mFrameAvailable;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private Context mContext;
    private int mWidth;
    private int mHeight;

    private float[] mDPMatrix = new float[16];  // display projection

    private int mTextureID = -12345;

    private GlFilter mFilter;

    private PreviewFilter mPreviewFilter;
    private FilterChain mFilterChain;
    private FilterChain mFilterChainExt; //后绘制，不会被特效滤镜 影响


    private SaveFrameFilter mSaveFrameFilter;
    private FrameBufferObject mSaveFrameFbo;
    private boolean mNeedSaveFrame;
    private CameraPreviewView.OnScreenshotListener mOnScreenshotListener;
    private Handler mSaveFrameHandler;

    /**
     * 视频帧处理封装
     *
     * @param context  Context
     * @param width    宽
     * @param height   高
     * @param glFilter 视频的第一个filter 在Android上必须是 GL_TEXTURE_EXTERNAL_OES
     * @see GLES11Ext#GL_TEXTURE_EXTERNAL_OES
     */
    public OutputSurface(Context context, @Size(min = 1) int width, @Size(min = 1) int height,
                         GlFilter glFilter) {
        mContext = context;
        this.mWidth = width;
        this.mHeight = height;
        mFilter = glFilter;
        mFilterChain = new FilterChain();

        initTexture();

        mFilter.setProjectionMatrix(mDPMatrix);
        mFilterChain.addFilter(mFilter);

        mFilterChain.setup(width, height);

        mFilterChainExt = new FilterChain();
        mFilterChainExt.setup(width, height);

        mPreviewFilter = new PreviewFilter(mContext);
        mPreviewFilter.setup();


        mSaveFrameFilter = new SaveFrameFilter(mContext);
        mSaveFrameFilter.setProjectionMatrix(mDPMatrix);
        mSaveFrameFilter.setSize(width, height);
        mSaveFrameFilter.setPosition(width >> 1, height >> 1);
        mSaveFrameFilter.setup();
        mSaveFrameFbo = new FrameBufferObject();
        mSaveFrameFbo.setup(mWidth, mHeight);

//        mFilterChain.addFilter(mSaveFrameFilter);
    }

    private void initTexture() {
        GLES20.glViewport(0, 0, mWidth, mHeight); // 画布大小
        Matrix.orthoM(mDPMatrix, 0, 0, mWidth, 0, mHeight, -1, 1);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
    }


    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(0, 0, mWidth, mHeight);
        Matrix.orthoM(mDPMatrix, 0, 0, mWidth, 0, mHeight, -1, 1);
    }

    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
        mSurfaceTexture.setOnFrameAvailableListener(listener);
    }


    public float[] getDPMatrix() {
        return mDPMatrix;
    }

    public void release() {
        mSurface.release();
        mSurfaceTexture.setOnFrameAvailableListener(null);
        mSurfaceTexture.release();
        mFilterChain.release();
        mFilterChainExt.release();
        mPreviewFilter.release();
        mFilter = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                } catch (InterruptedException ignore) {
                }
            }
            mFrameAvailable = false;
        }
        GlUtil.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
    }

    public int drawFrame(long presentationTimeNs) {
        int currentTexture = mFilterChain.onDraw(mTextureID, presentationTimeNs);

        mFilterChainExt.onDraw(currentTexture, presentationTimeNs);
        if (mNeedSaveFrame) {
            saveFrame(currentTexture);
            mNeedSaveFrame = false;
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.glClearColor();
        mPreviewFilter.onDraw(currentTexture);
        return currentTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (mFrameSyncObject) {
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }


    private void saveFrame(int texture) {
        mSaveFrameFbo.bind();
        mSaveFrameFilter.onDraw(texture);
        ByteBuffer buf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();
        if (mSaveFrameHandler !=null){
            mSaveFrameHandler.post(() -> {
                Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                mOnScreenshotListener.onScreenshot(bmp);
            });
        }else {
            Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            mOnScreenshotListener.onScreenshot(bmp);
        }

    }



    public void saveFrame(CameraPreviewView.OnScreenshotListener listener, Handler handler) {
        this.mNeedSaveFrame = true;
        mOnScreenshotListener = listener;
        mSaveFrameHandler = handler;
    }

    public void saveFrame(CameraPreviewView.OnScreenshotListener listener){
        this.mNeedSaveFrame = true;
        mOnScreenshotListener = listener;
    }


    /**
     * 添加一个滤镜
     *
     * @param filter 需要加到surface上的 滤镜
     */
    public void addFilter(GlFilter filter) {
        if (filter instanceof GlSpriteFilter) {
            filter.setProjectionMatrix(mDPMatrix);
            mFilterChainExt.addFilter(filter);
        } else {
            mFilterChain.addFilter(filter);
        }
    }

    /**
     * 添加一个滤镜
     *
     * @param config 需要加到surface上的 滤镜配置
     */
    public void addFilterConfig(GlFilterConfig config) {
        if (config.glFilter instanceof GlSpriteFilter) { // 这样写 继承 GlSpriteFilter 的后绘制
            config.glFilter.setProjectionMatrix(mDPMatrix);
            mFilterChainExt.addFilterConfig(config);
        } else {
            mFilterChain.addFilterConfig(config);
        }
    }

    public void addAllFilterConfig(Collection<GlFilterConfig> collection) {
        for (GlFilterConfig config : collection) {
            addFilterConfig(config);
        }
    }

    public ArrayList<GlFilterConfig> dumpConfig() {
        return null;
    }

    private static class FilterChain {

        ArrayList<GlFilterConfig> mFilterEntries = new ArrayList<>();
        private int mWidth;
        private int mHeight;

        public void setup(int width, int height) {
            mWidth = width;
            mHeight = height;
            for (GlFilterConfig entry : mFilterEntries) {
                maybeSetupFilter(entry);
            }
        }

        public void addFilter(GlFilter filter) {
            GlFilterConfig config = new GlFilterConfig(filter);
            addFilterConfig(config, GlFilterConfig.DEFAULT_START_VALUE, GlFilterConfig.DEFAULT_END_VALUE);
        }

        public void addFilterConfig(GlFilterConfig config) {
            mFilterEntries.add(config);
        }


        public void addFilterConfig(GlFilterConfig config, long start, long endTime) {
            config.startTimeNs = start;
            config.endTimeNs = endTime;
            mFilterEntries.add(config);
        }

        public int onDraw(int textureId, long presentationTimeNs) {
            int current = textureId;
            for (GlFilterConfig entry : mFilterEntries) {
                maybeSetupFilter(entry);
                if (!entry.glFilter.isSetup()) continue;
                if (entry.glFilter.isActive() && entry.startTimeNs <= presentationTimeNs && entry.endTimeNs >= presentationTimeNs) {
                    if (entry.frameBufferObject != null) {
                        entry.frameBufferObject.bind();
                    }
                    current = entry.glFilter.onDraw(current);
                    if (entry.frameBufferObject != null) {
                        current = entry.frameBufferObject.getTextureId();
                    }
                }

            }
            return current;
        }

        private void maybeSetupFilter(GlFilterConfig config) {
            if (!config.glFilter.isSetup() && config.glFilter.autoSetup()) {
                config.glFilter.setup();
                if (config.glFilter.needFrameBuffer() && config.frameBufferObject == null) {
                    config.frameBufferObject = new FrameBufferObject();
                    config.frameBufferObject.setup(mWidth, mHeight);
                }
            }
        }

        public void release() {
            for (GlFilterConfig entry : mFilterEntries) {
                if (entry.frameBufferObject != null) {
                    entry.frameBufferObject.release();
                    entry.frameBufferObject = null;
                }
                entry.glFilter.release();
            }
        }
    }

    public interface OnSaveFrameCallBack {
        void onSaveFrame(Buffer buffer);
    }

    @Deprecated
    private static class FilterEntry {

        /**
         * filter 内涵 序列 反序列 ，然后解析， 给特殊的file 赋值
         * Filter Chain 包含一个 filter 和一个 序列 反序列的 runnable 或者是 callable
         */

        public GlFilter filter;
        public FrameBufferObject frameBufferObject;

        public void setupFilterIfNeed(int width, int height) {
            if (!filter.isSetup()) {
                filter.setup();
                frameBufferObject.setup(width, width);
            }

        }

        public int onDraw(int textureId) {
            frameBufferObject.bind();
            filter.onDraw(textureId);
            return frameBufferObject.getTextureId();
        }
    }


}
