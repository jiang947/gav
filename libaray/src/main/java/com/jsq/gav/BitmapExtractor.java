package com.jsq.gav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.jsq.gav.gles.EglCore;
import com.jsq.gav.gles.OutputSurface;
import com.jsq.gav.gles.WindowSurface;
import com.jsq.gav.gles.filter.VideoFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by jiang on 2019-08-14
 * 从视频中提取bitmap
 */

public class BitmapExtractor implements Runnable {

    private MediaCodec mDecoder;
    private MediaExtractor mCurrentExtractor;

    private boolean mIsDecodeEndOfStream = false;
    private boolean mIsExtractEndOfStream = false;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();


    private OutputSurface mOutputSurface;
    private EglCore mEglCore;
    private WindowSurface mInputSurface;

    private MediaTrackInfo mCurrentTrackInfo;
    private VideoFilter mVideoFilter;
    private final Context mContext;
    private final List<MediaSourceInfo> mSourceInfoList;
    private int mSize;
    private int mExtractFrames = -1;
    private int mWidth;
    private int mHeight;
    private long mIntervalTimeUs = -1;
    private long mPrevRedressTimeUs;


    private Bitmap.Config mFormat;

    private OnBitmapAvailableListener mOnBitmapAvailableListener;
    private OnExtractCompleteListener mOnExtractCompleteListener;
    private OnExtractErrorListener mOnExtractErrorListener;
    private ImageReader mImageReader;
    private int mRenderCount;
    private List<MediaExtractor> mExtractorList;
    private List<MediaTrackInfo> mTrackInfoList;

    private int mCurrentIndex = 0;
    private long mFrameDelta;
    private long mPrevPresentUs;
    private long mStandardFrameRate;
    private long mRenderTimeUs;
    /**
     * 上次渲染时间
     */
    private long mPrevRenderTimeUs;
    private boolean useVirtualCut;


