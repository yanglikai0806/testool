package com.kevin.testool.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.kevin.fw.FloatingWindowService;
import com.kevin.fw.TextDisplayWindowService;
import com.kevin.share.CONST;
import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.share.utils.BitmapUtil;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ScreenShotHelper;
import com.kevin.share.utils.ShellUtils;
import com.kevin.testool.MyAdapter;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.testool.service.DeviceRemoteService;
import com.kevin.testool.service.MonitorService;
import com.kevin.testool.MyIntentService;
import com.kevin.share.service.RecordService;
import com.kevin.testool.utils.DateTimeUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.utils.MyWebSocketServer2;
import com.kevin.testool.utils.WifiUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.os.Process.killProcess;
import static com.kevin.share.CONST.CONFIG_FILE;
import static com.kevin.share.Common.CONFIG;
import static com.kevin.share.CONST.TARGET_APP;
import static com.kevin.share.Common.getProcessPid;
import static com.kevin.share.accessibility.AccessibilityHelper.goAccess;
import static com.kevin.testool.utils.MyWebSocketServer.stopMyWebsocketServer;

public class MainActivity extends FirstActivity
        implements NavigationView.OnNavigationItemSelectedListener, ScreenShotHelper.OnScreenShotListener {
    private ArrayList<String> list_item;
    private int checkNum; // 记录选中的条目数量
    private MyAdapter mAdapter;
    private BufferedWriter bw;
    private SimpleDateFormat sdf;
    private dlReceiver dl;
    private String test_env="production";
    private String casetag = "";
    private int loopNum = 1;

    private RecordService recordService = new RecordService();
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private UpdateCaseListReceiver updateCaseListReceiver;
    private MyWebSocketServer2 webSocketServer;
    private boolean socketIsStarted = false;
    Button startTest;
    Button selectAll;
    Button monitorTask;
    Button debugTask;

    private int toKeep;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        selectAll = findViewById(R.id.selectAll);
        monitorTask = findViewById(R.id.startMonitor);
        debugTask = findViewById(R.id.startDebug);
        startTest = findViewById(R.id.startTest);
        // 开启wifi
        new WifiUtils(MainActivity.this).openWifi();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, 1);
        }

        //注册广播接收器:用例更新
        updateCaseListReceiver = new UpdateCaseListReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(CONST.ACTION_UPDATECASELIST);
        registerReceiver(updateCaseListReceiver, filter);
        // 准备ocr识别的训练数据
        File chTrainDataFile = new File(CONST.TESSDATA + File.separator + "chi_sim.traineddata");
        if (!chTrainDataFile.exists()){
            Common.downloadResource(MainActivity.this,"https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/chi_sim.traineddata", "tessdata/chi_sim.traineddata");
            Common.downloadResource(MainActivity.this,"https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/eng.traineddata", "tessdata/eng.traineddata");
            logUtil.d("", "下载OCR训练数据");
        }

        //加载测试用例
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            updateCaseList();
            selectAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectAll.getText().toString().equals("全选")) {
                        for (int i = 0; i < list_item.size(); i++) {
                            MyAdapter.getIsSelected().put(i, true);
                        }
                        // 数量设为list的长度
                        checkNum = list_item.size();
                        // 刷新listview和TextView的显示
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                        selectAll.setText("取消");
                    } else {
                        for (int i = 0; i < list_item.size(); i++) {
                            if (MyAdapter.getIsSelected().get(i)) {
                                MyAdapter.getIsSelected().put(i, false);
//                        checkNum--;
                            } else {
                                MyAdapter.getIsSelected().put(i, true);
                            }

                        }
                        selectAll.setText("全选");
                        // 刷新listview和TextView的显示
//                dataChanged();
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }


        // 自动点击工具弹框
        new Thread(new Runnable() {
            @Override
            public void run() {
                Common.click_element(true, "text", "立即开始", 0, 0);
            }
        }).start();

        // 监控任务
        monitorTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (monitorTask.getText().toString().equals("监控任务")){
                    startMonitorTask();
                } else {
                    killProcess(getProcessPid(getPackageName() + ":MonitorService"));
                    int mPid = getProcessPid(getPackageName() + ":MyIntentService");
                    if (mPid > 0){
                        killProcess(mPid);
                    }
                    Intent intent_monitor = new Intent();
                    intent_monitor.setAction("com.kevin.testool.action.monitor.finish");
                    sendBroadcast(intent_monitor);
                    Toast.makeText(getApplicationContext(), "监听服务已终止！", Toast.LENGTH_LONG).show();
                }
            }
        });
        // 调试任务
        debugTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debugTask.getText().toString().equals("调试任务")) {

                    FileUtils.createTempFile(CONST.TEMP_FILE, FileUtils.creatLogDir()); //创建LOG日志
                    String APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    logUtil.i("REPORT_TITLE", ("测试机型：" + Common.getDeviceName() +"("+ Common.getDeviceAlias() + ") SN："+ Common.getSerialno()+" App版本：" + APP_VER ).replace("\n", ""));
                    logUtil.i("START", TARGET_APP);
                    Intent intent_debug = new Intent(MainActivity.this, MonitorService.class);
                    intent_debug.setAction("com.kevin.testool.task");
                    intent_debug.putExtra("IS_DEBUG", true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent_debug);
                    } else {
                        startService(intent_debug);
                    }
                    debugTask.setText("执行中");
                    debugTask.setTextColor(Color.parseColor("#FF0000"));
                    DebugFinishReceiver dfr = new DebugFinishReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("com.kevin.testool.action.debug.finish");
                    registerReceiver(dfr, filter);
                } else {
                    killProcess(getProcessPid(getPackageName() + ":MonitorService"));
                    Intent intent_monitor = new Intent();
                    intent_monitor.setAction("com.kevin.testool.action.monitor.finish");
                    sendBroadcast(intent_monitor);
                    debugTask.setText("调试任务");
                    debugTask.setTextColor(Color.parseColor("#000000"));
                }
            }
        });

        // 测试任务
        startTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startTest.getText().equals("测试中")) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("提示：")
                            .setMessage("确定要停止当前的任务吗？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startTest.setText("测试任务");
                                    startTest.setTextColor(Color.parseColor("#000000"));
                                    //结束测试服务（结束intentservice）
                                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                                    Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                                    while(iter.hasNext()){
                                        ActivityManager.RunningAppProcessInfo next = iter.next();
                                        String mProcess = getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
                                        if(next.processName.equals(mProcess)){
                                            killProcess(next.pid);
                                            break;
                                        }
                                    }
                                    while(iter.hasNext()){
                                        ActivityManager.RunningAppProcessInfo next = iter.next();
                                        String mProcess = getPackageName() + ":MonitorService"; //需要在manifest文件内定义android:process=":MyIntentService"
                                        if(next.processName.equals(mProcess)){
                                            killProcess(next.pid);
                                            break;
                                        }
                                    }
                                }
                            }).setNeutralButton("取消", null).show();
                } else {
                    //配置弹框
                    try {
                        configDialog().show();
                        //提示有未上传的结果
                        String unUploadDataStr = SPUtils.getString(SPUtils.TASK_RESULT, "result");
                        if (!TextUtils.isEmpty(unUploadDataStr) && unUploadDataStr.length() > 10) {
                            new AlertDialog.Builder(MainActivity.this).setTitle("提示：").setMessage("检测到上次云端测试任务有测试结果未上传，请先上传，否则数据将丢失！")
                                    .setPositiveButton("重新上传", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // 测试完成,检查是否有未上传的结果缓存
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        logUtil.d("", unUploadDataStr);
                                                        JSONArray unUploadData = new JSONArray(unUploadDataStr);
                                                        logUtil.d("未上传数据", unUploadData);
                                                        boolean fg = true; // 测试结果上传标志
                                                        for (int i = 0; i < unUploadData.length(); i++) {
                                                            String res = null;
                                                            int flag = 1;
                                                            res = HttpUtil.postResp(CONST.SERVER_BASE_URL + CONST.TEST_RESULT_URL[flag], unUploadData.get(i).toString());

                                                            if (!res.contains("测试结果已写入")) {
                                                                fg = false;
                                                                break;
                                                            }
                                                        }
                                                        if (fg) {
                                                            SPUtils.putString(SPUtils.TASK_RESULT, "result", ""); //缓存清空
                                                            ToastUtils.showShortByHandler(MainActivity.this, "上传完成");
                                                        } else {
                                                            ToastUtils.showShortByHandler(MainActivity.this, "上传失败");
                                                        }
                                                    } catch (Exception e) {
                                                        logUtil.e("", e);
                                                    }

                                                }
                                            }).start();

                                        }
                                    }).setNegativeButton("忽略", null)
                                    .setNeutralButton("清空", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            SPUtils.putString(SPUtils.TASK_RESULT, "result", ""); //缓存清空
                                            ToastUtils.showShortByHandler(MainActivity.this, "测试结果已清空");
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
                    } catch (Exception e) {
                        logUtil.e("", e);
                    }
                }
            }
        });

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 1);
        Intent intent = new Intent(MainActivity.this, RecordService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

    }

    private void startMonitorTask(){
        Intent intent_monitor = new Intent(MainActivity.this, MonitorService.class);
        intent_monitor.setAction("com.kevin.testool.task");
        intent_monitor.putExtra("IS_MONITOR", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent_monitor);
        } else {
            startService(intent_monitor);
        }
        monitorTask.setText("监控中");
        monitorTask.setTextColor(Color.parseColor("#FF0000"));
        //注册广播接收器
        MonitorReceiver monitorReceiver = new MonitorReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.kevin.testool.action.monitor.finish");
        registerReceiver(monitorReceiver, filter);
    }


    /**
     * 执行本地测试任务
     */
    private void startLocalTask(){

        JSONArray selected_cases = new JSONArray();
        for (int i = 0; i < list_item.size(); i++) {
            if (MyAdapter.getIsSelected().get(i)) {
                mAdapter.getItemId(i);

                String text = list_item.get(i).split("\\.",2)[1].trim();
                selected_cases.put(text);//过滤被选中的测试用例
            }

        }
        if (selected_cases.length() > 0) {
            try {
                casetag = Common.CONFIG().getString("CASE_TAG");
            } catch (JSONException e) {
                logUtil.d("", "config.json中无 CASE_TAG");
            }
            Intent intent_start = new Intent(MainActivity.this, MyIntentService.class);
            intent_start.setAction("com.kevin.testool.action.run");
            intent_start.putExtra("SELECTED_CASES", selected_cases + "");
            intent_start.putExtra("CASE_TAG", casetag);
            intent_start.putExtra("TEST_ENV", test_env);
            intent_start.putExtra("LOOP", loopNum);
            logUtil.d("执行本地测试", selected_cases.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent_start);
            } else {
                startService(intent_start);
            }
            moveTaskToBack(true); //隐藏activity到后台
        } else {
            TARGET_APP = CONFIG().optString("TARGET_APP");
            try {
                Common.switchTestEnv(TARGET_APP, test_env);
            } catch (Exception ignore) {
                ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                am.killBackgroundProcesses(TARGET_APP);
            }
            Toast.makeText(getApplicationContext(), "请选择测试用例！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从界面触发执行云端测试任务
     */

    private void startServerTask(int taskId, String taskResult, String taskDate){
        if (startTest.getText().toString().equals("测试任务")){
            Intent intent_test = new Intent(MainActivity.this, MonitorService.class);

            String _testcases = SPUtils.getString(SPUtils.TASK_CASE, taskId + "");
            JSONArray testcasesFromCash = new JSONArray();
            if (_testcases.length() > 0) {
                try {
                    testcasesFromCash = new JSONArray(_testcases);
                    logUtil.d("上次未执行用例", testcasesFromCash.length() + "条");
                } catch (JSONException e) {
                    logUtil.e("", e);
                }
            }

            if (testcasesFromCash.length() > 0){
                // 提示有未执行的测试
                new AlertDialog.Builder(MainActivity.this).setTitle("提示：").setMessage("检测到任务"+taskId+"有未完成的测试用例"+testcasesFromCash.length()+"条, 是否继续？")
                        .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                intent_test.putExtra("TASK_FLAG", 2); // 断点执行
                                intent_test.setAction("com.kevin.testool.task");
                                intent_test.putExtra("IS_TEST", true);
                                intent_test.putExtra("TASK_ID", taskId);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent_test);
                                } else {
                                    startService(intent_test);
                                }
                                startTest.setText("测试中");
                                startTest.setTextColor(Color.parseColor("#FF0000"));
                                SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, taskId+"");
                            }}).setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        intent_test.setAction("com.kevin.testool.task");
                        intent_test.putExtra("IS_TEST", true);
                        intent_test.putExtra("TASK_FLAG", 1);
                        intent_test.putExtra("TASK_ID", taskId);
                        intent_test.putExtra("TASK_RESULT", taskResult);
                        intent_test.putExtra("TASK_DATE", taskDate);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent_test);
                        } else {
                            startService(intent_test);
                        }
                        startTest.setText("测试中");
                        startTest.setTextColor(Color.parseColor("#FF0000"));
                    }
                }).show();

            } else {
                intent_test.setAction("com.kevin.testool.task");
                intent_test.putExtra("TASK_FLAG", 1);
                intent_test.putExtra("IS_TEST", true);
                intent_test.putExtra("TASK_ID", taskId);
                intent_test.putExtra("TASK_RESULT", taskResult);
                intent_test.putExtra("TASK_DATE", taskDate);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent_test);
                } else {
                    startService(intent_test);
                }
                startTest.setText("测试中");
                startTest.setTextColor(Color.parseColor("#FF0000"));
            }
        } else {
            ToastUtils.showShort(MainActivity.this, "任务执行中，请先结束再试");
        }
    }

    /**
     * 工具重启后自动执行云端任务
     */
    private void startServerTask(String taskId){
        try {
            if (!TextUtils.isEmpty(taskId)) {
                Intent intent_test = new Intent(MainActivity.this, MonitorService.class);
                intent_test.putExtra("TASK_FLAG", 2); // 断点执行
                intent_test.setAction("com.kevin.testool.task");
                intent_test.putExtra("IS_TEST", true);
                intent_test.putExtra("TASK_ID", Integer.valueOf(taskId));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent_test);
                } else {
                    startService(intent_test);
                }
                startTest.setText("测试中");
                startTest.setTextColor(Color.parseColor("#FF0000"));
            } else {
                ShellUtils.executeSocketShell("###testool", 0);
                ToastUtils.showShort(MainActivity.this, "无待执行任务");
            }
        } catch (Exception e) {
            logUtil.e("", e);
        }
    }

    @Override
    public void onShotFinish(Bitmap bitmap) {
        logUtil.d("", "bitmap:" + bitmap.getWidth());
        webSocketServer.sendBytes(BitmapUtil.getByteBitmap(bitmap));

    }

    //================ 广播接收器们===================================
    public class MonitorReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Button monitorTask = findViewById(R.id.startMonitor);
            monitorTask.setText("监控任务");
            monitorTask.setTextColor(Color.parseColor("#000000"));
        }
    }

    public class UpdateCaseListReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent){
            updateCaseList();
        }
    }

    public static class dlReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ToastUtils.showShort(context,"文件下载完成~");
            } catch (Exception e) {
                logUtil.e("", e);
                ToastUtils.showShort(context,"文件下载失败！！！");
            }
        }
    }

    public class DebugFinishReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent_debug) {
            if (intent_debug.hasExtra("RESULT")) {
                Intent textDisplayIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);
                textDisplayIntent.putExtra("TEXT", intent_debug.getStringExtra("DEBUG_LOG"));
                startService(textDisplayIntent);
                unregisterReceiver(this);
            }
        }
    }
    //=======================================================================

    private void exit() {
        AlertDialog.Builder popWindow = new AlertDialog.Builder(MainActivity.this);
        //设置对话框标题
        popWindow.setTitle("警告！");
        //设置对话框消息
        popWindow.setMessage("退出后测试将停止！");
        // 添加选择按钮并注册监听
        popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                //结束测试服务（结束intentservice）
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                while(iter.hasNext()){
                    ActivityManager.RunningAppProcessInfo next = iter.next();
                    String mProcess = getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
                    if(next.processName.equals(mProcess)){
                        killProcess(next.pid);
                        break;
                    }
                }
            }
        });
        popWindow.setNegativeButton("取消", null);
        //对话框显示
        popWindow.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 工具重启后根据启动参数恢复中止执行的任务
        Intent intentActivity = getIntent();
