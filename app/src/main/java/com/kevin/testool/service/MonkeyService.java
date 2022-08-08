package com.kevin.testool.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.UICrawler;
import com.kevin.testool.activity.MainActivity;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.Checkpoint;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
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
import static com.kevin.share.CONST.TARGET_APP;
import static com.kevin.share.UICrawler.UICrawlerConfig;
import static com.kevin.testool.activity.MonkeyTestActivity.ZipLogFolder;
import static com.kevin.share.utils.HttpUtil.uploadMonkeyLog;

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
    private static final String ACTION_MONKEY_LOG_UPLOAD = "com.kevin.testool.monkey.log.upload";
    private static final String ACTION_UICRAWLER = "com.kevin.testool.monkey.uicrawler";
    private static List<String> errorList = new ArrayList<>();
    private static String pkg = "";
    private static String activity = "";

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
        assert intent != null;
        pkg = intent.getStringExtra("PKG");
        // 对于activity进行处理
        if (pkg.contains("/")){
            activity = pkg;
            pkg = pkg.split("/",2)[0];
        }
        String con;
        String thro;
        String seed;
        String timeout;
        String logDir;
        int waitTime = 20000; // ms
        double KEEP_TIME;
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
                    timeout = intent.getStringExtra("TIMEOUT");
                    logDir = MONKEY_PATH + Common.getVersionName(AppContext.getContext(), pkg) + File.separator + dt + File.separator;
                    if (!TextUtils.isEmpty(timeout)) {
                        KEEP_TIME = Double.valueOf(timeout) * 3600 * 1000; //ms
                    } else {
                        KEEP_TIME = 8 * 3600 * 1000;
                    }
                    FileUtils.creatDir(logDir);
                    Log.i("Monkey", "开启monkey测试");
                    monkey(pkg, con, thro, seed, logDir);

                    while (KEEP_TIME > 0) {
                        Common.muteSound();
                        SystemClock.sleep(waitTime);
                        //如果测试pkg参数对应的是activity页面，则循环启动activity页面
                        if (!TextUtils.isEmpty(activity)) Common.startActivity(activity);

                        if (Common.getMonkeyProcess().equals("")) {  //判断monkey是否结束
                            monkey(pkg, con, thro, seed, logDir);
                        }
                        KEEP_TIME = KEEP_TIME - waitTime;
                    }
                    stopMonkey();
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
                        logUtil.e("", e);
                        logUtil.u("UICrawler", e.toString());
                    }
                    logUtil.u("UICrawler", "开启UICrawler测试, 遍历深度："+deepth);
                    UICrawler uc = new UICrawler(pkg, Common.getVersionName(this, pkg), deepth);
                    if (page.length() == 0) {

                        for (int i=0; i<pageList.length(); i++){
                            try {
                                ucpage = pageList.getString(i);
                            } catch (Exception e) {
                                logUtil.u("UICrawler", e.toString());
                            }
                            uc.guideStep(ucpage);
                            long startTime = System.currentTimeMillis();
                            try {
                                uc.crawlerPage(0,0,ucpage);
                                logUtil.u("UICrawler", "Page: " + ucpage + "Page遍历结束，点击次数：" + UICrawler.clickCount);
                            } catch (Exception e) {
                                logUtil.e("", e);
                                logUtil.u("UICrawler", "退出遍历，点击次数：" + UICrawler.clickCount);
                                logUtil.u("UICrawler", e.toString());
                            }
                            logUtil.u("UICrawler", "执行时长：" + (System.currentTimeMillis() - startTime)/60000 + "min");
                        }

                    } else {
                        uc.guideStep(page);
                        long startTime = System.currentTimeMillis();
                        try {
                            uc.crawlerPage(0,0,page);
                            logUtil.u("UICrawler", "遍历结束，点击次数：" + UICrawler.clickCount);
                        } catch (Exception e) {
                            logUtil.e("", e);
                            logUtil.u("UICrawler", "退出遍历，点击次数：" + UICrawler.clickCount);
                            logUtil.u("UICrawler", e.toString());
                        }
                        logUtil.u("UICrawler", "执行时长：" + (System.currentTimeMillis() - startTime)/60000 + "min");
                    }
                    uc.lastStep();

                    break;
                case ACTION_MONKEY_LOG_UPLOAD:
                    String uploadLogFolder = CONST.MONKEY_PATH + Common.getVersionName(AppContext.getContext(), TARGET_APP);
                    String zipFilePath = uploadLogFolder + ".zip";
                    try {
                        ZipLogFolder(uploadLogFolder, zipFilePath);
                        File zipFile = new File(zipFilePath);
                        if (zipFile.exists()){
                            uploadMonkeyLog(zipFile, CONST.SERVER_BASE_URL + "upload", zipFile.getName(), "zipfile", TARGET_APP);
                        } else {
                            logUtil.d("", "未生成log压缩文件");
                        }
                    } catch (Exception e) {
                        logUtil.e("", e);
                    }
                    break;

            }
        }
    }
    private static void monkey(final String pkg, final String con, final String thro, final String seed, final String logDir){

        Common.runMonkey(pkg, con,thro, seed,logDir);
        new Thread(){
            @Override
            public void run() {
                monkeyMonitor(logDir, pkg);
            }
        }.start();
    }

    private static void monkeyMonitor(String logDir, String pkg){
        logUtil.d("monkey", "正在监听monkey测试");
        boolean recordMeminfoFlag = true;
        try {
            recordMeminfoFlag = CONFIG().getString("RECORD_MEMINFO").equals("true");
        } catch (JSONException e) {
            logUtil.e("", e);
        }
        while (true) {
            Common.openWifi();  //monkey测试中可能会关闭网络
            try {
                int merrorcont = errorCount;
                int nerrorCount = readMonkeyLog(logDir + "monkey_error.txt", pkg);
                if (nerrorCount - merrorcont >0) {
                    Common.generateBugreport2(logDir + "bugreport_" + dateFormat.format(new Date()) + ".txt");
                    errorCount = nerrorCount;
                }
            } catch (IOException e) {
                logUtil.e("", e);
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
        logUtil.d("monkey", "结束monkey测试监听");
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

    private void stopMonkey(){
        while (Common.killProcess("com.android.commands.monkey")){
            SystemClock.sleep(10);
        }
        logUtil.d("", "stop monkey");
        Common.killProcess("com.kevin.testool:uicrawler");
    }



}
