package com.jsq.gav;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/13
 */

public class AudioMediaCodecDecoder implements AudioDecoder {

    private MediaCodec mDecoder;
    private Extractor mExtractor;
    private MediaFormat mMediaFormat;


    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS = false;
    private int mTrackIndex;
    private DecoderEvent mEvent;
    private Handler mEventHandler;

//    private byte[] mBuffer = null;

    public AudioMediaCodecDecoder(Context context, Uri file) {
        mExtractor = new MediaCodecExtractor();
        try {
            mExtractor.setDataSource(context, file, null);
            int trackCount = mExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mExtractor.getTrackFormat(i);
                if (trackFormat.getString(MediaFormat.KEY_MIME).contains("audio/")) {
                    mMediaFormat = trackFormat;
                    mTrackIndex = i;
                    mExtractor.selectTrack(mTrackIndex);
                    break;
                }
            }
            mDecoder = MediaCodec.createDecoderByType(mMediaFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void configure() {
        mDecoder.configure(mMediaFormat, null, null, 0);
        mDecoder.start();
    }


    public void drainExtractor() {
        while (!mIsExtractorEOS) {
            int trackIndex = mExtractor.getSampleTrackIndex();
            if (trackIndex >= 0 && trackIndex != this.mTrackIndex) break;

            int index = mDecoder.dequeueInputBuffer(0);

            if (index < 0) break;
            if (trackIndex < 0) {
                mIsExtractorEOS = true;
                mDecoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                break;
            }

            ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
            if (inputBuffer != null) {
                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
                long sampleTime = mExtractor.getSampleTime();
                mDecoder.queueInputBuffer(index, 0, sampleSize, sampleTime, isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
            }
            mExtractor.advance();
        }
    }

    @Override
    public void setDecoderEvent(DecoderEvent event) {
        mEvent = event;
    }

    @Override
    public void setDecoderEvent(DecoderEvent event, Handler handler) {
        mEventHandler = handler;
        this.mEvent = event;
    }

    public void drainDecoder() {
        if (mIsDecoderEOS) return;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index = mDecoder.dequeueOutputBuffer(bufferInfo, 0);
        switch (index) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MediaFormat newFormat = mDecoder.getOutputFormat();
                int sampleRateInHz = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                if (newFormat.containsKey("bit-width")) {
                    if (newFormat.getInteger("bit-width") == 16) {
                        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    }
                }
                if (newFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    audioFormat = newFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
                }

                int channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                String mine = newFormat.getString(MediaFormat.KEY_MIME);
                LogUtil.e("AudioDecoder: new format" + newFormat);
                if (mEventHandler != null) {
                    int finalAudioFormat = audioFormat;
                    mEventHandler.post(() -> {
                        mEvent.OnFormatChange(sampleRateInHz, channelCount, finalAudioFormat);
                    });
                } else {
                    mEvent.OnFormatChange(sampleRateInHz, channelCount, audioFormat);
                }
                //noinspection deprecation
                return;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                break;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsDecoderEOS = true;
            bufferInfo.size = 0;
            mEvent.onEof();
        }
        LogUtil.e("AudioDecoder: drainDecoder()" + index);
        if (index >= 0) {
            boolean doRender = bufferInfo.size > 0;
            LogUtil.e("drainDecoder: time :" + bufferInfo.presentationTimeUs + " size " + bufferInfo.size);
            ByteBuffer outputBuffer = null;

            if (doRender) {
                outputBuffer = mDecoder.getOutputBuffer(index);
                LogUtil.e("AudioDecoder:" + outputBuffer.capacity() + " :" + bufferInfo.size);
                byte[] mBuffer = new byte[bufferInfo.size];
                outputBuffer.limit(bufferInfo.size);
                outputBuffer.get(mBuffer, 0, outputBuffer.remaining());
                if (mEventHandler != null) {
                    mEventHandler.post(() -> mEvent.OnRenderFrame(mBuffer, bufferInfo.presentationTimeUs));
                } else {
                    mEvent.OnRenderFrame(mBuffer, bufferInfo.presentationTimeUs);
                }
            }
            mDecoder.releaseOutputBuffer(index, false);
        }
    }

    public boolean isDecoderEOS() {
        return mIsDecoderEOS;
    }

    public void release() {
        mDecoder.stop();
        mDecoder.release();
        mExtractor.release();
    }
}
