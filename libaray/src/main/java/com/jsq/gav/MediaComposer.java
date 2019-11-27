package com.jsq.gav;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;

import com.jsq.gav.gles.GlFilterConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

/**
 * Created by jiang on 2019/5/31
 */

public class MediaComposer {

    private static final int DEFAULT_AUDIO_BITRATE = 96000; // 96k
    private static final int DEFAULT_SAMPLE_RATE = 44100;  // 44.1 kHZ

    private final Context mContext;
    private final Handler mHandler;

    private List<MediaSourceInfo> mMediaSourceInfoList;
    private List<MediaSourceInfo> mBackgroundMusicList;
    private String mOutFile;

    private Listener mListener;

    // video params
    private int mFrameRate;
    private int mWidth;
    private int mHeight;

    private Collection<GlFilterConfig> mGlFilterConfigs;

    // audio params
    private int mAudioBitrate = DEFAULT_AUDIO_BITRATE;
    private int mSampleRate = DEFAULT_SAMPLE_RATE;

    private List<TimeScalePeriod> mTimeScalePeriodList;
    private float intensity;
    private List<String> imagePathList;
    private int imageIndex;
    private float primaryVolume;
    private float secondaryVolume;

    private MediaComposer(Context context) {
        this.mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public MediaComposer mediaInfoList(List<MediaSourceInfo> list) {
        this.mMediaSourceInfoList = list;
        return this;
    }

    public MediaComposer backgroundMusic(List<MediaSourceInfo> list) {
        mBackgroundMusicList = list;
        return this;
    }


    public static MediaComposer with(Context context) {
        return new MediaComposer(context);
    }

    public MediaComposer output(String output) {
        this.mOutFile = output;
        return this;
    }

    public MediaComposer frameRate(int frameRate) {
        mFrameRate = frameRate;
        return this;
    }

    public MediaComposer width(int width) {
        mWidth = width;
        return this;
    }

    public MediaComposer glFilterConfigs(Collection<GlFilterConfig> glFilterConfigs) {
        mGlFilterConfigs = glFilterConfigs;
        return this;
    }

    public MediaComposer height(int height) {
        mHeight = height;
        return this;
    }

    /**
     * 设置输出的 音频比特率 默认 96k
     *
     * @param bitrate 音频比特率
     * @return MediaComposer
     */
    public MediaComposer audioBitrate(int bitrate) {
        this.mAudioBitrate = bitrate;
        return this;
    }

    /**
     * 设置输出的音频采样率 默认 44.1k
     *
     * @param sampleRate 音频采样率
     * @return MediaComposer
     */
    public MediaComposer sampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
        return this;
    }

    /**
     * 设置 Listener
     *
     * @param listener 一个监听
     * @return MediaComposer
     */
    public MediaComposer listener(Listener listener) {
        mListener = listener;
        return this;
    }

    /**
     * 设置输出视频的速度控制时间段列表
     *
     * @param timeScalePeriodList
     * @return MediaComposer
     */
    public MediaComposer timeScalePeriodList(List<TimeScalePeriod> timeScalePeriodList) {
        this.mTimeScalePeriodList = timeScalePeriodList;
        return this;
    }


    public MediaComposer setIntensity(float intensity) {
        this.intensity = intensity;
        return this;
    }

    public MediaComposer setImagePathList(List<String> imagePathList) {
        this.imagePathList = imagePathList;
        return this;
    }

