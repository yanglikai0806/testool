package com.kevin.testool.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.kevin.share.CONST;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ShellUtils;
import com.kevin.testool.MyIntentService;
import com.kevin.testool.activity.MainActivity;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.utils.WifiUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.kevin.share.Common.isIdle;

/**
 * 测试任务服务监控
 */
public class MonitorService extends IntentService {

    private static final String TAG = "MonitorService";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static Date date = new Date();
    private static final int NOTIFICATION_ID = 0x2;
    private NotificationCompat.Builder builder;

    private static final String ACTION_MONITOR_TASK = "com.kevin.testool.task";

    private static boolean isDebug = false;

    public MonitorService() {
        super("MonitorService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //开启前台服务避免被杀
        openNotification(getString(R.string.service_text3));
    }

    private void openNotification(String notifyContent){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "my_channel_02";
            CharSequence NAME = "my_channel_name";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NAME, NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(notifyContent).build();

            startForeground(NOTIFICATION_ID, notification);
        }else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(notifyContent)
                    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    @Override
    protected void onHandleIntent(final Intent intent) {
        String dt = dateFormat.format(date);
        String pkg;
        // 监控任务标识
        assert intent != null;
        boolean isMonitor = intent.getBooleanExtra("IS_MONITOR", false);
        // 测试任务标识
        boolean isTest = intent.getBooleanExtra("IS_TEST", false);
        // 启动保活程序
        if (isTest){
            if (ShellUtils.isShellSocketConnect()) {
                ShellUtils.executeSocketShell("###testool.MyIntentService", 0);
            } else {
                logUtil.d(TAG, "shell server未启动,无法执行保活");
            }
        }
        /*
        测试执行模式标志:
        =0 执行动态分配测试,case一次下发量小无需缓存,
        =1 执行新测试, case量可能较大,需将测试用例缓存;
        =2 执行断点续测,前提是1任务未执行完时触发,无需重新缓存用例
         */
        int taskFlag = intent.getIntExtra("TASK_FLAG", 0);
        // 云端任务ID
        int taskId = intent.getIntExtra("TASK_ID", -1);
        if (taskId > 0){
            // 缓存用于恢复执行
            SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, taskId+"");
        }
        // 测试结果筛选
        String taskResult = intent.getStringExtra("TASK_RESULT");
        // 任务批次选择
        String taskDate = intent.getStringExtra("TASK_DATE");
        isDebug = intent.getBooleanExtra("IS_DEBUG", false);
        logUtil.d("Monitor", "开启任务轮询");
        String action = intent.getAction();
        if (action != null) {
            if (ACTION_MONITOR_TASK.equals(action)) {
                while (true) {
                    // 开启wifi
                    new WifiUtils(MonitorService.this).openWifi();

                    if (!isIdle() && taskFlag == 0) {
                        SystemClock.sleep(5000);
                        continue;
                    }
                    //上传设备状态
                    HttpUtil.postJson(CONST.SERVER_BASE_URL + "api/device_state", Common.getDeviceStatusInfo().toString());
                    try {
                        int flag = 0; // 根据此flag切换访问的服务接口，0，监控，1，测试，
                        if (isTest) flag = 1;
                        JSONArray testcases = new JSONArray();
                        JSONObject task = getTask(flag, taskId, taskResult, taskDate);

                        if (task.optInt("code") == 200) {

                            // 云端测试任务执行模式为 “分布” 时，实现动态获取
                            if (task.optString("description").equals("continue")) {
                                taskFlag = 0;
                            }
                            // 云端获取的测试任务
                            testcases = task.getJSONArray("data");
                            // 缓存测试用例
//                                    SPUtils.putString(SPUtils.TASK_CASE, taskId+"", testcases.toString() );
                            if (taskFlag == 1) {
                                HttpUtil.postResp(CONST.SERVER_BASE_URL + "api/device_state", Common.getDeviceStatusInfo().put("task_id", "0") + "");
                                logUtil.i(TAG, "消费掉task_id:" + taskId);
                            }
                            SystemClock.sleep(1000);

                        } else if (task.optInt("code") == 0) {
                            // 根据任务id获取缓存的测试用例
                            testcases = new JSONArray(SPUtils.getString(SPUtils.TASK_CASE, taskId + ""));
                        } else {
                            logUtil.d("", "获取任务失败：" + task.optInt("code"));
                        }

                        if (testcases.length() == 0) {
                            SystemClock.sleep(10000);
                            continue;
                        }

                        //初始化执行器intent
                        Intent intent_start = new Intent(this, MyIntentService.class);

                        if (isDebug) { //开启debug任务
                            logUtil.d("", "***********START DEBUG TASK***********");
                            intent_start.setAction(CONST.ACTION_DEBUG);
                            intent_start.putExtra("DEBUG_CASE", testcases.toString());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent_start);
                            } else {
                                startService(intent_start);
                            }

                        } else if (isMonitor) { // 开启监控任务
                            logUtil.d("", "***********START MONITOR TASK***********");
                            intent_start.setAction(CONST.ACTION_RUN_TASK);
                            intent_start.putExtra("TASK_CASES", testcases.toString());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent_start);
                            } else {
                                startService(intent_start);
                            }
                        } else if (isTest) { // 开始测试任务
                            logUtil.d("", "***********START TEST TASK***********");
                            if (testcases.length() > 0) {
                                if (taskFlag == 1) { // 测试执行模式标志, =1 执行新测试, 需将测试用例缓存; =2 执行断点续测,无需重新缓存用例
                                    SPUtils.putString(SPUtils.TASK_CASE, taskId + "", testcases.toString());
                                    testcases = new JSONArray(); // 将testcases清空, 执行器会根据flag从缓存中读取用例
                                } else if (taskFlag == 2) {
                                    testcases = new JSONArray(); // 将testcases清空, 执行器会根据flag从缓存中读取用例
                                }
                                intent_start.setAction(CONST.ACTION_RUN_TASK);
                                intent_start.putExtra("TASK_CASES", testcases + "");
                                intent_start.putExtra("FLAG", flag);
                                intent_start.putExtra("TASK_ID", taskId);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent_start);
                                } else {
                                    startService(intent_start);
                                }
                                openNotification("Running task：" + taskId);
                            } else {
                                // 用例执行完成,取消进程守护
                                ShellUtils.executeSocketShell("###testool", 0);
                                break;
                            }

