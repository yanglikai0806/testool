package com.kevin.testool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.util.Log;
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
import android.widget.TimePicker;
import android.widget.Toast;

import com.kevin.testool.common.Common;
import com.kevin.testool.utils.AdbUtils;
import com.kevin.testool.utils.AppUtils;
import com.kevin.testool.utils.DateTimeUtils;
import com.kevin.testool.utils.ToastUtils;
import com.kevin.testool.utils.logUtil;

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

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.os.Process.killProcess;
import static com.kevin.testool.CONST.AUTOTEST;
import static com.kevin.testool.CONST.CONFIG_FILE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ArrayList<String> list_item;
    private int checkNum; // 记录选中的条目数量
    private MyAdapter mAdapter;
    private BufferedWriter bw;
    private SimpleDateFormat sdf;
    private MyFile myFile;
    private JSONArray fileList;
    private dlReceiver dl;
    private sycReceiver syc;
    private String test_env="";
    private String casetag = "";
    private int loopNum = 1;


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
        if (!AdbUtils.hasRootPermission()){
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
        Button conSelect = findViewById(R.id.conSelect);
        Button setTime = findViewById(R.id.set_time);

        final Button startTest = findViewById(R.id.startTest);

        //config文件加载
        if (!new File(CONST.CONFIG_FILE).exists()) {
            //todo 自动加载config文件
//            downloadResource("");
            //初始化config文件
            String content = "{  \n" +
                    "  \"APP\" : {\n" +
                    "    \"微信\": \"com.tencent.mm\",\n" +
                    "},  \n" +
                    " \"TEST_ENV\": \"production\",\n" +
                    " \"RETRY\": 2, \n" +
                    " \"CASE_TAG\": \"\",\n" +
                    " \"LOG\": \"true\",\n" +
                    " \"SCREENSHOT\": \"true\",\n" +
                    " \"ALARM_MSG\": \"false\",\n" +
                    " \"SCREEN_LOCK_PW\": \"0000\",\n" +
                    " \"CHECK_TYPE\"：1,\n" +
                    " \"POST_RESULT\": \"false\",\n" +
                    " \"MYSQL\": {\n" +
                    "    \"url\": \"jdbc:mysql://your.mysql.ip/your_table?useUnicode=true&characterEncoding=UTF-8\",  \n" +
                    " \"user\": \"user_name\",  \n" +
                    " \"password\": \"your_pw\"  \n" +
                    "  }  \n" +
                    "}";
            try {
                MyFile.writeFile(CONFIG_FILE, content,false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        //加载测试用例
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            ArrayList<String> cases_list = Common.getCaseList(MainActivity.this);
            if (cases_list != null) {
                list_item = new ArrayList<String>();
                for (int i = 0; i < cases_list.size(); i++) {
                    list_item.add((i + 1) + ". " + cases_list.get(i));
                }
                ListView list_view = findViewById(R.id.list_test);
                mAdapter = new MyAdapter(list_item, this);
                list_view.setAdapter(mAdapter);
            }

            selectAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int i = 0; i < list_item.size(); i++) {
                        MyAdapter.getIsSelected().put(i, true);
                    }
                    // 数量设为list的长度
                    checkNum = list_item.size();
                    Log.d("KEVIN", "checkNum: " + checkNum);
                    // 刷新listview和TextView的显示
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
        conSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 遍历list的长度，将已选的设为未选，未选的设为已选
                for (int i = 0; i < list_item.size(); i++) {
                    System.out.println(MyAdapter.getIsSelected().get(i));
                    if (MyAdapter.getIsSelected().get(i)) {
                        MyAdapter.getIsSelected().put(i, false);
                    } else {
                        MyAdapter.getIsSelected().put(i, true);
                    }

                }
                // 刷新listview和TextView的显示
                mAdapter.notifyDataSetChanged();
            }
        });

        setTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        houre = hourOfDay;
                        MainActivity.this.minute = minute;
                    }
                }, 15, 20, true).show();

            }
