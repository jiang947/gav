package com.jsq.gav;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;

import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.GlFilterConfig;
import com.jsq.gav.gles.OutputSurface;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.GlImageBeautyFilter;
import com.jsq.gav.gles.filter.GlLutFilterPager;
import com.jsq.gav.gles.filter.VideoFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * Created by jiang on 2019/5/31
 */


class VideoComposer {

    private Context mContext;
    private int mFrameRate;
    private List<MediaExtractor> mExtractorList;
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private final MediaFormat mOutputFormat;
    private final QueuedMuxer mMuxer;

    private boolean mIsEncodeEndOfStream = false;
    private boolean mIsDecodeEndOfStream = false;
    private boolean mIsExtractEndOfStream = false;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private volatile long mWrittenPresentationTimeUs;
    private int mVideoTrackIndex;

    private OutputSurface mOutputSurface;
    private EglCore mEglCore;
    private WindowSurface mInputSurface;

    private int mCurrent = 0;

    private MediaExtractor mExtractor;
    private int mWidth;
    private int mHeight;
    private VideoFilter mVideoFilter;
    private List<MediaTrackInfo> mInputMediaTrackInfoList;
    private List<TimeScalePeriod> mTimeScalePeriodList;

    private int mWrittenFrames = 0;
    /**
     * 渲染的帧计数
     */
    private int mRenderFrames = 1;
    private MediaTrackInfo mCurrentTrackInfo;

    /**
     * 上次Buffer 上的Present time
     */
    private long mPrevPresentUs = 0;
    /**
     * 本次渲染时间
     */
    private long mRenderTimeUs = 0;

    /**
     * 上次纠正后的时间
     */
    private long mPrevRedressTimeUs = 0;
    private int mExtractOutCount;
    /**
     * 2帧之间的时间间隔
     */
    private long mFrameDelta;
    /**
     * 标准时间间隔， 视频的frameRate 计算而来
     */
    private long mStandardFrameDelta;
    /**
     * 美颜滤镜强度
     */
    private float mIntensity = -1;
    private List<String> mImagePathList;
    private int mImageIndex = -1;

    public VideoComposer(Context context, int width, int height, int frameRate,
                         List<MediaExtractor> mediaExtractorList, List<MediaTrackInfo> mediaTrackInfoList,
                         MediaFormat outputFormat, QueuedMuxer mediaMuxer, List<TimeScalePeriod> timeScalePeriodList,
                         float intensity, List<String> imagePathList, int imageIndex) throws IOException {
        mContext = context;
        this.mWidth = width;
        this.mHeight = height;
        this.mFrameRate = frameRate;
        this.mExtractorList = mediaExtractorList;
        this.mOutputFormat = outputFormat;
        this.mMuxer = mediaMuxer;
        this.mInputMediaTrackInfoList = mediaTrackInfoList;
        mTimeScalePeriodList = timeScalePeriodList;
        mIntensity = intensity;
        mImagePathList = imagePathList;
        mImageIndex = imageIndex;
        setup();
    }

    /**
     * 准备opengl 环境
     * 初始化编码器
     * @throws IOException
     */
    public void setup() throws IOException {
        mEncoder = MediaCodec.createEncoderByType(mOutputFormat.getString(MediaFormat.KEY_MIME));
        mEncoder.configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEglCore = new EglCore();
        mInputSurface = new WindowSurface(mEglCore, mEncoder.createInputSurface(), true);
        mInputSurface.makeCurrent();
        mEncoder.start();
        mVideoFilter = new VideoFilter(mContext);
        mVideoFilter.setSize(mWidth, mHeight);
        mVideoFilter.setPosition(mWidth >> 1, mHeight >> 1);
        mOutputSurface = new OutputSurface(mContext, mWidth, mHeight, mVideoFilter);


        GlLutFilterPager imageLookupFilterPager = new GlLutFilterPager(mContext);
        if (mImagePathList != null) {
            imageLookupFilterPager.setLookupImageList(mImagePathList);
            imageLookupFilterPager.setCurrentItem(mImageIndex);
        }
        mOutputSurface.addFilter(imageLookupFilterPager);

        GlImageBeautyFilter beautyFilter = new GlImageBeautyFilter(mContext);
        beautyFilter.setSize(mWidth, mHeight);
        beautyFilter.setIntensity(mIntensity);
        mOutputSurface.addFilter(beautyFilter);


        initDecoder(mInputMediaTrackInfoList.get(mCurrent), mOutputSurface.getSurface());
    }

