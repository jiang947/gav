package com.jsq.gav;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jiang on 2019/6/6
 */

public class SonicAudioProcessor implements AudioProcessor {


    private Sonic mSonic;
    private RawAudioFormat mOutRawAudioFormat;
    private RawAudioFormat mInRawAudioFormat;
    private List<TimeScalePeriod> mTimeScalePeriodList;
    private byte[] mOutBuffer;
    private byte[] mInBuffer;

    public SonicAudioProcessor(RawAudioFormat formats) {
        mSonic = new Sonic(formats.sampleRate, formats.channelCount);
        mOutRawAudioFormat = formats;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer, long presentationTimeUs) {
        int inputSize = inputBuffer.remaining();
        if (mInBuffer == null || mInBuffer.length < inputSize) {
            mInBuffer = new byte[inputSize];
        }
        inputBuffer.get(mInBuffer, 0, inputSize);
        mSonic.writeBytesToStream(mInBuffer, inputSize);

//        mSonic.queueInput(inputBuffer.asShortBuffer());
    }

    @Override
    public int getOutputBuffer(ByteBuffer byteBuffer) {
        int outputSize = getOutputSize();
        if (mOutBuffer == null || mOutBuffer.length < outputSize) {
            mOutBuffer = new byte[outputSize];
        }
        int readBytesFromStream = mSonic.readBytesFromStream(mOutBuffer, outputSize);
        byteBuffer.put(mOutBuffer, 0, readBytesFromStream);
        return readBytesFromStream;
//        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
//        int outputSize = mSonic.getOutput(shortBuffer)* mOutRawAudioFormat.channelCount * 2;
//        byteBuffer.position(outputSize);
//        return outputSize;
    }

    @Override
    public int getOutputSize() {
        return mSonic.samplesAvailable() * mOutRawAudioFormat.channelCount * 2;
    }

    @Override
    public void onInputFormatChange(RawAudioFormat rawAudioFormat) {

    }

    public void setTimeScalePeriodList(List<TimeScalePeriod> timeScalePeriodList) {
        mTimeScalePeriodList = timeScalePeriodList;
    }

    @Override
    public boolean isActive(long presentationTimeUs) {
        boolean isActive = false;
        if (mTimeScalePeriodList != null && !mTimeScalePeriodList.isEmpty()) {
            for (TimeScalePeriod period : mTimeScalePeriodList) {
                if (presentationTimeUs >= period.startTimeUs && presentationTimeUs <= period.endTimeUs) {
                    mSonic.setSpeed(period.speed);
                    isActive = true;
                    break;
                }
            }
        }
        return isActive;
    }


    public void setSpeed(float speed) {
        mSonic.setSpeed(speed);
    }

    public void setPitch(float pitch) {
        mSonic.setPitch(pitch);
    }

    public void setRate(float rate) {
        mSonic.setRate(rate);
    }

}
