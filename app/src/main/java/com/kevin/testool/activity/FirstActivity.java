package com.kevin.testool.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.kevin.share.CONST;
import com.kevin.share.service.InstallService;
import com.kevin.share.utils.AppUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ShellUtils;
import com.kevin.testool.R;

import java.io.File;

import static com.kevin.share.CONST.CONFIG_FILE;
import static com.kevin.share.CONST.LOGPATH;
import static com.kevin.share.Common.downloadResource;

/**
 * 初次使用时的权限，及资源准备
 */

public abstract class FirstActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {// 没有权限。
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(FirstActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(FirstActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(FirstActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
            }
        }
        // 检查工具文件存储路径
        if (!new File(LOGPATH).exists()){
            new File(LOGPATH).mkdirs();
        }

        //shellserver.dex文件加载
        if (!new File(CONST.SHELL_SERVER_DEX).exists()) {
            downloadResource(FirstActivity.this,"https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/shellserver.dex", "shellserver.dex");
        }

        //检查测试辅助应用是否安装
        if (!AppUtils.isApkInstalled(this, "com.kevin.testassist")||!AppUtils.isApkInstalled(this, "com.kevin.testassist.test")){

            AlertDialog.Builder installDialog =
                    new AlertDialog.Builder(FirstActivity.this);
            final View dialogView = LayoutInflater.from(FirstActivity.this)
                    .inflate(R.layout.dialog_layout1,null);

            installDialog.setTitle("提示：");
            installDialog.setView(dialogView);
            installDialog.setPositiveButton("我已安装", null);
            installDialog.setNegativeButton("取消", null);
            installDialog.show();
            Button ins1 = dialogView.findViewById(R.id.btn1);
            Button ins2 = dialogView.findViewById(R.id.btn2);
            if (!AppUtils.isApkInstalled(FirstActivity.this, "com.kevin.testassist")){
                ins1.setText("点击安装“测试辅助”应用");
                ins1.setTextColor(Color.parseColor("#FFF44336"));
            } else {
                ins1.setText("已安装“测试辅助”应用，点击重装");
                ins1.setTextColor(Color.parseColor("#FF4CAF50"));
            }
            if (!AppUtils.isApkInstalled(FirstActivity.this, "com.kevin.testassist.test")){
                ins2.setText("点击安装“测试辅助-test”");
                ins2.setTextColor(Color.parseColor("#FFF44336"));
            } else {
                ins2.setText("已安装“测试辅助-test”，点击重装");
                ins2.setTextColor(Color.parseColor("#FF4CAF50"));
            }
            ins1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent ins = new Intent(FirstActivity.this, InstallService.class);
                    ins.putExtra("APK_URL", "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testassist.apk");
                    startService(ins);
                }
            });

            ins2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent ins = new Intent(FirstActivity.this, InstallService.class);
                    ins.putExtra("APK_URL", "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testassist-test.apk");
                    startService(ins);
                }
            });

        }

        if (!ShellUtils.isShellEnable()){
            new AlertDialog.Builder(FirstActivity.this)
                    .setTitle("提示：")
                    .setMessage("未获得shell执行权限")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }
}
