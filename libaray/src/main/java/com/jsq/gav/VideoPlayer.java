package com.jsq.gav;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/7
 */

public class VideoPlayer {

    private static final int MSG_PREPARE = 1;
    private static final int MSG_START = 2;
    private static final int MSG_PAUSE = 3;
    private static final int MSG_DECODING = 4;
    private static final int MSG_STOP = 5;
    private static final int MSG_SET_SURFACE = 6;
    private static final int MSG_RELEASE = 7;

    private Surface mSurface;

    protected boolean mIsPlaying = false;

    private boolean mNeedRenderFirst = true;


    private Handler mCallbackHandler;
    private OnErrorListener mOnErrorListener;
    private HandlerThread mVideoDecoderThread;
    private Handler mDecoderEventHandler;


    private DataSourceChain mDataSourceChain;
    private MediaCodec mVideoDecoder;

    private int mVideoTrackIndex = -1;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mFrameRate;
    private volatile float mSpeedScale = 1;
    private String mFilename;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer[] mInputBuffers;

    private long prevMonoUsec = 0;
    private long prevPresentUsec = 0;


    public VideoPlayer() {

    }

    public void prepare() {
        mVideoDecoderThread = new HandlerThread("video-decoder");
        mVideoDecoderThread.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });
        mVideoDecoderThread.start();
        mDecoderEventHandler = new Handler(mVideoDecoderThread.getLooper(), this::handlerVideoDecoderEvent);
        mDecoderEventHandler.sendEmptyMessage(MSG_PREPARE);
    }


    public void start() {
        mDecoderEventHandler.sendEmptyMessage(MSG_START);
    }


    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void stop() {
        mIsPlaying = false;
        mDecoderEventHandler.sendEmptyMessage(MSG_STOP);
    }

    public void pause() {
        mIsPlaying = false;
    }

    public void release() {
        mIsPlaying = false;
        mDecoderEventHandler.sendEmptyMessage(MSG_RELEASE);
        if (mVideoDecoderThread != null) {
            mVideoDecoderThread.quit();
            try {
                mVideoDecoderThread.join();
            } catch (InterruptedException ignore) {

            }
            mVideoDecoderThread = null;
        }
    }


    public void setSurface(Surface surface) {
        if (mSurface == null) {
            mSurface = surface;
            if (mDecoderEventHandler != null) {
                mDecoderEventHandler.obtainMessage(MSG_SET_SURFACE, surface).sendToTarget();
            }
        }
    }


    public void setDataSource(DataSourceChain mediaDataSource) {
        mDataSourceChain = mediaDataSource;
    }

    public void setDataSource(String filename) {
        mFilename = filename;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.setOnErrorListener(onErrorListener, new Handler(Looper.getMainLooper()));
    }

    public void setOnErrorListener(OnErrorListener onErrorListener, @NonNull Handler callbackHandler) {
        mCallbackHandler = callbackHandler;
        mOnErrorListener = onErrorListener;
    }

    public void setSpeedScale(float sc) {
        mSpeedScale = sc;
    }

    private boolean handlerVideoDecoderEvent(Message msg) {
        switch (msg.what) {
            case MSG_PREPARE:
                onPrepare();
                break;
            case MSG_START:
                onStartPlay();
                break;
            case MSG_DECODING:
                decoding();
                break;
            case MSG_PAUSE:
                break;
            case MSG_STOP:
                onStopPlay();
                break;
            case MSG_SET_SURFACE:
                onSetSurface((Surface) msg.obj);
                break;
            case MSG_RELEASE:
                onRelease();
                break;
        }
        return true;
    }

    private void onSetSurface(Surface surface) {
        this.mSurface = surface;
    }

    private void onPrepare() {
        try {
//            mDataSourceChain = new MediaExtractor();
//            mDataSourceChain.setDataSource(mFilename);
            mDataSourceChain.prepare();
            MediaFormat videoMediaFormat = mDataSourceChain.getTrackFormat("video/");

            mDataSourceChain.selectTrack("video/");
            mVideoWidth = videoMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            mVideoHeight = videoMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            mFrameRate = videoMediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            mVideoDecoder = MediaCodec.createDecoderByType(videoMediaFormat.getString(MediaFormat.KEY_MIME));
            mVideoDecoder.configure(videoMediaFormat, mSurface, null, 0);
            mVideoDecoder.start();
            mInputBuffers = mVideoDecoder.getInputBuffers();

            mNeedRenderFirst = true;
            mDecoderEventHandler.sendEmptyMessage(MSG_DECODING);

        } catch (Exception e) {
            if (mOnErrorListener != null) {
                mCallbackHandler.post(() -> mOnErrorListener.onError(e));
            }
        }
    }


    private void initDecoder() {

    }

    public void seekTo(long time) {
        pause();
        mDataSourceChain.seekTo(time, 0);
        mNeedRenderFirst = true;
        start();
        pause();
    }

    private void onStartPlay() {
        mIsPlaying = true;
        mDecoderEventHandler.sendEmptyMessage(MSG_DECODING);
    }

    private void decoding() {
        if (mVideoDecoder != null) {
            while (renderer()) {
                //nothing to do
            }
            while (!mDataSourceChain.isEof()) {
                int index = mVideoDecoder.dequeueInputBuffer(0);
                if (index < 0) {
                    break;
                }
                ByteBuffer inputBuffer = mVideoDecoder.getInputBuffer(index);
                int size = mDataSourceChain.readSampleData(inputBuffer, 0);
                if (size < 0) {
                    mVideoDecoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    long sampleTime = mDataSourceChain.getSampleTime();
                    mVideoDecoder.queueInputBuffer(index, 0, size, sampleTime, 0);
                }
                mDataSourceChain.advance();
            }
            if (mIsPlaying || mNeedRenderFirst) {
                mDecoderEventHandler.sendEmptyMessage(MSG_DECODING);
            }
        }

    }


    private boolean renderer() {
        if (mVideoDecoder != null) {
            int index = mVideoDecoder.dequeueOutputBuffer(mBufferInfo, 0);
            LogUtil.e("decoder : index:" + index);
            switch (index) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    return false;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    return true;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    return true;
            }
            if (index >= 0) {
                boolean doRender = (mBufferInfo.size != 0);
                // time ctrl
                if (doRender) {
                    long presentationTimeUs = mBufferInfo.presentationTimeUs;
                    if (prevMonoUsec == 0) {
                        prevMonoUsec = System.nanoTime() / 1000;
                        prevPresentUsec = presentationTimeUs;
                    } else {
                        long frameDelta = presentationTimeUs - prevPresentUsec;
                        frameDelta = Math.max(0, frameDelta);
                        long runDelta = Math.max(0, System.nanoTime() / 1000 - prevMonoUsec);

                        long frameDeltaScale = (long) (frameDelta / mSpeedScale);
//                        long frameDeltaScale = (frameDelta *1000 - sleepDelta
                        LogUtil.e("VideoPlayer: frameDeltaScale:" + frameDeltaScale + " runDelta:" + runDelta);
                        try {
                            Thread.sleep(frameDeltaScale / 1000, (int) (frameDeltaScale % 1000) * 1000);
                        } catch (InterruptedException ignore) {

                        }

                        prevMonoUsec += frameDelta;
                        prevPresentUsec += frameDelta;
                    }
                }
                mVideoDecoder.releaseOutputBuffer(index, doRender);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    LogUtil.e("VideoPlayer: decoder eos");
//                        isDecodeEOS = true;
                    mVideoDecoder.stop();
                    mVideoDecoder.release();
                    mVideoDecoder = null;
                    MediaFormat nextTrackFormat = mDataSourceChain.getNextTrackFormat("video/");
                    if (nextTrackFormat != null) {
                        try {
                            mVideoDecoder = MediaCodec.createDecoderByType(nextTrackFormat.getString(MediaFormat.KEY_MIME));
                            mVideoDecoder.configure(nextTrackFormat, mSurface, null, 0);
                            mVideoDecoder.start();
                            prevMonoUsec = 0;
                            prevPresentUsec = 0;
                            mDataSourceChain.changeNext();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                if (mNeedRenderFirst && doRender) {
                    mNeedRenderFirst = false;
                    return false;
                }
            }
            return isPlaying();
        }
        return false;
    }


    private void onStopPlay() {
        mIsPlaying = false;
        mVideoDecoder.flush();
        mDataSourceChain.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC);
    }

    private void onRelease() {
        if (mDataSourceChain != null) {
            mDataSourceChain.release();
            mDataSourceChain = null;
        }
        if (mVideoDecoder != null) {
            mVideoDecoder.flush();
            mVideoDecoder.stop();
            mVideoDecoder.release();
            mVideoDecoder = null;
        }
    }

    public interface OnErrorListener {
        void onError(Throwable throwable);
    }

    public interface OnPreparedListener {
        void onPrepared(int width, int height);
    }


}
