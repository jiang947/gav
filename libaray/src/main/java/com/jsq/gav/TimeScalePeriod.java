package com.jsq.gav;

/**
 * Created by jiang on 2019/6/5
 */

public class TimeScalePeriod {

    public TimeScalePeriod(long startTimeMs, long endTimeMs, float scale) {
        this.startTimeUs = startTimeMs * 1000L;
        this.endTimeUs = endTimeMs * 1000L;
        this.speed = scale;
    }

    public long startTimeUs;

    public long endTimeUs;

    /**
     * 速度 1 普通速度，2:是2倍速度
     */
    public float speed;


}
