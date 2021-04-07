package com.example.face_xp.Activiity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.registerlibrary.R;
import com.baidu.idl.main.facesdk.registerlibrary.user.api.FaceApi;
import com.baidu.idl.main.facesdk.registerlibrary.user.callback.CameraDataCallback;
import com.baidu.idl.main.facesdk.registerlibrary.user.callback.FaceDetectCallBack;
import com.baidu.idl.main.facesdk.registerlibrary.user.callback.FaceFeatureCallBack;
import com.baidu.idl.main.facesdk.registerlibrary.user.camera.AutoTexturePreviewView;
import com.baidu.idl.main.facesdk.registerlibrary.user.camera.CameraPreviewManager;
import com.baidu.idl.main.facesdk.registerlibrary.user.listener.SdkInitListener;
import com.baidu.idl.main.facesdk.registerlibrary.user.manager.FaceSDKManager;
import com.baidu.idl.main.facesdk.registerlibrary.user.manager.FaceTrackManager;
import com.baidu.idl.main.facesdk.registerlibrary.user.model.LivenessModel;
import com.baidu.idl.main.facesdk.registerlibrary.user.model.SingleBaseConfig;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.BitmapUtils;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.DensityUtils;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.FaceOnDrawTexturViewUtil;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.FileUtils;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.KeyboardsUtils;
import com.baidu.idl.main.facesdk.registerlibrary.user.utils.ToastUtils;
import com.example.face_xp.Utils.IntentUtils;
import com.example.face_xp.databinding.RegisterActivityBinding;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;


public class FaceRegisterActivity extends AppCompatActivity {
    private static final String TAG = FaceRegisterActivity.class.getSimpleName();

    // RGB摄像头图像宽和高
    private static final int PREFER_WIDTH = SingleBaseConfig.getBaseConfig().getRgbAndNirWidth();
    private static final int PERFER_HEIGHT = SingleBaseConfig.getBaseConfig().getRgbAndNirHeight();


    // 包含适配屏幕后的人脸的x坐标，y坐标，和width
    private float[] mPointXY = new float[4];
    private byte[] mFeatures = new byte[512];
    private Bitmap mCropBitmap;
    private boolean mCollectSuccess = false;

