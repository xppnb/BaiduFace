package com.example.face_xp.Activiity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.baidu.idl.main.facesdk.callback.Callback;
import com.baidu.vis.unified.license.AndroidLicenser;
import com.example.face_xp.R;
import com.example.face_xp.Utils.IntentUtils;
import com.example.face_xp.Utils.NavigationUtils;
import com.example.face_xp.Utils.ToastUtils;
import com.example.face_xp.databinding.ActivityLincenseBinding;


public class LicenseActivity extends AppCompatActivity {

    private ActivityLincenseBinding binding;
    public static final String TAG = "LicenseActivity";
    private FaceAuth faceAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_lincense);
        NavigationUtils.setNavigation(LicenseActivity.this);
        //获取指纹
        String deviceId = AndroidLicenser.getDeviceId(LicenseActivity.this);
        binding.fingerText.setText(deviceId);
        Log.i(TAG, "指纹: " + deviceId);
        initView();
    }

    private void initView() {
        faceAuth = new FaceAuth();
    }

    public void onClick(View view) {
        faceAuth.initLicenseOffLine(this, new Callback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(int code, String response) {
                Log.i(TAG, "onResponse: " + code + "" + "response" + response);
                if (code == 0) {
                    //如果正确
                    IntentUtils.intentActivity(LicenseActivity.this, FaceAnalyzeActivity.class);
                    ToastUtils.toast(LicenseActivity.this, "激活成功!");
                } else {
                    switch (code) {
                        case -1:
                            ToastUtils.toast(LicenseActivity.this, "未检测到License.zip文件");
                            binding.codeText.setText("未检测到License.zip文件");
                            break;
                        case 4:
                        case 7:
                            ToastUtils.toast(LicenseActivity.this, "激活失败，设备硬件指纹与License.zip不符");
                            binding.codeText.setText("激活失败，设备硬件指纹与License.zip不符");
                            break;
                        case 11:
                        case 14:
                            ToastUtils.toast(LicenseActivity.this, "激活失败，License.zip文件对应的序列号不在有效期范围内");
                            binding.codeText.setText("激活失败，License.zip文件对应的序列号不在有效期范围内");
                            break;
                        default:
                            binding.codeText.setText(String.valueOf(code));
                            break;
                    }
                }
            }
        });
    }
}
