package com.kevin.testool.utils;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.BatteryManagerUtils;
import com.kevin.share.utils.BitmapUtil;
import com.kevin.share.utils.CvUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ScreenShotHelper;
import com.kevin.share.utils.StringUtil;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.MyIntentService;
import com.kevin.testool.service.MonitorService;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.kevin.share.Common.isIdle;


public class MyWebSocketServer extends WebSocketServer implements ScreenShotHelper.OnScreenShotListener{

    private static String TAG = "WebSocketServer";
    private static String screenshotJpg = CONST.LOGPATH + "screen.jpg";
    private static boolean isOpening = false;
    private static int TEMP_BASE64_STR_LEN = 0; // 存储图片转base64后的字符串长度
    public static WebSocket WS;
    private List<WebSocket> mWebSocketList;

    public MyWebSocketServer(InetSocketAddress host) {
        super(host);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

//        Common.postJson(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().toString());
        isOpening = true;
        TEMP_BASE64_STR_LEN = 0;
        logUtil.d(TAG, "客户端连接成功：" + conn.getRemoteSocketAddress());
        WS = conn;
        if (mWebSocketList == null)
            mWebSocketList = new ArrayList<>();
        mWebSocketList.add(conn);

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        isOpening = false;
//        Common.postJson(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().toString());
        TEMP_BASE64_STR_LEN = 0;
        logUtil.d(TAG, "服务关闭");

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logUtil.d(TAG, "接受消息：" + message);
        try {
            JSONObject msgJson = new JSONObject(message);
            // mode支持字段:screen,dump,activity,action,task, case,check_point
            if (!msgJson.isNull("mode")){
                String mode = msgJson.getString("mode");
                logUtil.d("", mode);
                if (mode.equals("screen")){
                    sendScreen(conn);

                } else if (mode.equals("dump")){
                    Thread th = new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            FileUtils.deleteFile(screenshotJpg);
                            Common.screenShot(screenshotJpg, 1, 50);

                            if (Common.get_elements(true, "", "", 0).size() > 0) {
                                String base64Str = FileUtils.xToBase64(screenshotJpg);
                                JSONObject data = new JSONObject();
                                try {
                                    data.put("device_id", Common.getDeviceId());
                                    data.put("screen", base64Str);
                                    data.put("dump", FileUtils.readFile(CONST.DUMP_PATH));
                                    HttpUtil.postResp(CONST.SERVER_BASE_URL + "dump", data.toString());
                                } catch (Exception e){
                                    logUtil.e("", e);
                                }
                            }
                        }
                    });
                    th.start();
                    while (th.isAlive()){
                        SystemClock.sleep(100);
                    }
                    String data = "*dump_finish*";
                    conn.send(data);
                    logUtil.d(TAG, "发送消息：" + data);
                } else if (mode.equals("action")) {
                    Object param = msgJson.get("param");
                    logUtil.d("", param.toString());
                    sendScreen(conn);
                    Common.execute_step(new JSONArray().put(param), new JSONArray().put(1));
                } else if (mode.equals("case")){
                    Object param = msgJson.get("param");
                    Intent intent_case = new Intent(AppContext.getContext(), MyIntentService.class);
                    intent_case.setAction("com.kevin.testool.action.remote");
                    intent_case.putExtra("REMOTE", param + "");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        AppContext.getContext().startForegroundService(intent_case);
                    } else {
                        AppContext.getContext().startService(intent_case);
                    }

                } else if (mode.equals("activity")){
                    conn.send("*activity*" + Common.getActivity());

                } else if (mode.equals("task")){
                    JSONObject param = (JSONObject) msgJson.get("param");

                    if (!param.isNull("status")){ //询问设备状态
                        conn.send(isIdle()+"");
                    } else {
                        int taskId = param.getInt("task_id");
                        if (!param.isNull("apk_url")) {
                            String apk_url = new JSONObject(param.toString()).getString("apk_url");
                            logUtil.d("apk_url", apk_url);
                            boolean installed = Common.execute_step(new JSONArray().put(new JSONObject().put("install", apk_url)), new JSONArray().put(1));
                            if (installed) {
                                // 启动测试任务
                                Intent intent_task = new Intent(AppContext.getContext(), MonitorService.class);
                                intent_task.putExtra("FROM_CACH", false); // 断点执行
                                intent_task.setAction("com.kevin.testool.task");
                                intent_task.putExtra("IS_TEST", true);
                                intent_task.putExtra("TASK_ID", taskId);
                                SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK, "test");
                                SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, taskId+"");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    AppContext.getContext().startForegroundService(intent_task);
                                } else {
                                    AppContext.getContext().startService(intent_task);
                                }
                            } else {
                                logUtil.d("", "应用安装失败" + apk_url);
                            }


                        }
                    }
                }
            }
        } catch (Exception e) {
            logUtil.e("", e);
        }

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        // 接收到的是Byte数据，需要转成文本数据，根据你的客户端要求
        // 看看是string还是byte，工具类在下面贴出
        logUtil.d(TAG, "onMessage()接收到ByteBuffer的数据->" + StringUtil.byteBufferToString(message));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logUtil.e(TAG, ex);
        try {
            HttpUtil.postJson(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo("-2").put("remote", 1).toString());
        } catch (JSONException e) {
            logUtil.e("", e);
        }
        ToastUtils.showShortByHandler(AppContext.getContext(), "异常：" + ex.toString());

    }

    @Override
    public void onStart() {
        logUtil.d(TAG, "onStart()-> WebSocket服务端启动成功");
    }


    public static MyWebSocketServer myWebSocketServer;
    // 实现方法，在服务中或者OnCreate()方法调用此方法
    public static void startMyWebsocketServer() {
//         上传设备状态到数据库
//        Common.postResp(CONST.SERVER_BASE_URL + "devices_state", Common.getDeviceStatusInfo().toString());

        InetSocketAddress myHost = new InetSocketAddress(new WifiUtils(AppContext.getContext()).getIPAddress() + "", Integer.parseInt(CONST.SERVER_PORT));
        if (myWebSocketServer != null) {
            try {
                myWebSocketServer.stop();
                SystemClock.sleep(3000);
                logUtil.d(TAG, "重启websocket服务");
            } catch (Exception e) {
                logUtil.e("", e);
            }
        }
        myWebSocketServer = new MyWebSocketServer(myHost);
        myWebSocketServer.start();
    }

    public static void stopMyWebsocketServer() {

        if (myWebSocketServer != null) {
            try {
                myWebSocketServer.stop();
                logUtil.d(TAG, "停止websocket服务");
            } catch (Exception e) {
                logUtil.e("", e);
            }
        }
    }

    /**
     * 将图片分片发送
     */
    private static void sendScreen(WebSocket conn){

        Common.screenShot(screenshotJpg, 2, 30);
        if(!isOpening){
            return;
        }
        String base64Str = FileUtils.xToBase64(screenshotJpg);
        int base64StrLen = 0;
        if (!TextUtils.isEmpty(base64Str)){
            base64StrLen = base64Str.length();
            if (Math.abs(TEMP_BASE64_STR_LEN - base64StrLen) < 100){ // 通过长度是否改变判断页面是否有变化
                String send_pause = "*pause*" + TEMP_BASE64_STR_LEN + ":"+base64StrLen;
                conn.send(send_pause);
                SystemClock.sleep(100);
                return;
            }
            TEMP_BASE64_STR_LEN = base64StrLen;
        }
        String send_start = "*start*";
        if (Common.getScreenWidth() > Common.getScreenHeight()){
            send_start = "*start*landscape"; //横屏图像
        }

        conn.send(send_start);
        for (int i=0; i < base64StrLen; i = i + 10240){
            if ((i+10240) > base64StrLen){
                conn.send(base64Str.substring(i));
//                SystemClock.sleep(5);
                break;
            }
            conn.send(base64Str.substring(i, i + 10240));

        }
        String send_end = "*end*";
        conn.send(send_end);
        SystemClock.sleep(10);
    }

    /**
     * 发送二进制
     *
     * @param bytes
     */
    public void sendBytes(byte[] bytes) {
        if (mWebSocketList == null) return;
        for (WebSocket socket : mWebSocketList)
            socket.send(bytes);
    }

    @Override
    public void onShotFinish(Bitmap bitmap) {
        logUtil.d("", "bitmap:" + bitmap.getWidth());
        myWebSocketServer.sendBytes(BitmapUtil.getByteBitmap(bitmap));
    }

}