    //-----------------------
    private RegisterActivityBinding dataBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initListener();
        dataBinding = DataBindingUtil.setContentView(this, com.example.face_xp.R.layout.register_activity);
        initView();
    }

    private void initListener() {

        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_MODEL_LOAD_SUCCESS) {
            Log.i(TAG, "initListener: 开始验证证书");
            FaceSDKManager.getInstance().initModel(this, new SdkInitListener() {
                @Override
                public void initStart() {
                    Log.i(TAG, "initListener: 开始验证证书");
                }

                @Override
                public void initLicenseSuccess() {
                }

                @Override
                public void initLicenseFail(int errorCode, String msg) {
                }

                @Override
                public void initModelSuccess() {
                    Log.i(TAG, "initModelSuccess: 证书验证成功");
                    ToastUtils.toast(FaceRegisterActivity.this, "模型加载成功，欢迎使用");
                }

                @Override
                public void initModelFail(int errorCode, String msg) {
                    Log.i(TAG, "initModelFail: 模型加载失败");
                    if (errorCode != -12) {
                        ToastUtils.toast(FaceRegisterActivity.this, "模型加载失败，请尝试重启应用");
                    }
                }
            });
        }
    }

    private void initView() {
        dataBinding.autoCameraPreviewView.setIsRegister(true);

        dataBinding.circleHead.setBorderWidth(DensityUtils.dip2px(FaceRegisterActivity.this, 3));
        dataBinding.circleHead.setBorderColor(Color.parseColor("#0D9EFF"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 摄像头图像预览
        startCameraPreview();
        Log.e(TAG, "start camera");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭摄像头
        CameraPreviewManager.getInstance().stopPreview();
        if (mCropBitmap != null) {
            if (!mCropBitmap.isRecycled()) {
                mCropBitmap.recycle();
            }
            mCropBitmap = null;
        }
    }

    /**
     * 摄像头图像预览
     */
    private void startCameraPreview() {
        SingleBaseConfig.getBaseConfig().setVideoDirection(90);
        // 设置前置摄像头
        CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_FACING_FRONT);
        // 设置后置摄像头
        // CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_FACING_BACK);
        // 设置USB摄像头
        // CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_USB);

        CameraPreviewManager.getInstance().startPreview(this, dataBinding.autoCameraPreviewView,
                PREFER_WIDTH, PERFER_HEIGHT, new CameraDataCallback() {
                    @Override
                    public void onGetCameraData(byte[] data, Camera camera, int width, int height) {
                        if (mCollectSuccess) {
                            return;
                        }
                        // 摄像头数据处理
                        Log.i(TAG, "onFaceDetectCallback: 我到这里了1");
                        faceDetect(data, width, height);
                    }
                });
    }

    /**
     * 摄像头数据处理
     */
    private void faceDetect(byte[] data, final int width, final int height) {
        Log.i(TAG, "onFaceDetectCallback: 我到这里了2");
        if (mCollectSuccess) {
            return;
        }

        SingleBaseConfig.getBaseConfig().setDetectDirection(270);

//        int liveType = SingleBaseConfig.getBaseConfig().getType();
//        // int liveType = 2;
//
//        if (liveType == 0) { // 无活体检测
//            FaceTrackManager.getInstance().setAliving(false);
//        } else if (liveType == 1) { // 活体检测
//            FaceTrackManager.getInstance().setAliving(true);
//        }


        // 摄像头预览数据进行人脸检测
        FaceTrackManager.getInstance().faceTrack(data, width, height, new FaceDetectCallBack() {
            @Override
            public void onFaceDetectCallback(LivenessModel livenessModel) {
                Log.i(TAG, "onFaceDetectCallback: 我到这里了3");

                checkFaceBound(livenessModel);
            }

            @Override
            public void onTip(int code, final String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dataBinding.roundView == null) {
                            return;
                        }
                        dataBinding.roundView.setTipText("保持面部在取景框内");
                        dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    }
                });
            }

            @Override
            public void onFaceDetectDarwCallback(LivenessModel livenessModel) {

            }
        });
    }

    /**
     * 检查人脸边界
     *
     * @param livenessModel LivenessModel实体
     */
    private void checkFaceBound(final LivenessModel livenessModel) {
        Log.i(TAG, "onFaceDetectCallback: 我到这里了4");
        // 当未检测到人脸UI显示
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCollectSuccess) {
                    return;
                }

                Log.i(TAG, "livenessModel: " + livenessModel);

                if (livenessModel == null || livenessModel.getFaceInfo() == null) {
                    dataBinding.roundView.setTipText("保持面部在取景框内");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    return;
                }

                Log.i(TAG, "run: 到这里了111111111");

                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_grey);

                mPointXY[0] = livenessModel.getFaceInfo().centerX;   // 人脸X坐标
                mPointXY[1] = livenessModel.getFaceInfo().centerY;   // 人脸Y坐标
                mPointXY[2] = livenessModel.getFaceInfo().width;     // 人脸宽度
                mPointXY[3] = livenessModel.getFaceInfo().height;    // 人脸高度

                FaceOnDrawTexturViewUtil.converttPointXY(mPointXY, dataBinding.autoCameraPreviewView,
                        livenessModel.getBdFaceImageInstance(), livenessModel.getFaceInfo().width);

                float leftLimitX = AutoTexturePreviewView.circleX - AutoTexturePreviewView.circleRadius;
                float rightLimitX = AutoTexturePreviewView.circleX + AutoTexturePreviewView.circleRadius;
                float topLimitY = AutoTexturePreviewView.circleY - AutoTexturePreviewView.circleRadius;
                float bottomLimitY = AutoTexturePreviewView.circleY + AutoTexturePreviewView.circleRadius;
                float previewWidth = AutoTexturePreviewView.circleRadius * 2;


