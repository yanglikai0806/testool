package com.kevin.testool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.kevin.share.Common;
import com.kevin.testool.activity.EditCaseActivity;
import com.kevin.testool.activity.MonkeyTestActivity;
import com.kevin.testool.activity.RecordCaseActivity;
import com.kevin.testool.activity.ReportActivity;
import com.kevin.testool.activity.UICrawlerActivity;
import com.kevin.testool.activity.WirelessAdb;
import com.kevin.testool.service.InstallService;
import com.kevin.testool.service.MonitorService;
import com.kevin.share.utils.AdbUtils;
import com.kevin.share.utils.AppUtils;
import com.kevin.testool.utils.DateTimeUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.os.Process.killProcess;
import static com.kevin.share.CONST.AUTOTEST;
import static com.kevin.share.CONST.CONFIG_FILE;
import static com.kevin.share.CONST.LOGPATH;
import static com.kevin.share.CONST.NEED_REMIND_ACCESSIBILITY;
import static com.kevin.share.CONST.TESTOOL_SETTING;
import static com.kevin.share.Common.CONFIG;
import static com.kevin.share.accessibility.AccessibilityHelper.goAccess;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ArrayList<String> list_item;
    private int checkNum; // 记录选中的条目数量
    private MyAdapter mAdapter;
    private BufferedWriter bw;
    private SimpleDateFormat sdf;
    private FileUtils myFile;
    private JSONArray fileList;
    private dlReceiver dl;
    private String test_env="production";
    private String casetag = "";
    private int loopNum = 1;
    // 申明代码覆盖测试状态
    public static Boolean isCoverageTestStart = false;

    int year = 2016;
    int month = 10;
    int day = 8;
    int houre = 15;
    int minute = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {// 没有权限。
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
            }
        }
        //注册广播接收器
        UpdateCaseListReceiver updateCaseListReceiver = new UpdateCaseListReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(CONST.ACTION_UPDATECASELIST);
        registerReceiver(updateCaseListReceiver, filter);
        //

        SharedPreferences ts = getSharedPreferences(TESTOOL_SETTING, 0);
        boolean remindAccess = true;
        if (ts != null) {
            remindAccess = ts.getBoolean(NEED_REMIND_ACCESSIBILITY, true);
        }

        if (!AccessibilityHelper.checkAccessibilityEnabled() && remindAccess){
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示：")
                    .setMessage("是否为“测试工具”开启辅助功能？")
                    .setPositiveButton("前往开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goAccess();
                        }
                    })
                    .setNegativeButton("不再提醒", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ts.edit().putBoolean(NEED_REMIND_ACCESSIBILITY, false).apply();
                        }
                    })
                    .show();
        }

        if (!new File(LOGPATH).exists()){
            new File(LOGPATH).mkdirs();
        }
        //config文件加载
        if (!new File(CONST.CONFIG_FILE).exists()) {
            String configContent = "\"APP\" : {\"微信\": \"com.tencent.mm\"},\n" +
                    "  \"TEST_ENV\": \"production\",\n" +
                    "  \"RETRY\": \"2\",\n" +
                    "  \"CASE_TAG\": \"\",\n" +
                    "  \"LOG\": \"false\",\n" +
                    "  \"SCREENSHOT\": \"true\",\n" +
                    "  \"SCREEN_RECORD\": \"true\",\n" +
                    "  \"MP42GIF\": \"false\",\n" +
                    "  \"ALARM_MSG\": \"false\",\n" +
                    "  \"SCREEN_LOCK_PW\": \"0000\",\n" +
                    "  \"OFFLINE\": \"false\",\n" +
                    "  \"SERVER_IP\":\"127.0.0.1\",\n" +
                    "  \"SERVER_PORT\":\"9999\",\n" +
                    "  \"TABLE\": \"test_cases\",\n" +
                    "  \"POST_RESULT\": \"true\",\n" +
                    "  \"TARGET_APP\": \"\",\n" +
                    "  \"RECORD_MEMINFO\":\"true\",\n" +
                    "  \"RECORD_CURRENT\":\"true\",\n" +
                    "  \"POP_WINDOW_LIST\":[\"同意并继续\", \"允许\", \"确定\", \"同意\", \"继续\", \"好\", \"暂不升级\", \"跳过\", \"立即体验\", \"知道了\", \"我知道了\", \"更新\", \"立即开通\", \"我同意\", \"继续安装\", \"接受\", \"以后再说\", \"同意并使用\", \"您已阅读并同意\", \"同意并加入\"],\n" +
                    "  \"DEBUG\":\"true\",\n" +
                    "  \"CHECK_TYPE\": 1,\n" +
                    "  \"MYSQL\": {\n" +
                    "    \"server_ip\":\"127.0.0.1\",\n" +
                    "    \"port\":\"3306\",\n" +
                    "    \"database\":\"\",\n" +
                    "    \"table\":\"\",\n" +
                    "    \"url\": \"jdbc:mysql://127.0.0.1:3306/test_mp?useUnicode=true&characterEncoding=UTF-8\",\n" +
                    "    \"user\": \"user\",\n" +
                    "    \"password\": \"pwd\"\n" +
                    "  }\n" +
                    "}";
            try {
                FileUtils.writeFile(CONFIG_FILE, configContent, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


        //检查测试辅助应用是否安装
        if (!AppUtils.isApkInstalled(this, "com.kevin.testassist")||!AppUtils.isApkInstalled(this, "com.kevin.testassist.test")){

            AlertDialog.Builder installDialog =
                    new AlertDialog.Builder(MainActivity.this);
            final View dialogView = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.dialog_layout1,null);

            installDialog.setTitle("提示：");
            installDialog.setView(dialogView);
            installDialog.setPositiveButton("我已安装", null);
            installDialog.setNegativeButton("取消", null);
            installDialog.show();
            Button ins1 = dialogView.findViewById(R.id.btn1);
            Button ins2 = dialogView.findViewById(R.id.btn2);
            if (!AppUtils.isApkInstalled(MainActivity.this, "com.kevin.testassist")){
                ins1.setText("点击安装“测试辅助”应用");
                ins1.setTextColor(Color.parseColor("#FFF44336"));
            } else {
                ins1.setText("已安装“测试辅助”应用，点击重装");
                ins1.setTextColor(Color.parseColor("#FF4CAF50"));
            }
            if (!AppUtils.isApkInstalled(MainActivity.this, "com.kevin.testassist.test")){
                ins2.setText("点击安装“测试辅助-test”");
                ins2.setTextColor(Color.parseColor("#FFF44336"));
            } else {
                ins2.setText("已安装“测试辅助-test”，点击重装");
                ins2.setTextColor(Color.parseColor("#FF4CAF50"));
            }
            ins1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent ins = new Intent(MainActivity.this, InstallService.class);
                    ins.putExtra("APK_URL", "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testassist.apk");
                    startService(ins);
                }
            });

            ins2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent ins = new Intent(MainActivity.this, InstallService.class);
                    ins.putExtra("APK_URL", "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/testassist-test.apk");
                    startService(ins);
                }
            });


        }


        if (!AdbUtils.isAdbEnable()){
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示：")
                    .setMessage("请先开启root权限, 或 usb连接电脑 输入\"adb tcpip 5555\"执行测试")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button selectAll = findViewById(R.id.selectAll);
//        Button conSelect = findViewById(R.id.conSelect);
        Button monitorTask = findViewById(R.id.startMonitor);
        Button debugTask = findViewById(R.id.startDebug);

        final Button startTest = findViewById(R.id.startTest);

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
//                dataChanged();
                        mAdapter.notifyDataSetChanged();
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
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
//        conSelect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // 遍历list的长度，将已选的设为未选，未选的设为已选
//                for (int i = 0; i < list_item.size(); i++) {
//                    if (MyAdapter.getIsSelected().get(i)) {
//                        MyAdapter.getIsSelected().put(i, false);
////                        checkNum--;
//                    } else {
//                        MyAdapter.getIsSelected().put(i, true);
//                    }
//
//                }
//                // 刷新listview和TextView的显示
////                dataChanged();
//                mAdapter.notifyDataSetChanged();
//            }
//        });

        monitorTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (monitorTask.getText().toString().equals("监控任务")) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
                    } else {

                        startService(new Intent(MainActivity.this, FloatingWindowService.class));
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
                }
            }
        });

        debugTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debugTask.getText().toString().equals("调试任务")) {
                    Intent intent_debug = new Intent(MainActivity.this, MonitorService.class);
                    intent_debug.setAction("com.kevin.testool.task");
                    intent_debug.putExtra("IS_MONITOR", true);
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
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                    Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                    while(iter.hasNext()){
                        ActivityManager.RunningAppProcessInfo next = iter.next();
                        String pricessName = getPackageName() + ":MonitorService"; //需要在manifest文件内定义android:process=":MyIntentService"
                        if(next.processName.equals(pricessName)){
                            killProcess(next.pid);
                            Intent intent_monitor = new Intent();
                            intent_monitor.setAction("com.kevin.testool.action.monitor.finish");
                            sendBroadcast(intent_monitor);
                            break;
                        }
                    }

                    debugTask.setText("调试任务");
                    debugTask.setTextColor(Color.parseColor("#000000"));
                }
            }
        });



        startTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    //配置弹框
                    try {
                        configDialog().setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //
                                ArrayList<String> selected_cases = new ArrayList<String>();
                                for (int i = 0; i < list_item.size(); i++) {
                                    if (MyAdapter.getIsSelected().get(i)) {
                                        mAdapter.getItemId(i);

                                        String text = list_item.get(i).split("\\.")[1].trim();
                                        selected_cases.add(text);//过滤被选中的测试用例
                                    }

                                }
                                if (selected_cases.size() > 0) {
                                    try {
                                        casetag = Common.CONFIG().getString("CASE_TAG");
                                    } catch (JSONException e) {
                                        logUtil.d("", "config.json中无 CASE_TAG");
                                    }
                                    Intent intent_start = new Intent(MainActivity.this, MyIntentService.class);
                                    intent_start.setAction("com.kevin.testool.action.run");
                                    intent_start.putStringArrayListExtra("SELECTED_CASES", selected_cases);
                                    intent_start.putExtra("CASE_TAG", casetag);
                                    intent_start.putExtra("TEST_ENV", test_env);
                                    intent_start.putExtra("LOOP", loopNum);
                                    logUtil.d("DEBUG", selected_cases.toString());
                                    //                    intent_start.putExtra("","");
                                    //                    Toast.makeText(getApplicationContext(), "测试已启动", Toast.LENGTH_SHORT).show();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent_start);
                                    } else {
                                        startService(intent_start);
                                    }
                                    moveTaskToBack(true); //隐藏activity到后台
                                } else {
                                    String TARGET_APP = "";
                                    try {
                                        TARGET_APP = CONFIG().getString("TARGET_APP");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        Common.switchTestEnv(TARGET_APP, test_env);
                                    } catch (Exception ignore) {
                                        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                                        am.killBackgroundProcesses(TARGET_APP);
                                    }
                                    Toast.makeText(getApplicationContext(), "请选择测试用例！", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }).setNegativeButton("取消", null).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });

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
//                MyFile.unzip(LOGPATH + "testcases.zip", LOGPATH+"/"+"testcases");
                ToastUtils.showShort(context,"文件下载完成~");
            } catch (Exception e) {
                e.printStackTrace();
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
            } else {
                goAccess();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_syc) {
            Intent intent = new Intent(this, MyIntentService.class);
            intent.setAction("com.kevin.testool.action.testcases.syc");
            startService(intent);

        } else if (id == R.id.record_case){
            startActivity(new Intent(MainActivity.this, RecordCaseActivity.class));

        } else if (id == R.id.nav_update) {
            updateCaseList();

        } else if (id == R.id.nav_report) {
            startActivity(new Intent(MainActivity.this, ReportActivity.class));
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            Uri uri = Uri.parse(CONST.LOGPATH);
//            intent.setDataAndType(uri, "*/*");
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            startActivity(intent);

        } else if (id == R.id.nav_uicrawler) {
            startActivity(new Intent(MainActivity.this, UICrawlerActivity.class));

        } else if (id == R.id.nav_adb) {
            startActivity(new Intent(MainActivity.this, WirelessAdb.class));
        } else if (id == R.id.add_new){
            startActivity(new Intent(MainActivity.this, EditCaseActivity.class));
        }else if (id == R.id.nav_monkey){
            startActivity(new Intent(MainActivity.this, MonkeyTestActivity.class));
        }else if (id == R.id.nav_log){
            Common.generateBugreport(CONST.LOGPATH + "bugreport_" + DateTimeUtils.getCurrentDateTimeString() + ".txt");
            ToastUtils.showShort(MainActivity.this, "正在抓取...，稍后请在autotest文件夹下查看");
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
                    // 权限被用户同意，可以去放肆了。
                } else {
                    // 权限被用户拒绝了，洗洗睡吧。
                }
                return;
            }
        }
    }

    private void downloadResource(String url){
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
//        System.out.println(Uri.parse(url));
        String fileName =  url.substring(url.lastIndexOf("/") + 1);
        request.setDestinationInExternalPublicDir(AUTOTEST, fileName);
        FileUtils.deleteFile(CONST.LOGPATH + fileName );
        request.setNotificationVisibility(VISIBILITY_VISIBLE);
        request.setTitle("下载");
        request.setDescription("正在下载资源");
//        request.setAllowedOverRoaming(false);

        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
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
        configDialog.setTitle("Settings:");
        configDialog.setView(dialogView);
        configDialog.setNegativeButton("取消", null);
//        configDialog.show();
        // 获取当前config配置
//        JSONObject configNow = new JSONObject(MyFile.readJsonFile(CONST.CONFIG_FILE));
        Rbgroup.clearCheck();
        Rbgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
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
                    e.printStackTrace();
                }
            }
        });

        //测试设置：
        final Switch bugreport_switch = dialogView.findViewById(R.id.log);
        final Switch screenshot_switch =  dialogView.findViewById(R.id.screenshot);
        final Switch post_switch = dialogView.findViewById(R.id.post_result);
        final Switch alarm_msg = dialogView.findViewById(R.id.alarm_msg);
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
        if (!Common.CONFIG().isNull("ALARM_MSG") && Common.CONFIG().getString("ALARM_MSG").equals("true")) {
            alarm_msg.setChecked(true);
        } else{
            alarm_msg.setChecked(false);
        }
        alarm_msg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("ALARM_MSG", "true"));
                    } else {
                        FileUtils.editJsonFile(CONFIG_FILE, new JSONObject().put("ALARM_MSG", "false"));
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
                    e.printStackTrace();
                }

            }
        });
