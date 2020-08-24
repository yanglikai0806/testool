package com.kevin.testool.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.kevin.testool.receiver.AlarmReceiver;
import com.kevin.testool.utils.AlarmManagerUtils;
import com.kevin.testool.utils.DateTimeUtils;

public class AlarmService extends IntentService {

    // 从其他地方通过Intent传递过来的提醒时间
    private String alarmDateTime;

    public AlarmService() {
        super("AlarmService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        alarmDateTime = intent.getStringExtra("alarm_time");
        // taskId用于区分不同的任务

        long alarmDateTimeMillis = DateTimeUtils.stringToMillis(alarmDateTime);

        int taskId = intent.getIntExtra("task_id",0);
        switch (taskId){
            case 0:

                AlarmManagerUtils.sendAlarmBroadcast(this, taskId, AlarmManager.RTC_WAKEUP, alarmDateTimeMillis, AlarmReceiver.class);

                break;
            case 1:
                int cycleTime = intent.getIntExtra("cycle_time",10);
                AlarmManagerUtils.sendRepeatAlarmBroadcast(this, taskId, AlarmManager.RTC_WAKEUP, alarmDateTimeMillis, cycleTime * 1000,AlarmReceiver.class);
                break;
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Destroy", "Alarm Service Destroy");
    }

}
