package com.kevin.testool.utils;

import android.os.SystemClock;

import com.kevin.share.utils.logUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MyWebSocketClient {
    private static final String TAG = "WebSocketClient";
    private static OkHttpClient mClient;
    public static WebSocket mWebSocket;
    private static int flag = 0; // 0 表示发出消息；1 表示接收到回复
    private static String MSG = "";

    //初始化WebSocket
    public WebSocket init(String websocketUrl) {
        if (mWebSocket == null) {
            mClient = new OkHttpClient.Builder()
                    .pingInterval(10, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(websocketUrl)
                    .build();
            mWebSocket = mClient.newWebSocket(request, new WsListener());
        }
        return mWebSocket;
    }


    //监听事件，用于收消息，监听连接的状态
    class WsListener extends WebSocketListener {
        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            logUtil.d("", "客户端收到消息:" + text);
            flag = 1;
            MSG = text;
//            try {
//                JSONObject dataJson = new JSONObject(text);
//                String mode = dataJson.getString("mode");
//            } catch (JSONException e) {
//                logUtil.e("", e);
//                logUtil.d("", "接收的数据需满足json格式");
//            }
            //测试发消息
            //webSocket.send("我是客户端，你好啊");
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            logUtil.d(TAG,"连接成功！");
        }


    }
    public static void sendMsg(String msg){
        mWebSocket.send(msg);
        flag = 0;
    }

    public static String waitResult(double timeout){
        double useTime = 0;
        while (flag != 1 && useTime < timeout){
            SystemClock.sleep(1000);
            useTime += 1000;
        }
        return MSG;

    }

}