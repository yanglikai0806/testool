package com.kevin.testool;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kevin.testool.common.Common;
import com.kevin.testool.utils.ToastUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.kevin.testool.CONST.MONKEY_PATH;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MonkeyService extends IntentService {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static Date date = new Date();
    private static int errorCount = 0;
    private static final int NOTIFICATION_ID = 0x2;
    private NotificationCompat.Builder builder;

    private static final String ACTION_MONKEY = "com.kevin.testool.monkey";
    private static final String ACTION_MONKEY_VOICEASSIST = "com.kevin.testool.monkey.voiceassist";

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
        final String dt = dateFormat.format(date);
        final String pkg = intent.getStringExtra("PKG");
        final String con = intent.getStringExtra("CON");
        final String thro = intent.getStringExtra("THRO");
        final String seed = intent.getStringExtra("SEED");
//        String timeout = intent.getStringExtra("TIMEOUT");
        System.out.println(seed);
        final String logDir = MONKEY_PATH + Common.getVersionName(this, pkg) + File.separator + dt + File.separator;
        int KEEP_TIME = Integer.valueOf(con)*Integer.valueOf(thro);
//        final Thread tm;
        int waitTime = 2000;
        MyFile.creatDir(logDir);
        Log.i("Monkey", "开启monkey测试");
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_MONKEY:
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
                case ACTION_MONKEY_VOICEASSIST:
                    monkey(pkg, con, thro, seed, logDir);
                    File querysFile = new File(CONST.MONKEY_QUERYS_FILE);
//                    StringBuilder sb = new StringBuilder();
                    if (querysFile.exists()){
                        String line;
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(querysFile));
                            while(true){
                                line = br.readLine();
                                if (line == null){
                                    br = new BufferedReader(new FileReader(querysFile));
                                    line = "打开wifi";
                                }
                                logUtil.d("QUERY", line);
                                //处理音量避免扰民
                                AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                                if (audioManager != null) {
                                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    if (currentVolume > 1) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                                    }
                                }

                                if (Common.getMonkeyProcess().length() == 0){
                                    break;
                                }
                            }
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ToastUtils.showShort(this, "querys.txt 文件不存在");
                    }
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
                monkeyMonitor(tm, logDir);
            }
        }.start();
    }

    private static void monkeyMonitor(Thread tm, String logDir){
        while (tm.isAlive()) {
            Common.openWifi();
            try {
                int merrorcont = errorCount;
                int nerrorCount = readMonkeyLog(logDir + "monkey_error.txt");
                if (nerrorCount - merrorcont >0) {
                    Common.generateBugreport(logDir + "bugreport_" + dateFormat.format(new Date()) + ".txt");
                    errorCount = nerrorCount;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("monitor-------------");
            if (Common.getMonkeyProcess().length()==0){
                break;
            }
            SystemClock.sleep(60000);

        }
    }

    public static int readMonkeyLog(String filePath) throws IOException {
        int _errorCount = 0;
//        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        if (!file.exists()) {
            logUtil.d("MyFile", "文件不存在:" + filePath);
            return 0;
        }
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        while((line = br.readLine()) != null){
            logUtil.d("readMonkeyLog:", line);
            if (line.contains("CRASH:") | line.contains("NOT RESPONDING:")){
                 _errorCount += 1;
            }

        }

        br.close();
        return _errorCount;
    }
}
