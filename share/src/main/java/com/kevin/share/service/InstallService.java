package com.kevin.share.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.kevin.share.Common;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

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

            String url = intent.getStringExtra("APK_URL");
            String saveSubPath = url.substring(url.lastIndexOf("/") + 1);
            apkFilePath = Common.downloadResource(getApplicationContext(), url, saveSubPath);
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
                    logUtil.d("应用不存在", "");
                    return;
                }
            } else {
                return;
            }
            try{
                // Android系统安装方法
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
                logUtil.e("", e);
                ToastUtils.showShortByHandler(context,"自动安装失败，请到autotest目录下手动安装");
            }
        }
    }


}
