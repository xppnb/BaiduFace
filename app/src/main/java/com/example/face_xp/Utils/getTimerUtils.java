package com.example.face_xp.Utils;

import android.text.format.DateFormat;

/**
 * 获取时间
 */
public class getTimerUtils {
    //获取当前系统时间
    public static CharSequence getCurrentTime(){
        return DateFormat.format("yyyy年MM月dd日 HH:mm:ss", System.currentTimeMillis());
    }
}
