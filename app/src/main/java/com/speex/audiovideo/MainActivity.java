package com.speex.audiovideo;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.speex.audiovideo.audio.AudioRecordManager;
import com.speex.audiovideo.audio.AudioTrackManager;
import com.speex.audiovideo.audio.PcmToWavUtil;
import com.speex.audiovideo.exception.AudioConfigurationException;
import com.speex.audiovideo.exception.AudioStartRecordingException;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private static final int DISPLAY_RECORDING_TIME = 1;//显示录音时长的标记
    private TextView mTvTime;
    private TextView mTvFilePath;
    private Button mBtnRecord;
    private Button mBtnPlay;
    private Timer mTimer;//计时器
    //    private String mFilePath = Environment.getExternalStorageDirectory() + "/audio.pcm";
//    private String mFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/audio.pcm";
    private String mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/audio.pcm";
//    private String mFilePath = getExternalCacheDir().getAbsolutePath() + "/audio.pcm";
//    private String mFilePath = getCacheDir().getAbsolutePath() + "/audio.pcm";


    private int mTimerTime = 0;//计时的时间，单位是秒
    private TimerTask mTimerTask;
    private boolean mIsRecording = false;//是否正在录音
    private AudioRecordManager mAudioRecordManager;
    private AudioTrackManager mAudioTrackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAudioRecordManager = AudioRecordManager.getInstance();
        mAudioTrackManager = AudioTrackManager.getInstance();

        initView();

        initListener();
    }

    private void initView() {
        mTvTime = findViewById(R.id.tv_time);
        mTvFilePath = findViewById(R.id.tv_file_path);
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnPlay = findViewById(R.id.btn_play);

        mTvFilePath.setText("文件存储路径：" + mFilePath);
    }

    private void initListener() {
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    recordAndStop();
                } catch (AudioStartRecordingException e) {
                    e.printStackTrace();
                } catch (AudioConfigurationException e) {
                    e.printStackTrace();
                }
            }
        });
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    playRecordFile();
                } catch (AudioConfigurationException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 开始和结束录音
     */
    private void recordAndStop() throws AudioStartRecordingException, AudioConfigurationException {
        Log.e(TAG, "在录音吗:" + mAudioRecordManager.isRecording());
        if (mIsRecording) {//正在录音--》停止录音
            mBtnRecord.setText("开始录音");
            mAudioRecordManager.stopRecord();
            mTimerTask.cancel();
            mTimer.cancel();
            mIsRecording = false;
        } else {//空闲--》开始录音
            mBtnRecord.setText("停止录音");
            mAudioRecordManager.initConfig();
            mAudioRecordManager.startRecord(mFilePath);
            mTimerTime = 0;//初始化计时时间
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    sHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME);
                    mTimerTime++;
                }
            };
            mTimer = new Timer();
            mTimer.schedule(mTimerTask, 0, 1000);
            mIsRecording = true;
        }
    }


    /**
     * 播放录音文件
     */
    private void playRecordFile() throws AudioConfigurationException {
        mAudioTrackManager.initConfig();
        mAudioTrackManager.play(mFilePath);
    }

    private Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DISPLAY_RECORDING_TIME) {
                int minutes = mTimerTime / 60;
                int seconds = mTimerTime % 60;
                String timeRecoding = String.format("%02d:%02d", minutes, seconds);
                mTvTime.setText(timeRecoding);
            }
        }
    };


    /**
     * PCM转换为WAV
     *
     * @param view
     */
    public void pcmConvertWav(View view) {
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(AudioRecordManager.SAMPLE_RATE_IN_HZ, AudioRecordManager.CHANNEL_CONFIGURATION, AudioRecordManager.AUDIO_FORMAT);
        File pcmFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "audio.pcm");
        File wavFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "audio.wav");
        if (!wavFile.mkdirs()) {
            Log.e(TAG, "wavFile pcmConvertWav");
        }
        if (wavFile.exists()) {
            wavFile.delete();
        }
        pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
    }


    /**
     * 获取音量
     *
     * @param view
     */
    public void getVolume(View view) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //通话音量
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        int current = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        Log.d(TAG, "通话音量 max : " + max + " ,current: " + current);

        //系统音量
        max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        current = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        Log.d(TAG, "系统音量 max : " + max + " current: " + current);

        //铃声音量
        max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        current = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        Log.d(TAG, "铃声音量 max : " + max + " ,current: " + current);

        //音乐音量
        max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "音乐音量 max : " + max + " ,current: " + current);

        //提示声音音量
        max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        current = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        Log.d(TAG, "提示声音音量 max : " + max + " ,current: " + current);
    }
}
