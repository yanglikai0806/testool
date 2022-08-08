package com.kevin.share.shell;

import com.kevin.share.utils.logUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;


public class SocketClient {

    private String TAG = "SocketClient";
    private String HOST = "127.0.0.1";
    private int port = 3721;
    PrintWriter printWriter;//发送用的
    onServiceSend mOnServiceSend;
    BufferedReader bufferedReader;
    private static String MESSAGE;

    public SocketClient(String command, int timeout, onServiceSend onServiceSend) {
        mOnServiceSend = onServiceSend;
        try {
//            logUtil.d(TAG, "与service进行socket通讯,地址=" + HOST + ":" + port);
            //创建Socket
            // 创建一个流套接字并将其连接到指定 IP 地址的指定端口号(本处是本机)
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(HOST, port), 3000);//设置连接请求超时时间3 s
            // 接收超时时间
            socket.setSoTimeout(Math.abs(timeout));
//            logUtil.d(TAG, "与service进行socket通讯,超时为：" + 3000);
            //发送客户端准备传输的信息
            // 由Socket对象得到输出流，并构造PrintWriter对象
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            //用于获取服务端传输来的信息
            // 由Socket对象得到输入流，并构造相应的BufferedReader对象
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new CreateServerThread(socket);
//            logUtil.d(TAG, command);
            send(command);
        } catch (Exception e) {
            e.printStackTrace();
            logUtil.d(TAG, "与service进行socket通讯发生错误" + e);
            mOnServiceSend.getSend("###ShellRunError:" + e.toString());
        }
    }

    //线程类
    class CreateServerThread extends Thread {
        Socket socket;
        InputStreamReader inputStreamReader;
        BufferedReader reader;
        public CreateServerThread(Socket s) {
//            logUtil.d(TAG, "创建了一个新的连接线程");
            socket = s;
            start();
        }

        public void run() {
            try {
                // 打印读入一字符串并回调
                try {
                    inputStreamReader = new InputStreamReader(socket.getInputStream());
                    reader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        mOnServiceSend.getSend(line.trim());
                    }
                } catch (Exception e){
                    logUtil.e(TAG, e);
                }
                bufferedReader.close();
                printWriter.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                logUtil.d(TAG, "socket 接收线程发生错误：" + e.toString());
            }
        }
    }


    public void send(String cmd){
        printWriter.println(cmd);
        // 刷新输出流，使Server马上收到该字符串
        printWriter.flush();
    }

    public interface onServiceSend{
        void getSend(String result);
    }
}


