package com.jsq.gav;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import androidx.annotation.IntDef;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * Created by jiang on 2019/6/4
 * 复用
 */

public class QueuedMuxer {

    public static final int SAMPLE_TYPE_VIDEO = 0;
    public static final int SAMPLE_TYPE_AUDIO = 1;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SAMPLE_TYPE_VIDEO, SAMPLE_TYPE_AUDIO})
    @interface SampleType {

    }

    private static final int BUFFER_SIZE = 64 * 1024; // I have no idea whether this value is appropriate or not...
    private final MediaMuxer mMuxer;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private ByteBuffer mByteBuffer;
    private final List<SampleInfo> mSampleInfoList;
    private boolean mStarted;
    private Semaphore semaphore;

    public QueuedMuxer(String out, int format) throws IOException {
        mMuxer = new MediaMuxer(out, format);
        mSampleInfoList = new ArrayList<>();
    }

    public void enableMultiThread() {
        semaphore = new Semaphore(1, true);
        try {
            semaphore.acquire();
        } catch (InterruptedException ignore) {

        }
    }

    public void setOutputFormat(@SampleType int sampleType, MediaFormat format) {
        switch (sampleType) {
            case SAMPLE_TYPE_VIDEO:
                if (mVideoFormat != null) {
                    throw new RuntimeException("Video output format changed twice.");
                }
                if (mAudioFormat == null && semaphore != null) {
                    try {
                        semaphore.acquire();
                    } catch (InterruptedException ignore) {

                    }
                }
                mVideoFormat = format;
                break;
            case SAMPLE_TYPE_AUDIO:
                if (mAudioFormat != null) {
                    throw new RuntimeException("Audio output format changed twice.");
                }
                mAudioFormat = format;
                if (semaphore != null) {
                    semaphore.release();
                }
                break;
            default:
                throw new AssertionError();
        }
        onSetOutputFormat();
    }

    private void onSetOutputFormat() {
        if (mVideoFormat == null || mAudioFormat == null) return;

        mVideoTrackIndex = mMuxer.addTrack(mVideoFormat);
        LogUtil.v("Added track #" + mVideoTrackIndex + " with " + mVideoFormat.getString(MediaFormat.KEY_MIME) + " to muxer");
        mAudioTrackIndex = mMuxer.addTrack(mAudioFormat);
        LogUtil.v("Added track #" + mAudioTrackIndex + " with " + mAudioFormat.getString(MediaFormat.KEY_MIME) + " to muxer");
        mMuxer.start();

        mStarted = true;

        if (mByteBuffer == null) {
            mByteBuffer = ByteBuffer.allocate(0);
        }
        mByteBuffer.flip();
        LogUtil.v("Output format determined, writing " + mSampleInfoList.size() +
                " samples / " + mByteBuffer.limit() + " bytes to muxer.");
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int offset = 0;
        for (SampleInfo sampleInfo : mSampleInfoList) {
            sampleInfo.writeToBufferInfo(bufferInfo, offset);
            mMuxer.writeSampleData(getTrackIndexForSampleType(sampleInfo.mSampleType), mByteBuffer, bufferInfo);
            offset += sampleInfo.mSize;
        }
        mSampleInfoList.clear();
        mByteBuffer = null;
    }

    /**
     * 些数据到 mp4文件
     *
     * @param sampleType
     * @param byteBuf
     * @param bufferInfo
     */
    public void writeSampleData(@SampleType int sampleType, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        LogUtil.e("write :" + sampleType + ",presentationTimeUs:" + bufferInfo.presentationTimeUs);
        if (mStarted) {
            mMuxer.writeSampleData(getTrackIndexForSampleType(sampleType), byteBuf, bufferInfo);
            return;
        }
        byteBuf.limit(bufferInfo.offset + bufferInfo.size);
        byteBuf.position(bufferInfo.offset);
        if (mByteBuffer == null) {
            mByteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
        }
        mByteBuffer.put(byteBuf);
        mSampleInfoList.add(new SampleInfo(sampleType, bufferInfo.size, bufferInfo));
    }


    private int getTrackIndexForSampleType(@SampleType int sampleType) {
        switch (sampleType) {
            case SAMPLE_TYPE_VIDEO:
                return mVideoTrackIndex;
            case SAMPLE_TYPE_AUDIO:
                return mAudioTrackIndex;
            default:
                throw new AssertionError();
        }
    }

    /*public void stop() {
        if (mVideoFormat == null || mAudioFormat == null) {
            mMuxer.stop();
        }
    }*/

    public void release() {
        if (mStarted) {
            mMuxer.release();
        }
    }

    private static class SampleInfo {
        private final int mSampleType;
        private final int mSize;
        private final long mPresentationTimeUs;
        private final int mFlags;

        private SampleInfo(int sampleType, int size, MediaCodec.BufferInfo bufferInfo) {
            mSampleType = sampleType;
            mSize = size;
            mPresentationTimeUs = bufferInfo.presentationTimeUs;
            mFlags = bufferInfo.flags;
        }

        private void writeToBufferInfo(MediaCodec.BufferInfo bufferInfo, int offset) {
            bufferInfo.set(offset, mSize, mPresentationTimeUs, mFlags);
        }
    }


}
