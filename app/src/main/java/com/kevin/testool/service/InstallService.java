package com.kevin.testool.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import com.kevin.share.Common;
import com.kevin.share.utils.AdbUtils;
import com.kevin.share.utils.ToastUtils;

import java.io.File;

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;

public class InstallService extends Service {
    private static String apkFilePath = "";
    private static String appPkg = "";
    public InstallService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("APK_URL")) {
            //注册广播 接收下载完成的广播
            BroadcastReceiver intentInstall = new InstallReceiver();
            IntentFilter filter=new IntentFilter();
            filter.addAction(ACTION_DOWNLOAD_COMPLETE);
            getApplicationContext().registerReceiver(intentInstall, filter);
            apkFilePath = Common.downloadApk(getApplicationContext(), intent.getStringExtra("APK_URL"));
        }
        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    public class InstallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            File file;
            if (!TextUtils.isEmpty(apkFilePath)) {
                file = new File(apkFilePath);
                if (!file.exists()){
                    return;
                }
            } else {
                return;
            }
            try{
                if (AdbUtils.hasRootPermission()){
                    AdbUtils.runShellCommand("pm install -r -d " + file,0);
                    return;
                }
                Intent intentInstall = new Intent(Intent.ACTION_VIEW);
                intentInstall.addCategory("android.intent.category.DEFAULT");
                String packageName = context.getPackageName();
                Uri data;
                if (Build.VERSION.SDK_INT >= 24){
                    intentInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    data = FileProvider.getUriForFile(context, packageName + ".fileprovider", file);
                }else {
                    data = Uri.fromFile(file);
                }
                intentInstall.setDataAndType(data, "application/vnd.android.package-archive");
                intentInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentInstall);
            } catch (Exception e){
                e.printStackTrace();
                ToastUtils.showShort(context,"自动安装失败，请到autotest目录下手动安装");
            }
        }
    }

}