//        int startMode = intentActivity.getIntExtra("MODE", 0);
        try {
            String startTask = intentActivity.getStringExtra("TASK");
            logUtil.d("自启动任务", startTask);
            if (startTask.equals("testool.MyIntentService")){
                String taskId = SPUtils.getString(SPUtils.MY_DATA, SPUtils.TASK_ID);
                logUtil.d("执行任务ID",taskId);
                if (taskId.equals("-1")){ // 执行本地测试任务
                    // 从缓存中读取未执行的测试用例
                    String selectedCases = SPUtils.getString(SPUtils.TASK_CASE, taskId);

                    Intent intent_start = new Intent(MainActivity.this, MyIntentService.class);
                    intent_start.setAction("com.kevin.testool.action.run");
                    intent_start.putExtra("SELECTED_CASES", selectedCases);
                    intent_start.putExtra("CASE_TAG", casetag);
                    intent_start.putExtra("TEST_ENV", test_env);
                    intent_start.putExtra("LOOP", loopNum);
                    logUtil.d("执行本地测试", selectedCases);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent_start);
                    } else {
                        startService(intent_start);
                    }
//                    moveTaskToBack(true); //隐藏activity到后台

                } else {
                    startServerTask(taskId);
                }
            } else if (startTask.equals("testool.MonitorService")){
                startMonitorTask();
            }


            // 重启后恢复界面状态
            if (getProcessPid(getPackageName() + ":MonitorService") > 0){
                if (startTask.equals("testool.MonitorService")) {
                    monitorTask.setText("监控中");
                    monitorTask.setTextColor(Color.parseColor("#FF0000"));
                }
                if (startTask.equals("testool.MyIntentService")) {
                    startTest.setText("测试中");
                    startTest.setTextColor(Color.parseColor("#FF0000"));
                }

            }
        } catch (Exception ignore){

        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            exit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        //按需启动远程服务
        if (SPUtils.getRemoteFlag() == 1) {
            menu.findItem(R.id.remote).setIcon(R.drawable.eye).setTitle(R.string.disable_remote_control);
            Intent intent_remote = new Intent(MainActivity.this, DeviceRemoteService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent_remote);
            } else {
                startService(intent_remote);
            }

        } else {
            menu.findItem(R.id.remote).setIcon(R.drawable.eye_disabled).setTitle(R.string.remote_control);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear_log) {
            new android.app.AlertDialog.Builder(MainActivity.this).setTitle("警告！")
                    .setMessage("历史LOG文件将全部被清除")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            FileUtils.RecursionDeleteFile(new File(CONST.REPORT_PATH));
                            ToastUtils.showShort(MainActivity.this, "log清除完成");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
        if (id == R.id.action_start_accessibility){
            if (AccessibilityHelper.checkAccessibilityEnabled()){
                ToastUtils.showShort(MainActivity.this, "辅助功能已开启");
            }
            goAccess();

        }

        if (id == R.id.action_start_float) {
            //        显示悬浮球
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                try {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
                } catch (Exception e){
                    logUtil.e("", e.getMessage());
                }
            } else {
                Intent fIntent = new Intent(getApplicationContext(), FloatingWindowService.class);
                fIntent.putExtra("MODE", 1);
                startService(fIntent);
            }
        }

        //远程控制
        if (id == R.id.remote){
            String titleStr = item.getTitle().toString();
            if (titleStr.equals(this.getString(R.string.remote_control))){
                SPUtils.setRemoteFlag(1);
                Intent intent_remote = new Intent(MainActivity.this, DeviceRemoteService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent_remote);
                } else {
                    startService(intent_remote);
                }
                item.setIcon(R.drawable.eye);
                item.setTitle(R.string.disable_remote_control);

            } else {
                //结束远程服务
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                while(iter.hasNext()){
                    ActivityManager.RunningAppProcessInfo next = iter.next();
                    String mProcess = getPackageName() + ":RemoteService";
                    if(next.processName.equals(mProcess)){
                        killProcess(next.pid);
                        break;
                    }
                }
                SPUtils.setRemoteFlag(0);
                item.setIcon(R.drawable.eye_disabled);
                item.setTitle(R.string.remote_control);
            }
            ToastUtils.showShort(MainActivity.this, titleStr);


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_syc) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Common.syncTestcases();
                        sendBroadcast(new Intent(CONST.ACTION_UPDATECASELIST));
                        ToastUtils.showShortByHandler(MainActivity.this, "同步完成");
                    } catch (Exception e){
                        logUtil.e("", e);
                    }
                }
            }).start();

        } else if (id == R.id.record_case){
            startActivity(new Intent(MainActivity.this, RecordCaseActivity.class));

        } else if (id == R.id.nav_update) {
            updateCaseList();

        } else if (id == R.id.nav_report) {
            startActivity(new Intent(MainActivity.this, ReportActivity.class));

        } else if (id == R.id.nav_uicrawler) {
            startActivity(new Intent(MainActivity.this, UICrawlerActivity.class));

        }else if (id == R.id.image_edit) {
            startActivity(new Intent(MainActivity.this, ImageEditActivity.class));
//            startActivity(new Intent(MainActivity.this, ImageCompareActivity.class));
        }else if (id == R.id.nav_share) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));

        } else if (id == R.id.nav_keep){
            keepDialog().show();

        } else if (id == R.id.nav_adb) {
            startActivity(new Intent(MainActivity.this, WirelessAdb.class));
        } else if (id == R.id.add_new){
            startActivity(new Intent(MainActivity.this, EditCaseActivity.class));
        }else if (id == R.id.nav_monkey){
            startActivity(new Intent(MainActivity.this, MonkeyTestActivity.class));
        }else if (id == R.id.nav_log){
            Common.generateBugreport(CONST.LOGPATH + "bugreport_" + DateTimeUtils.getCurrentDateTimeString() + ".txt");
            ToastUtils.showShort(MainActivity.this, "正在抓取...，稍后请在autotest文件夹下查看");
        } else if (id == R.id.nav_test_plateform){
            startActivity(new Intent(MainActivity.this, TestPlateformActivity.class));
        } else if (id == R.id.nav_config){
            startActivity(new Intent(MainActivity.this, ConfigActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被用户同意
                } else {
                    // 权限被用户拒绝了
                    logUtil.d("", "用户拒绝了该权限");
                }
                return;
            }
        }
    }

    /**
     * 实现工具保活的设置
     * @return
     */
    private AlertDialog.Builder keepDialog() {
        final AlertDialog.Builder popWindow = new AlertDialog.Builder(MainActivity.this);
        popWindow.setTitle("选择保活的进程：");


        ArrayList<String> processList = new ArrayList<String>(); // 进程名称
        ArrayList<String> processList2 = new ArrayList<String>(); // 进程名,发送给app_process程序的进程名
//        Collections.sort(processList, String.CASE_INSENSITIVE_ORDER);
        processList.add("测试工具主进程");
        processList2.add("testool");
        processList.add("远程控制服务进程");
        processList2.add("Remote");
        processList.add("测试执行服务进程");
        processList2.add("testool.MyIntentService");
        processList.add("监控服务进程");
        processList2.add("testool.MonitorService");
        processList.add("关闭守护");
        final String[] strs = processList.toArray(new String[]{}); // arryList 转 数组
        popWindow.setSingleChoiceItems(strs, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                ToastUtils.showShort(MainActivity.this, strs[which]);
                toKeep = which;
            }
        });
        popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (toKeep == processList.size() - 1) {
                    // 通知app_process程序守护该进程
                    if (ShellUtils.isShellSocketConnect()) {
                        ShellUtils.executeSocketShell("###testool", 0);
                    } else {
                        ToastUtils.showLong(MainActivity.this, "shell server未启动,无法执行保活");
                    }
                } else {
                    // 通知app_process程序守护该进程
                    if (ShellUtils.isShellSocketConnect()) {
                        ToastUtils.showLong(MainActivity.this, ShellUtils.executeSocketShell("###" + processList2.get(toKeep), 0));
                    } else {
                        ToastUtils.showLong(MainActivity.this, "shell server未启动,无法执行保活");
                    }
                }
            }
        });

        popWindow.setNegativeButton("取消", null);
        return popWindow;
    }
    /**
     * 展示&修改 配置参数对话框
     * @return
     * @throws JSONException
     */
    @SuppressLint("ResourceType")
    private AlertDialog.Builder configDialog() throws JSONException {
        AlertDialog.Builder configDialog = new AlertDialog.Builder(MainActivity.this);
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_config,null);
        final RadioButton proRb = dialogView.findViewById(R.id.proRb);
        final RadioButton preRb = dialogView.findViewById(R.id.preRb);
        final RadioButton stgRb = dialogView.findViewById(R.id.stgRb);
        final RadioGroup Rbgroup = dialogView.findViewById(R.id.RdGroup);
        configDialog.setView(dialogView);
        configDialog.setNegativeButton("关闭", null);
        // 获取当前config配置
        Rbgroup.clearCheck();
        Rbgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.proRb:
                        test_env = proRb.getText().toString();
                        break;
                    case R.id.preRb:
                        test_env = preRb.getText().toString();
                        break;
                    case R.id.stgRb:
                        test_env = stgRb.getText().toString();
                        break;
                }

                try {
                    FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("TEST_ENV", test_env));
                } catch (JSONException e) {
                    logUtil.e("", e);
                }
            }
        });

        //测试设置：
        final Switch bugreport_switch = dialogView.findViewById(R.id.log);
        final Switch screenshot_switch =  dialogView.findViewById(R.id.screenshot);
        final Switch post_switch = dialogView.findViewById(R.id.post_result);
        final Switch auto_mute = dialogView.findViewById(R.id.auto_mute);