//                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
//
//
//                    @Override
//                    public void onDateSet(DatePicker view, int year, int monthOfYear,
//                                          int dayOfMonth) {
//                        MainActivity.this.year = year;
//                        month = monthOfYear;
//                        day = dayOfMonth;
//
//                    }
//                }, 2016, 10, 8).show();
////                showDate();
//
//            }

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
                                    //创建log文件夹
                                    moveTaskToBack(true); //隐藏activity到后台
                                } else {
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

    public static class MainReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

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
                    String pricessName = getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
                    if(next.processName.equals(pricessName)){
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear_log) {
            new android.app.AlertDialog.Builder(MainActivity.this).setTitle("警告！")
                    .setMessage("历史LOG文件将全部被清除")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MyFile.RecursionDeleteFile(new File(CONST.REPORT_PATH));
                            ToastUtils.showShort(MainActivity.this, "log清除完成");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_syc) {
            Intent intent = new Intent(this, MyIntentService.class);
            intent.setAction("com.kevin.testool.action.testcases.syc");
            startService(intent);

        } else if (id == R.id.nav_update) {
            ArrayList<String> cases_list = Common.getCaseList(MainActivity.this);
            list_item = new ArrayList<String>();
            if (cases_list != null) {
                for (int i = 0; i < cases_list.size(); i++) {
                    list_item.add((i + 1) + ". " + cases_list.get(i));
                }
                ListView list_test = findViewById(R.id.list_test);
                mAdapter = new MyAdapter(list_item, this);
                list_test.setAdapter(mAdapter);

                ToastUtils.showShort(this, "导入成功");
            }
        } else if (id == R.id.nav_report) {
            startActivity(new Intent(MainActivity.this, ReportActivity.class));
        } else if (id == R.id.nav_share) {
            about();
        } else if (id == R.id.nav_adb) {
            startActivity(new Intent(MainActivity.this, WirelessAdb.class));
        } else if (id == R.id.add_new){
            startActivity(new Intent(MainActivity.this, EditCaseActivity.class));
        } else if (id == R.id.nav_monkey){
            startActivity(new Intent(MainActivity.this, MonkeyTestActivity.class));
        } else if (id == R.id.nav_log){
            Common.generateBugreport(CONST.LOGPATH + "bugreport_" + DateTimeUtils.getCurrentDateTimeString() + ".txt");
            ToastUtils.showShort(MainActivity.this, "正在抓取...，稍后请在autotest文件夹下查看");
        } else if (id == R.id.nav_uicrawler) {
            startActivity(new Intent(MainActivity.this, UICrawlerActivity.class));
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
                } else {
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
        MyFile.deleteFile(CONST.LOGPATH + fileName );
        request.setNotificationVisibility(VISIBILITY_VISIBLE);
        request.setTitle("下载");
        request.setDescription("正在下载资源");
//        request.setAllowedOverRoaming(false);
        //注册广播 接收下载完成的广播
        dl = new dlReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(dl, filter);

        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
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

    public static class sycReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
                ToastUtils.showShort(context,"用例同步成功~");

        }
    }

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
                    MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("TEST_ENV", test_env));
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

        if (Common.CONFIG().getString("LOG").equals("true")) {
            bugreport_switch.setChecked(true);
        } else{
            bugreport_switch.setChecked(false);
        }
        bugreport_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("LOG", "true"));
                    } else {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("LOG", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        if (Common.CONFIG().getString("SCREENSHOT").equals("true")) {
            screenshot_switch.setChecked(true);
        } else{
            screenshot_switch.setChecked(false);
        }
        screenshot_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREENSHOT", "true"));
                    } else {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("SCREENSHOT", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        if (Common.CONFIG().getString("POST_RESULT").equals("true")) {
            post_switch.setChecked(true);
        } else{
            post_switch.setChecked(false);
        }
        post_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("POST_RESULT", "true"));
                    } else {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("POST_RESULT", "false"));
                    }
                } catch (Exception ignored){

                }
            }
        });
        if (Common.CONFIG().getString("ALARM_MSG").equals("true")) {
            alarm_msg.setChecked(true);
        } else{
            alarm_msg.setChecked(false);
        }
        alarm_msg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("ALARM_MSG", "true"));
                    } else {
                        MyFile.editJsonFile(CONFIG_FILE, new JSONObject().put("ALARM_MSG", "false"));
                    }
                } catch (Exception ignored){

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
                    MyFile.editJsonFile(CONST.CONFIG_FILE, new JSONObject().put("RETRY", s));
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
                    MyFile.editJsonFile(CONST.CONFIG_FILE, new JSONObject().put("CASE_TAG", s));
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

        //Mysql数据库配置
        EditText dbUrl = dialogView.findViewById(R.id.dbUrl);
        EditText dbUser = dialogView.findViewById(R.id.dbUser);
        EditText dbPassword = dialogView.findViewById(R.id.dbPassword);
        JSONObject mysql = null;
        try {
            mysql = Common.CONFIG().getJSONObject("MYSQL");
            dbUrl.setText("");
            dbUser.setText("test");
//            dbUrl.setText(String.format("url: %s", mysql.getString("url")));
//            dbUser.setText(String.format("username: %s", mysql.getString("user")));
//            dbPassword.setText(String.format("password:%s",mysql.getString("password")));
            dbPassword.setText(String.format("password: %s","*******"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return configDialog;
    }

    private void about(){
        final String githubUrl = "https://github.com/yanglikai0806/testool.git";
        new android.app.AlertDialog.Builder(MainActivity.this).setTitle("关于")
                .setMessage("当前版本：" + AppUtils.getVersionName(MainActivity.this, getPackageName()) +"\n" + githubUrl)
                .setPositiveButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        // 创建普通字符型ClipData
                        ClipData mClipData = ClipData.newPlainText("Label", githubUrl);
                        // 将ClipData内容放到系统剪贴板里。
                        cm.setPrimaryClip(mClipData);
                        ToastUtils.showLong(MainActivity.this, "已复制github连接到剪贴板");

                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dl != null) {
            unregisterReceiver(dl);
        }

    }

}
