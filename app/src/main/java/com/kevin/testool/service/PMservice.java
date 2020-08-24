package com.kevin.testool.service;

import android.app.Service;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;

public class PMservice extends Service {
    public int a;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    public void onCreate(){
        super.onCreate();
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        a = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int b = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        int c = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int d = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);///当前电量百分比

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
        Intent intent=new Intent();
        intent.putExtra("a", a);
        intent.putExtra("b", b);
        intent.putExtra("c", c);
        intent.putExtra("d", d);
        intent.setAction("com.kevin.testool.service.PMservice");
        sendBroadcast(intent);

}

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent_PMservice, int flags, int startId) {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);

        a = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int b = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        int c = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int d = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);///当前电量百分比

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);
        Intent intent=new Intent();
        intent.putExtra("a", a);
        intent.putExtra("b", b);
        intent.putExtra("c", c);
        intent.putExtra("d", d);
        intent.setAction("com.kevin.testool.service.PMservice");
        sendBroadcast(intent);
        return super.onStartCommand(intent_PMservice, flags, startId);
    }

    public void onDestroy(){
        super.onDestroy();
    }
}
