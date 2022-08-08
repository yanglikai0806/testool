package com.kevin.testool.utils;


import com.kevin.share.utils.logUtil;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * websocket server
 */
public class MyWebSocketServer2 extends WebSocketServer {

    private final String TAG = "MyWebSocketServer";

    private boolean mIsStarted = false;
    private CallBack mCallBack;

    private List<WebSocket> myWebSocketList;

    public MyWebSocketServer2(int port, CallBack callBack) {
        super(new InetSocketAddress(port));
        this.mCallBack = callBack;
        setReuseAddr(true);
        setConnectionLostTimeout(5 * 1000);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        logUtil.d(TAG, "有用户链接");
        if (myWebSocketList == null)
            myWebSocketList = new ArrayList<>();
        myWebSocketList.add(webSocket);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logUtil.d(TAG, "有用户离开");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logUtil.d(TAG, "接收到消息：" + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logUtil.d(TAG, "发生error:" + ex.toString());
    }

    @Override
    public void onStart() {
        updateServerStatus(true);
    }

    /**
     * 停止服务器
     */
    public void socketStop() {
        try {
            super.stop(100);
            updateServerStatus(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送二进制
     *
     * @param bytes
     */
    public void sendBytes(byte[] bytes) {
        if (myWebSocketList == null) return;
        for (WebSocket socket : myWebSocketList)
            socket.send(bytes);
    }

    private void updateServerStatus(boolean isStarted) {
        mIsStarted = isStarted;
        logUtil.d(TAG, "mIsStarted:" + mIsStarted);
        // 回调
        if (mCallBack != null)
            mCallBack.onServerStatus(isStarted);
    }

    public boolean isStarted() {
        logUtil.d(TAG, "mIsStarted:" + mIsStarted);
        return mIsStarted;
    }

    public interface CallBack {
        void onServerStatus(boolean isStarted);
    }

}
