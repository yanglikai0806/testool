package com.kevin.testool.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * AlarmManager工具类
 *
 */

public class AlarmManagerUtils {
    // 获取AlarmManager实例
    public static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    // 发送定时广播（执行广播中的定时任务）
    // 参数：
    // context:上下文
    // requestCode:请求码，用于区分不同的任务
    // type:alarm启动类型
    // triggerAtTime:定时任务开启的时间，毫秒为单位
    // cls:广播接收器的class
    public static void sendAlarmBroadcast(Context context, int requestCode,
                                          int type, long triggerAtTime, Class cls) {
        AlarmManager mgr = getAlarmManager(context);

        Intent intent = new Intent(context, cls);
        intent.setAction("com.kevin.testool.ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, 0);
        mgr.set(type, triggerAtTime, pi);
    }

    // 取消指定requestCode的定时任务
    // 参数：
    // context:上下文
    // requestCode:请求码，用于区分不同的任务
    // cls:广播接收器的class
    public static void cancelAlarmBroadcast(Context context, int requestCode,
                                            Class cls) {
        AlarmManager mgr = getAlarmManager(context);

        Intent intent = new Intent(context, cls);
        intent.setAction("com.kevin.testool.ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode,
                intent, 0);

        mgr.cancel(pi);
        ToastUtils.showShort(context, "取消定时服务成功" + " @requestCode:" + requestCode);
        Log.d("取消定时服务成功", "@requestCode:" + requestCode);
    }

    // 周期性执行定时任务
    // 参数：
    // context:上下文
    // requestCode:请求码，用于区分不同的任务
    // type:alarm启动类型
    // startTime:开始的时间，毫秒为单位
    // cycleTime:定时任务的重复周期，毫秒为单位
    // cls:广播接收器的class
    public static void sendRepeatAlarmBroadcast(Context context,
                                                int requestCode, int type, long startTime, long cycleTime, Class cls) {
        AlarmManager mgr = getAlarmManager(context);

        Intent intent = new Intent(context, cls);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode,
                intent, 0);

        mgr.setRepeating(type, startTime, cycleTime, pi);
    }
}
