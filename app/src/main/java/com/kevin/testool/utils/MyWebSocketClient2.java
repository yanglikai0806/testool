package com.kevin.testool.utils;

import android.graphics.Bitmap;
import android.os.SystemClock;

import com.kevin.share.utils.BitmapUtil;
import com.kevin.share.utils.logUtil;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.SocketException;
import java.net.URI;
import java.nio.ByteBuffer;


public class MyWebSocketClient2 extends WebSocketClient {

    private final String TAG = "MWebSocketClient";

    private boolean mIsConnected = false;
    private CallBack mCallBack;
    private static int flag;
    private static String MSG = "";

    public MyWebSocketClient2(URI serverUri, CallBack callBack) {
        super(serverUri);
        this.mCallBack = callBack;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        logUtil.d(TAG, "onOpen");
        updateClientStatus(true);

        try {
            getSocket().setReceiveBufferSize(5 * 1024 * 1024);
        } catch (SocketException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        byte[] buf = new byte[bytes.remaining()];
        bytes.get(buf);
        if (mCallBack != null)
            mCallBack.onBitmapReceived(BitmapUtil.decodeImg(buf));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        updateClientStatus(false);
    }

    @Override
    public void onError(Exception ex) {
        updateClientStatus(false);
    }

    public void sendMsg(String msg){
        send(msg);
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

    private void updateClientStatus(boolean isConnected) {

        mIsConnected = isConnected;
        logUtil.d(TAG, "mIsConnected:" + mIsConnected);
        // 回调
        if (mCallBack != null)
            mCallBack.onClientStatus(isConnected);
    }

    public boolean isConnected() {
        logUtil.d(TAG, "mIsConnected:" + mIsConnected);
        return mIsConnected;
    }

    public interface CallBack {
        void onClientStatus(boolean isConnected);

        void onBitmapReceived(Bitmap bitmap);
    }


}
