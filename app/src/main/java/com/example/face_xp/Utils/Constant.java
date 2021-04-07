package com.example.face_xp.Utils;

import android.Manifest;

public class Constant {
    //权限数组
    public static final String[] PERMISSION = new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    //摄像头权限返回状态码
    public static final int CODE = 200;
    //宽度
    public static final int PREFER_WIDTH = 640;
    //高度
    public static final int PREFER_HEIGHT = 480;
    //appId
    public static final String APPID = "6020f626";
}
