package com.jsq.gav;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jiang on 2019/6/12
 */

public class AvUtil {


    /**
     * @param audioChannel See {@link AudioFormat#CHANNEL_IN_MONO}
     * @return 音频声道数量
     */
    public static int getChannelCount(int audioChannel) {
        int channelCount = 0;
        switch (audioChannel) {
            case AudioFormat.CHANNEL_IN_DEFAULT:
            case AudioFormat.CHANNEL_IN_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                channelCount = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
            case (AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK):
                channelCount = 2;
                break;
            case AudioFormat.CHANNEL_INVALID:
            default:
                return 0;
        }
        return channelCount;
    }


    /**
     * 计算pcm时长
     *
     * @param size         pcm size,
     * @param sampleRate   pcm sampleRate
     * @param bitWidth     位宽
     * @param channelCount 声道数量
     * @return pcm 时间长 单位 us
     */
    public static long computePcmDurationUs(int size, int sampleRate, int bitWidth, int channelCount) {
        return (long) (Math.round(1f * size / sampleRate / bitWidth / channelCount * 8 * 1000_000L));
    }

    /**
     * 根据时长 计算pcm数据大小，单位是byte数量
     */
    public static int getPcmBufferSize(long timeUs, int sampleRate, int bitWidth, int channelCount) {
        return Math.round(1f * timeUs * sampleRate * bitWidth * channelCount / 8 / 1000_000);
    }


    public static int calcBitRate(int width, int height, int frameRate) {
        return (int) (width * height * frameRate * 0.1);
    }

    /**
     * 计算视频帧的pts
     *
     * @param frameIndex 视频帧的index
     * @param frameRate  fps
     * @return 视频帧的pts 单位 us
     */
    public static long computePresentationTime(int frameIndex, int frameRate) {
        return /*123 + */frameIndex * 1000000 / frameRate;
    }


    public static String findDecoderByMimeType(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        for (MediaCodecInfo info : codecInfos) {
            if (info.isEncoder()) continue;

            for (String type : info.getSupportedTypes()) {
                if (type.equals(mimeType)) {
                    return info.getName();
                }
            }
        }
        return null;
    }

    public static int getAudioBitDepthByWidth(int bitWidth) {
        if (bitWidth == 16) return 2;
        else if (bitWidth == 8) return 1;
        else {
            LogUtil.w(String.format("bitWidth=%s暂时不支持", bitWidth));
            return 2;
        }
    }


    public static void extractVideoFrame(Context context, Uri videoFile, String savePath, int maxSize) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        retriever.setDataSource(context, videoFile);
        Bitmap bitmap = retriever.getFrameAtTime();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int max = Math.max(width, height);
        if (max > maxSize) {
            float scale = 1f * maxSize / max;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        try (FileOutputStream outputStream = new FileOutputStream(savePath)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            bitmap.recycle();
        }
    }




}
