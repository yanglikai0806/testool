package com.kevin.testool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.service.DeviceRemoteService;

/**
 * 开机重新启动服务
 *
 */

public class BootCompleteReceiver extends BroadcastReceiver {
    // 模拟的task id
    private static int mTaskId = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        logUtil.i("定时服务", "开机启动");
        ShellUtils.adbStart();

        //按需启动远程服务
        if (SPUtils.getInt(SPUtils.MY_DATA, DeviceRemoteService.REMOTE) == 1) {
            Intent intent_remote = new Intent(context, DeviceRemoteService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent_remote);
            } else {
                context.startService(intent_remote);
            }

        }
    }
}
