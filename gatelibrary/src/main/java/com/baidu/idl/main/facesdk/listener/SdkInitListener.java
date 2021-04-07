package com.baidu.idl.main.facesdk.listener;

public interface SdkInitListener {
    public void initStart();  //初始化

    public void initLicenseSuccess(); //证书验证成功

    public void initLicenseFail(int errorCode, String msg); //失败

    public void initModelSuccess();  //模型加载成功

    public void initModelFail(int errorCode, String msg); //失败
}
