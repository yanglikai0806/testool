package com.kevin.share.utils;

import android.content.SharedPreferences;

import com.kevin.share.AppContext;

public class SPUtils {

    public static String getString(String name, String key){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        return rc.getString(key, "");
    }
    public static void putString(String name, String key, String value){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        rc.edit().putString(key, value).apply();
    }
}
