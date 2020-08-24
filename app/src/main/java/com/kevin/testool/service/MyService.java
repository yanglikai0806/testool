package com.kevin.testool.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import com.kevin.testool.utils.AVUtils;

public class MyService extends Service {
    private final String TAG = "MonkeyService";
    //必须要实现的方法
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Service被创建时调用
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //Service被启动时调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mp4File = intent.getStringExtra("mp4File");
        int mp4Time = intent.getIntExtra("mp4Time", 10);
        boolean toDeleteMp4 = intent.getBooleanExtra("toDeleteMp4", false);
        AVUtils.mp4ToGif(mp4File, mp4Time, toDeleteMp4);

        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    //Service被关闭之前回调
    @Override
    public void onDestroy() {

        super.onDestroy();
    }
}