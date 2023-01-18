package com.kevin.testool.activity;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kevin.share.CONST;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.WifiUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;

import java.io.File;
import java.util.UUID;

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static com.kevin.share.CONST.AUTOTEST;

public class AboutActivity extends BasicActivity {
    private TextView txv_version;
    private Button btn_upgrade;
    private Button gen_device_id;
//    private Button btn_upgrade2;
    private TextView content_upgrade;
    private TextView deviceInfo;
//    private Spinner branche;
    private UpgradeReceiver upgrade;
    private static String apkFile = "";
    String deviceId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        txv_version = findViewById(R.id.txv_version);
        btn_upgrade = findViewById(R.id.btn_upgrade);
        gen_device_id = findViewById(R.id.gen_device_id);
        content_upgrade = findViewById(R.id.upgrade_content);
        deviceInfo = findViewById(R.id.deviceInfo);
        deviceId = Common.getDeviceId();
        txv_version.setText(Common.getVersionName(this,"com.kevin.testool"));
        deviceInfo.setText(String.format("名称：%s\n代号：%s\nID: %s\nIP:%s", Common.getDeviceName(), Common.getDeviceAlias(), deviceId, new WifiUtils().getIPAddress()));
        content_upgrade.setText("  1. 优化getActivity方法\n 2. 增加对配置管理页面"); //更新内容

//        if (TextUtils.isEmpty(deviceId)){
            gen_device_id.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deviceId = Common.getDeviceId();
                    if (TextUtils.isEmpty(deviceId)) {
                        String device_id = UUID.randomUUID().toString().split("-")[0];
                        Common.setDeviceId(device_id);
                        deviceInfo.setText(String.format("名称：%s\n代号：%s\nID: %s", Common.getDeviceName(), Common.getDeviceAlias(), device_id));
                    }
                    }
            });
//        } else {
//            gen_device_id.setVisibility(View.GONE);
//        }

        btn_upgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                downloadApk("https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testool.apk");

            }
        });

    }

    private void downloadApk(String url){
//        String url = "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testool.apk";
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
        logUtil.d("",Uri.parse(url).toString());
        apkFile = url.substring(url.lastIndexOf("/") + 1);
        request.setDestinationInExternalPublicDir(AUTOTEST, apkFile);
        FileUtils.deleteFile(CONST.LOGPATH + apkFile); // 删除旧文件
        request.setNotificationVisibility(VISIBILITY_VISIBLE);
        request.setTitle(apkFile);
        request.setDescription("正在下载" + apkFile);
//        request.setAllowedOverRoaming(false);
        //注册广播 接收下载完成的广播
        upgrade = new UpgradeReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(upgrade, filter);

        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
    }


    public class UpgradeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            File file = new File(CONST.LOGPATH + apkFile);
            if (!file.exists()){
                ToastUtils.showShort(context, apkFile + "文件下载失败，请检查网络状况！");
                return;
            }
            try{
//                if (AdbUtils.hasRootPermission()){
//                    AdbUtils.runShellCommand("pm install -r -d "+file,0);
//                    return;
//                }
                Intent intentInstall = new Intent(Intent.ACTION_VIEW);
                intentInstall.addCategory("android.intent.category.DEFAULT");
                String packageName = context.getPackageName();
                Uri    data;
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
                ToastUtils.showShort(context,"自动安装失败，请到autotest目录下手动安装");

            }

        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upgrade != null) {
            unregisterReceiver(upgrade);
        }

    }



}
