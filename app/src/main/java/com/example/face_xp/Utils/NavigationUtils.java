package com.example.face_xp.Utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * 导航栏工具
 */
public class NavigationUtils {
    /**
     * 设置导航栏状体
     */
    public static void setNavigation(Activity activity) {
        //设置不需要顶部导航
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //sdk > 19 设置不需要底部导航栏
        View decorView = activity.getWindow().getDecorView();
        int view = View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(view);
    }
}