    public MediaComposer setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        return this;
    }

    public MediaComposer setVolume(float primaryVolume, float secondaryVolume) {
        this.primaryVolume = primaryVolume;
        this.secondaryVolume = secondaryVolume;
        return this;
    }

    /**
     * 执行
     */
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                compose();
            } catch (Exception e) {
                if (mListener != null) {
                    mHandler.post(() -> mListener.onError(e));
                }
            }
        });
        thread.setName("media-composer");
        thread.start();
    }


    /**
     * 处理音视频数据
     * @throws Exception
     */
    private void compose() throws Exception {
        long startCompose = System.currentTimeMillis();
        List<MediaExtractor> videoExtractors = new ArrayList<>();
        List<MediaExtractor> audioExtractors = new ArrayList<>();
        List<MediaTrackInfo> mediaTrackInfos = new ArrayList<>();
        List<MediaExtractor> bgMusicExtractors = new ArrayList<>();
        List<MediaTrackInfo> bgMusicTrackInfos = new ArrayList<>();
        VideoComposer videoComposer = null;
        AudioComposer audioComposer = null;
        QueuedMuxer muxer = null;
        try {
            int totalDurationUs = 0;
            for (MediaSourceInfo sourceInfo : mMediaSourceInfoList) {
                long start = 0;
                long end = 0;

                MediaExtractor videoExtractor = new MediaExtractor();
                MediaExtractor audioExtractor = new MediaExtractor();
                videoExtractor.setDataSource(mContext, sourceInfo.source, null);
                audioExtractor.setDataSource(mContext, sourceInfo.source, null);
                MediaTrackInfo mediaTrackInfo = MediaTrackInfo.parse(videoExtractor);
                if (mediaTrackInfo.hasVideoTrack) {
                    if (mWidth == 0 && mHeight == 0) {
                        mWidth = mediaTrackInfo.width;
                        mHeight = mediaTrackInfo.height;
                    }
                    if (mFrameRate == 0) {
                        mFrameRate = mediaTrackInfo.frameRate;
                    }
                    videoExtractor.selectTrack(mediaTrackInfo.videoTrackIndex);
                }
                if (mediaTrackInfo.hasAudioTrack) {
                    audioExtractor.selectTrack(mediaTrackInfo.audioTrackIndex);
                }
                mediaTrackInfo.isMute = sourceInfo.isMute;
                if (sourceInfo.startTimeUs > 0) {
                    mediaTrackInfo.startTimeUs = sourceInfo.startTimeUs;
                }
                if (sourceInfo.endTimeUs > 0) {
                    mediaTrackInfo.endTimeUs = sourceInfo.endTimeUs;
                }
                start = sourceInfo.startTimeUs > 0 ? sourceInfo.startTimeUs : 0;
                if (sourceInfo.endTimeUs < 0) {
                    end = sourceInfo.durationMs * 1000L;
                } else {
                    end = sourceInfo.endTimeUs;
                }
                totalDurationUs += (end - start);

                mediaTrackInfos.add(mediaTrackInfo);
                videoExtractors.add(videoExtractor);
                audioExtractors.add(audioExtractor);
            }

            if (mBackgroundMusicList != null) {
                for (MediaSourceInfo sourceInfo : mBackgroundMusicList) {
                    MediaExtractor audioExtractor = new MediaExtractor();
                    audioExtractor.setDataSource(mContext, sourceInfo.source, null);
                    MediaTrackInfo mediaTrackInfo = MediaTrackInfo.parse(audioExtractor);
                    if (!mediaTrackInfo.hasAudioTrack) {
                        throw new Exception("背景音乐无法读取");
                    }
                    audioExtractor.selectTrack(mediaTrackInfo.audioTrackIndex);
                    if (sourceInfo.startTimeUs > 0) {
                        mediaTrackInfo.startTimeUs = sourceInfo.startTimeUs;
                    }
                    if (sourceInfo.endTimeUs > 0) {
                        mediaTrackInfo.endTimeUs = sourceInfo.endTimeUs;
                    }
                    if (sourceInfo.paddingStartTimeUs > 0) {
                        mediaTrackInfo.audioPaddingStartTimeUs = sourceInfo.paddingStartTimeUs;
                    }

                    bgMusicExtractors.add(audioExtractor);
                    bgMusicTrackInfos.add(mediaTrackInfo);
                }
            }


            muxer = new QueuedMuxer(mOutFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //初始化 video 的 format
            MediaFormat videoOutFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, mWidth, mHeight);
            videoOutFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            videoOutFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatSurface);
            videoOutFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
            videoOutFormat.setInteger(MediaFormat.KEY_BIT_RATE, AvUtil.calcBitRate(mWidth, mHeight, mFrameRate));


            videoComposer = new VideoComposer(mContext, mWidth, mHeight, mFrameRate,
                    videoExtractors, mediaTrackInfos, videoOutFormat, muxer, mTimeScalePeriodList, intensity, imagePathList, imageIndex);
            if (mGlFilterConfigs != null) {
                videoComposer.addAddFilterConfig(mGlFilterConfigs);
            }
            audioComposer = new AudioComposer(audioExtractors, mediaTrackInfos,
                    bgMusicExtractors, bgMusicTrackInfos,
                    MIMETYPE_AUDIO_AAC, mSampleRate, 2, mAudioBitrate,
                    muxer, mTimeScalePeriodList, primaryVolume, secondaryVolume);
            while (!(videoComposer.isFinished() && audioComposer.isFinished())) {
                videoComposer.process();
                audioComposer.process();

                if (mListener != null) {
                    double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / totalDurationUs);
                    double audioProgress = audioComposer.isFinished() ? 1.0 : Math.min(1.0, (double) audioComposer.getWrittenPresentationTimeUs() / totalDurationUs);
                    double progress = (videoProgress + audioProgress) / 2.0;
                    mHandler.post(() -> mListener.onProcess(progress));
                }
            }
//            muxer.stop();
            if (mListener != null) {
                mHandler.post(() -> mListener.onComplete());
            }
            LogUtil.e("compose time:" + (System.currentTimeMillis() - startCompose));
            LogUtil.e("compose finished path: " + mOutFile);
        } finally {
            releaseExtractors(videoExtractors);
            releaseExtractors(audioExtractors);
            releaseExtractors(bgMusicExtractors);
            if (muxer != null) {
                muxer.release();
            }
            if (audioComposer != null) {
                audioComposer.release();
            }
            if (videoComposer != null) {
                videoComposer.release();
            }
        }
    }

    private void releaseExtractors(List<MediaExtractor> extractors) {
        for (MediaExtractor extractor : extractors) {
            extractor.release();
        }
    }


    private static int findTrackIndex(MediaExtractor extractor, String flag) {
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            if (mediaFormat.getString(MediaFormat.KEY_MIME).contains(flag)) {
                return i;
            }
        }
        return -1;
    }


    public interface Listener {

        default void onProcess(double progress) {
        }

        default void onComplete() {
        }

        default void onError(Throwable throwable) {
        }
    }

}
