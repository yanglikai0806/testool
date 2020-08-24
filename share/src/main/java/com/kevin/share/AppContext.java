package com.kevin.share;

import android.content.Context;

/**
 * 全局context
 */
public class AppContext {
    private static Context appContext;

    public static void init(Context context) {
        if (appContext == null){
            appContext = context.getApplicationContext();
        }
    }

    public static Context getContext() {
        return appContext;
    }
}
