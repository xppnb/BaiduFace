package com.example.face_xp.Activiity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.api.GateFaceApi;
import com.baidu.idl.main.facesdk.callback.FaceDetectCallBack;
import com.baidu.idl.main.facesdk.gatecamera.CameraPreviewManager;
import com.baidu.idl.main.facesdk.listener.SdkInitListener;
import com.baidu.idl.main.facesdk.manager.FaceSDKManager;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
import com.baidu.idl.main.facesdk.model.LivenessModel;
import com.baidu.idl.main.facesdk.model.SingleBaseConfig;
import com.baidu.idl.main.facesdk.model.User;
import com.baidu.idl.main.facesdk.callback.CameraDataCallback;
import com.baidu.idl.main.facesdk.utils.BitmapUtils;
import com.baidu.idl.main.facesdk.utils.FaceOnDrawTexturViewUtil;
import com.example.face_xp.R;
import com.example.face_xp.Utils.Constant;
import com.example.face_xp.Utils.FileUtils;
import com.example.face_xp.Utils.IntentUtils;
import com.example.face_xp.Utils.NavigationUtils;
import com.example.face_xp.Utils.ToastUtils;
import com.example.face_xp.databinding.ActivityFaceAnalyzeMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("SetTextI18n")
public class FaceAnalyzeActivity extends AppCompatActivity {

    private ActivityFaceAnalyzeMainBinding binding;
    public static final String TAG = "FaceAnalyze";


    //权限数组
    private List<String> permissionList;

    private User mUser;
    private Paint paint;
    private Paint paintBg;
    private RectF rectF;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_analyze_main);
        NavigationUtils.setNavigation(FaceAnalyzeActivity.this);
        permissionList = new ArrayList<>();
        //判断是否有权限
        havePermission();

    }

    private void initListener() {
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_INIT_SUCCESS) {
            FaceSDKManager.getInstance().initModel(this, new SdkInitListener() {
                @Override
                public void initStart() {

                }

                @Override
                public void initLicenseSuccess() {

                }

                @Override
                public void initLicenseFail(int errorCode, String msg) {

                }

                @Override
                public void initModelSuccess() {
                    FaceSDKManager.initModelSuccess = true;
                    ToastUtils.toast(FaceAnalyzeActivity.this, "模型加载成功");
                }

                @Override
                public void initModelFail(int errorCode, String msg) {
                    FaceSDKManager.initModelSuccess = false;
                    if (errorCode != -12) {
                        com.baidu.idl.main.facesdk.utils.ToastUtils.toast(FaceAnalyzeActivity.this, "模型加载失败，请尝试重启应用");
                    }
                }
            });
        }
        FaceSDKManager.getInstance().initDataBases(this);
        rectF = new RectF();
        paint = new Paint();
        paintBg = new Paint();
        binding.textTureView.setOpaque(false); //设置不透明
        binding.textTureView.setKeepScreenOn(true);  //设置屏幕总时开启
        int i = GateFaceApi.getInstance().getmUserNum();
        Log.i(TAG, "initListener: " + i);
    }

    /*判断权限*/
    private void havePermission() {
        permissionList.clear();  //清空没有通过的权限
        permissionList.addAll(Arrays.asList(Constant.PERMISSION)); //将所有权限放入
        //判断是否已经有权限了
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            if (permissionList.size() > 0) {
                //sdk > 23
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    //弹出请求权限请求
                    ActivityCompat.requestPermissions(this, Constant.PERMISSION, Constant.CODE);
                } else {
                    initListener();
                    enableCamera();
                }
            }
        } else {
            initListener();
            enableCamera();
        }
    }

    private void enableCamera() {
        SingleBaseConfig.getBaseConfig().setVideoDirection(90);
        CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_FACING_FRONT);

        CameraPreviewManager.getInstance().startPreview(this, binding.autoCameraPreviewView, Constant.PREFER_WIDTH, Constant.PREFER_HEIGHT, new CameraDataCallback() {
            @Override
            public void onGetCameraData(byte[] data, Camera camera, int width, int height) {
                // 摄像头预览数据进行人脸检测
                FaceSDKManager.getInstance().onDetectCheck(data, null, null, height, width, 0, new FaceDetectCallBack() {
                    @Override
                    public void onFaceDetectCallback(LivenessModel livenessModel) {
                        //当检测到人脸的时候才会进行的回调
                        Log.i(TAG, "onFaceDetectDarwCallback: 运行到这里了7");
                        //Log.i(TAG, "onFaceDetectCallback: " + livenessModel.getUser());
                        //isInserLimit(livenessModel);
                        checkCloseDebugResult(livenessModel);
                    }

                    @Override
                    public void onTip(int code, String msg) {
                        Log.e(TAG, "onTip: " + msg);
                    }

                    @Override
                    public void onFaceDetectDarwCallback(LivenessModel livenessModel) {
                        Log.i(TAG, "onFaceDetectDarwCallback: 到这里了2");
                        // 绘制人脸框
                        showFrame(livenessModel);
                    }
                });
            }
        });
    }


    /**
     * 绘制人脸框
     */
    private void showFrame(final LivenessModel model) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "showFrame1: " + model);
                SingleBaseConfig.getBaseConfig().setDetectDirection(270);
                Canvas canvas = binding.textTureView.lockCanvas();
                if (canvas == null) {
                    binding.textTureView.unlockCanvasAndPost(canvas);
                    return;
                }
                if (model == null) {
                    Log.i(TAG, "showFrame2: 清空了");
                    // 清空canvas
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    binding.textTureView.unlockCanvasAndPost(canvas);
                    return;
                }
                FaceInfo[] faceInfos = model.getTrackFaceInfo();
                if (faceInfos == null || faceInfos.length == 0) {
                    // 清空canvas
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    binding.textTureView.unlockCanvasAndPost(canvas);
                    return;
                }
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                FaceInfo faceInfo = faceInfos[0];

                rectF.set(FaceOnDrawTexturViewUtil.getFaceRectTwo(faceInfo));

                // 检测图片的坐标和显示的坐标不一样，需要转换。
                FaceOnDrawTexturViewUtil.mapFromOriginalRect(rectF,
                        binding.textTureView, model.getBdFaceImageInstance());
                // 人脸框颜色
                FaceOnDrawTexturViewUtil.drawFaceColor(mUser, paint, paintBg, model);
                // 绘制人脸框
                FaceOnDrawTexturViewUtil.drawCircle(canvas, binding.autoCameraPreviewView,
                        rectF, paint, paintBg, faceInfo);

