package com.example.face_xp.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.style.TtsSpan;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

/**
 * 讯飞语音工具类
 */
public class XunFeiUtils {
    //引擎类型
    private String mEngineType = SpeechConstant.TYPE_LOCAL;

    //设置云端发音人
    private static String voicerCloud = "xiaoyan";

    // 默认本地发音人
    public static String voicerLocal = "xiaoyan";

    public static String voicerXtts = "xiaoyan";

    private SharedPreferences sharedPreferences;
    private SpeechSynthesizer mTts;

    private Context mContent;
    private Activity activity;
    public static final String TAG = "XunFeiUtils";

    public XunFeiUtils(Context mContent) {
        this.mContent = mContent;
    }

    public void init() {
        sharedPreferences = mContent.getSharedPreferences("fileName", Activity.MODE_PRIVATE);
        SpeechUtility.createUtility(mContent, SpeechConstant.APPID + "=" + Constant.APPID);


        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(mContent, new InitListener() {
            @Override
            public void onInit(int code) {
                if (code != ErrorCode.SUCCESS) {
                    Log.i(TAG, "onInit: " + code);
                } else {
                    // TODO: 2021/2/9
                    //startSpeeking
                }
            }
        });

        setParameter();

    }

    /**
     * 设置参数
     */
    private void setParameter() {
        //清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);

        //设置合成
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);

        } else if (mEngineType.equals(SpeechConstant.TYPE_LOCAL)) {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerXtts);
        }

        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, sharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, sharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, sharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, sharedPreferences.getString("stream_preference", "3"));
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");

        // 设置合成音频保存路径，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");


//        mTts.startSpeaking("Hello World", new SynthesizerListener() {
//            @Override
//            public void onSpeakBegin() {
//                //开始
//            }
//
//            @Override
//            public void onBufferProgress(int i, int i1, int i2, String s) {
//
//            }
//
//            @Override
//            public void onSpeakPaused() {
//
//            }
//
//            @Override
//            public void onSpeakResumed() {
//
//            }
//
//            @Override
//            public void onSpeakProgress(int i, int i1, int i2) {
//                //进度
//            }
//
//            @Override
//            public void onCompleted(SpeechError speechError) {
//                //完成回调
//            }
//
//            @Override
//            public void onEvent(int i, int i1, int i2, Bundle bundle) {
//
//            }
//        });
    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "tts";
        if (mEngineType.equals(SpeechConstant.TYPE_XTTS)) {
            type = "xtts";
        }
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(mContent, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        if (mEngineType.equals(SpeechConstant.TYPE_XTTS)) {
            tempBuffer.append(ResourceUtil.generateResourcePath(mContent, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + XunFeiUtils.voicerXtts + ".jet"));
        } else {
            tempBuffer.append(ResourceUtil.generateResourcePath(mContent, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + XunFeiUtils.voicerLocal + ".jet"));
        }

        return tempBuffer.toString();
    }

    public void onSpeak(String msg) {
        if (!mTts.isSpeaking()) {
            mTts.startSpeaking(msg, null);
        }
    }

    //结束
    public void onStop() {
        if (mTts.isSpeaking()) {
            mTts.stopSpeaking();
            //注销连接
            mTts.destroy();
        }
    }
}
