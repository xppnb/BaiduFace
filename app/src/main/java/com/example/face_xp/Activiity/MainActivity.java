package com.example.face_xp.Activiity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.baidu.idl.main.facesdk.listener.SdkInitListener;
import com.baidu.idl.main.facesdk.utils.ToastUtils;
import com.example.face_xp.R;
import com.example.face_xp.Utils.IntentUtils;
import com.example.face_xp.Utils.NavigationUtils;
import com.example.face_xp.Utils.Speaker;
import com.example.face_xp.Utils.XunFeiUtils;
import com.example.face_xp.baidu.FaceSDKManager;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private Timer timer;
    private TimerTask timerTask;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acticity_main);


        //设置导航栏状态
        NavigationUtils.setNavigation(MainActivity.this);

        Speaker.getInstance().init(this).speak("系统启动,请稍后");

        //判断机器是否有证书,进入下一个界面
        initLincense();
        //isHasLicense(MainActivity.this, FaceRegisterNewActivity.class);
    }

    private void initLincense() {
        FaceSDKManager.getInstance().init(this, new SdkInitListener() {
            @Override
            public void initStart() {
                Log.i(TAG, "initStart: 开始了");
            }

            @Override
            public void initLicenseSuccess() {
                isHasLicense(MainActivity.this, FaceAnalyzeActivity.class);
//                isHasLicense(MainActivity.this, FaceRGBGateActivity.class);
                ToastUtils.toast(MainActivity.this, "证书验证成功");
                Log.i(TAG, "initLicenseSuccess: 开始验证证书了");
            }

            @Override
            public void initLicenseFail(int errorCode, String msg) {
                isHasLicense(MainActivity.this, LicenseActivity.class);
                ToastUtils.toast(MainActivity.this, "证书验证失败");
                Log.i(TAG, "initLicenseFail: 证书验证失败了");
            }

            @Override
            public void initModelSuccess() {
                ToastUtils.toast(context, "模型加载成功");
                Log.i(TAG, "initModelSuccess: 加载模型中");
            }

            @Override
            public void initModelFail(int errorCode, String msg) {
                Log.i(TAG, "initModelFail: 加载模型失败");
            }
        });
    }

    //判断是否有证书,并进行跳转
    private void isHasLicense(Context context, Class cls) {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                IntentUtils.intentActivity(context, cls);
                finish();
            }
        };
        timer.schedule(timerTask, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        Speaker.getInstance().destroy();
    }
}