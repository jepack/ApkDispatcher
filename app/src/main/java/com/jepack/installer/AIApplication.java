package com.jepack.installer;

import android.app.Application;
import android.content.Context;

public class AIApplication extends Application {
    private static Context appCtx;
    @Override
    public void onCreate() {
        super.onCreate();
        appCtx = this;
    }

    public static Context getAppCtx(){
        return appCtx;
    }
}
