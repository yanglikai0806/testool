package com.kevin.share.utils;

import android.annotation.SuppressLint;
import android.os.BatteryManager;
import android.os.SystemClock;

import com.kevin.share.AppContext;
import java.io.File;
import java.io.IOException;

import static android.content.Context.BATTERY_SERVICE;
import static com.kevin.share.utils.StringUtil.isNumeric;

public class BatteryManagerUtils {
    private static BatteryManager manager = (BatteryManager) AppContext.getContext().getSystemService(BATTERY_SERVICE);
    private static boolean RECORD_FLAG = true;

//    public BatteryManagerUtils(){
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
    @SuppressLint("DefaultLocale")
    public static String recordCurrentAverage(String recordFile, int recordRate, int recordTime){
        long startTime = System.currentTimeMillis();
        setRecordFlag(true);
        logUtil.i("", "开始记录电流");
        if (new File(recordFile).exists()){
            recordFile = recordFile.replace(".", "_1.");
        }
        while(getRecordFlag() && System.currentTimeMillis()-startTime < recordTime * 1000){
            int current = getCurrentNow();
            if (current > 0){
                FileUtils.writeFile(recordFile, current + ",\n", true);
            }

            SystemClock.sleep(1000/recordRate);
        }
        logUtil.i("", "停止记录电流");
        logUtil.i("", new File(recordFile).getName());
        SystemClock.sleep(1000);
        String[] currentLst = new String[0];
        try {
            currentLst = FileUtils.readFile(recordFile).replace("\n", "").split(",");
            double sum = 0;
            int mCount = 0;
            for (String c : currentLst){
                logUtil.d("----", c);
                if (isNumeric(c)) {
                    sum += Integer.parseInt(c.trim());
                    mCount += 1;
                }
            }
            logUtil.i("", String.format("平均电流值：%.2f mA", sum/(mCount*1000)));
        } catch (IOException e) {
            logUtil.e("", e);
        }
        return recordFile;

    }

    /**
     * 记录手机平均电流
     * @param recordFile 记录输出文件 .csv
     * @param recordRate 采样率 每秒采集数据次数
     */
    public static String recordCurrentAverage(String recordFile, int recordRate){
        setRecordFlag(true);
        logUtil.i("", "开始记录电流");
        if (new File(recordFile).exists()){
            recordFile = recordFile.replace(".", "_1.");
        }
        while(getRecordFlag()){
            int current = getCurrentNow();
            if (current > 0){
                FileUtils.writeFile(recordFile, current + ",\n", true);
            }
            SystemClock.sleep(1000/recordRate);
        }
        logUtil.i("", "停止记录电流");
        logUtil.i("", new File(recordFile).getName());
        SystemClock.sleep(1000);
        SystemClock.sleep(1000);
        String[] currentLst = new String[0];
        try {
            currentLst = FileUtils.readFile(recordFile).replace("\n", "").split(",");
            double sum = 0;
            int mCount = 0;
            for (String c : currentLst){
                logUtil.d("----", c);
                if (isNumeric(c)) {
                    sum += Integer.parseInt(c.trim());
                    mCount += 1;
                }
            }
            logUtil.i("", String.format("平均电流值：%.2f mA", sum/(mCount*1000)));
        } catch (IOException e) {
            logUtil.e("", e);
        }
        return recordFile;
    }

    public static void setRecordFlag(boolean flag){
        RECORD_FLAG = flag;
    }

    public static boolean getRecordFlag(){
        return RECORD_FLAG;
    }


}
