package com.kevin.testool;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.kevin.share.AppContext;

/**
 * 全局context
 */
public class MyApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context =getApplicationContext();
        AppContext.init(context);
    }

    public static Context getContext() {
        return context;
    }
}
