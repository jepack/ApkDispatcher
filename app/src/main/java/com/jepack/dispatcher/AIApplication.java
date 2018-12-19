package com.jepack.dispatcher;

import android.app.Application;
import android.content.Context;

import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

public class AIApplication extends Application {
    private static Context appCtx;
    @Override
    public void onCreate() {
        super.onCreate();
        appCtx = this;
        UMConfigure.init(this, "5c1865ceb465f561280004f5", "1000", UMConfigure.DEVICE_TYPE_PHONE, "9eca06721ddc99408fc437d92f6d5797");
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }

    public static Context getAppCtx(){
        return appCtx;
    }
}
