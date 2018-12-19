package com.jepack.dispatcher;

import android.util.Log;

public class LogUtil {
    public static void e(Exception e){
        Log.e("AIINSTALL", e.getMessage());
    }

    public static void d(String msg) {
        Log.d("AIINSTALL", msg);
    }
}
