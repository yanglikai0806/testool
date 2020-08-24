package com.kevin.testool.service;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kevin.share.CONST;
import com.kevin.share.utils.FileUtils;
import com.kevin.testool.MyIntentService;
import com.kevin.testool.MainActivity;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.share.utils.AdbUtils;
import com.kevin.testool.utils.BatteryManagerUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 测试任务服务监控
 */
public class MonitorService extends IntentService {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static Date date = new Date();
    private static int errorCount = 0;
    private static boolean isIdle = false;
    private static final int NOTIFICATION_ID = 0x2;
    private NotificationCompat.Builder builder;

    private static final String ACTION_MONITOR_COVERAGE_RUNNING = "com.kevin.testool.coverage.running";
    private static final String ACTION_MONITOR_COVERAGE_UPLOAD = "com.kevin.testool.coverage.upload";
    private static final String ACTION_MONITOR_TASK = "com.kevin.testool.task";

    private static boolean isDebug = false;
    private static boolean isDumpTask = false;

    public MonitorService() {
        super("MonitorService");
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
                    .setContentText(getString(R.string.service_text3)).build();

            startForeground(NOTIFICATION_ID, notification);
        }else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text3))
                    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    @Override
    protected void onHandleIntent(final Intent intent) {
        final String dt = dateFormat.format(date);
        final String pkg;
        final Boolean isMonitor = intent.getBooleanExtra("IS_MONITOR", false);
        isDebug = intent.getBooleanExtra("IS_DEBUG", false);
        final Boolean isAuto = intent.getBooleanExtra("IS_AUTO", false);
        Log.i("Monitor", "开启monitor");
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_MONITOR_COVERAGE_RUNNING:
                    pkg = intent.getStringExtra("PKG");
                    while (isMonitor) {
                        if (isAuto){
//                        String res = AdbUtils.runShellCommand("dumpsys activity | grep \"#0 ActivityRecord\"", 0);
                        String res = Common.getActivity();
                        if (res.contains(pkg)) {
                            logUtil.d("Monitor", "---------monitor is running------");
                            //发送广播生成ec文件
                            SystemClock.sleep(60000);
                            Intent intent2 = new Intent();
                            intent2.setAction("com.kevin.testool.coverage.finish");
                            sendBroadcast(intent2);
                        } else {
//                            重新启动
                            try {
                                Common.launchActivity(res);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        } else {
                            //发送广播生成ec文件
                            SystemClock.sleep(60000);
                            Intent intent2 = new Intent();
                            intent2.setAction("com.kevin.testool.coverage.finish");
                            sendBroadcast(intent2);
                        }
                    }
                    break;
                case ACTION_MONITOR_COVERAGE_UPLOAD:
                    pkg = intent.getStringExtra("PKG");
                    final int res;
                    final String filePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "coverage.ec";
                    final String fileName = String.format("coverage_%s_%s.ec", pkg, Common.getVersionCode(getApplicationContext(), pkg));
                    try {
                        res = Common.postFile(CONST.URL_PORT+"api/ec", filePath, fileName);

                        Handler mHandler = new Handler(getMainLooper());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtils.showShort(getApplicationContext(), "response code："+res);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case ACTION_MONITOR_TASK:
                    //上传设备执行状态
                    if (!isIdle()){
                        Common.postJson(CONST.URL_PORT + "client_test/devices_state", getDeviceStatusInfo().toString());
                    }
                    while (isMonitor) {
                        //上传设备状态
                        if (!isIdle()){
                            SystemClock.sleep(5000);
                            continue;
                        } else {
                            Common.postJson(CONST.URL_PORT + "client_test/devices_state", getDeviceStatusInfo().toString());
                            try {
                                JSONObject task = getRetryTask();
                                if (task.getInt("code") != 200){
                                    logUtil.d("", "获取任务失败："+task.getInt("code"));
                                } else {
                                    JSONArray testcases = task.getJSONArray("data");
                                    if (testcases.length() == 0){
                                        SystemClock.sleep(5000);
                                        continue;
                                    }

                                    Intent intent_start = new Intent(this, MyIntentService.class);
                                    if (testcases.length() > 0 && testcases.getJSONObject(0).getString("case_type").equals("dump")){
                                        isDumpTask = true;
                                    }


                                    if (isDebug){ //开启debug任务
                                        logUtil.d("", "***********START DEBUG TASK***********");
                                        if (isDumpTask){
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String activity = Common.getActivity();
                                                    try {
                                                        String activityTxt = CONST.LOGPATH + Common.getSerialno() + ".txt";
                                                        FileUtils.writeFile(activityTxt, activity, false);
                                                        uploadFile(activityTxt, CONST.URL_PORT + "upload");
                                                    } catch (FileNotFoundException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }).start();

                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    String xmlName = CONST.DUMP_PATH.replace("window_dump", Common.getSerialno());
                                                    String pngName = CONST.DUMP_PNG_PATH.replace("window_dump", Common.getSerialno());
                                                    FileUtils.deleteFile(xmlName);
                                                    FileUtils.deleteFile(pngName);
                                                    if (Common.get_elements(true, "", "", 0).size() > 0) {
                                                        new File(CONST.DUMP_PATH).renameTo(new File(xmlName));
                                                        uploadFile(xmlName, CONST.URL_PORT + "upload");
                                                        SystemClock.sleep(2000);
                                                        new File(CONST.DUMP_PNG_PATH).renameTo(new File(pngName));
                                                        uploadFile(pngName, CONST.URL_PORT + "upload");
                                                        ToastUtils.showShortByHandler(getApplicationContext(), "界面文件上传完成");
                                                    } else {
                                                        ToastUtils.showShortByHandler(getApplicationContext(), "获取界面失败");
                                                    }
                                                }
                                            }).start();
                                        } else {
                                            intent_start.setAction("com.kevin.testool.action.debug");
                                            String jsonStr = testcases.getJSONObject(0).getString("test_case");
                                            JSONObject mTestcase = new JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1));
                                            intent_start.putExtra("DEBUG_CASE", mTestcase.toString());
                                        }
                                    } else { // 开启监控任务
                                        intent_start.setAction("com.kevin.testool.action.run.retry");
                                        intent_start.putExtra("RETRY_CASES", testcases.toString());
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent_start);
                                    } else {
                                        startService(intent_start);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (isDumpTask){
                            SystemClock.sleep(1000);
                        } else {

                            SystemClock.sleep(15000);
                        }
                    }
                    break;
            }
        }


    }
    private boolean isIdle(){
        //判断是否有任务进程运行
        isIdle = true;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
        while(iter.hasNext()){
            ActivityManager.RunningAppProcessInfo next = iter.next();
            String mProcess = getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
            if(next.processName.equals(mProcess)){
                isIdle = false;
                break;
            }
        }
        return isIdle;
    }

    /**
     *
     * @return JSONObject 获取设备状态信息
     */
    private JSONObject getDeviceStatusInfo(){
        JSONObject deviceStatusInfo = new JSONObject();
        try {
            deviceStatusInfo.put("idle", String.valueOf(isIdle));
            deviceStatusInfo.put("device_name",Common.getDeviceAlias());
            deviceStatusInfo.put("device_id", Common.getSerialno());
            deviceStatusInfo.put("battery", BatteryManagerUtils.getBatteryCapacity());
            String targetApp = "";
            if (!Common.CONFIG().isNull("TARTGET_APP") && Common.CONFIG().getString("TARTGET_APP").length() > 0) {
               targetApp = Common.CONFIG().getString("TARTGET_APP");
            }
            deviceStatusInfo.put("apk_version", Common.getVersionName(MyApplication.getContext(), targetApp));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return deviceStatusInfo;
    }

    /**
     * 获取重试任务
     * @return JSONObject {"code":200, "data":[{}]}
     * @throws JSONException
     */
    private JSONObject getRetryTask() throws JSONException {
        JSONObject data = new JSONObject();
        try {
            data.put("idle", String.valueOf(isIdle()));
            data.put("device_id", Common.getSerialno());
            String targetApp = "";
            if (!Common.CONFIG().isNull("TARTGET_APP") && Common.CONFIG().getString("TARTGET_APP").length() > 0) {
                targetApp = Common.CONFIG().getString("TARTGET_APP");
            }
            data.put("apk", targetApp);
            data.put("apk_version", Common.getVersionName(MyApplication.getContext(), targetApp));
            if (isDebug) {
                data.put("debug", isDebug);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject(Common.postResp(CONST.URL_PORT + "client_test/test_retry", data.toString()));

    }

    public static void uploadFile(String filePath, String url){
        File mFile = new File(filePath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("headimg", mFile.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), mFile))
                .build();

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url(url)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            logUtil.d("uploadFile", response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
            logUtil.d("uploadFile", e.toString());

        }
    }
}
