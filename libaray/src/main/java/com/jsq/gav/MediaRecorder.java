package com.jsq.gav;

import android.content.Context;
import android.media.MediaMuxer;

import com.jsq.gav.widget.CameraPreviewView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by jiang on 2019/6/11
 * 音视频录制
 */

public class MediaRecorder {

    private Context mContext;
    private VideoRecorder mVideoRecorder;
    private AudioRecorder mAudioRecorder;
    private boolean mIsRecording = false;
    private long mMaxDuration = 60_000;
    private int mWidth = 1280;
    private int mHeight = 720;
    private int mFrameRate = 24;

    private int mSampleRate = 44100;


    private CameraPreviewView mCameraPreviewView;
    private String mOutPath;

    private QueuedMuxer mMuxer;
    private AtomicInteger mStoppedCount = new AtomicInteger(0);

    private RecordEventListener mRecordEventListener;

    public MediaRecorder(Context context) {
        mContext = context;
    }

    public void start(String outPath) throws IOException {
        this.start(outPath, mMaxDuration);
    }

    /**
     * 开始录制
     *
     * @param outPath
     * @param maxDurationMs
     * @throws IOException
     */
    public void start(String outPath, long maxDurationMs) throws IOException {
        if (mIsRecording) return;
        this.mMaxDuration = maxDurationMs;
        mStoppedCount.set(0);
        mOutPath = outPath;
        mMuxer = new QueuedMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mMuxer.enableMultiThread();
        mAudioRecorder = new AudioRecorder(mSampleRate, mMuxer);
        mAudioRecorder.setOnRecordListener(OnRecordListener);
        mAudioRecorder.start();
        mVideoRecorder = new VideoRecorder(mContext, mMuxer, mWidth, mHeight, mFrameRate, mMaxDuration);
        mCameraPreviewView.addOnDrawFrameListener(mVideoRecorder);
        mVideoRecorder.setOnRecordListener(OnRecordListener);
        mVideoRecorder.start();
        mIsRecording = true;
    }

    /**
     * 停止录制
     */
    public void stop() {
        mVideoRecorder.stop();
        mCameraPreviewView.removeOnDrawFrameListener();
        mVideoRecorder.waitStop();
        mAudioRecorder.stop();
        mAudioRecorder.waitStop();
        mVideoRecorder.quit();
        mAudioRecorder.quit();
        LogUtil.e("stoped");
        mIsRecording = false;
        mMuxer.release();
        mMuxer = null;
        mVideoRecorder = null;
        mAudioRecorder = null;
    }

    private OnRecordListener OnRecordListener = new OnRecordListener() {
        private long lastDuration;  //最后一次回调的时长

        @Override
        public synchronized void onRecordProgress(long duration) {
            //50毫秒回调一次，避免频繁刷新UI
            if (mRecordEventListener != null && duration - lastDuration > 50 * 1000) {
                mRecordEventListener.onRecordProgress(duration);
                lastDuration = duration;
            }
        }

        /**
         * 回调： 2个线程已经停止回调此方法
         * @param duration
         */
        @Override
        public synchronized void onRecordStopped(long duration) {
//            mVideoRecorder.quit();
            mStoppedCount.addAndGet(1);
            if (mStoppedCount.get() >= 2) {
                if (mRecordEventListener != null) {
                    mRecordEventListener.onRecordComplete(mOutPath);
                }
            }
        }
    };

    public void setRecordEventListener(RecordEventListener recordEventListener) {
        mRecordEventListener = recordEventListener;
    }

    /**
     * 返回录制状态
     *
     * @return
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * 设置最大录制时间
     *
     * @param maxDuration
     */
    public void setMaxDuration(long maxDuration) {
        mMaxDuration = maxDuration;
    }

    public void setPreviewDisplay(CameraPreviewView previewDisplay) {
        this.mCameraPreviewView = previewDisplay;
        if (mWidth > 0 && mHeight > 0) {
            mCameraPreviewView.setAspectRatio(mWidth, mHeight);
        }
    }

    /**
     * 设置视频大小
     *
     * @param width
     * @param height
     */
    public void setVideoSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        if (mCameraPreviewView != null) {
            mCameraPreviewView.setAspectRatio(mWidth, mHeight);
        }
    }

    /**
     * 这是音频 采样率
     *
     * @param sampleRate
     */
    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
    }

    /**
     * 设置fps
     *
     * @param frameRate
     */
    public void setVideoFrameRate(int frameRate) {
        this.mFrameRate = frameRate;
    }

    public interface RecordEventListener {

        /**
         * 录制进度
         *
         * @param duration TimeUnit.MICROSECONDS
         */
        void onRecordProgress(long duration);

        /**
         * 片段录制完成
         *
         * @param filename 路径
         */
        void onRecordComplete(String filename);

    }

}
