package com.jsq.gav;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by jiang on 2019/5/30
 */

public class MediaSourceInfo implements Serializable, Parcelable {

    /**
     * 视频源文件uri
     */
    public Uri source;
    /**
     * 开始时间
     */
    public long startTimeUs = 0;
    /**
     * 结束时间
     */
    public long endTimeUs = 0;
    /**
     * 距离开始的时间
     */
    public long paddingStartTimeUs = 0;
    /**
     * 视频封面图片路径
     */
    public String videoCover;
    public boolean isMute = false;

    /**
     * 单位毫秒
     */
    public long durationMs;

    public MediaSourceInfo(Uri source) {
        this.source = source;
    }

    public MediaSourceInfo(Uri source, long startTimeUs) {
        this.source = source;
        this.startTimeUs = startTimeUs;
    }


    protected MediaSourceInfo(Parcel in) {
        source = in.readParcelable(Uri.class.getClassLoader());
        startTimeUs = in.readLong();
        endTimeUs = in.readLong();
        paddingStartTimeUs = in.readLong();
        videoCover = in.readString();
        durationMs = in.readLong();
    }

    public static final Creator<MediaSourceInfo> CREATOR = new Creator<MediaSourceInfo>() {
        @Override
        public MediaSourceInfo createFromParcel(Parcel in) {
            return new MediaSourceInfo(in);
        }

        @Override
        public MediaSourceInfo[] newArray(int size) {
            return new MediaSourceInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(source, flags);
        dest.writeLong(startTimeUs);
        dest.writeLong(endTimeUs);
        dest.writeLong(paddingStartTimeUs);
        dest.writeString(videoCover);
        dest.writeLong(durationMs);
    }
}