//                Log.i(TAG, "checkFaceBound: " + leftLimitX + " " + rightLimitX + " " + topLimitY + " " + bottomLimitY);

                Log.e(TAG, "faceX = " + mPointXY[0] + ", faceY = " + mPointXY[1]
                        + ", faceW = " + mPointXY[2] + ", prw = " + previewWidth + "faceH =" + mPointXY[3]);
                Log.e(TAG, "leftLimitX = " + leftLimitX + ", rightLimitX = " + rightLimitX
                        + ", topLimitY = " + topLimitY + ", bottomLimitY = " + bottomLimitY);
                Log.e(TAG, "cX = " + AutoTexturePreviewView.circleX + ", cY = " + AutoTexturePreviewView.circleY
                        + ", cR = " + AutoTexturePreviewView.circleRadius);

                if (mPointXY[2] < 50 || mPointXY[3] < 50) {
                    dataBinding.roundView.setTipText("请向前靠近镜头");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // 释放内存
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if (mPointXY[2] > previewWidth || mPointXY[3] > previewWidth) {
                    dataBinding.roundView.setTipText("请向后远离镜头");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // 释放内存
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if (mPointXY[0] - mPointXY[2] / 2 < leftLimitX
                        || mPointXY[0] + mPointXY[2] / 2 > rightLimitX
                        || mPointXY[1] - mPointXY[3] / 2 < topLimitY
                        || mPointXY[1] + mPointXY[3] / 2 > bottomLimitY) {
                    dataBinding.roundView.setTipText("保持面部在取景框内");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // 释放内存
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if ((Math.abs(AutoTexturePreviewView.circleX - mPointXY[0]) < mPointXY[2] / 2)
                        && (Math.abs(AutoTexturePreviewView.circleY - mPointXY[1]) < mPointXY[2] / 2)
                        && (mPointXY[2] <= previewWidth && mPointXY[3] <= previewWidth)) {

                }
                dataBinding.roundView.setTipText("请保持面部在取景框内");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_blue);
                // 检验活体分值
                checkLiveScore(livenessModel);
            }
        });
    }

    /**
     * 检验活体分值
     *
     * @param livenessModel LivenessModel实体
     */
    private void checkLiveScore(LivenessModel livenessModel) {
        Log.i(TAG, "checkLiveScore: 我到这里了5");
        if (livenessModel == null || livenessModel.getFaceInfo() == null) {
            dataBinding.roundView.setTipText("保持面部在取景框内");
            return;
        }

        // 获取活体类型
        SingleBaseConfig.getBaseConfig().setType(0);
        int liveType = SingleBaseConfig.getBaseConfig().getType();
        // int liveType = 2;

        if (liveType == 0) {         // 无活体
            getFeatures(livenessModel);
        } else if (liveType == 1) { // RGB活体检测
            float rgbLivenessScore = livenessModel.getRgbLivenessScore();
            float liveThreadHold = SingleBaseConfig.getBaseConfig().getRgbLiveScore();
            // Log.e(TAG, "score = " + rgbLivenessScore);
            if (rgbLivenessScore < liveThreadHold) {
                dataBinding.roundView.setTipText("活体检测未通过");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                // 释放内存
                destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                return;
            }
            // 提取特征值
            getFeatures(livenessModel);
        }
    }

    /**
     * 提取特征值
     *
     * @param model 人脸数据
     */
    private void getFeatures(final LivenessModel model) {
        Log.i(TAG, "getFeatures: 我到这里了6");
        if (model == null) {
            return;
        }

        // 获取选择的特征抽取模型
        int modelType = SingleBaseConfig.getBaseConfig().getActiveModel(); //默认是生活照
        if (modelType == 1) {
            // 生活照
            FaceSDKManager.getInstance().onFeatureCheck(model.getBdFaceImageInstance(), model.getLandmarks(),
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, new FaceFeatureCallBack() {
                        @Override
                        public void onFaceFeatureCallBack(final float featureSize, final byte[] feature, long time) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCollectSuccess) {
                                        return;
                                    }
                                    displayCompareResult(featureSize, feature, model);
                                    Log.e(TAG, String.valueOf(feature.length));
                                }
                            });

                        }
                    });

        } else if (modelType == 2) {
            // 证件照
            FaceSDKManager.getInstance().onFeatureCheck(model.getBdFaceImageInstance(), model.getLandmarks(),
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_ID_PHOTO, new FaceFeatureCallBack() {
                        @Override
                        public void onFaceFeatureCallBack(final float featureSize, final byte[] feature, long time) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    displayCompareResult(featureSize, feature, model);
                                }
                            });
                        }
                    });
        }
    }

    // 根据特征抽取的结果 注册人脸
    private void displayCompareResult(float ret, byte[] faceFeature, LivenessModel model) {

        Log.i(TAG, "displayCompareResult: 我到这里了7");
        if (model == null) {
            dataBinding.roundView.setTipText("保持面部在取景框内");
            dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
            return;
        }

        // 特征提取成功
        if (ret == 128) {
            // 抠图
            BDFaceImageInstance imageInstance = model.getBdFaceImageInstanceCrop(); //图像实例裁剪
            AtomicInteger isOutoBoundary = new AtomicInteger();
            BDFaceImageInstance cropInstance = FaceSDKManager.getInstance().getFaceCrop()
                    .cropFaceByLandmark(imageInstance, model.getLandmarks(),
                            2.0f, false, isOutoBoundary);
            if (cropInstance == null) {
                dataBinding.roundView.setTipText("抠图失败");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                // 释放内存
                destroyImageInstance(model.getBdFaceImageInstanceCrop());
                return;
            }
            mCropBitmap = BitmapUtils.getInstaceBmp(cropInstance);
            // 获取头像
            if (mCropBitmap != null) {
                mCollectSuccess = true;
                dataBinding.circleHead.setImageBitmap(mCropBitmap);
            }
            cropInstance.destory();
            // 释放内存
            destroyImageInstance(model.getBdFaceImageInstanceCrop());

            for (int i = 0; i < faceFeature.length; i++) {
                mFeatures[i] = faceFeature[i];
            }

            showControls();

            Log.i(TAG, "displayCompareResult: " + model.getUser());

        } else {
            dataBinding.roundView.setTipText("特征提取失败");
            dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
        }
    }

    /*显示控件*/
    public void showControls() {
        dataBinding.roundView.setTipText("");
        dataBinding.roundView.setVisibility(View.INVISIBLE);
        dataBinding.autoCameraPreviewView.setVisibility(View.GONE);
        dataBinding.circleHead.setVisibility(View.VISIBLE);
        dataBinding.okBtn.setVisibility(View.VISIBLE);
        dataBinding.editTextName.setVisibility(View.VISIBLE);
        dataBinding.editTextAccommodation.setVisibility(View.VISIBLE);
        dataBinding.editTextClass.setVisibility(View.VISIBLE);
        dataBinding.editCollege.setVisibility(View.VISIBLE);
        dataBinding.editSchool.setVisibility(View.VISIBLE);
    }

    /**
     * 释放图像
     *
     * @param imageInstance
     */
    private void destroyImageInstance(BDFaceImageInstance imageInstance) {
        if (imageInstance != null) {
            imageInstance.destory();
        }
    }

    public void onClick(View view) {
        String userName = dataBinding.editTextName.getEditText().getText().toString();
        String mClass = dataBinding.editTextClass.getEditText().getText().toString();
        String isAccommodation = dataBinding.editTextAccommodation.getEditText().getText().toString();
        String college = dataBinding.editCollege.getEditText().getText().toString();
        String school = dataBinding.editSchool.getEditText().getText().toString();
        ToastUtils.toast(FaceRegisterActivity.this, userName);
        String nameResult = FaceApi.getInstance().isValidName(userName);
        if (!"0".equals(nameResult)) {
            ToastUtils.toast(getApplicationContext(), nameResult);
            return;
        }
        String imageName = userName + ".jpg";
        // 注册到人脸库
        boolean isSuccess = FaceApi.getInstance().registerUserIntoDBmanager(null,
                userName, imageName, null, mClass, isAccommodation, college, school, mFeatures);
        if (isSuccess) {
            // 保存人脸图片
            File faceDir = FileUtils.getBatchImportSuccessDirectory();
            File file = new File(faceDir, imageName);
            FileUtils.saveBitmap(file, mCropBitmap);
            // 数据变化，更新内存
            FaceApi.getInstance().initDatabases(true);
            Log.i(TAG, "File: 注册成功");
//             关闭摄像头
            CameraPreviewManager.getInstance().stopPreview();
            IntentUtils.intentActivity(FaceRegisterActivity.this, FaceAnalyzeActivity.class);
            finish();
        } else {
            ToastUtils.toast(getApplicationContext(), "保存数据库失败，" +
                    "可能是用户名格式不正确");
        }
    }

    /**
     * 点击非编辑区域收起键盘
     * 获取点击事件
     */
    @CallSuper
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (KeyboardsUtils.isShouldHideKeyBord(view, ev)) {
                KeyboardsUtils.hintKeyBoards(view);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

}
