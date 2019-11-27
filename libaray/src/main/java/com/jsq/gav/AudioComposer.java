package com.jsq.gav;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC;

/**
 * Created by jiang on 2019/6/3
 */


class AudioComposer {

    private MediaCodec mEncoder;

    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private int mSampleRate;
    private int mChannelCount;
    private final QueuedMuxer mMuxer;
    private boolean mIsEncodeEndOfStream = false;

    private volatile long mWrittenPresentationTimeUs;

    private int mWrittenFrames = 0;
    private long mPrevRenderTimeUs = 0;

    private AudioPipe mAudioPipe;
    private Decoder mPrimaryDecoder;
    private Decoder mSecondaryDecoder;
    private boolean mIsFeedEndOfStream;
    private int mBitWidth = 16;
    private final RawAudioFormat mDstRawAudioFormat;

    public AudioComposer(List<MediaExtractor> mediaExtractorList, List<MediaTrackInfo> mediaTrackInfoList,
                         List<MediaExtractor> bgMusicExtractors, List<MediaTrackInfo> bgMusicTrackInfos,
                         String mimeType, int sampleRate, int channelCount, int bitrate,
                         QueuedMuxer mediaMuxer, List<TimeScalePeriod> timeScalePeriodList,
                         float primaryVolume, float secondaryVolume) throws IOException {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        this.mMuxer = mediaMuxer;

        MediaFormat outFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
        outFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AACObjectLC);
        outFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        outFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 2 * channelCount);
        mDstRawAudioFormat = new RawAudioFormat(sampleRate, channelCount, mBitWidth);
        mEncoder = MediaCodec.createEncoderByType(mimeType);
        mEncoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();

        if (!bgMusicExtractors.isEmpty() && !bgMusicTrackInfos.isEmpty()) {
            mAudioPipe = new MixedAudioPipe(mDstRawAudioFormat);
            CacheAudioProcessor cacheAudioProcessor = new CacheAudioProcessor(mDstRawAudioFormat);
            ResampleAudioProcessor resampleAudioProcessor = new ResampleAudioProcessor(mDstRawAudioFormat);

            SonicAudioProcessor sonicAudioProcessor = new SonicAudioProcessor(mDstRawAudioFormat);
            sonicAudioProcessor.setTimeScalePeriodList(timeScalePeriodList);
            ((MixedAudioPipe) mAudioPipe).primaryPipe
                    .addAudioProcessor(cacheAudioProcessor, resampleAudioProcessor, sonicAudioProcessor);
            ((MixedAudioPipe) mAudioPipe).secondaryPipe
                    .addAudioProcessor(cacheAudioProcessor, resampleAudioProcessor);
            setVolume(primaryVolume, secondaryVolume);
            mPrimaryDecoder = new Decoder(mediaExtractorList, mediaTrackInfoList, ((MixedAudioPipe) mAudioPipe).primaryPipe);
            mSecondaryDecoder = new Decoder(bgMusicExtractors, bgMusicTrackInfos, ((MixedAudioPipe) mAudioPipe).secondaryPipe);
        } else {
            mAudioPipe = new SingleAudioPipe(mDstRawAudioFormat);
            mAudioPipe.addAudioProcessor(new CacheAudioProcessor(mDstRawAudioFormat));
            mAudioPipe.addAudioProcessor(new ResampleAudioProcessor(mDstRawAudioFormat));
            SonicAudioProcessor sonicAudioProcessor = new SonicAudioProcessor(mDstRawAudioFormat);
            sonicAudioProcessor.setTimeScalePeriodList(timeScalePeriodList);
            mAudioPipe.addAudioProcessor(sonicAudioProcessor);
            mPrimaryDecoder = new Decoder(mediaExtractorList, mediaTrackInfoList, mAudioPipe);
        }
    }


    /**
     * 设置声音大小
     * @param primaryVolume 视频声音
     * @param secondaryVolume 背景音乐声音
     */
    public void setVolume(float primaryVolume, float secondaryVolume) {
        if (mAudioPipe instanceof MixedAudioPipe) {
            ((MixedAudioPipe) mAudioPipe).setVolume(primaryVolume, secondaryVolume);
        }
    }

    /**
     * 处理数据
     * 文件读取数据->解码->处理音频->编码->写文件
     * @throws IOException
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void process() throws IOException {
        while (drainEncoder()) {
        }
        mPrimaryDecoder.process();
        if (mSecondaryDecoder != null) {
            mSecondaryDecoder.process();
        }
        while (drainPipe()) {

        }
    }

    /**
     * 提起管道数据输入到编码器编码
     * @return
     */
    private boolean drainPipe() {
        if (mIsFeedEndOfStream) return false;
        int index = mEncoder.dequeueInputBuffer(-1);
        if (index < 0) return index != -1;
        ByteBuffer inputBuffer = mEncoder.getInputBuffer(index);
        int size = mAudioPipe.getOutputBuffer(inputBuffer, inputBuffer.remaining());
        long timeLengthUs = AvUtil.computePcmDurationUs(size, mSampleRate, 16, mChannelCount);
        if (mAudioPipe.isEnded()) {
            mEncoder.queueInputBuffer(index, 0, size, mPrevRenderTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mIsFeedEndOfStream = true;
        } else {
            mEncoder.queueInputBuffer(index, 0, size, mPrevRenderTimeUs, 0);
        }
        mPrevRenderTimeUs += timeLengthUs;
        return mAudioPipe.getOutputSize() > 0;
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
                mMuxer.setOutputFormat(QueuedMuxer.SAMPLE_TYPE_AUDIO, newFormat);
                return true;
            }

            if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                return true;
            }
            return false;
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            mEncoder.releaseOutputBuffer(index, false);
            return false;
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsEncodeEndOfStream = true;
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
        if (outputBuffer != null) {
            mWrittenFrames++;
            outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
            outputBuffer.position(mBufferInfo.offset);
            mMuxer.writeSampleData(QueuedMuxer.SAMPLE_TYPE_AUDIO, outputBuffer, mBufferInfo);
            LogUtil.e(String.format("audio mWrittenFrames: %s, size :%s ,timeUs : %s", mWrittenFrames,
                    mBufferInfo.size, mBufferInfo.presentationTimeUs));
            mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;
        }
        mEncoder.releaseOutputBuffer(index, false);
        return true;
    }

    /**
     * 是否已经完成
     * @return
     */
    public boolean isFinished() {
        return mIsEncodeEndOfStream;
    }

    /**
     * 获取当前的编码时间
     * @return
     */
    public long getWrittenPresentationTimeUs() {
        return mWrittenPresentationTimeUs;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mPrimaryDecoder != null) {
            mPrimaryDecoder.release();
            mPrimaryDecoder = null;
        }
        if (mSecondaryDecoder != null) {
            mSecondaryDecoder.release();
            mSecondaryDecoder = null;
        }
    }

    private static class Decoder {
        private static final int EMPTY_FRAME_SIZE_BYTES = 4096;
        private static final ByteBuffer EMPTY_FRAME_BUFFER = ByteBuffer.allocate(EMPTY_FRAME_SIZE_BYTES);

        private final List<MediaExtractor> mMediaExtractorList;
        private final List<MediaTrackInfo> mMediaTrackInfoList;
        private AudioSink mAudioSink;
        private MediaExtractor mExtractor;
        private MediaCodec mDecoder;

        private boolean mIsDecodeEndOfStream = false;
        private boolean mIsExtractEndOfStream = false;
        private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        private int mCurrent = 0;
        private MediaTrackInfo mCurrentTrackInfo;

        private int mDecoderSampleRate;
        private int mDecoderChannelCount;
        private int mDecoderBitWidth;
        private long filledTime;


        public Decoder(List<MediaExtractor> extractorList, List<MediaTrackInfo> trackInfoList,
                       AudioSink audioSink) throws IOException {
            mMediaExtractorList = extractorList;
            mMediaTrackInfoList = trackInfoList;
            mAudioSink = audioSink;
            initDecoder();
        }

        @SuppressWarnings("StatementWithEmptyBody")
        void process() throws IOException {
            if (mCurrentTrackInfo.isMute) {
                while (feed()) {
                }
            } else {
                while (drainDecoder()) {
                }
                while (drainExtractor()) {
                }
            }
        }

        //没有考虑变速

        /**
         * 解码好的数据输入到 管道
         * @return
         * @throws IOException
         */
        private boolean feed() throws IOException {
            if (mIsDecodeEndOfStream) return false;
            if (mAudioSink.isFull()) return false;
            long emptyFrameLength = AvUtil.computePcmDurationUs(EMPTY_FRAME_SIZE_BYTES,
                    mDecoderSampleRate, mDecoderBitWidth, mDecoderChannelCount);
            ByteBuffer buffer;
            if (mCurrentTrackInfo.videoDurationUs - filledTime > emptyFrameLength) {
                buffer = EMPTY_FRAME_BUFFER;
                buffer.position(0);
            } else {
                buffer = ByteBuffer.allocate(AvUtil.getPcmBufferSize(mCurrentTrackInfo.videoDurationUs - filledTime,
                        mDecoderSampleRate, mDecoderBitWidth, mDecoderChannelCount));
            }
            filledTime += AvUtil.computePcmDurationUs(buffer.remaining(),
                    mDecoderSampleRate, mDecoderBitWidth, mDecoderChannelCount);
            mAudioSink.queueInput(buffer, buffer.remaining(), 0);
            if (mCurrentTrackInfo.videoDurationUs <= filledTime) {
                if (hasNext()) {
                    maybeResetDecoder();
                    LogUtil.e("change extractor");
                } else {
                    mAudioSink.queueEndOfStream();
                    mIsDecodeEndOfStream = true;
                    mBufferInfo.size = 0;
                }
            }
            return true;
        }

        /**
         * 解码
         * @return : Decoder 缓存满的时候返回true, 让其他的  Extractor和Encoder运行
         */
        private boolean drainDecoder() throws IOException {
            if (mIsDecodeEndOfStream) return false;
            if (mAudioSink.isFull()) return false;

            // TODO: 2019/7/10 这里可以添加送帧拦截器
            // 解码后 判断时间决定是否送帧出去，解出来的帧数据可以缓存到拦截器里面

            int index = mDecoder.dequeueOutputBuffer(mBufferInfo, 0);
            if (index < 0) {
                if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    return true;
                }
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mDecoder.getOutputFormat();
                    if (newFormat.containsKey("bit-width")) {
                        mDecoderBitWidth = newFormat.getInteger("bit-width");
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            newFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        int audioFormat = newFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                        if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                            mDecoderBitWidth = 16;
                        } else if (audioFormat == AudioFormat.ENCODING_PCM_8BIT) {
                            mDecoderBitWidth = 8;
                        } else {
                            LogUtil.w("unknown audioFormat,bit-width will be set to 16");
                            mDecoderBitWidth = 16;
                        }
                    }

                    mDecoderSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mDecoderChannelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mAudioSink.onFormatChange(new RawAudioFormat(mDecoderSampleRate, mDecoderChannelCount, mDecoderBitWidth));
                    return true;
                }
                return false;
            }
            boolean doRender = mBufferInfo.size != 0;

            long prevPresentationTimeUs = 0;
            for (int i = 0; i < mCurrent; i++) {
                prevPresentationTimeUs += mMediaTrackInfoList.get(i).audioDurationUs;
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

            if (doRender) {
                ByteBuffer outputBuffer = mDecoder.getOutputBuffer(index);
                if (outputBuffer != null) {
                    mAudioSink.queueInput(outputBuffer, mBufferInfo.size, mBufferInfo.presentationTimeUs);
                }
            }

            mDecoder.releaseOutputBuffer(index, false);
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (hasNext()) {
                    maybeResetDecoder();
                    LogUtil.e("change extractor");
                } else {
                    mAudioSink.queueEndOfStream();
                    mIsDecodeEndOfStream = true;
                    mBufferInfo.size = 0;
                }
            }
            return true;
        }


        /**
         * 提取帧数据
         * @return : Encoder 缓存满的时候返回true, 让其他的  Decoder和Encoder运行
         */
        private boolean drainExtractor() {
            if (mIsExtractEndOfStream) return false;
            int trackIndex = mExtractor.getSampleTrackIndex();
            if (trackIndex >= 0 && trackIndex != mCurrentTrackInfo.audioTrackIndex) return false;

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
                sampleTime += mMediaTrackInfoList.get(i).audioDurationUs;
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
         * 重置解码器
         * @throws IOException
         */
        public void maybeResetDecoder() throws IOException {
            mCurrent++;
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
            initDecoder();
        }

        /**
         * 初始化编码器
         * @throws IOException
         */
        private void initDecoder() throws IOException {
            mCurrentTrackInfo = mMediaTrackInfoList.get(mCurrent);
            filledTime = 0;
            if (mCurrentTrackInfo.isMute) {//如果静音
                mDecoderSampleRate = 44100;
                mDecoderChannelCount = 2;
                mDecoderBitWidth = 16;
                mAudioSink.onFormatChange(new RawAudioFormat(mDecoderSampleRate, mDecoderChannelCount, mDecoderBitWidth));
                return;
            }
            if (mCurrentTrackInfo.hasAudioTrack) {
                mDecoderSampleRate = 0;
                mDecoderChannelCount = 0;
                mDecoderBitWidth = 0;
                mExtractor = mMediaExtractorList.get(mCurrent);
                mDecoder = MediaCodec.createDecoderByType(mCurrentTrackInfo.audioMiniType);
                mDecoder.configure(mCurrentTrackInfo.audioMediaFormat, null, null, 0);
                mDecoder.start();
                mIsDecodeEndOfStream = false;
                mIsExtractEndOfStream = false;
            } else {
                if (hasNext()) {
                    maybeResetDecoder();
                } else {
                    mIsDecodeEndOfStream = true;
                    mIsExtractEndOfStream = true;
                }
            }

        }


        /**
         * 返回是够还有下一个音频
         * @return
         */
        private boolean hasNext() {
            return mCurrent + 1 < mMediaExtractorList.size();
        }


        /**
         * 释放资源
         */
        void release() {
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
        }
    }

}