    /**
     * 初始化解码器
     * @param trackInfo
     * @param surface
     * @throws IOException
     */
    private void initDecoder(MediaTrackInfo trackInfo, Surface surface) throws IOException {
        mExtractor = mExtractorList.get(mCurrent);
        mCurrentTrackInfo = trackInfo;
        mVideoFilter.setRotation(-mCurrentTrackInfo.degrees);
        int degrees = Math.abs(mCurrentTrackInfo.degrees) % 360;

        if (degrees == 90 || degrees == 270) {
            //noinspection SuspiciousNameCombination 旋转90或270度
            mVideoFilter.setSize(mHeight, mWidth);
        } else {
            mVideoFilter.setSize(mWidth, mHeight);
        }
        mVideoFilter.setPosition(mWidth >> 1, mHeight >> 1);
        mStandardFrameDelta = (long) (1000000f / trackInfo.frameRate);
        mDecoder = MediaCodec.createDecoderByType(trackInfo.videoMiniType);
        mDecoder.configure(trackInfo.videoMediaFormat, surface, null, 0);
        mDecoder.start();

    }

    /**
     * 是否有下一个视频
     * @return
     */
    private boolean hasNext() {
        return mCurrent + 1 < mExtractorList.size();
    }

    /**
     * 重新初始化解码器
     * @throws IOException
     */
    public void maybeResetDecoder() throws IOException {
        mCurrent++;
        MediaTrackInfo mediaTrackInfo = mInputMediaTrackInfoList.get(mCurrent);
        mDecoder.stop();
        mDecoder.release();
        initDecoder(mediaTrackInfo, mOutputSurface.getSurface());

        mIsDecodeEndOfStream = false;
        mIsExtractEndOfStream = false;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void process() throws IOException {
        while (drainEncoder()) {
        }
        while (drainDecoder()) {
        }
        while ((drainExtractor())) {
        }
    }


    /**
     * 提取帧数据
     *
     * @return : Encoder 缓存满的时候返回true, 让其他的  Decoder和Encoder运行
     */
    private boolean drainExtractor() {
        if (mIsExtractEndOfStream) return false;
        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != mCurrentTrackInfo.videoTrackIndex) return false;

        int index = mDecoder.dequeueInputBuffer(0);
        if (index < 0) {
            return false;
        }
        if (trackIndex < 0) {
            mIsExtractEndOfStream = true;
            mDecoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }
        long sampleTime = mExtractor.getSampleTime();

        for (int i = 0; i < mCurrent; i++) {
            sampleTime += mInputMediaTrackInfoList.get(i).videoDurationUs;
        }

        ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
        if (inputBuffer != null) {
            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
            boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
            mDecoder.queueInputBuffer(index, 0, sampleSize, sampleTime,
                    isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
        }
        mExtractor.advance();
        return true;
    }


    /**
     * 解码
     *
     * @return : Decoder 缓存满的时候返回true, 让其他的  Extractor和Encoder运行
     */
    private boolean drainDecoder() throws IOException {
        if (mIsDecodeEndOfStream) return false;
        int index = mDecoder.dequeueOutputBuffer(mBufferInfo, 0);
        if (index < 0) {
            return false;
        }
        boolean doRender = mBufferInfo.size != 0;

        long prevPresentationTimeUs = 0;
        for (int i = 0; i < mCurrent; i++) {
            prevPresentationTimeUs += mInputMediaTrackInfoList.get(i).videoDurationUs;
        }

        if (mCurrentTrackInfo.startTimeUs > 0 && mBufferInfo.presentationTimeUs >= 0 &&
                mBufferInfo.presentationTimeUs < (mCurrentTrackInfo.startTimeUs + prevPresentationTimeUs)) {
            doRender = false;
        }
        if (mCurrentTrackInfo.endTimeUs > 0) {
            if (mBufferInfo.presentationTimeUs > (mCurrentTrackInfo.endTimeUs + prevPresentationTimeUs)) {
                doRender = false;
                mBufferInfo.flags = mBufferInfo.flags | MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            }
        }

        if ((mBufferInfo.presentationTimeUs - mPrevPresentUs) < mStandardFrameDelta * 2) {
            mFrameDelta = mBufferInfo.presentationTimeUs - mPrevPresentUs;
        } else {
            mFrameDelta = mStandardFrameDelta;
        }
        if (doRender) {
            if (mTimeScalePeriodList != null) {  //  时间控制，  绝对时间 or 相对时间
                for (TimeScalePeriod period : mTimeScalePeriodList) {
                    if (mBufferInfo.presentationTimeUs >= period.startTimeUs && mBufferInfo.presentationTimeUs <= period.endTimeUs) {
                        mFrameDelta = (long) (mFrameDelta / period.speed);
                        break;
                    }
                }
            }
            mRenderTimeUs = mPrevRedressTimeUs + mFrameDelta;

            long timeNs = AvUtil.computePresentationTime(mRenderFrames, mFrameRate);
            if (mRenderTimeUs < timeNs) {
                doRender = false;
                LogUtil.e(String.format("drop frame: timeUs : %s", mBufferInfo.presentationTimeUs));
            }
        }

        mDecoder.releaseOutputBuffer(index, doRender);

        if (doRender) {
            LogUtil.e("mRenderTimeUs:" + mRenderTimeUs + ",presentationTimeUs:" + mBufferInfo.presentationTimeUs);
            mInputSurface.makeCurrent();
            mOutputSurface.awaitNewImage();
            mOutputSurface.drawFrame(mBufferInfo.presentationTimeUs * 1000L);
            mInputSurface.setPresentationTime(mRenderTimeUs * 1000L);
            mInputSurface.swapBuffers();
            mRenderFrames++;
        }
        mPrevRedressTimeUs = mRenderTimeUs;
        mPrevPresentUs = mBufferInfo.presentationTimeUs;

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (hasNext()) {
                maybeResetDecoder();
                LogUtil.e("VideoComposer: change extractor");
            } else {
                mEncoder.signalEndOfInputStream();
                mIsDecodeEndOfStream = true;
                mBufferInfo.size = 0;
            }
        }
        return false;
    }


