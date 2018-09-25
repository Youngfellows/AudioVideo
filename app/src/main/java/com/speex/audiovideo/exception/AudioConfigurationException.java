package com.speex.audiovideo.exception;


public class AudioConfigurationException extends Exception {
    public AudioConfigurationException() {
        super("录音器初始化配置失败");
    }
}