//        final Switch alarm_msg = dialogView.findViewById(R.id.alarm_msg);
        final Switch screen_record = dialogView.findViewById(R.id.screenRecord);
        final Switch recordMemInfo = dialogView.findViewById(R.id.recordMemInfo);
        final Switch recordCurrent = dialogView.findViewById(R.id.recordCurrent);

        if (!Common.CONFIG().isNull("LOG") && Common.CONFIG().getString("LOG").equals("true")) {
            bugreport_switch.setChecked(true);
        } else{
            bugreport_switch.setChecked(false);
        }
        bugreport_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("LOG", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("LOG", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        if (!Common.CONFIG().isNull("SCREENSHOT") && Common.CONFIG().getString("SCREENSHOT").equals("true")) {
            screenshot_switch.setChecked(true);
        } else{
            screenshot_switch.setChecked(false);
        }
        screenshot_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREENSHOT", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREENSHOT", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });

        if (!Common.CONFIG().isNull("SCREEN_RECORD") && Common.CONFIG().getString("SCREEN_RECORD").equals("true")) {
            screen_record.setChecked(true);
        } else{
            screen_record.setChecked(false);
        }
        screen_record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREEN_RECORD", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREEN_RECORD", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });

        if (!Common.CONFIG().isNull("POST_RESULT") && Common.CONFIG().getString("POST_RESULT").equals("true")) {
            post_switch.setChecked(true);
        } else{
            post_switch.setChecked(false);
        }
        post_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("POST_RESULT", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("POST_RESULT", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });

        if (!Common.CONFIG().isNull("MUTE") && Common.CONFIG().getString("MUTE").equals("true")) {
            auto_mute.setChecked(true);
        } else{
            auto_mute.setChecked(false);
        }
        auto_mute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("MUTE", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("MUTE", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });

        if (!Common.CONFIG().isNull("RECORD_MEMINFO") && Common.CONFIG().getString("RECORD_MEMINFO").equals("true")) {
            recordMemInfo.setChecked(true);
        } else{
            recordMemInfo.setChecked(false);
        }
        recordMemInfo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("RECORD_MEMINFO", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("RECORD_MEMINFO", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        if (!Common.CONFIG().isNull("RECORD_CURRENT") && Common.CONFIG().getString("RECORD_CURRENT").equals("true")) {
            recordCurrent.setChecked(true);
        } else{
            recordCurrent.setChecked(false);
        }
        recordCurrent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("RECORD_CURRENT", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("RECORD_CURRENT", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        // 目标应用 设置
        final EditText branch = dialogView.findViewById(R.id.input_test_app);
        if (!Common.CONFIG().isNull("TARGET_APP")) {
            branch.setText(Common.CONFIG().getString("TARGET_APP"));
        }
        branch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("TARGET_APP", s));
                } catch (JSONException e) {
                    logUtil.e("", e);
                }

            }
        });
        final EditText retry = dialogView.findViewById(R.id.input_retry);
        if (!Common.CONFIG().isNull("RETRY")) {
            retry.setText(Common.CONFIG().getString("RETRY"));
        }
        retry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    FileUtils.editJsonFile(CONST.CONFIG_FILE, new JSONObject().put("RETRY", s));
                } catch (JSONException e) {
                    logUtil.e("", e);
                }

            }
        });

        final EditText casetag = dialogView.findViewById(R.id.input_case_tag);
        if (!Common.CONFIG().isNull("CASE_TAG")) {
            casetag.setText(Common.CONFIG().getString("CASE_TAG"));
        }
        casetag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    FileUtils.editJsonFile(CONST.CONFIG_FILE, new JSONObject().put("CASE_TAG", s));
                } catch (JSONException e) {
                    logUtil.e("", e);
                }

            }
        });
        // 循环次数
        final EditText loop = dialogView.findViewById(R.id.input_loop);
        loop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try{
                loopNum = Integer.valueOf(s.toString());
                } catch (Exception ignore){

                }

            }
        });

        final Button configDetail = dialogView.findViewById(R.id.configDetail);
        configDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder popWindow = new AlertDialog.Builder(MainActivity.this);
                View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.case_detail,null);
                //设置对话框标题
                popWindow.setTitle("配置文件");