    /**
     * 编码
     *
     * @return : Encoder 缓存清空后返回true, 让其他的  Extractor和Encoder运行
     */
    private boolean drainEncoder() {
        if (mIsEncodeEndOfStream) return false;
        int index = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
        if (index < 0) {
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mMuxer.setOutputFormat(QueuedMuxer.SAMPLE_TYPE_VIDEO, newFormat);
                return true;
            }

            if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                return true;
            }

            return false;
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsEncodeEndOfStream = true;
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
        if (outputBuffer != null) {
            outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
            outputBuffer.position(mBufferInfo.offset);
            mMuxer.writeSampleData(QueuedMuxer.SAMPLE_TYPE_VIDEO, outputBuffer, mBufferInfo);
            mWrittenFrames++;
            mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;
        }

        mEncoder.releaseOutputBuffer(index, false);
        return true;
    }

    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

    /**
     * 整个流程是否已经结束
     *
     * @return
     */
    public boolean isFinished() {
        return mIsEncodeEndOfStream;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mOutputSurface != null) {
            mOutputSurface.release();
            mOutputSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }

        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mExtractorList != null) {
            for (MediaExtractor extractor : mExtractorList) {
                extractor.release();
            }
            mExtractorList = null;
        }

    }


    /**
     * 添加一个filter 到surface
     * @param collection
     */
    public void addAddFilterConfig(Collection<GlFilterConfig> collection) {
        mOutputSurface.addAllFilterConfig(collection);
    }

}
