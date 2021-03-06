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

    // RGB????????????????????????
    private static final int PREFER_WIDTH = SingleBaseConfig.getBaseConfig().getRgbAndNirWidth();
    private static final int PERFER_HEIGHT = SingleBaseConfig.getBaseConfig().getRgbAndNirHeight();


    // ?????????????????????????????????x?????????y????????????width
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
            Log.i(TAG, "initListener: ??????????????????");
            FaceSDKManager.getInstance().initModel(this, new SdkInitListener() {
                @Override
                public void initStart() {
                    Log.i(TAG, "initListener: ??????????????????");
                }

                @Override
                public void initLicenseSuccess() {
                }

                @Override
                public void initLicenseFail(int errorCode, String msg) {
                }

                @Override
                public void initModelSuccess() {
                    Log.i(TAG, "initModelSuccess: ??????????????????");
                    ToastUtils.toast(FaceRegisterActivity.this, "?????????????????????????????????");
                }

                @Override
                public void initModelFail(int errorCode, String msg) {
                    Log.i(TAG, "initModelFail: ??????????????????");
                    if (errorCode != -12) {
                        ToastUtils.toast(FaceRegisterActivity.this, "??????????????????????????????????????????");
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
        // ?????????????????????
        startCameraPreview();
        Log.e(TAG, "start camera");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ???????????????
        CameraPreviewManager.getInstance().stopPreview();
        if (mCropBitmap != null) {
            if (!mCropBitmap.isRecycled()) {
                mCropBitmap.recycle();
            }
            mCropBitmap = null;
        }
    }

    /**
     * ?????????????????????
     */
    private void startCameraPreview() {
        SingleBaseConfig.getBaseConfig().setVideoDirection(90);
        // ?????????????????????
        CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_FACING_FRONT);
        // ?????????????????????
        // CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_FACING_BACK);
        // ??????USB?????????
        // CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_USB);

        CameraPreviewManager.getInstance().startPreview(this, dataBinding.autoCameraPreviewView,
                PREFER_WIDTH, PERFER_HEIGHT, new CameraDataCallback() {
                    @Override
                    public void onGetCameraData(byte[] data, Camera camera, int width, int height) {
                        if (mCollectSuccess) {
                            return;
                        }
                        // ?????????????????????
                        Log.i(TAG, "onFaceDetectCallback: ???????????????1");
                        faceDetect(data, width, height);
                    }
                });
    }

    /**
     * ?????????????????????
     */
    private void faceDetect(byte[] data, final int width, final int height) {
        Log.i(TAG, "onFaceDetectCallback: ???????????????2");
        if (mCollectSuccess) {
            return;
        }

        SingleBaseConfig.getBaseConfig().setDetectDirection(270);

//        int liveType = SingleBaseConfig.getBaseConfig().getType();
//        // int liveType = 2;
//
//        if (liveType == 0) { // ???????????????
//            FaceTrackManager.getInstance().setAliving(false);
//        } else if (liveType == 1) { // ????????????
//            FaceTrackManager.getInstance().setAliving(true);
//        }


        // ???????????????????????????????????????
        FaceTrackManager.getInstance().faceTrack(data, width, height, new FaceDetectCallBack() {
            @Override
            public void onFaceDetectCallback(LivenessModel livenessModel) {
                Log.i(TAG, "onFaceDetectCallback: ???????????????3");

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
                        dataBinding.roundView.setTipText("???????????????????????????");
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
     * ??????????????????
     *
     * @param livenessModel LivenessModel??????
     */
    private void checkFaceBound(final LivenessModel livenessModel) {
        Log.i(TAG, "onFaceDetectCallback: ???????????????4");
        // ?????????????????????UI??????
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCollectSuccess) {
                    return;
                }

                Log.i(TAG, "livenessModel: " + livenessModel);

                if (livenessModel == null || livenessModel.getFaceInfo() == null) {
                    dataBinding.roundView.setTipText("???????????????????????????");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    return;
                }

                Log.i(TAG, "run: ????????????111111111");

                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_grey);

                mPointXY[0] = livenessModel.getFaceInfo().centerX;   // ??????X??????
                mPointXY[1] = livenessModel.getFaceInfo().centerY;   // ??????Y??????
                mPointXY[2] = livenessModel.getFaceInfo().width;     // ????????????
                mPointXY[3] = livenessModel.getFaceInfo().height;    // ????????????

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
                    dataBinding.roundView.setTipText("?????????????????????");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // ????????????
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if (mPointXY[2] > previewWidth || mPointXY[3] > previewWidth) {
                    dataBinding.roundView.setTipText("?????????????????????");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // ????????????
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if (mPointXY[0] - mPointXY[2] / 2 < leftLimitX
                        || mPointXY[0] + mPointXY[2] / 2 > rightLimitX
                        || mPointXY[1] - mPointXY[3] / 2 < topLimitY
                        || mPointXY[1] + mPointXY[3] / 2 > bottomLimitY) {
                    dataBinding.roundView.setTipText("???????????????????????????");
                    dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                    // ????????????
                    destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                    return;
                }

                if ((Math.abs(AutoTexturePreviewView.circleX - mPointXY[0]) < mPointXY[2] / 2)
                        && (Math.abs(AutoTexturePreviewView.circleY - mPointXY[1]) < mPointXY[2] / 2)
                        && (mPointXY[2] <= previewWidth && mPointXY[3] <= previewWidth)) {

                }
                dataBinding.roundView.setTipText("??????????????????????????????");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_blue);
                // ??????????????????
                checkLiveScore(livenessModel);
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param livenessModel LivenessModel??????
     */
    private void checkLiveScore(LivenessModel livenessModel) {
        Log.i(TAG, "checkLiveScore: ???????????????5");
        if (livenessModel == null || livenessModel.getFaceInfo() == null) {
            dataBinding.roundView.setTipText("???????????????????????????");
            return;
        }

        // ??????????????????
        SingleBaseConfig.getBaseConfig().setType(0);
        int liveType = SingleBaseConfig.getBaseConfig().getType();
        // int liveType = 2;

        if (liveType == 0) {         // ?????????
            getFeatures(livenessModel);
        } else if (liveType == 1) { // RGB????????????
            float rgbLivenessScore = livenessModel.getRgbLivenessScore();
            float liveThreadHold = SingleBaseConfig.getBaseConfig().getRgbLiveScore();
            // Log.e(TAG, "score = " + rgbLivenessScore);
            if (rgbLivenessScore < liveThreadHold) {
                dataBinding.roundView.setTipText("?????????????????????");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                // ????????????
                destroyImageInstance(livenessModel.getBdFaceImageInstanceCrop());
                return;
            }
            // ???????????????
            getFeatures(livenessModel);
        }
    }

    /**
     * ???????????????
     *
     * @param model ????????????
     */
    private void getFeatures(final LivenessModel model) {
        Log.i(TAG, "getFeatures: ???????????????6");
        if (model == null) {
            return;
        }

        // ?????????????????????????????????
        int modelType = SingleBaseConfig.getBaseConfig().getActiveModel(); //??????????????????
        if (modelType == 1) {
            // ?????????
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
            // ?????????
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

    // ??????????????????????????? ????????????
    private void displayCompareResult(float ret, byte[] faceFeature, LivenessModel model) {

        Log.i(TAG, "displayCompareResult: ???????????????7");
        if (model == null) {
            dataBinding.roundView.setTipText("???????????????????????????");
            dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
            return;
        }

        // ??????????????????
        if (ret == 128) {
            // ??????
            BDFaceImageInstance imageInstance = model.getBdFaceImageInstanceCrop(); //??????????????????
            AtomicInteger isOutoBoundary = new AtomicInteger();
            BDFaceImageInstance cropInstance = FaceSDKManager.getInstance().getFaceCrop()
                    .cropFaceByLandmark(imageInstance, model.getLandmarks(),
                            2.0f, false, isOutoBoundary);
            if (cropInstance == null) {
                dataBinding.roundView.setTipText("????????????");
                dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
                // ????????????
                destroyImageInstance(model.getBdFaceImageInstanceCrop());
                return;
            }
            mCropBitmap = BitmapUtils.getInstaceBmp(cropInstance);
            // ????????????
            if (mCropBitmap != null) {
                mCollectSuccess = true;
                dataBinding.circleHead.setImageBitmap(mCropBitmap);
            }
            cropInstance.destory();
            // ????????????
            destroyImageInstance(model.getBdFaceImageInstanceCrop());

            for (int i = 0; i < faceFeature.length; i++) {
                mFeatures[i] = faceFeature[i];
            }

            showControls();

            Log.i(TAG, "displayCompareResult: " + model.getUser());

        } else {
            dataBinding.roundView.setTipText("??????????????????");
            dataBinding.roundView.setBitmapSource(R.mipmap.ic_loading_red);
        }
    }

    /*????????????*/
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
     * ????????????
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
        // ??????????????????
        boolean isSuccess = FaceApi.getInstance().registerUserIntoDBmanager(null,
                userName, imageName, null, mClass, isAccommodation, college, school, mFeatures);
        if (isSuccess) {
            // ??????????????????
            File faceDir = FileUtils.getBatchImportSuccessDirectory();
            File file = new File(faceDir, imageName);
            FileUtils.saveBitmap(file, mCropBitmap);
            // ???????????????????????????
            FaceApi.getInstance().initDatabases(true);
            Log.i(TAG, "File: ????????????");
//             ???????????????
            CameraPreviewManager.getInstance().stopPreview();
            IntentUtils.intentActivity(FaceRegisterActivity.this, FaceAnalyzeActivity.class);
            finish();
        } else {
            ToastUtils.toast(getApplicationContext(), "????????????????????????" +
                    "?????????????????????????????????");
        }
    }

    /**
     * ?????????????????????????????????
     * ??????????????????
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