//        branch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                System.out.println(v.getText());
//                return false;
//            }
//        });
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
                    e.printStackTrace();
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
                    e.printStackTrace();
                }

            }
        });
        // 循环次数
        final EditText loop = dialogView.findViewById(R.id.input_loop);
//        loopNum = Integer.parseInt(loop.getText().toString());
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

//        //Mysql数据库配置
//        EditText dbUrl = dialogView.findViewById(R.id.dbUrl);
//        EditText dbName = dialogView.findViewById(R.id.dbName);
//        EditText dbUser = dialogView.findViewById(R.id.dbUser);
//        EditText dbPassword = dialogView.findViewById(R.id.dbPassword);
//        JSONObject mysql = null;
//        try {
//            mysql = Common.CONFIG().getJSONObject("MYSQL");
//            dbUrl.setText(String.format("%s: %s", mysql.getString("server_ip"), mysql.getString("port")));
//            dbName.setText(mysql.getString("database"));
//            dbUser.setText(mysql.getString("user"));
//            dbPassword.setText(mysql.getString("password"));
//        } catch (JSONException e) {
//            ToastUtils.showLong(MainActivity.this, "请检查config.json文件中的MYSQL配置");
//        }

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
                String fp = LOGPATH + "config.json";
                String res = FileUtils.readJsonFile(fp);
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
                        try {
                            FileUtils.writeFile(fp, et.getText().toString(), false);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
                    editWindow.setNegativeButton("取消", null);
                    editWindow.show();
                });
                popWindow.setNegativeButton("关闭", null);
                //对话框显示
                popWindow.show();
            }
        });
        return configDialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(MainActivity.this, FloatingWindowService.class));
            }
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

//            ToastUtils.showShort(this, "完成");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dl != null) {
            unregisterReceiver(dl);
        }
    }

}
