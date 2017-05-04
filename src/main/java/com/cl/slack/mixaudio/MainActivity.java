package com.cl.slack.mixaudio;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.cl.slack.mixaudio.play.PlayBackMusic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    public AudioMixerNative _AudioMix;
    private String LOGMODULE = "slack";
    private Button _RecordStartButton;
    private Button _RecordStopButton;
    private Button _RecordCutButton;
    private AudioRecord _AudioRecorder = null;
    private final int _iSampleRateDef = 32000;
    private final int _iBitRate = 16000;
    private int _iRecorderBufferSize;
    private byte[] _RecorderBuffer;
    private boolean _mbStop;
    private HandlerThread _ProcessingThread;
    private Handler _AudioHandler;
    private int _FramePeriod;
    private FileOutputStream aacDataOutStream = null;
    private final String SAVE_FILENAME = "record_file.aac";
    private final String NEW_SAVE_FILENAME = "record_cut_file.aac";

    private Button playBackGround;
    private PlayBackMusic playBackMusic;
    private int musicSampleRate;
    private int musicChannelNumber;


    private int AudioMixInit() {
        int iRet = 0;
        _AudioMix = new AudioMixerNative();
        iRet = _AudioMix.PcmMixEncoderInit();
        _AudioMix.MicGain(1.0f);
        _AudioMix.MusicGain(1.0f);
        Log.i(LOGMODULE, "PcmMixEncoderInit return " + iRet + "....");

        return iRet;
    }

    private void SetButtonEvent() {
        _RecordStartButton = (Button) findViewById(R.id.RecordStartButton);
        _RecordStopButton = (Button) findViewById(R.id.RecordStopButton);
        _RecordCutButton = (Button) findViewById(R.id.RecordCutButton);
        _mbStop = true;

        _RecordStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!_mbStop) {
                    return;
                }
                initFileOutputStream();
                _mbStop = false;
                _AudioRecorder.startRecording();
                playBackMusic.setNeedRecodeDataEnable(true);
            }
        });

        _RecordStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (_mbStop) {
                    return;
                }
                playBackMusic.setNeedRecodeDataEnable(false);
                _mbStop = true;
                _AudioRecorder.stop();
                closeFileOutputStream();
            }
        });

        _RecordCutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!_mbStop) {
                    return;
                }
                String strPath = Environment.getExternalStorageDirectory().getPath();
                String filename = strPath + "/" + SAVE_FILENAME;
                String outputFilename = strPath + "/" + NEW_SAVE_FILENAME;

                File file = new File(outputFilename);
                if (file.exists()) {
                    file.delete();
                }
                _AudioMix.audioFileCut(filename, outputFilename, 5, 10, 64000);
            }
        });

        playBackGround = (Button) findViewById(R.id.playBackgroundMusic);
        playBackGround.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playBackGround.getTag() == null) {
                    playBackGround.setTag(this);
                    playBackGround.setText("停止播放");
//                    initFileOutputStream();
                    playBackMusic.startPlayBackMusic(backgroundListener);
                    musicSampleRate = playBackMusic.getSampleRate();
                    musicChannelNumber = playBackMusic.getChannelNumber();
                } else {
                    playBackGround.setTag(null);
                    playBackGround.setText("播放背景音乐");
                    playBackMusic.stop();
//                    closeFileOutputStream();
                }
            }
        });

    }

    private void initFileOutputStream(){
        String strPath = Environment.getExternalStorageDirectory().getPath();
        String filename = strPath + "/" + SAVE_FILENAME;
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
        try {
            aacDataOutStream = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void closeFileOutputStream(){
        _AudioMix.clearQueue();
        try {
            aacDataOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PlayBackMusic.BackGroundFrameListener backgroundListener =
            new PlayBackMusic.BackGroundFrameListener() {
                @Override
                public void onFrameArrive(byte[] bytes) {
                    if (_mbStop) {
                        return;
                    }
                    _AudioMix.addMusicPcmQueue(musicSampleRate, musicChannelNumber, bytes);

                    /**
                     * test _AudioMix.MusicPcmEncode
                     * 设置正确的 SampleRate 和 ChannelNumber 音频速率可以保证正确 */
//                    byte[] result = _AudioMix.MusicPcmEncode(musicSampleRate, musicChannelNumber, bytes, bytes.length);
//                    if (result != null) {
//                        Log.i(LOGMODULE, "write to file...");
//                        try {
//                            aacDataOutStream.write(result);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _iRecorderBufferSize = AudioRecord.getMinBufferSize(_iSampleRateDef,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        _AudioRecorder = new AudioRecord(AudioSource.MIC,
                _iSampleRateDef, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, _iRecorderBufferSize);
        _RecorderBuffer = new byte[_iRecorderBufferSize];

        _ProcessingThread = new HandlerThread("AudioProcessing");
        _ProcessingThread.start();
        _AudioHandler = new Handler(_ProcessingThread.getLooper());

        int iChannelNum = 2;
        _FramePeriod = _iRecorderBufferSize / (_iBitRate * iChannelNum / 8);
        _AudioRecorder.setRecordPositionUpdateListener(updateListener, _AudioHandler);
        _AudioRecorder.setPositionNotificationPeriod(_FramePeriod);

        this.SetButtonEvent();
        this.AudioMixInit();
        playBackMusic = new PlayBackMusic(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp3");

    }

    /**
     * TODO : 写入时，边播放边录制,最后一点播放的音频回丢失
     */
    private AudioRecord.OnRecordPositionUpdateListener updateListener =
            new AudioRecord.OnRecordPositionUpdateListener() {
                public void onPeriodicNotification(AudioRecord recorder) {
                    if (_mbStop) {
                        return;
                    }
                    int iPCMLen = _AudioRecorder.read(_RecorderBuffer, 0, _RecorderBuffer.length); // Fill buffer
                    if (iPCMLen != _AudioRecorder.ERROR_BAD_VALUE) {
                        byte[] result ;
                        if(playBackMusic.isPlayingMusic()){
                            result = _AudioMix.MicPcmMixEncode(_iSampleRateDef, 2,
                                    _RecorderBuffer, iPCMLen);
                        }else {
                            /** only mic data code is ok */
                            result = _AudioMix.MicPcmEncode(_iSampleRateDef, 2,
                                    _RecorderBuffer, iPCMLen);
                        }
                        if (result != null) {
                            Log.i(LOGMODULE, "write to file..." + playBackMusic.isPlayingMusic());
                            try {
                                aacDataOutStream.write(result);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                }

                @Override
                public void onMarkerReached(AudioRecord arg0) {
                    // TODO Auto-generated method stub

                }
            };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _AudioMix.PcmMixEncoderDeInit();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}