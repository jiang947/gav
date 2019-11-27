package com.jsq.gav;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LogPrinter;
import android.view.Surface;

import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.PreviewFilter;
import com.jsq.gav.widget.CameraPreviewView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

/**
 * Created by jiang on 2019/6/11
 */


public class VideoRecorder implements CameraPreviewView.OnDrawFrameListener {
    private static final String DEFAULT_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    private static final int MSG_START = 1;
    private static final int MSG_DRAW = 2;
    private static final int MSG_STOP = 3;
    private static final int MSG_SETUP = 5;


    private Context mContext;

    private QueuedMuxer mMuxer;
    private Surface mSurface;
    private MediaCodec mEncoder;
    private EglCore mEglCore;
    private WindowSurface mEncodeSurface;
    private PreviewFilter mPreviewFilter;
    private int mWidth;
    private int mHeight;
    private int mFrameRate;
    private long mMaxDurationUs;

    private boolean mIsSetup = false;
    private boolean mIsRecording = false;

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private volatile long mWrittenPresentationTimeUs;
    private long mFirstWrittenPresentationTimeUs = -1;

    private HandlerThread mVideoRecorderThread;
    private Handler mRecorderEventHandler;
    private Handler mCallbackHandler;
    private final Object mSyncLock = new Object();
    private boolean mIsStopped = false;
    private long mRenderTimeUs = 0;
    private int mRenderFrames = 0;

    private OnRecordListener mOnRecordListener;

    public VideoRecorder(Context context, QueuedMuxer muxer, int width, int height, int frameRate,
                         long maxDurationMs) throws IOException {
        this(context, muxer, DEFAULT_MIME_TYPE, width, height, frameRate, maxDurationMs);
    }


    public VideoRecorder(Context context, QueuedMuxer muxer, String mimeType, int width, int height,
                         int frameRate, long maxDurationMs) throws IOException {
        this.mContext = context;
        this.mMuxer = muxer;
        this.mWidth = width;
        this.mHeight = height;
        this.mFrameRate = frameRate;
        this.mMaxDurationUs = maxDurationMs * 1000;
        mVideoRecorderThread = new HandlerThread("video-recorder");
        mVideoRecorderThread.start();
        mRecorderEventHandler = new Handler(mVideoRecorderThread.getLooper(), this::handleMessage);
        mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat outputFormat = MediaFormat.createVideoFormat(mimeType, width, height);
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatSurface);
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, AvUtil.calcBitRate(mWidth, mHeight, mFrameRate));

        mEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
        mEncoder.start();
    }

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START:
                mIsRecording = true;
                Log.e("VideoRecorder MSG_START", "quit " + mIsRecording);
                break;
            case MSG_DRAW:
                onDraw((Integer) msg.obj);
                break;
            case MSG_STOP:
                onStop();
                break;
            case MSG_SETUP:
                onSetup((EGLContext) msg.obj);
                break;
        }
        return true;
    }

    /**
     * 停止录制
     */
    private void onStop() {
        mIsRecording = false;
        mIsSetup = false;
        mEncoder.signalEndOfInputStream();
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mPreviewFilter != null) {
            mPreviewFilter.release();
            mPreviewFilter = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mEncodeSurface != null) {
            mEncodeSurface.release();
            mEncodeSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        //录制完成回调
        if (mOnRecordListener != null) {
            mCallbackHandler.post(() -> mOnRecordListener.onRecordStopped(mWrittenPresentationTimeUs));
        }

        synchronized (mSyncLock) {
            mIsStopped = true;
            mSyncLock.notify();
        }


    }

    public void quit() {
        mRecorderEventHandler.removeCallbacksAndMessages(null);
        mVideoRecorderThread.quit();
        Log.e("VideoRecorder quit", "quit " + mIsRecording);
        try {
            mVideoRecorderThread.join();
        } catch (InterruptedException e) {
            Log.e("", e.getMessage(), e);
        }
    }

    private void onSetup(EGLContext eglContext) {
        if (mIsStopped) return;
        mEglCore = new EglCore(eglContext, EglCore.FLAG_RECORDABLE);
        mEncodeSurface = new WindowSurface(mEglCore, getSurface(), true);
        mEncodeSurface.makeCurrent();
        mPreviewFilter = new PreviewFilter(mContext);
        mPreviewFilter.setup();
        mIsSetup = true;
    }

    @Override
    public void onDrawFrame(int texture) {
        maybeSetup(EGL14.eglGetCurrentContext());
        maybeDraw(texture);
    }

    public void maybeDraw(int texture) {
        if (mIsRecording) {
            mRecorderEventHandler.obtainMessage(MSG_DRAW, texture).sendToTarget();
        }
    }

    public void maybeSetup(EGLContext eglContext) {
        if (!mIsSetup) {
            mRecorderEventHandler.obtainMessage(MSG_SETUP, eglContext).sendToTarget();
            mIsSetup = true;
        }
    }

    /**
     * 抢占 surface 资源，
     */
    private void onDraw(int texture) {
        if (mIsStopped) return;
        mEncodeSurface.makeCurrent();
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mPreviewFilter.onDraw(texture);
        mRenderTimeUs = AvUtil.computePresentationTime(mRenderFrames, mFrameRate);
        mEncodeSurface.setPresentationTime(mRenderTimeUs * 1000);
        mEncodeSurface.swapBuffers();
        mRenderFrames++;
        writeFrame();
    }

    /**
     * 录制写入一帧
     */
    private void writeFrame() {
        while (true) {
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            if (index < 0) {
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                }
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mMuxer.setOutputFormat(QueuedMuxer.SAMPLE_TYPE_VIDEO, mEncoder.getOutputFormat());
                    continue;
                }
                if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    continue;
                }
            }

            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mBufferInfo.set(0, 0, 0, mBufferInfo.flags);

            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                mEncoder.releaseOutputBuffer(index, false);
                continue;
            }
            ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
            if (outputBuffer != null) {
                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                outputBuffer.position(mBufferInfo.offset);
                mMuxer.writeSampleData(QueuedMuxer.SAMPLE_TYPE_VIDEO, outputBuffer, mBufferInfo);
                mEncoder.releaseOutputBuffer(index, false);
                if (mFirstWrittenPresentationTimeUs < 0) {
                    mFirstWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;
                }
                mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs - mFirstWrittenPresentationTimeUs;
                //录制进度回调
                if (mOnRecordListener != null) {
                    mCallbackHandler.post(() -> mOnRecordListener.onRecordProgress(mWrittenPresentationTimeUs));
                }
            }
        }
    }


    public void waitStop() {
        synchronized (mSyncLock) {
            while (!mIsStopped) {
                try {
                    mSyncLock.wait(100);
                } catch (InterruptedException ignore) {

                }
            }
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

    public void start() {
        mRecorderEventHandler.sendEmptyMessage(MSG_START);
    }

    public void stop() {
        mRecorderEventHandler.sendEmptyMessage(MSG_STOP);
        mRecorderEventHandler.dump(new LogPrinter(Log.DEBUG, "VideoRecorder"), "stop");
    }

    public void setOnRecordListener(OnRecordListener onRecordStoppedListener) {
        setOnRecordListener(onRecordStoppedListener, new Handler());
    }

    public void setOnRecordListener(OnRecordListener onRecordStoppedListener, Handler handler) {
        this.mOnRecordListener = onRecordStoppedListener;
        this.mCallbackHandler = handler;
    }

}