//                设置对话框消息
                popWindow.setView(dialogView);
//                String res = FileUtils.readJsonFile(CONFIG_FILE);
                String res = Common.reloadCONFIG().toString();
                final TextView case_detail = dialogView.findViewById(R.id.case_detail);
                case_detail.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
                case_detail.setTextIsSelectable(true);

//                 添加选择按钮并注册监听
                popWindow.setPositiveButton("编辑", (dialog, which) -> {
                    AlertDialog.Builder editWindow = new AlertDialog.Builder(MainActivity.this);
                    View editWindowView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_edit,null);
//                    editWindow.setTitle("编辑用例");
                    editWindow.setView(editWindowView);
                    final EditText et = editWindowView.findViewById(R.id.caseEdit);
                    et.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
                    editWindow.setPositiveButton("保存", (dialog1, which1) -> {
                        Common.updateCONFIG(et.getText().toString());

                    });
                    editWindow.setNegativeButton("取消", null);
                    editWindow.show();
                });
                popWindow.setNegativeButton("关闭", null);
                //对话框显示
                popWindow.show();
            }
        });
        final Button startLocalTest = dialogView.findViewById(R.id.startLocalTest);
        startLocalTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocalTask();
            }
        });

        final Button startServerTest = dialogView.findViewById(R.id.startServerTest);
        EditText task_id_et = dialogView.findViewById(R.id.input_task_id);
        EditText task_result_et = dialogView.findViewById(R.id.input_result);
        EditText task_date_et = dialogView.findViewById(R.id.input_date);
        startServerTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.getDeviceId().length() == 0){
                    ToastUtils.showShort(MainActivity.this, "设备id不能为空");
                    return;
                }
                String task_id = task_id_et.getText().toString();
                String task_result = task_result_et.getText().toString();
                String task_date = task_date_et.getText().toString();
                if (!TextUtils.isEmpty(task_id)){
                    //判断 post_result 开关状态
                    if (!post_switch.isChecked()){
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示：")
                            .setMessage("检测到未开启 结果上传 开关, 测试结果将保留本地不会上传服务器, 是否继续？")
                            .setPositiveButton("前去开启", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setNegativeButton("依然执行", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startServerTask(Integer.parseInt(task_id), task_result, task_date);
                                }
                            })
                            .show();
                    } else {
                        startServerTask(Integer.parseInt(task_id), task_result, task_date);
                    }
                } else {
                    ToastUtils.showShort(MainActivity.this, "任务ID不能为空");
                }
            }
        });
        return configDialog;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 显示悬浮球
        if (requestCode == 0) {
            if (Settings.canDrawOverlays(this)) {
                Intent intent_fw = new Intent(MainActivity.this, FloatingWindowService.class);
                startService(intent_fw);
            }
        }
        // 屏幕录制
        if (requestCode == 1 && resultCode == RESULT_OK){
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            RecordService.setMediaProject(mediaProjection);
        }

        // 截图
        if (requestCode == 2 && resultCode == RESULT_OK){
            ScreenShotHelper screenShotHelper = new ScreenShotHelper(this, resultCode, data, this);
            screenShotHelper.startScreenShot();
        }
    }


    /**
     * 更新界面显示的测试用例列表
     */
    public void updateCaseList(){
        ArrayList<String> cases_list = Common.getCaseList();
        list_item = new ArrayList<String>();
        if (cases_list != null) {
            for (int i = 0; i < cases_list.size(); i++) {
                list_item.add((i + 1) + ". " + cases_list.get(i));
            }
            ListView list_test = findViewById(R.id.list_test);
            mAdapter = new MyAdapter(list_item, this);
            list_test.setAdapter(mAdapter);

        }
    }



    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dl != null) {
            unregisterReceiver(dl);
        }
        if (updateCaseListReceiver != null){
            unregisterReceiver(updateCaseListReceiver);
        }
        stopMyWebsocketServer(); // 结束远程服务
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
            recordService = binder.getRecordService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

}
