package com.jsq.gav;

/**
 * Created by jiang on 2019/6/13
 */

public interface OnRecordListener {

    /**
     * 录制时长回调
     *
     * @param duration 当前录制片段时长
     */
    void onRecordProgress(long duration);

    /**
     * 录制停止
     *
     * @param duration 当前录制片段时长
     */
    void onRecordStopped(long duration);

}
