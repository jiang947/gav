package com.jsq.gav.widget;

import androidx.annotation.Nullable;

/**
 * Created by jiang on 2019/5/8
 */

public class VideoFrameFormat {

    public int width;
    public int height;
    public int degrees;

    public VideoFrameFormat(int width, int height, int degrees) {
        this.width = width;
        this.height = height;
        this.degrees = degrees;
    }

    @Override
    public String toString() {
        return "VideoFrameFormat{" +
                "width=" + width +
                ", height=" + height +
                ", degrees=" + degrees +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof VideoFrameFormat)) return false;
        return this.width==((VideoFrameFormat) obj).width && this.height==((VideoFrameFormat) obj).height
                && this.degrees == ((VideoFrameFormat) obj).degrees;
    }
}