//                paint.setColor(Color.GREEN);
//                paint.setStyle(Paint.Style.STROKE);
//                canvas.drawRect(rectF, paint);   //方框

                // 清空canvas
                binding.textTureView.unlockCanvasAndPost(canvas);
            }
        });
    }

    //预览模式
    private void checkCloseDebugResult(LivenessModel livenessModel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (livenessModel == null || livenessModel.getFaceInfo() == null) {
                    Log.i(TAG, "run: 我是checkCloseDebugResult");
                    binding.imageView.setImageResource(R.mipmap.ic_image_video);
                    binding.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    cleanBoard();

                } else {
                    BDFaceImageInstance image = livenessModel.getBdFaceImageInstance(); //获取图片帧

                    if (image != null) {
                        binding.imageView.setImageBitmap(BitmapUtils.getInstaceBmp(image)); //播放图片帧
                        binding.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        image.destory();
                    }
                    Log.i(TAG, "checkCloseDebugResult: " + livenessModel.getUser());
                    if (livenessModel.getUser() != null) {
                        Log.i(TAG, "用户是: " + livenessModel.getUser().getUserName() + "我的班级是" + livenessModel.getUser().getUserClass() + "是否住宿:" + livenessModel.getUser().getUserAccommodation());
                        //ToastUtils.toast(FaceAnalyzeActivity.this, livenessModel.getUser().getUserName() + "我的班级是" + livenessModel.getUser().getUserClass() + "是否住宿:" + livenessModel.getUser().getUserAccommodation());

                        showBoard();

                        showText(livenessModel.getUser().getUserName(), livenessModel.getUser().getUserClass(), livenessModel.getUser().getUserCollege(), livenessModel.getUser().getUserAccommodation(), livenessModel.getUser().getUserSchool());

                        //获取本地路径
                        File facePicDir = FileUtils.getBatchImportSuccessDirectory();
                        //获取本地对应图片
                        File userFile = new File(facePicDir, livenessModel.getUser().getUserName() + ".jpg");

                        try {
                            FileInputStream fis = new FileInputStream(userFile);
                            Bitmap bitmapImage = BitmapFactory.decodeStream(fis);
                            binding.userImage.setImageBitmap(bitmapImage);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }


                    } else {
                        errorBoard();
                        Log.e(TAG, "error " + "找不到此用户");
                    }
                }
            }
        });
    }

    private void showText(String userName, String userClass, String userCollege, String userAccommodation, String userSchool) {
        binding.userName.setText(userName);
        binding.userClass.setText(userClass);
        binding.userCollege.setText(userCollege);
        binding.userAccommodation.setText(userAccommodation);
        binding.userSchool.setText(userSchool);
    }

    public void onClick(View view) {
        IntentUtils.intentActivity(this, FaceRegisterActivity.class);
    }

    //将输入框清空
    public void cleanBoard() {
        binding.successLayout.setVisibility(View.GONE);
        binding.userName.setVisibility(View.GONE);
        binding.userClass.setVisibility(View.GONE);
        binding.userAccommodation.setVisibility(View.GONE);
        binding.userSchool.setVisibility(View.GONE);
        binding.activityError.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.GONE);
        binding.userImage.setVisibility(View.GONE);
        binding.userImage.setImageBitmap(null);

        //防止空指针
        showText("", "", "", "", "");

    }

    /*显示成功框*/
    public void showBoard() {
        binding.successLayout.setVisibility(View.VISIBLE);
        binding.userName.setVisibility(View.VISIBLE);
        binding.userClass.setVisibility(View.VISIBLE);
        binding.userCollege.setVisibility(View.VISIBLE);
        binding.userAccommodation.setVisibility(View.VISIBLE);
        binding.userSchool.setVisibility(View.VISIBLE);
        binding.userImage.setVisibility(View.VISIBLE);
        binding.userImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        binding.errorLayout.setVisibility(View.GONE);
        binding.activityError.setVisibility(View.GONE);
    }

    /*错误框*/
    public void errorBoard() {
        //binding.errorLayout.setVisibility(View.VISIBLE);
        cleanBoard();
        binding.activityError.setVisibility(View.VISIBLE);
    }

    /*权限返回值*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false; //如果有程序没有通过
        if (requestCode == Constant.CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;  //没有全部通过
                }
            }
            if (hasPermissionDismiss) {
                // TODO: 2021/1/28  弹窗或者跳转到设置让用户手动输入
                ToastUtils.toast(this, "有权限没有通过");
            } else {
                enableCamera();
            }
        }
    }

    @Override
    protected void onDestroy() {
        CameraPreviewManager.getInstance().stopPreview();
        super.onDestroy();
    }
}