    /**
     * 从视频中提取bitmap
     *
     * @param context                   the Context to use when resolving the Uri
     * @param path                      视频路径
     * @param useVirtualCut             使用虚拟裁剪Video, false :不使用MediaSourceInfo.startTimeUs,和
     *                                  MediaSourceInfo.endTimeUs, 导出的bitmap 从视频的第一帧开始
     * @param intervalTimeMs            间隔时间 单位 毫秒
     * @param extractFrames             提取的帧数
     * @param size                      bitmap 大小， 单位 像素
     * @param format                    Bitmap format
     * @param onExtractCompleteListener Complete Listener
     * @param onExtractErrorListener    Error Listener
     */
    private BitmapExtractor(Context context, List<MediaSourceInfo> path, boolean useVirtualCut, long intervalTimeMs, int extractFrames,
                            int size, Bitmap.Config format,
                            OnBitmapAvailableListener bitmapAvailableListener,
                            OnExtractCompleteListener onExtractCompleteListener,
                            OnExtractErrorListener onExtractErrorListener) {
        mContext = context;
        mSourceInfoList = path;
        mSize = size;
        this.mIntervalTimeUs = intervalTimeMs * 1000;
        this.mExtractFrames = extractFrames; //
        this.mFormat = format;
        mOnBitmapAvailableListener = bitmapAvailableListener;
        mOnExtractCompleteListener = onExtractCompleteListener;
        mOnExtractErrorListener = onExtractErrorListener;
        this.useVirtualCut = useVirtualCut;
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            init();
            process();
            if (mOnExtractCompleteListener != null) {
                mOnExtractCompleteListener.onExtractCompleted();
            }
        } catch (IOException e) {
            if (mOnExtractErrorListener != null) {
                mOnExtractErrorListener.onExtractError(e);
            }
        } finally {
            release();
        }

    }

    @WorkerThread
    public void init() throws IOException {


        mExtractorList = new ArrayList<>();
        mTrackInfoList = new ArrayList<>();
        long totalDuration = 0;
        for (MediaSourceInfo info : mSourceInfoList) {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(mContext, info.source, null);
            MediaTrackInfo trackInfo = MediaTrackInfo.parse(extractor);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mContext, info.source);
            LogUtil.e("ROTATION:" + retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (!TextUtils.isEmpty(rotation)) {
                trackInfo.degrees = Integer.parseInt(rotation);
            }
            retriever.release();
            //trackInfo.degrees =
            trackInfo.startTimeUs = info.startTimeUs;
            if (info.endTimeUs > 0) {
                trackInfo.endTimeUs = info.endTimeUs;
            }


            extractor.selectTrack(trackInfo.videoTrackIndex);
            if (info.endTimeUs > 0 && useVirtualCut) {
                totalDuration += (info.endTimeUs - info.startTimeUs);
            } else {
                totalDuration += (trackInfo.videoDurationUs - info.startTimeUs);
            }

            mExtractorList.add(extractor);
            mTrackInfoList.add(trackInfo);
        }

        mCurrentExtractor = mExtractorList.get(mCurrentIndex);
        mCurrentTrackInfo = mTrackInfoList.get(mCurrentIndex);

        if (mCurrentTrackInfo.width >= mCurrentTrackInfo.height) {
            mWidth = mSize;
            float scale = 1f * mSize / mCurrentTrackInfo.width;
            mHeight = (int) (scale * mCurrentTrackInfo.height);
        } else {
            mHeight = mSize;
            float scale = 1f * mSize / mCurrentTrackInfo.height;
            mWidth = (int) (scale * mCurrentTrackInfo.width);
        }

        mImageReader = ImageReader.newInstance(mSize, mSize, PixelFormat.RGBA_8888, 2);
        mEglCore = new EglCore();
        mInputSurface = new WindowSurface(mEglCore, mImageReader.getSurface(), true);
        mInputSurface.makeCurrent();
        mVideoFilter = new VideoFilter(mContext);
        mVideoFilter.setSize(mWidth, mHeight);
        mVideoFilter.setPosition(mWidth >> 1, mHeight >> 1);

        mOutputSurface = new OutputSurface(mContext, mWidth, mHeight, mVideoFilter);
        initDecoder();

        if (mIntervalTimeUs < 0) {
            mIntervalTimeUs = totalDuration / mExtractFrames;
        }
    }

    private void initDecoder() throws IOException {
        mStandardFrameRate = (long) (1000000f / mCurrentTrackInfo.frameRate);
        mDecoder = MediaCodec.createDecoderByType(mCurrentTrackInfo.videoMiniType);
        mDecoder.configure(mCurrentTrackInfo.videoMediaFormat, mOutputSurface.getSurface(), null, 0);
        mDecoder.start();
    }

    private void dispatchImageAvailableListener() {
        Image image = mImageReader.acquireNextImage();
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();

            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(width +
                    rowPadding / pixelStride, height, mFormat);
            bitmap.copyPixelsFromBuffer(buffer);

            Matrix matrix = new Matrix();
            matrix.setRotate(mCurrentTrackInfo.degrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, (height - mHeight), mWidth, (height - (height - mHeight)), matrix, true);


            if (mOnBitmapAvailableListener != null) {
                mOnBitmapAvailableListener.onBitmapAvailable(bitmap);
            }
            image.close();
        }

    }

    @WorkerThread
    public void process() throws IOException {
        while (!mIsDecodeEndOfStream) {
            while (drainDecoder()) {
            }
            while ((drainExtractor())) {
            }
        }
    }


    private boolean drainExtractor() {
        if (mIsExtractEndOfStream) return false;
        int trackIndex = mCurrentExtractor.getSampleTrackIndex();
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
        long sampleTime = mCurrentExtractor.getSampleTime();

        for (int i = 0; i < mCurrentIndex; i++) {
            sampleTime += mTrackInfoList.get(i).videoDurationUs;
        }

        ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
        if (inputBuffer != null) {
            int sampleSize = mCurrentExtractor.readSampleData(inputBuffer, 0);
            boolean isKeyFrame = (mCurrentExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
            mDecoder.queueInputBuffer(index, 0, sampleSize, sampleTime,
                    isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
        }
        mCurrentExtractor.advance();
        return true;
    }


    private boolean drainDecoder() throws IOException {
        if (mIsDecodeEndOfStream) return false;
        int index = mDecoder.dequeueOutputBuffer(mBufferInfo, 0);
        if (index < 0) {
            return false;
        }
        boolean doRender = mBufferInfo.size != 0;


        long prevPresentationTimeUs = 0;
        for (int i = 0; i < mCurrentIndex; i++) {
            prevPresentationTimeUs += mTrackInfoList.get(i).videoDurationUs;
        }

        if (useVirtualCut) {
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
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0 //最后一帧
                && (mBufferInfo.presentationTimeUs - mPrevPresentUs) < mStandardFrameRate * 2) {
            mFrameDelta = mBufferInfo.presentationTimeUs - mPrevPresentUs;
        } else {
            mFrameDelta = mStandardFrameRate;
        }
        mRenderTimeUs = mPrevRedressTimeUs + mFrameDelta;
        boolean drop = (mRenderCount == 0 || mRenderTimeUs - mPrevRenderTimeUs >= mIntervalTimeUs);
        if (drop) LogUtil.e("drop:" + mBufferInfo.presentationTimeUs);
        doRender = doRender && drop;
        mDecoder.releaseOutputBuffer(index, doRender);
        if (doRender) {
            mInputSurface.makeCurrent();
            mOutputSurface.awaitNewImage();
            mOutputSurface.drawFrame(mBufferInfo.presentationTimeUs * 1000L);
            mInputSurface.setPresentationTime(mRenderTimeUs * 1000L);
            mInputSurface.swapBuffers();
            mPrevRenderTimeUs = mRenderTimeUs;
            mRenderCount++;
            LogUtil.e("dispatchImage:" + mRenderCount + ",presentationTimeUs:" + mBufferInfo.presentationTimeUs);
            dispatchImageAvailableListener();
            if (mExtractFrames > 0 && mRenderCount >= mExtractFrames) {
                mIsDecodeEndOfStream = true;
                return false;
            }
        }
        mPrevRedressTimeUs = mRenderTimeUs;
        mPrevPresentUs = mBufferInfo.presentationTimeUs;

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (hasNext()) {
                initNextDecoder();
            } else {
                mIsDecodeEndOfStream = true;
                mBufferInfo.size = 0;
            }
        }
        return false;
    }


    private boolean hasNext() {
        return mCurrentIndex + 1 < mExtractorList.size();
    }


    public void initNextDecoder() throws IOException {
        mCurrentIndex++;
        mDecoder.stop();
        mDecoder.release();
        mCurrentExtractor = mExtractorList.get(mCurrentIndex);
        mCurrentTrackInfo = mTrackInfoList.get(mCurrentIndex);

        initDecoder();
        mIsDecodeEndOfStream = false;
        mIsExtractEndOfStream = false;
    }

    private void release() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
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

        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mExtractorList != null) {
            for (MediaExtractor extractor : mExtractorList) {
                extractor.release();
            }
        }

    }

    public interface OnBitmapAvailableListener {
        void onBitmapAvailable(Bitmap bitmap);
    }

    public interface OnExtractCompleteListener {
        void onExtractCompleted();
    }

    public interface OnExtractErrorListener {
        void onExtractError(Throwable throwable);
    }


    public static class Builder {
        private Context mContext;
        private List<MediaSourceInfo> mSourceInfoList;
        private long mIntervalTimeMs = -1;
        private int mExtractFrames = -1;
        private int mSize;
        private Bitmap.Config mFormat = Bitmap.Config.ARGB_8888;
        private BitmapExtractor.OnBitmapAvailableListener mBitmapAvailableListener;
        private BitmapExtractor.OnExtractCompleteListener mOnExtractCompleteListener;
        private BitmapExtractor.OnExtractErrorListener mOnExtractErrorListener;
        private boolean useVirtualCut = true;

        public Builder setContext(Context context) {
            mContext = context;
            return this;
        }

        public Builder setSourceInfoList(List<MediaSourceInfo> sourceInfoList) {
            mSourceInfoList = sourceInfoList;
            return this;
        }

        public Builder setIntervalTimeMs(long intervalTimeMs) {
            mIntervalTimeMs = intervalTimeMs;
            return this;
        }

        /**
         * 设置导出Bitmap的数量
         *
         * @param extractFrames 导出Bitmap的数量
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setExtractFrames(int extractFrames) {
            mExtractFrames = extractFrames;
            return this;
        }

        /**
         * 设置导出Bitmap的大小
         *
         * @param size 导出Bitmap的大小
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setSize(int size) {
            mSize = size;
            return this;
        }

        /**
         * 设置导出bitmap 的配置
         *
         * @param format 导出bitmap 的配置
         * @return This Builder object to allow for chaining of calls to set methods
         * @see Bitmap.Config
         */
        public Builder setFormat(Bitmap.Config format) {
            mFormat = format;
            return this;
        }


        public Builder setUseVirtualCut(boolean useVirtualCut) {
            this.useVirtualCut = useVirtualCut;
            return this;
        }

        /**
         * Register a listener to be invoked when a new bitmap becomes available
         * from the BitmapExtractor.
         *
         * @param bitmapAvailableListener The listener that will be run.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setBitmapAvailableListener(BitmapExtractor.OnBitmapAvailableListener bitmapAvailableListener) {
            mBitmapAvailableListener = bitmapAvailableListener;
            return this;
        }

        public Builder setOnExtractCompleteListener(BitmapExtractor.OnExtractCompleteListener onExtractCompleteListener) {
            mOnExtractCompleteListener = onExtractCompleteListener;
            return this;
        }

        public Builder setOnExtractErrorListener(BitmapExtractor.OnExtractErrorListener onExtractErrorListener) {
            mOnExtractErrorListener = onExtractErrorListener;
            return this;
        }


        private void checkParams() {
            if (mExtractFrames < 0 && mIntervalTimeMs < 0) {
                throw new RuntimeException("必须设置mMaxFrame或者mIntervalTimeUs");
            }
            Objects.requireNonNull(mSourceInfoList, "sourceInfoList cannot be null");

        }

        public BitmapExtractor build() {
            checkParams();
            return new BitmapExtractor(mContext, mSourceInfoList, useVirtualCut, mIntervalTimeMs, mExtractFrames,
                    mSize, mFormat, mBitmapAvailableListener,
                    mOnExtractCompleteListener, mOnExtractErrorListener);
        }


    }


}
