package com.kevin.testool;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ArrayList<String> list_view;
    private int checkNum; // 记录选中的条目数量
    private MyAdapter mAdapter;
    private BufferedWriter bw;
    private SimpleDateFormat sdf;
    private MyFile myFile;

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

        Button selectAll = findViewById(R.id.selectAll);
        Button conSelect = findViewById(R.id.conSelect);
        final Button startTest = findViewById(R.id.startTest);

        //启动com.kevin.testcases，获取测试用例
//        Intent intent = new Intent(getPackageManager().getLaunchIntentForPackage("com.kevin.testcases"));
//        startActivity(intent);
        final Intent intent = getIntent();
        String[] cases_list = intent.getStringArrayExtra("test_case");//获取测试用例
        if (cases_list == null) {
            Toast.makeText(getApplicationContext(), "导入用例失败", Toast.LENGTH_SHORT).show();
        } else {
            list_view = new ArrayList<String>();
            list_view.addAll(Arrays.asList(cases_list));
            ListView list_test = (ListView) findViewById(R.id.list_test);
            mAdapter = new MyAdapter(list_view, this);
            list_test.setAdapter(mAdapter);
        }
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < list_view.size(); i++) {
                    MyAdapter.getIsSelected().put(i, true);
                }
                // 数量设为list的长度
                checkNum = list_view.size();
                Log.d("KEVIN", "checkNum: " + checkNum);
                // 刷新listview和TextView的显示
//                dataChanged();
                mAdapter.notifyDataSetChanged();
            }
        });
        conSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 遍历list的长度，将已选的设为未选，未选的设为已选
                for (int i = 0; i < list_view.size(); i++) {
                    System.out.println(MyAdapter.getIsSelected().get(i));
                    if (MyAdapter.getIsSelected().get(i)) {
                        MyAdapter.getIsSelected().put(i, false);
//                        checkNum--;
                    } else {
                        MyAdapter.getIsSelected().put(i, true);
//                        checkNum++;
                    }

                }
                // 刷新listview和TextView的显示
//                dataChanged();
                mAdapter.notifyDataSetChanged();
            }
        });
        startTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> selected_cases = new ArrayList<String>();
                for (int i = 0; i < list_view.size(); i++) {
                    if (MyAdapter.getIsSelected().get(i)) {
                        System.out.println(i);

                        String text = list_view.get(i);
                        selected_cases.add(text);//过滤被选中的测试用例
                    }

                }
                if (selected_cases.size() > 0) {
                    Intent intent_start = new Intent(MainActivity.this, MyService.class);
                    intent_start.setAction("com.kevin.testool.MyService");
                    intent_start.putStringArrayListExtra("SELECTED_CASES", selected_cases);
                    intent_start.putExtra("","");
//                    Toast.makeText(getApplicationContext(), "测试已启动", Toast.LENGTH_SHORT).show();
                    startService(intent_start);
                    //创建log文件夹
                    String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
                    myFile = new MyFile();
                    try {
                        myFile.createTempFile(tempFile, myFile.creatLogDir());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "请选择测试用例！", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    // 定义一个变量，来标识是否退出
//    private static boolean isExit = false;
//    @SuppressLint("HandlerLeak")
//    Handler mHandler = new Handler() {
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            isExit = false;
//        }
//    };
    //双击back键退出应用
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            exit();
//            return false;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    private void exit() {
//        if (!isExit) {
//            isExit = true;
//            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
//            // 利用handler延迟发送更改状态信息
//            mHandler.sendEmptyMessageDelayed(0, 2000);
//        } else {
//            SystemClock.sleep(500);
//            finish();
//            System.exit(0);
//        }
//    }
//    private static boolean isExit = false;
//
//    @SuppressLint("HandlerLeak")
//    Handler mHandler = new Handler() {
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            isExit = false;
//        }
//    };

    //提示框后选择是否退出
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            exit();
//            return false;
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    private void exit() {
        AlertDialog.Builder popWindow = new AlertDialog.Builder(MainActivity.this);
        //设置对话框标题
        popWindow.setTitle("消息提醒");
        //设置对话框消息
        popWindow.setMessage("确定要退出测试吗？");
        // 添加选择按钮并注册监听
        popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        popWindow.setNegativeButton("取消", null);
        //对话框显示
        popWindow.show();
    }

//    DialogInterface.OnClickListener diaListener = new DialogInterface.OnClickListener() {
//
//        @Override
//        public void onClick(DialogInterface dialog, int buttonId) {
//            // TODO Auto-generated method stub
//            switch (buttonId) {
//                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
//                    finish();
//                    break;
//                case AlertDialog.BUTTON_NEGATIVE:// "确认"按钮退出程序
//                    //什么都不做
//                    break;
//                default:
//                    break;
//            }
//        }
//    }

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_import) {

            Intent intent = getIntent();
            String[] cases_list = intent.getStringArrayExtra("test_case");//获取测试用例
            if(cases_list == null){
                Toast.makeText(getApplicationContext(), "导入用例失败", Toast.LENGTH_SHORT).show();
            }else {
                list_view = new ArrayList<String>();
                list_view.addAll(Arrays.asList(cases_list));
                ListView list_test = (ListView) findViewById(R.id.list_test);
                mAdapter = new MyAdapter(list_view, this);
                list_test.setAdapter(mAdapter);
            }
//            SystemClock.sleep(2 * 1000);

                //启动com.kevin.testcases，获取测试用例
//                Intent intent = new Intent(getPackageManager().getLaunchIntentForPackage("com.kevin.testcases"));
//                startActivity(intent);




        } else if (id == R.id.nav_report) {
            //打开log文件路径
            String logFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest";
//            File file = new File(logFile);
//            File parentFlie = new File(file.getParent());
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(logFile);
            intent.setDataAndType(uri, "*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            startActivity(Intent.createChooser(intent, "文件管理"));
            startActivity(intent);

        } else if (id == R.id.nav_power) {

        } else if (id == R.id.nav_memery) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    //创建文件

    @SuppressLint("SimpleDateFormat")
    private File creatLogFile() throws FileNotFoundException {
        sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        String time_tag = sdf.format(new Date(System.currentTimeMillis()));
        File ssDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "AutoTest" + File.separator + time_tag+ File.separator +"screenshot");
        ssDir.mkdirs();
        String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
        FileOutputStream fos = new FileOutputStream(new File(tempFile),true);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        bw = new BufferedWriter(osw);
        try {
            bw.write(time_tag);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