                            if (taskFlag != 0) { //测试任务标志，当任务动态分配时循环获取任务，直到无任务
                                break;
                            }
                        }

                    } catch (JSONException e) {
                        logUtil.e("", e);
                    }

                    SystemClock.sleep(15000);

                }
            }
        }


    }

    /**
     * 获取执行任务
     * @param flag 0：监控任务；1：测试任务
     * @param taskId 云端测试任务需要的任务id
     * @return
     * @throws JSONException
     */
    public JSONObject getTask(int flag, int taskId, String taskResult, String taskDate) throws JSONException {
        String deviceId = Common.getDeviceId();
        if (flag==0) {
            JSONObject data = new JSONObject();
            try {
                data.put("idle", String.valueOf(isIdle()));
                data.put("device_id", deviceId);
                String targetApp = Common.CONFIG().optString("TARGET_APP");
                data.put("apk_version", Common.getVersionName(MyApplication.getContext(), targetApp));
                String testTag = Common.CONFIG().optString("TEST_TAG");
                data.put("test_tag", testTag);
                data.put("apk", targetApp);
                if (isDebug) {
                    data.put("debug", isDebug);
                }
            } catch (Exception e) {
                logUtil.e("", e);
            }
            return new JSONObject(HttpUtil.postResp(CONST.SERVER_BASE_URL + CONST.TEST_TASK_URL[flag], data.toString()));
        } else if (flag == 1){
            if (TextUtils.isEmpty(taskResult)) {
                return new JSONObject(HttpUtil.getResp(CONST.SERVER_BASE_URL + CONST.TEST_TASK_URL[flag] + "?task_id=" + taskId));
            } else {
                return new JSONObject(HttpUtil.getResp(CONST.SERVER_BASE_URL + CONST.TEST_TASK_URL[flag] + "?task_id=" + taskId + "&result=" + taskResult + "&task_date=" + taskDate + "&device_id=" +deviceId));

            }
        } else {
            return new JSONObject();
        }

    }

}
