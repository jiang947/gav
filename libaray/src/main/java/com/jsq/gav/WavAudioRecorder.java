package com.jsq.gav;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Created by jiang on 2019/5/12
 * <p>
 * -------------------------------------------
 * |          RIFF WAVE Chunk                |
 * |          ID   = "RIFF"                  |
 * |          RiffType = "WAVE"              |
 * -------------------------------------------
 * |          Format Chunk                   |
 * |          ID = "fmt "                    |
 * -------------------------------------------
 * |          Fact Chunk(optional)           |
 * |          ID = "fact"                    |
 * -------------------------------------------
 * |          Data Chunk                     |
 * |          ID = "data"                    |
 * -------------------------------------------
 * <p>
 * Wave文件格式详细说明
 * 别名      |  字节数   |  类型  |    注释
 * ckid            4        char      "RIFF" 标志, 大写
 * cksize          4        int32     文件长度。这个长度不包括"RIFF"标志 和文件长度 本身所占字节, 下面的
 * fcc type        4        char      "WAVE" 类型块标识, 大写。
 */

public class WavAudioRecorder implements Runnable {
    public static final int RIFF_FOURCC = getIntegerCodeForString("RIFF");
    public static final int WAVE_FOURCC = getIntegerCodeForString("WAVE");
    public static final int FMT_FOURCC = getIntegerCodeForString("fmt ");
    public static final int DATA_FOURCC = getIntegerCodeForString("data");


    private final int mBufferSize;
    private AudioRecord mAudioRecord;

    static final int audioSampleRate = 44100;
    static final int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
    static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private String mFilepath;
    private volatile boolean mIsRecording = false;

    public WavAudioRecorder() {
        mBufferSize = AudioRecord.getMinBufferSize(audioSampleRate, audioChannel, audioFormat);
    }


    public void start(String filepath) {
        if (mIsRecording) return;
        this.mFilepath = filepath;
        mIsRecording = true;
        new Thread(this).start();
    }


    public void stop() {
        mIsRecording = false;
    }


    private void writeWaveFileHeader(RandomAccessFile raf, int sampleRate,
                                     int audioChannel) throws IOException {
        short channelCount = getChannelCount(audioChannel);
        raf.writeInt(RIFF_FOURCC);//4
        raf.writeInt(-1); //8
        raf.writeInt(WAVE_FOURCC);
        raf.writeInt(FMT_FOURCC);
        raf.write(int2bytes(16));
        raf.write((byte) 1);//PCM format
        raf.write((byte) 0);
        raf.write((byte) channelCount); //channelCount
        raf.write((byte) 0);

        raf.write(int2bytes(sampleRate)); //sampleRate

        int bytesPerSample = 2 * channelCount; // sampleDeep * channelCount

        raf.write(int2bytes(bytesPerSample * sampleRate)); //byteRate

        raf.write((byte) bytesPerSample);
        raf.write((byte) 0);
        raf.write((byte) (8 * bytesPerSample / channelCount));
        raf.write((byte) 0);
        raf.writeInt(DATA_FOURCC);  //40
        raf.writeInt(-1);
    }

    private void updateFileHeader(RandomAccessFile file) throws IOException {
        long size = file.getChannel().size();
        long ckSize = size - 8;
        file.seek(4);
        file.write(int2bytes((int) ckSize));
        file.seek(40);
        ckSize = size - 44;
        file.write(int2bytes((int) ckSize));
    }

    static short getChannelCount(int audioChannel) {
        switch (audioChannel) {
            case AudioFormat.CHANNEL_IN_MONO:
                return 1;
            case AudioFormat.CHANNEL_IN_STEREO:
                return 2;
            default:
                throw new RuntimeException("audioChannel 只支持 CHANNEL_IN_MONO | CHANNEL_IN_STEREO");
        }
    }

    static byte[] int2bytes(int i) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (i & 0xff);
        bytes[1] = (byte) (i >> 8 & 0xff);
        bytes[2] = (byte) (i >> 16 & 0xff);
        bytes[3] = (byte) (i >> 24 & 0xff);
        return bytes;
    }

    static int getIntegerCodeForString(String string) {
        int length = string.length();
        int result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 8;
            result |= string.charAt(i);
        }
        return result;
    }

    @Override
    public void run() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, audioSampleRate, audioChannel,
                audioFormat, mBufferSize);
        mAudioRecord.startRecording();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(mFilepath, "rw")) {
            writeWaveFileHeader(randomAccessFile, audioSampleRate, audioChannel);
            ByteBuffer audioBuffer = ByteBuffer.allocateDirect(mBufferSize);
            byte[] bytes = new byte[mBufferSize];
            int result;
            while (mIsRecording) {
                result = mAudioRecord.read(audioBuffer, audioBuffer.capacity());
                if (result > 0) {
                    audioBuffer.get(bytes, 0, audioBuffer.limit());
                    randomAccessFile.write(bytes);
                    audioBuffer.clear();
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

            }
            updateFileHeader(randomAccessFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioRecord.stop();
        mAudioRecord.release();
    }


}
