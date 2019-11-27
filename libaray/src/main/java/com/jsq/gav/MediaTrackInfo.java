package com.jsq.gav;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/**
 * Created by jiang on 2019/6/2
 */

public class MediaTrackInfo {

    public int width;
    public int height;
    public int frameRate;

    public boolean hasVideoTrack;

    public boolean hasAudioTrack;
    public int degrees;
    public MediaFormat videoMediaFormat;

    public MediaFormat audioMediaFormat;

    public String videoMiniType;
    public String audioMiniType;


    public int videoTrackIndex;
    public int audioTrackIndex;
    public long videoDurationUs;

    public long audioDurationUs;
    public long sampleRate;

    public long startTimeUs = -1;
    public long endTimeUs = -1;
    public long audioPaddingStartTimeUs = -1;

    public boolean isMute = false;


    public static MediaTrackInfo parse(MediaExtractor mediaExtractor) {
        MediaTrackInfo info = new MediaTrackInfo();
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            if (hasVideoTrack(format)) {
                info.videoTrackIndex = i;
                info.hasVideoTrack = true;
                info.videoMediaFormat = format;
                info.width = format.getInteger(MediaFormat.KEY_WIDTH);
                info.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                info.videoMiniType = format.getString(MediaFormat.KEY_MIME);
                info.videoDurationUs = format.getLong(MediaFormat.KEY_DURATION);
                info.frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                if (format.containsKey("rotation-degrees")) { //MediaFormat.KEY_ROTATION
                    info.degrees = format.getInteger("rotation-degrees");
                }
            }
            if (hasAudioGTrack(format)) {
                info.audioMiniType = format.getString(MediaFormat.KEY_MIME);
                info.audioMediaFormat = format;
                info.audioTrackIndex = i;
                info.hasAudioTrack = true;
                info.audioDurationUs = format.getLong(MediaFormat.KEY_DURATION);
                info.sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            }
        }
        return info;
    }


    static boolean hasVideoTrack(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME).contains("video/");
    }

    static boolean hasAudioGTrack(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME).contains("audio/");
    }


}
