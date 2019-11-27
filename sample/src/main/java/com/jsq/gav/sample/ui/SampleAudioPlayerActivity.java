package com.jsq.gav.sample.ui;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jsq.gav.AudioMediaCodecDecoder;
import com.jsq.gav.AudioPlayer;
import com.jsq.gav.DataSourceChain;
import com.jsq.gav.FfmpegResample;
import com.jsq.gav.WavAudioRecorder;
import com.jsq.gav.sample.R;
import com.jsq.gav.sample.base.BaseActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SampleAudioPlayerActivity extends BaseActivity {

    private AudioTrack audioTrack;

    private WavAudioRecorder mWavAudioRecorder;

    static final int audioSampleRate = 44100;
    static final int audioChannel = AudioFormat.CHANNEL_OUT_STEREO; //CHANNEL_OUT_STEREO CHANNEL_OUT_MONO
    static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private volatile boolean isStarted = false;
    AudioPlayer mMusicPlayer;

    public static void start(Context context) {
        Intent starter = new Intent(context, SampleAudioPlayerActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_audio_player);
//        Uri videoUri = Uri.parse("android.resource://"
//                + getPackageName() + "/"
//                + R.raw.bgs);
        mMusicPlayer = new AudioPlayer();
        DataSourceChain chain = new DataSourceChain.Builder()
                .addMediaDataSource("/storage/emulated/0/Music/1.aac")
                .build();

        mMusicPlayer.setDataSource(chain);

        mMusicPlayer.prepare();

        findViewById(R.id.extract_pcm).setOnClickListener(v -> {
            if (mMusicPlayer.isPlaying()) {
                mMusicPlayer.pause();
            } else {
                mMusicPlayer.start();
            }
        });

        findViewById(R.id.test).setOnClickListener(v -> {
            Observable.just(0)
                    .map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer integer) throws Exception {
                            File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
                            File file1 = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "code.pcm");
                            FileInputStream inputStream = new FileInputStream(file);
                            FileOutputStream outputStream = new FileOutputStream(file1);
                            int inSize = 4096;
                            ByteBuffer inBuffer = ByteBuffer.allocateDirect(inSize).order(ByteOrder.nativeOrder());
                            inBuffer.position(0);
                            inBuffer.limit(inSize);


                            int outSize = 65536;
                            ByteBuffer outBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder());
                            outBuffer.position(0);
                            outBuffer.limit(outSize);
                            FfmpegResample resample = new FfmpegResample(2, 44100, 16, 2, 22050, 16);
                            FileChannel channel = inputStream.getChannel();
                            while (channel.read(inBuffer) > 0) {
                                int size = resample.process(inBuffer, outBuffer);
                                inBuffer.compact();
                                outBuffer.position(0);
                                outBuffer.limit(size);
                                outputStream.getChannel().write(outBuffer);
                                outBuffer.clear();
                            }
                            outputStream.flush();
                            inputStream.close();
                            outputStream.close();
                            resample.release();
                            return 0;

                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Object>() {
                        @Override
                        public void accept(Object o) throws Exception {
                            Toast.makeText(SampleAudioPlayerActivity.this, "ok", Toast.LENGTH_SHORT).show();
                        }
                    }, Throwable::printStackTrace);

        });

        findViewById(R.id.test2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        findViewById(R.id.recorder_pcm).setOnClickListener(this::recorderPcm);

    }

    private void extractPcm(View v) {

        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                + R.raw.bgs);
//        videoUri = Uri.parse("/storage/emulated/0/Movies/cyy_b6596d27-b623-4b82-922b-e064f0172807.mp4");
        AudioMediaCodecDecoder audioDecoder = new AudioMediaCodecDecoder(this, videoUri);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int session = audioManager.generateAudioSessionId();
        int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannel, audioFormat);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(audioSampleRate)
                .setEncoding(audioFormat)
                .build();
        audioTrack = new AudioTrack(audioAttributes, format, minBufferSize, AudioTrack.MODE_STREAM, session);

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
//        Equalizer equalizer = new Equalizer(0, session);
//        final short minEqualizer = equalizer.getBandLevelRange()[0];
//        final short maxEqualizer = equalizer.getBandLevelRange()[1];
//        short bands = equalizer.getNumberOfBands();
//        audioTrack.attachAuxEffect(equalizer.getId());
//        equalizer.setBandLevel((short) 0, minEqualizer);
////        audioTrack.attachAuxEffect(equalizer.getId());
//        Visualizer visualizer = new Visualizer(audioTrack.getAudioSessionId());
////        audioTrack.attachAuxEffect(visualizer.getCaptureSize());
//        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
//            @Override
//            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
//                for (int i = 0; i < waveform.length; i++) {
//                    int x = waveform[i];
//                    LogUtil.e("SampleAudioPlayerActivity: 采集的频率" + x);
//                }
//            }
//
//            @Override
//            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//
//            }
//        }, Visualizer.getMaxCaptureRate() / 2, true, false);

//        addDisposable(Observable.just(1)
//                .map(integer -> {
//                    FileOutputStream outputStream = new FileOutputStream(file);
//                    audioTrack.play();
//                    audioDecoder.configure();
//                    while (!audioDecoder.isDecoderEOS()) {
//                        audioDecoder.drainExtractor();
//                        byte[] buffer = audioDecoder.drainDecoder();
//                        if (buffer != null) {
////                            audioTrack.write(byteBuffer, byteBuffer.limit(), AudioTrack.WRITE_BLOCKING);
////                            for (int i = 0; i < buffer.length; i++) {
////                                buffer[i] *= 0.5;
////                            }
//                            audioTrack.write(buffer, 0, buffer.length);
////                            outputStream.write(buffer, 0, buffer.length);
//                        }
//                    }
//                    outputStream.flush();
//                    outputStream.close();
//                    audioTrack.stop();
//                    audioTrack.release();
//                    return integer;
//                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(integer -> {
//                    Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
//                    LogUtil.e("SampleAudioPlayerActivity: ok");
//                }, Throwable::printStackTrace));

    }


    private void recorderPcm(View v) {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "record.pcm");

        if (!isStarted) {
            ((TextView) v).setText("停止");
            isStarted = true;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        int bufferSize = AudioRecord.getMinBufferSize(16000,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                                16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                        audioRecord.startRecording();
                        byte[] buffer = new byte[bufferSize * 2];
                        while (isStarted) {
                            int len = audioRecord.read(buffer, 0, buffer.length);
                            outputStream.write(buffer, 0, len);
                        }
                        outputStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            isStarted = false;
            ((TextView) v).setText("开始录制");
        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
