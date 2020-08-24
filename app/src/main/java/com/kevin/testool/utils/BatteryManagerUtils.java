package com.kevin.testool.utils;

import android.os.BatteryManager;
import android.os.SystemClock;

import com.kevin.share.utils.FileUtils;
import com.kevin.testool.MyApplication;

import java.io.FileNotFoundException;

import static android.content.Context.BATTERY_SERVICE;

public class BatteryManagerUtils {
    private static BatteryManager manager = (BatteryManager) MyApplication.getContext().getSystemService(BATTERY_SERVICE);

//    public BatteryManagerUtils{
//
//    }

    public static int getBatteryCapacity() {
//        BatteryManager manager = (BatteryManager) MyApplication.getContext().getSystemService(BATTERY_SERVICE);
//        manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
//        manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
//        manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
       return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);///当前电量百分比
    }

    public static int getCurrentNow(){
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
    }

    /**
     * 记录手机平均电流
     * @param recordFile 记录输出文件 .csv
     * @param recordRate 采样率 每秒采集数据次数
     * @param recordTime 记录时长，单位s(秒)
     */
    public static void recordCurrentAverage(String recordFile, int recordRate, int recordTime){
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis()-startTime < recordTime * 1000){
            try {
                FileUtils.writeFile(recordFile, getCurrentNow()+",", true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            SystemClock.sleep(1000/recordRate);
        }

    }

}
