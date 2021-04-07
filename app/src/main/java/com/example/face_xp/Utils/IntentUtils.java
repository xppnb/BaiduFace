package com.example.face_xp.Utils;

import android.content.Context;
import android.content.Intent;


/**
 * 跳转工具类
 */
public class IntentUtils {

    //没有参数
    public static void intentActivity(Context context, Class<?> cls){
        Intent intent = new Intent(context,cls);
        context.startActivity(intent);
    }

}
