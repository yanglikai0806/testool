package com.kevin.share.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

/**
 * 有关屏幕操作
 */

public class ScreenUtils {

    /**
     * 得到设备屏幕的宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到设备屏幕的高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 得到设备的密度
     */
    public static float getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * 得到设备的dpi
     */
    public static int getScreenDensityDpi(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }


    /**
     * 把密度转换为像素
     */
    public static int dip2px(Context context, float px) {
        final float scale = getScreenDensity(context);
        return (int) (px * scale + 0.5f);
    }
    /**
     * 把像素转换为密度
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = getScreenDensity(context);
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 设置全屏显示
     * @param context
     */
    public static void setFullScreen(Activity context){
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        Window myWindow = context.getWindow();
        myWindow.setFlags(flag, flag);// 设置为全屏
    }

}
