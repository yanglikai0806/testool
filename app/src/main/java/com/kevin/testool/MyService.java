package com.kevin.testool;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

//
//public class MyService extends Service {
//    public MyService() {
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//}
public class MyService extends Service {
    private final String TAG = "MyService";
    //必须要实现的方法
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind方法被调用!");
        return null;
    }

    //Service被创建时调用
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate方法被调用!");
        super.onCreate();
    }

    //Service被启动时调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand方法被调用!");
        ArrayList<String> SELECTED_CASES;
        SELECTED_CASES = intent.getStringArrayListExtra("SELECTED_CASES");
        for(int i=0; i < SELECTED_CASES.size(); i++){
            String command=CMDUtils.generateCommand("com.kevin.testcases", SELECTED_CASES.get(i));
            CMDUtils.runCMD(command, true, true);
            SystemClock.sleep(1000);
        }
        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    //Service被关闭之前回调
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestory方法被调用!");

        super.onDestroy();
    }
}