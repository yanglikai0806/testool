package com.kevin.testool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.widget.Toast;

import com.kevin.testool.common.Common;


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //耗电测试完成后启动服务
        Intent intent_pt_finish = new Intent(context, MyIntentService.class);
        intent_pt_finish.setAction("com.kevin.testool.action.powertest.finish");
        context.startService(intent_pt_finish);

//        ToastUtils.showShort(context,
//                "从服务启动广播：at " + DateTimeUtils.getCurrentDateTimeString());
//        Log.i("Alarm", "从服务启动广播：at " + DateTimeUtils.getCurrentDateTimeString());
    }

}
