package com.kevin.testool.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kevin.share.CONST;
import com.kevin.share.UICrawler;
import com.kevin.testool.MainActivity;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.Checkpoint;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.kevin.share.CONST.MONKEY_PATH;
import static com.kevin.share.Common.CONFIG;
import static com.kevin.share.UICrawler.UICrawlerConfig;

/**
 * 执行monkey测试，界面遍历测试
 */
public class MonkeyService extends IntentService {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static Date date = new Date();
    private static int errorCount = 0;
    private static final int NOTIFICATION_ID = 0x2;
    private NotificationCompat.Builder builder;

    private static final String ACTION_MONKEY = "com.kevin.testool.monkey";
    private static final String ACTION_UICRAWLER = "com.kevin.testool.monkey.uicrawler";
    private static List<String> errorList = new ArrayList<>();

    public MonkeyService() {
        super("MonkeyService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //开启前台服务避免被杀
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "my_channel_01";
            CharSequence NAME = "my_channel_name";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NAME, NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text2)).build();

            startForeground(NOTIFICATION_ID, notification);
        }else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text2))
                    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    @Override
    protected void onHandleIntent(final Intent intent) {
        String dt = dateFormat.format(date);
        String pkg = intent.getStringExtra("PKG");
        String con;
        String thro;
        String seed;
        String logDir;
        int waitTime = 2000;
        String page = "";
        if (intent.hasExtra("Page")) {
            page = intent.getStringExtra("PAGE");
        }
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_MONKEY:
                    con = intent.getStringExtra("CON");
                    thro = intent.getStringExtra("THRO");
                    seed = intent.getStringExtra("SEED");
                    logDir = MONKEY_PATH + Common.getVersionName(this, pkg) + File.separator + dt + File.separator;
                    int KEEP_TIME = Integer.valueOf(con)*Integer.valueOf(thro);
                    FileUtils.creatDir(logDir);
                    Log.i("Monkey", "开启monkey测试");
                    monkey(pkg, con, thro, seed, logDir);
                    if (pkg.contains("/")) {
                        Common.startActivity(pkg);
                        while (KEEP_TIME > 0) {
                            SystemClock.sleep(waitTime);
                            Common.startActivity(pkg);
                            if (Common.getMonkeyProcess().equals("")) {  //判断monkey是否结束
                                break;
                            }
                            KEEP_TIME = KEEP_TIME - waitTime;
                        }
                    }

                    break;
                case ACTION_UICRAWLER:
                    logDir = CONST.LOGPATH + "uicrawler" + File.separator + Common.getVersionName(this, pkg) + File.separator;
                    FileUtils.creatDir(logDir);
                    int deepth = intent.getIntExtra("DEEPTH", 1);
                    JSONArray pageList = new JSONArray();
                    String ucpage = "";
                    try {
                        pageList = UICrawlerConfig.getJSONArray("PAGE_LIST");
                    } catch (JSONException e) {
                        pageList.put("recommend");
                        e.printStackTrace();
                        logUtil.u("UICrawler", e.toString());
                    }
                    logUtil.u("UICrawler", "开启UICrawler测试, 遍历深度："+deepth);
                    UICrawler uc = new UICrawler(pkg, Common.getVersionName(this, pkg), deepth);
                    if (page.length() == 0) {
                        uc.guideStep("default");
                        long startTime = System.currentTimeMillis();
                        try {
                            uc.crawlerPage(0,0,"default");
                            logUtil.u("UICrawler", "遍历结束，点击次数：" + UICrawler.clickCount);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logUtil.u("UICrawler", "退出遍历，点击次数：" + UICrawler.clickCount);
                            logUtil.u("UICrawler", e.toString());
                        }
                        logUtil.u("UICrawler", "执行时长：" + (System.currentTimeMillis() - startTime)/60000 + "min");

                    } else {
                        uc.guideStep(page);
                        long startTime = System.currentTimeMillis();
                        try {
                            uc.crawlerPage(0,0,page);
                            logUtil.u("UICrawler", "遍历结束，点击次数：" + UICrawler.clickCount);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logUtil.u("UICrawler", "退出遍历，点击次数：" + UICrawler.clickCount);
                            logUtil.u("UICrawler", e.toString());
                        }
                        logUtil.u("UICrawler", "执行时长：" + (System.currentTimeMillis() - startTime)/60000 + "min");
                    }
                    uc.lastStep();

                    break;

            }
        }
    }
    private static void monkey(final String pkg, final String con, final String thro, final String seed, final String logDir){
        final Thread tm = new Thread(){
            @Override
            public void run() {
                Common.runMonkey(pkg, con,thro, seed,logDir);
            }
        };
        tm.start();
        SystemClock.sleep(10000);
        new Thread(){
            @Override
            public void run() {
                monkeyMonitor(tm, logDir, pkg);
            }
        }.start();
    }

    private static void monkeyMonitor(Thread tm, String logDir, String pkg){
        boolean recordMeminfoFlag = true;
        try {
            recordMeminfoFlag = CONFIG().getString("RECORD_MEMINFO").equals("true");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        while (tm.isAlive()) {
            Common.openWifi();  //monkey测试中可能会关闭网络
            try {
                int merrorcont = errorCount;
                int nerrorCount = readMonkeyLog(logDir + "monkey_error.txt", pkg);
                if (nerrorCount - merrorcont >0) {
                    Common.generateBugreport(logDir + "bugreport_" + dateFormat.format(new Date()) + ".txt");
                    errorCount = nerrorCount;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Common.getMonkeyProcess().length()==0){
                break;
            }
            if (!Checkpoint.checkActivity(pkg)){
                Common.launchApp(MyApplication.getContext(), pkg);
            }

            if (recordMeminfoFlag){
                Common.recordMeminfo(pkg, logDir.substring(0, logDir.length()-20) + "meminfo.csv", 5000, 180);
            } else {
                SystemClock.sleep(180000);
            }

        }
    }

    public static int readMonkeyLog(String filePath, String pkg) throws IOException {
        int _errorCount = 0;
//        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        boolean isRealError = false;
        while((line = br.readLine()) != null){
//            logUtil.d("readMonkeyLog:", line);
            if (line.contains("CRASH:") | line.contains("NOT RESPONDING:") && line.contains(pkg)){
                isRealError = true;
                continue;
            }
            if (isRealError){
                if (line.contains("Long Msg:")){
                    String errorMsg = line.split("Long Msg:")[1].trim();
                    if (!errorList.contains(errorMsg)){
                        _errorCount += 1;
                        errorList.add(errorMsg);
                        isRealError = false;
                    }
                }
                if (line.contains("Reason:")){
                    String errorMsg = line.split("Reason:")[1].trim();
                    if (!errorList.contains(errorMsg)){
                        _errorCount += 1;
                        errorList.add(errorMsg);
                        isRealError = false;
                    }
                }
            }
        }
        br.close();
        return _errorCount;
    }
}
