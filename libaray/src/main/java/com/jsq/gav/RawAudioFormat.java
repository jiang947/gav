package com.jsq.gav;

/**
 * Created by jiang on 2019/7/11
 * pcm params wrapper
 */

public class RawAudioFormat {


    public int sampleRate;
    public int channelCount;
    public int bitWidth;

    public RawAudioFormat(int sampleRate, int channelCount, int bitWidth) {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.bitWidth = bitWidth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof RawAudioFormat)) return false;
        return this.sampleRate == ((RawAudioFormat) obj).sampleRate
                && this.channelCount == ((RawAudioFormat) obj).channelCount
                && this.bitWidth == ((RawAudioFormat) obj).bitWidth;
    }
}
