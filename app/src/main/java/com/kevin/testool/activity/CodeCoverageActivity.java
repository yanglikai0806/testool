package com.kevin.testool.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.kevin.testool.MainActivity;
import com.kevin.testool.R;
import com.kevin.testool.service.MonitorService;
import com.kevin.share.utils.AppUtils;
import com.kevin.share.utils.ToastUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.os.Process.killProcess;

public class CodeCoverageActivity extends AppCompatActivity {

    private EditText pkgName;
    private TextView htu;
    private Button startCover;
    private Button uploadec;
    private Intent intent_monitor;
    private CheckBox checkBox;
    private Intent intent_upload;
    private Boolean isNew = false;
    private static String DEFAULT_COVERAGE_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "coverage.ec";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_coverage);

        pkgName  = findViewById(R.id.pkgName);
        startCover = findViewById(R.id.startCover);
        uploadec = findViewById(R.id.uploadec);
        checkBox = findViewById(R.id.checkBox);
        htu = findViewById(R.id.htu);
        String howToUse = "使用说明：\n\n1. 选中“清除已...” 会删除历史log，适用于更新待测app版本时使用；否则log会追加。\n2. 点击“开始”按钮会启动待测app，并开始记录覆盖率数据；" +
                "\n3. 点击“停止” 按钮会保存覆盖率数据。\n4. 点击“上传”按钮会将生成的覆盖率数据文件上传服务器。\n5. 覆盖率数据查看及报告下载请登录";

        htu.setText(howToUse);
        htu.setTextColor(Color.parseColor("#FF0000"));
        final Map<String, String> cmdMap = new HashMap<>();
        cmdMap.put("com.my.app","am instrument com.my.app/com.my.app.test.JacocoInstrumentation");

        if (AppUtils.getVersionName(CodeCoverageActivity.this, "com.my.app").length() > 0){
            pkgName.setText("com.my.app");
        }

        if (MainActivity.isCoverageTestStart){
            startCover.setText("停止");
            startCover.setTextColor(Color.parseColor("#FF0000"));
        }


        startCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bt = startCover.getText().toString();
                final String[] res = {""};

                switch (bt){
                    case "开启":
                        if (checkBox.isChecked()){
                            isNew = true;
                            File mCoverageFilePath = new File(DEFAULT_COVERAGE_FILE_PATH);
                            if (mCoverageFilePath.exists()){
                                mCoverageFilePath.delete();
                            }
                        }

                        if (cmdMap.containsKey(pkgName.getText().toString())){

                            PackageManager manager = getPackageManager();
                            Intent launchIntentForPackage = manager.getLaunchIntentForPackage(pkgName.getText().toString());
                            startActivity(launchIntentForPackage);

                            startCover.setText("停止");
                            startCover.setTextColor(Color.parseColor("#FF0000"));
                            MainActivity.isCoverageTestStart = true;




//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    res[0] = AdbUtils.runShellCommand(cmdMap.get(pkgName.getText().toString()), 0);
//                                }
//                            }).start();
//                            SystemClock.sleep(500);
//                            if (res[0].equals("-1")) {
//                                ToastUtils.showShort(CodeCoverageActivity.this, "执行失败");
//                            } else {
//                                startCover.setText("停止");
//                                startCover.setTextColor(Color.parseColor("#FF0000"));
//                                MainActivity.isCoverageTestStart = true;
                                intent_monitor = new Intent(CodeCoverageActivity.this, MonitorService.class);
                                intent_monitor.setAction("com.kevin.testool.coverage.running");
                                intent_monitor.putExtra("PKG", pkgName.getText().toString());
                                intent_monitor.putExtra("IS_MONITOR", true);
                                intent_monitor.putExtra("IS_AUTO", true);
                                startService(intent_monitor);
//                            }
                        } else {
                            ToastUtils.showLong(CodeCoverageActivity.this, "此应用还未支持，请联系工具开发人员");
                        }
                        break;
                    case "停止":
                        Intent intent = new Intent();
                        intent.setAction("com.kevin.testool.coverage.finish");
                        intent.putExtra("IS_NEW", false);
                        sendBroadcast(intent);
                        startCover.setText("开启");
                        startCover.setTextColor(Color.parseColor("#000000"));
                        MainActivity.isCoverageTestStart = false;
//                        startCover.setBackgroundResource(android.R.drawable.btn_default);
                        //结束监控服务
                        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();

                        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();

                        while(iter.hasNext()){
                            ActivityManager.RunningAppProcessInfo next = iter.next();
                            String pricessName = getPackageName() + ":MonitorService"; //需要在manifest文件内定义android:process=":MyIntentService"
                            if(next.processName.equals(pricessName)){
                                killProcess(next.pid);
                                break;
                            }
                        }

                        break;
                }
            }
        });
        uploadec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent_upload = new Intent(CodeCoverageActivity.this, MonitorService.class);
                intent_upload.setAction("com.kevin.testool.coverage.upload");
                intent_upload.putExtra("PKG", pkgName.getText().toString());
                startService(intent_upload);


            }
        });




    }


}
