package com.jsq.gav;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.jsq.gav.gles.FrameBufferObject;
import com.jsq.gav.gles.filter.SaveFrameFilter;
import com.jsq.gav.widget.CameraPreviewView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jiang on 2019-08-23
 */

public class FrameSaver implements CameraPreviewView.OnDrawFrameListener {
    private static final int MAG_ON_SAVE = 494;
    private static final int MAG_ON_SETUP = 493;
    private final Context mContext;

    private HandlerThread mRenderThread;
    private Handler mRenderEventHandler;

    private volatile boolean mIsSetup = false;
    private volatile boolean needSave = false;

    private SaveFrameFilter mSaveFrameFilter;
    private FrameBufferObject mSaveFrameFbo;
    private volatile int mWidth;
    private volatile int mHeight;
    private volatile OnSaveListener mListener;


    public FrameSaver(Context context) {
        mContext = context;
        mRenderThread = new HandlerThread("Snapshotting");
        mRenderThread.start();
        mRenderEventHandler = new Handler(mRenderThread.getLooper(), this::handleEventMessage);
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void save(OnSaveListener listener) {
        mListener = listener;
        needSave = true;
    }


    @Override
    public void onDrawFrame(int texture) {
        if (!mIsSetup) {
            mRenderEventHandler.obtainMessage(MAG_ON_SETUP, texture).sendToTarget();
        }
        if (needSave) {
            mRenderEventHandler.obtainMessage(MAG_ON_SAVE, texture).sendToTarget();
        }

    }

    private void onSave(int texture) {
        needSave = false;
        GLES20.glViewport(0, 0, mWidth, mHeight);
        mSaveFrameFilter = new SaveFrameFilter(mContext);
        mSaveFrameFilter.setSize(mWidth, mHeight);
        mSaveFrameFilter.setPosition(mWidth >> 1, mHeight >> 1);
        mSaveFrameFilter.setup();
        mSaveFrameFbo = new FrameBufferObject();
        mSaveFrameFbo.setup(mWidth, mHeight);
        mSaveFrameFbo.bind();
        mSaveFrameFilter.onDraw(texture);

        ByteBuffer buffer = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        buffer.rewind();
        Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buffer);
        buffer.clear();
        if (mListener != null) {
            mListener.onSave(bmp);
        }
    }

    private void onSetup() {
        mIsSetup = true;
    }


    private boolean handleEventMessage(Message message) {
        switch (message.what) {
            case MAG_ON_SAVE:
                onSave((Integer) message.obj);
                break;
            case MAG_ON_SETUP:
                onSetup();
                break;

        }
        return true;
    }


    public interface OnSaveListener {
        void onSave(Bitmap bitmap);
    }

}
