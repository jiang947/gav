package com.jsq.gav;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC;


/**
 * Created by jiang on 2019/6/12
 */

public class AudioRecorder {

    private static final String DEFAULT_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int DEFAULT_AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int MSG_START = 1;
    private static final int MSG_RECORDING = 2;
    private static final int MSG_STOP = 3;
    private final int mChannelCount;

    private MediaCodec mEncoder;
    private QueuedMuxer mMuxer;
    //    private MediaMuxer mMuxer;
    private int mOutTrackIndex;

    private boolean mIsStopped = false;
    private final Object mSyncLock = new Object();

    private HandlerThread mRecordThread;
    private Handler mRecordEventHandler;
    private volatile boolean mIsRecording = false;
    private int mBufferSize;
    private AudioRecord mAudioRecord;
    private int mSampleRate;
    private final String mMimeType;
    private int mAudioFormat;
    private int mAudioChannel;
    private int mBitWidth;

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private OnRecordListener mOnRecordListener;
    private Handler mCallbackHandler;

    private long mPresentationTimeUs;
    private volatile long mWrittenPresentationTimeUs;


    public AudioRecorder(int sampleRate, QueuedMuxer muxer) throws IOException {
        this(sampleRate, muxer, DEFAULT_MIME_TYPE);
    }

    public AudioRecorder(int sampleRate, QueuedMuxer muxer, String mimeType) throws IOException {
        this(sampleRate, muxer, mimeType, DEFAULT_AUDIO_FORMAT);
    }

    public AudioRecorder(int sampleRate, QueuedMuxer muxer, String mimeType, int audioFormat) throws IOException {
        this(sampleRate, muxer, mimeType, audioFormat, DEFAULT_AUDIO_CHANNEL);
    }

    public AudioRecorder(int sampleRate, QueuedMuxer muxer, String mimeType, int audioFormat, int audioChannel) throws IOException {
        mSampleRate = sampleRate;
        mMimeType = mimeType;
        mAudioFormat = audioFormat;
        mAudioChannel = audioChannel;
        mBitWidth = 16;
        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
            mBitWidth = 16;
        } else if (audioFormat == AudioFormat.ENCODING_PCM_8BIT) {
            mBitWidth = 8;
        }
        mChannelCount = AvUtil.getChannelCount(audioChannel);
        mMuxer = muxer;
        mRecordThread = new HandlerThread("audio-recorder");
        mRecordThread.start();
        mRecordEventHandler = new Handler(mRecordThread.getLooper(), this::handRecorderMessage);
    }

    public void start() {
        mRecordEventHandler.sendEmptyMessage(MSG_START);
    }

    public void stop() {
        mRecordEventHandler.sendEmptyMessage(MSG_STOP);
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

    private boolean handRecorderMessage(Message msg) {
        try {
            switch (msg.what) {
                case MSG_START:
                    onStart();
                    break;
                case MSG_RECORDING:
                    recording();
                    break;
                case MSG_STOP:
                    onStop();
                    break;
            }
        } catch (IOException e) {
            Log.e("handRecorderMessage", e.getMessage(), e);
        }

        return true;
    }

    private void onStart() throws IOException {
        mIsRecording = true;
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mAudioChannel, mAudioFormat);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, mSampleRate,
                mAudioChannel, mAudioFormat, mBufferSize);
        mAudioRecord.startRecording();

        MediaFormat outputFormat = MediaFormat.createAudioFormat(mMimeType, mSampleRate, mChannelCount);
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AACObjectLC);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024);

        mEncoder = MediaCodec.createEncoderByType(mMimeType);
        mEncoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
        LogUtil.e("start audio record");

        mRecordEventHandler.sendEmptyMessage(MSG_RECORDING);
    }


    private void onStop() {
        mIsRecording = false;
//        int index = mEncoder.dequeueInputBuffer(0);
//        mEncoder.queueInputBuffer(index, 0, 0, mPresentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//        mEncoder.signalEndOfInputStream();
        mAudioRecord.stop();
        mAudioRecord.release();
        mEncoder.stop();
        mEncoder.release();
        mRecordEventHandler.removeCallbacksAndMessages(null);
        LogUtil.e("stop audio record -- mEncoder is released");
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
        mRecordThread.quit();
        try {
            mRecordThread.join();
        } catch (InterruptedException ignore) {

        }
    }

    private void recording() {
        if (mIsRecording) {
            int index = mEncoder.dequeueInputBuffer(-1);
            LogUtil.e("index:" + index);
            if (index >= 0) {
                ByteBuffer inputBuffer = mEncoder.getInputBuffer(index);
                int size = mAudioRecord.read(inputBuffer, inputBuffer.capacity());
                long frameDelta = AvUtil.computePcmDurationUs(size, mSampleRate, mBitWidth, mChannelCount);
                mEncoder.queueInputBuffer(index, 0, size, mPresentationTimeUs, 0);
                mPresentationTimeUs += frameDelta;
                writeFrame();
            }
            mRecordEventHandler.sendEmptyMessage(MSG_RECORDING);
        }
    }

    /**
     * 录制写入一帧
     */
    private void writeFrame() {
        while (true) {
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
            LogUtil.e("write " + index + "   " + this);
            if (index < 0) {
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                }
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mMuxer.setOutputFormat(QueuedMuxer.SAMPLE_TYPE_AUDIO, mEncoder.getOutputFormat());
                    continue;
                }
                if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    continue;
                }
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                mEncoder.releaseOutputBuffer(index, false);
                break;
            }
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
            }
            ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
            if (outputBuffer != null) {
                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                outputBuffer.position(mBufferInfo.offset);
                mMuxer.writeSampleData(QueuedMuxer.SAMPLE_TYPE_AUDIO, outputBuffer, mBufferInfo);
                mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;

                //录制进度回调
                if (mOnRecordListener != null) {
                    mCallbackHandler.post(() -> mOnRecordListener.onRecordProgress(mWrittenPresentationTimeUs));
                }
            }
            mEncoder.releaseOutputBuffer(index, false);
        }
    }


    public boolean isRecording() {
        return mIsRecording;
    }


    public void setOnRecordListener(OnRecordListener onRecordStoppedListener) {
        setOnRecordListener(onRecordStoppedListener, new Handler());
    }

    public void setOnRecordListener(OnRecordListener onRecordListener, Handler handler) {
        this.mOnRecordListener = onRecordListener;
        this.mCallbackHandler = handler;
    }


}
