package com.jsq.gav;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

/**
 * Created by jiang on 2019/5/5
 */

public class VideoMediaCodecDecoder {


    private MediaFormat mMediaFormat;
    private String mInputPath;
    private int mWidth;
    private int mHeight;

    private long mStartTime = -1;
    private long mEndTime = -1;
    private int mTrackIndex = -1;

    private MediaExtractor mExtractor;


    private MediaCodec mDecoder;

    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS = false;
    private ByteBuffer[] mDecoderInputBuffers;
    private long writtenTime;

    private Surface mSurface;


    public VideoMediaCodecDecoder() {

    }


    public void prepare() throws IOException {
        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(mInputPath);
        int trackCount = mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mMediaFormat = mExtractor.getTrackFormat(i);
            if (mMediaFormat.getString(MediaFormat.KEY_MIME).contains("video/")) {
                mTrackIndex = i;
                break;
            }
        }
        if (mMediaFormat == null) {
            throw new RuntimeException("没有视频");
        }
        mExtractor.selectTrack(mTrackIndex);
        mWidth = mMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        try {
            mDecoder = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            mExtractor.release();
            throw e;
        }

        mDecoder.configure(mMediaFormat, mSurface, null, 0);
        mDecoder.start();
        //noinspection deprecation
        mDecoderInputBuffers = mDecoder.getInputBuffers();
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setInputPath(String inputPath) {
        mInputPath = inputPath;
    }

    public void drainExtractor() {
        while (!mIsExtractorEOS) {
            int trackIndex = mExtractor.getSampleTrackIndex();
            if (trackIndex >= 0 && trackIndex != this.mTrackIndex) break;
            int index = mDecoder.dequeueInputBuffer(0);
            if (index < 0) break;
            int sampleSize = mExtractor.readSampleData(mDecoderInputBuffers[index], 0);
            boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
            long sampleTime = mExtractor.getSampleTime();

            if (isEnableClip() && sampleTime > mEndTime * 1000) {
                mIsExtractorEOS = true;
                mExtractor.unselectTrack(this.mTrackIndex);
                mDecoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDecoder.queueInputBuffer(index, 0, sampleSize, sampleTime, isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
            } else {
                mDecoder.queueInputBuffer(index, 0, sampleSize, sampleTime, 0);
            }
            mExtractor.advance();
        }
    }

    public boolean drainDecoder() {
        if (mIsDecoderEOS) return false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index = mDecoder.dequeueOutputBuffer(bufferInfo, 0);
        switch (index) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return false;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                //noinspection deprecation
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return true;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            //mEncoder.signalEndOfInputStream();
            mIsDecoderEOS = true;
            bufferInfo.size = 0;
        }
        if (isEnableClip() && bufferInfo.presentationTimeUs > mEndTime * 1000) {
            //mEncoder.signalEndOfInputStream();
            mIsDecoderEOS = true;
            bufferInfo.flags = bufferInfo.flags | MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        }
        boolean doRender = bufferInfo.size > 0;
        LogUtil.e("drainDecoder: time :" + bufferInfo.presentationTimeUs + " size " + bufferInfo.size);
        writtenTime = bufferInfo.presentationTimeUs;
        //AudioProcessor
        mDecoder.releaseOutputBuffer(index, doRender);
        return true;
    }

    public long getWrittenTime() {
        return writtenTime;
    }

    public void enableClip(long startTime, long endTime) {
        mStartTime = startTime;
        mEndTime = endTime;
        mExtractor.seekTo(startTime, SEEK_TO_PREVIOUS_SYNC);
    }

    public boolean isEnableClip() {
        return mStartTime > -1 && mEndTime > -1;
    }


    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }


    public boolean isDecoderEOS() {
        return mIsDecoderEOS;
    }

    public void release() {
        mDecoder.stop();
        mDecoder.release();
        mExtractor.release();
    }


}
