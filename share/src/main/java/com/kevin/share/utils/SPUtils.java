package com.kevin.share.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.kevin.share.AppContext;

import static android.content.Context.MODE_MULTI_PROCESS;

/**record_case
 * mock_data
 * shuttle
 * my_data
 */
public class SPUtils {
    // 数据缓存文件名
    public static final String RECORD_CASE= "record_case";
    public static final String MOCK_DATA= "mock_data";
    public static final String SHUTTLE= "shuttle";
    public static final String MY_DATA= "my_data";
    public static final String TASK_CASE= "task_case";
    public static final String TASK_RESULT= "task_result";
    // 数据缓存的key值
    public static final String TASK= "task";
    public static final String TASK_ID= "task_id";
    public static final String REMOTE = "REMOTE"; //是否允许远程的标志, 0:不允许， 1：允许
    public static final String TASK_RUN = "TASK_RUN"; // 任务执行标志, 0:安装失败， 1：安装成功
    public static final String APK_INSTALL = "APK_INSTALL"; //应用自动安装的标志, 0:安装失败， 1：安装成功
    public static final String SCREENSHOT = "SCREENSHOT"; //截图是否完成的标志, 0:安装失败， 1：安装成功

    public static String getString(String name, String key){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        return rc.getString(key, "");
    }

    public static boolean getBoolean(String name, String key){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        return rc.getBoolean(key, false);
    }

    public static int getInt(String name, String key){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        return rc.getInt(key, 0);
    }

    public static boolean putString(String name, String key, String value){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        SharedPreferences.Editor editor = rc.edit();
        return editor.putString(key, value).commit();
    }

    public static boolean putInt(String name, String key, int value){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        SharedPreferences.Editor editor = rc.edit();
        return editor.putInt(key, value).commit();
    }

    public static boolean putBoolean(String name, String key, boolean value){
        SharedPreferences rc = AppContext.getContext().getSharedPreferences(name, 0);
        SharedPreferences.Editor editor = rc.edit();
        return editor.putBoolean(key, value).commit();
    }

    // 设备远程标志
    public static int getRemoteFlag(){
        return SPUtils.getInt(SPUtils.MY_DATA, REMOTE);
    }

    public static void setRemoteFlag(int value){
        SPUtils.putInt(SPUtils.MY_DATA, REMOTE, value);
    }

    // 任务执行标志
    public static int getTaskRunFlag(){
        return SPUtils.getInt(SPUtils.MY_DATA, TASK_RUN);
    }

    public static void setTaskRunFlag(int value){
        SPUtils.putInt(SPUtils.MY_DATA, TASK_RUN, value);
    }

    // 应用安装标志
    public static int getApkInstallFlag(){
        return SPUtils.getInt(SPUtils.MY_DATA, APK_INSTALL);
    }

    public static void setApkInstallFlag(int value){
        SPUtils.putInt(SPUtils.MY_DATA, APK_INSTALL, value);
    }



}
