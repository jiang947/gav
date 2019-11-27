package com.jsq.gav.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.jsq.gav.LoopMediaPlayer;
import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.GlConstants;
import com.jsq.gav.gles.OutputSurface;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.CameraFilter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiang on 2019/5/6
 */

public class MultiVideoView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private TextureView mTextureView;

    private RenderThread mRenderThread;
    private List<String> mFileList;

    public MultiVideoView(@NonNull Context context) {
        super(context);
        init();
    }

    public MultiVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MultiVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mRenderThread = new RenderThread(getContext());
        mRenderThread.start();
        mRenderThread.await();
        mTextureView = new TextureView(getContext());
        addView(mTextureView);
        mTextureView.setSurfaceTextureListener(this);
    }


    public void setSource(List<String> fileList) {
        mFileList = fileList;
        mRenderThread.getEventHandle().sendSetSource(fileList);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mRenderThread.getEventHandle().sendPrepare(surface, width, height);
        mRenderThread.getEventHandle().sendStart();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mRenderThread.getEventHandle().sendStop();
        mRenderThread.getEventHandle().sendRelease();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    private static class RenderThread extends Thread implements SurfaceTexture.OnFrameAvailableListener {

        private static final int MSG_PREPARE = 1;
        private static final int MSG_START = 2;
        private static final int MSG_PAUSE = 3;
        private static final int MSG_STOP = 4;
        private static final int MSG_RELEASE = 5;
        private static final int MSG_SET_SOURCE = 6;

        private boolean mReady = false;
        private final Object syncLock = new Object();

        private EventHandle mEventHandle;

        private LoopMediaPlayer mMediaPlayer;

        private WindowSurface mWindowSurface;
        private EglCore mEglCore;
        private OutputSurface mOutputSurface;

        private CameraFilter mGlOESFilter;

        private final Context mContext;

        private RenderThread(Context context) {
            mContext = context;
        }


        @Override
        public void run() {
            Looper.prepare();
            mEventHandle = new EventHandle(this);
            synchronized (syncLock) {
                mReady = true;
                syncLock.notifyAll();
            }
            Looper.loop();

            if (mMediaPlayer != null) {
                mMediaPlayer.release();
            }
            if (mOutputSurface != null) {
                mOutputSurface.release();
                mOutputSurface = null;
            }
            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
            if (mEglCore != null) {
                mEglCore.release();
                mEglCore = null;
            }

        }


        public void await() {
            synchronized (syncLock) {
                while (!mReady) {
                    try {
                        syncLock.wait(100);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }


        public void setSource(ArrayList<String> fileList) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new LoopMediaPlayer(fileList);
            }
        }

        public void startPlay() {
            mMediaPlayer.start();
        }

        public void pausePlay() {
            mMediaPlayer.pause();
        }

        public void stopPlay() {
            mMediaPlayer.stop();
        }

        public void quit() {
            Looper.myLooper().quit();
        }

        private int mTextureId;

        private float[] mMVPMatrix = new float[16];

        public void prepare(SurfaceTexture surfaceTexture, int width, int height) {
            surfaceTexture.getTransformMatrix(mMVPMatrix);
            mEglCore = new EglCore();
            mWindowSurface = new WindowSurface(mEglCore, surfaceTexture);
            mWindowSurface.makeCurrent();
            mGlOESFilter = new CameraFilter(mContext, GlConstants.FULL_RECTANGLE_COORDS);
            mGlOESFilter.setup();
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mTextureId = textures[0];
            SurfaceTexture surfaceTexture1 = new SurfaceTexture(mTextureId);
            Surface surface = new Surface(surfaceTexture1);
            surfaceTexture1.setOnFrameAvailableListener(this);
//            mOutputSurface = new OutputSurface(width, height);
//            mOutputSurface.setOnFrameAvailableListener(this);
            mMediaPlayer.setSurface(surface);
        }

        private EventHandle getEventHandle() {
            return mEventHandle;
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            surfaceTexture.updateTexImage();
            mWindowSurface.makeCurrent();
//            mOutputSurface.drawFrame(surfaceTexture.getTimestamp());
            mGlOESFilter.onDraw( mTextureId);
            mWindowSurface.swapBuffers();
        }

        private static class EventHandle extends Handler {

            private WeakReference<RenderThread> mWeakReference;

            public EventHandle(RenderThread renderThread) {
                mWeakReference = new WeakReference<>(renderThread);
            }

            public void sendPrepare(SurfaceTexture surfaceTexture, int width, int height) {
                sendMessage(obtainMessage(MSG_PREPARE, width, height, surfaceTexture));
            }

            public void sendStart() {
                sendMessage(obtainMessage(MSG_START));
            }

            public void sendSetSource(List<String> fileList) {
                sendMessage(obtainMessage(MSG_SET_SOURCE, fileList));
            }

            public void sendStop() {
                sendMessage(obtainMessage(MSG_STOP));
            }

            public void sendPause() {
                sendMessage(obtainMessage(MSG_PAUSE));
            }

            public void sendRelease() {
                sendMessage(obtainMessage(MSG_RELEASE));
            }


            @Override
            public void handleMessage(Message msg) {
                RenderThread renderThread = mWeakReference.get();
                if (renderThread == null) return;
                switch (msg.what) {
                    case MSG_PREPARE:
                        renderThread.prepare((SurfaceTexture) msg.obj, msg.arg1, msg.arg2);
                        break;
                    case MSG_START:
                        renderThread.startPlay();
                        break;
                    case MSG_PAUSE:
                        renderThread.pausePlay();
                        break;
                    case MSG_STOP:
                        renderThread.stopPlay();
                        break;
                    case MSG_RELEASE:
                        renderThread.quit();
                        break;
                    case MSG_SET_SOURCE:
                        renderThread.setSource((ArrayList<String>) msg.obj);
                        break;


                }
            }
        }


    }

}
