package com.speex.audiovideo.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

import com.speex.audiovideo.exception.AudioConfigurationException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class AudioTrackManager {
    private static final String TAG = "AudioTrackManager";
    public static AudioTrackManager sInstance;
    private AudioTrack mAudioTrack;
    /*音频管理策略*/
//    private static int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private static int STREAM_TYPE = AudioManager.STREAM_ALARM;

    /*音频的采样率，44.1kHz可以所有手机*/
//    private static int SAMPLE_RATE_IN_HZ = 44100;
    private static int SAMPLE_RATE_IN_HZ = 16000;

    /*音频的声道数，此处为单声道*/
//    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_STEREO;//立体声
//    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_MONO;//单声道
//    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_CONFIGURATION_MONO;//单声道
//    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_CONFIGURATION_STEREO;//双声道;
    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_MONO;//单声道输出(左)
//    private static int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_OUT_STEREO;//立体声输出(左和右)

//    AudioFormat.CHANNEL_OUT_STEREO; // 设置输出声道类型为双声道立体声
//    AudioFormat.CHANNEL_OUT_MONO; // 设置输出声道类型为单声道

    /*采样格式，数据位宽是16位*/
    private static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /*音频缓存区大小*/
    private int mBufferSizeInBytes = 0;
    /*是否正在播放*/
    private boolean mIsPlaying = false;
    private Thread mPlayingThread;
    /*播放文件的路径*/
    private String mFilePath;
    /*读取文件IO流*/
    private DataInputStream mDataInputStream;

    /*单例模式*/
    private AudioTrackManager() {
    }

    public static AudioTrackManager getInstance() {
        if (null == sInstance) {
            synchronized (AudioTrackManager.class) {
                if (null == sInstance) {
                    sInstance = new AudioTrackManager();
                    return sInstance;
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化配置
     */
    public void initConfig() throws AudioConfigurationException {

        if (null != mAudioTrack) mAudioTrack.release();

        mBufferSizeInBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT);
        mAudioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT, mBufferSizeInBytes, AudioTrack.MODE_STREAM);

        if (mAudioTrack == null || mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED)
            throw new AudioConfigurationException();

        //wang
//        mAudioTrack = new AudioTrack(mStreamType, FREQUENCY, AudioFormat.CHANNEL_OUT_MONO,
//                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//        mAudioTrack.play();

//        //且播之前会获取焦点：
//        mAudioManager.requestAudioFocus(listener, streamType, gainType);
//        gainType = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;

        //xing
//        int bufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,
//                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//        mAudioTrack.play();


        //如果是音箱的话，可以把第二个参数改成
        //AudioFormat.CHANNEL_OUT_STEREO;

    }

    /**
     * 开始播放录音
     */
    public synchronized void play(String filePath) {
        Log.e(TAG, "播放状态：" + mAudioTrack.getState());
        if (mIsPlaying) return;
        if (null != mAudioTrack && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            mAudioTrack.play();
        }
        this.mFilePath = filePath;
        mIsPlaying = true;
        mPlayingThread = new Thread(new PlayingRunnable(), "PlayingThread");
        mPlayingThread.start();
    }

    /**
     * 停止播放
     */
    private void stop() {
        try {
            if (mAudioTrack != null) {

                mIsPlaying = false;

                //首先停止播放
                mAudioTrack.stop();

                //关闭线程
                try {
                    if (mPlayingThread != null) {
                        mPlayingThread.join();
                        mPlayingThread = null;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //释放资源
                releaseAudioTrack();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     */
    private void releaseAudioTrack() {
        if (mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }


    class PlayingRunnable implements Runnable {
        @Override
        public void run() {
            try {
                FileInputStream fileInputStream = new FileInputStream(new File(mFilePath));
                mDataInputStream = new DataInputStream(fileInputStream);
                byte[] audioDataArray = new byte[mBufferSizeInBytes];
                int readLength = 0;
                while (mDataInputStream.available() > 0) {
                    readLength = mDataInputStream.read(audioDataArray);
                    if (readLength > 0) {
                        mAudioTrack.write(audioDataArray, 0, readLength);
                    }
                }
                stop();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mIsPlaying = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
