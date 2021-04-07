package com.example.face_xp.Utils;

import android.content.Context;

public class Speaker {
    public static final Speaker speaker = new Speaker();
    private XunFeiUtils xunFeiUtils;

    public static Speaker getInstance(){
        return speaker;
    }

    public Speaker() {
    }

    public Speaker init(Context context){
        xunFeiUtils = new XunFeiUtils(context);
        xunFeiUtils.init();
        return speaker;
    }

    public void speak(String msg){
        try {
            if(xunFeiUtils!=null){
                xunFeiUtils.onSpeak(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void destroy(){
        try {
            xunFeiUtils.onStop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
