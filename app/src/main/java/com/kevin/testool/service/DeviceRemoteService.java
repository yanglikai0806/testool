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

import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.R;
import com.kevin.testool.activity.MainActivity;
import com.kevin.testool.utils.WifiUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import static com.kevin.share.Common.isIdle;
import static com.kevin.testool.utils.MyWebSocketServer.startMyWebsocketServer;


/**
 * 设备远程管理服务
 */
public class DeviceRemoteService extends IntentService {
    private static final String TAG = "DeviceRemoteService";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static Date date = new Date();
    private static final int NOTIFICATION_ID = 0x2;
    private NotificationCompat.Builder builder;
    public static String REMOTE = "REMOTE"; //是否允许远程的标志, 0:不允许， 1：允许

    private static final String ACTION_DEVICE_REMOTE = "com.kevin.testool.action.device.remote";

    public DeviceRemoteService() {
        super("DeviceRemoteService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //开启前台服务避免被杀
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "my_channel_02";
            CharSequence NAME = "my_channel_name";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NAME, NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text4)).build();

            startForeground(NOTIFICATION_ID, notification);
        } else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text4))
                    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }
        logUtil.d(TAG, "启动DeviceRemoteService");
    }


    @Override
    protected void onHandleIntent(final Intent intent) {
        final String dt = dateFormat.format(date);
        boolean runTask = false;
        logUtil.d(TAG, new WifiUtils(AppContext.getContext()).getIPAddress() + "");
        // 启动websocket服务
        startMyWebsocketServer();
        // 上传设备初始状态
        try {
            HttpUtil.postResp(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().put("remote", 1).toString());
        } catch (JSONException e) {
            logUtil.e(TAG, e);
        }
        // 等待5s
        SystemClock.sleep(5000);

        // 开始任务轮训
        while (true){
            // 开启wifi
            new WifiUtils(DeviceRemoteService.this).openWifi();
            try {
                String respMsg = HttpUtil.postResp(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().put("remote", 1).toString());
                logUtil.d(TAG, respMsg);
                if (!isIdle()){
                    SystemClock.sleep(10000);
                    continue;
                }
                JSONObject resp = new JSONObject(HttpUtil.getResp(CONST.SERVER_BASE_URL + "devices_state?device_id=" + Common.getDeviceId()));
                logUtil.d(TAG, resp);
                if (resp.optInt("code", 0) == 200){
                    JSONObject deviceInfo = resp.getJSONObject("data");
                    String taskId = deviceInfo.optString("task_id", "");
                    logUtil.d(TAG, "task id：" + taskId);
                    if (TextUtils.isEmpty(taskId)) {
                        SystemClock.sleep(5000);
                        continue;
                    }
                    // 获取任务状态, task_status为1表示任务可执行
                    String task_status = HttpUtil.getResp(CONST.SERVER_BASE_URL + "task_plane?task_id=" + taskId + "&get_task_info=task_status");
                    if (isIdle() && task_status.equals("1")) { // 设备空闲且任务为可执行状态
                        if (taskId.length() > 0 && Integer.parseInt(taskId) > 0) {
                            String task_mode = "";
                            // 执行测试任务条件要求
                            String taskCondition = HttpUtil.getResp(CONST.SERVER_BASE_URL + "task_plane?task_id=" + taskId + "&get_task_info=task_condition");
                            logUtil.d("", CONST.SERVER_BASE_URL + "task_plane?task_id=" + taskId + "&get_task_info=task_condition");
                            logUtil.d(TAG, "任务执行条件:" + taskCondition);
                            try {
                                JSONObject taskConditionJson = new JSONObject(taskCondition);
                                task_mode = taskConditionJson.optString("task_mode", "全量");
                                // apk 自动安装流程
                                if (!taskConditionJson.isNull("apk_url")) {
                                    logUtil.d("应用安装标志", SPUtils.getApkInstallFlag());
                                    if (SPUtils.getApkInstallFlag() != 1) { // 判断是否安装过, 未安装过则安装
                                        String apk_url = taskConditionJson.getString("apk_url");
                                        logUtil.d("", apk_url);
                                        runTask = Common.execute_step(new JSONArray().put(new JSONObject().put("install", apk_url)), new JSONArray().put(1));
                                        SPUtils.setApkInstallFlag(runTask ? 1:0);

                                    } else {
                                        runTask = SPUtils.getTaskRunFlag() == 1;

                                    }
                                } else {
                                    runTask = true;
                                }
                            } catch (JSONException e) {
                                logUtil.e("JSON格式不正确", e);
                                runTask = false;
                            }
                            logUtil.d("任务执行", runTask);
                            if (runTask) {
                                SPUtils.setTaskRunFlag(1);
                                // 任务执行
                                logUtil.d("", "接受测试任务task_id:" + taskId);
                                Intent intent_test = new Intent(this, MonitorService.class);
//                        intent_test.putExtra("FROM_CACH", true); // 断点执行
                                intent_test.setAction("com.kevin.testool.task");
                                intent_test.putExtra("IS_TEST", true);
                                intent_test.putExtra("TASK_ID", Integer.valueOf(taskId));

                                SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK, "test");
                                SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, taskId + "");


                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(intent_test);
                                } else {
                                    startService(intent_test);
                                }
                                //消费掉task_id, “分布“ 模式下消费task_id, 写在 MonitorService 中
                                if (!task_mode.equals("分布")) {
                                    HttpUtil.postResp(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().put("task_id", "0") + "");
                                    logUtil.d("", "消费掉task_id:" + taskId);
                                }
                                // 等待几秒钟让任务执行一会
                                SystemClock.sleep(5000);
                            }
                        } else {
                            // 任务执行完成则恢复标志状态
                            SPUtils.setTaskRunFlag(0);
                            SPUtils.setApkInstallFlag(0);
                        }
                    }

                }
                // 等待几秒钟
                SystemClock.sleep(5000);
            } catch (JSONException e) {
                logUtil.e("", e);
            }
        }

    }


}


