package com.kevin.share.utils;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.kevin.share.AppContext;
import com.kevin.share.adblib.CmdTools;
import com.kevin.share.shell.SocketClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static com.kevin.share.CONST.ADB_PORT;

/**
 * shell 工具类
 */

public class ShellUtils {
    static String TAG = "AdbUtils";
    private static Boolean rooted = null;
    public static int shellFlag = 0; // 1:系统root；2： socket shell； 3： adbLib
    private static String SocketShellResult = "";

    /**
     * 设置为空字符串""或者"/system/bin/"
     */
    private static final String EXE_PREFIX = "";


    /**
     * shell执行su判断是否有root权限
     *
     * @return 当前app是否有root权限
     */
    public static boolean hasRootPermission() {
        Process process = null;
        DataOutputStream os = null;
        if (rooted != null){
            logUtil.d(TAG, "hasRootPermission: " + rooted);
            return rooted;
        }
        try {
            process = Runtime.getRuntime().exec(EXE_PREFIX+"su");
//            process = Runtime.getRuntime().exec("echo hello");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
//            logUtil.d("", process.exitValue()+"");
            rooted = process.exitValue() == 0;
        } catch (Exception e) {
//            e.printStackTrace();
            rooted = false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                    process.destroy();
                } catch (Exception e) {
                    logUtil.e("", e.toString());
                    e.printStackTrace();
                }
            }
        }
        logUtil.d(TAG, "hasRootPermission: " + rooted);
        if (rooted){
            shellFlag = 1;
        }
        return rooted;
    }

    public static boolean isShellEnable(){
        return isShellSocketConnect() | isAdbConnect() | hasRootPermission();
    }

    /**
     * 是否可以执行adb命令
     * @return boolean
     */
    public static boolean isAdbConnect(){
        final boolean[] result = new boolean[1];
        if (hasRootPermission()){
            return true;
        } else {
         Thread isAdb = new Thread(new Runnable() {
             @Override
             public void run() {
                 result[0] = CmdTools.generateConnection();
             }
         });
         isAdb.start();
         while (isAdb.isAlive()){
             SystemClock.sleep(100);
         }
         if (result[0]){
             shellFlag = 3;
             logUtil.d("", "adblib 已连接");
             return true;
         }
            logUtil.d("", "adblib 未连接");
         return false;
        }
    }

    public static boolean isShellSocketConnect(){
        boolean result = executeSocketShell("###AreYouOK", 3000).contains("###IamOK");
        if (result){
            shellFlag = 2;
            logUtil.d("", "shellservice 已连接");
            return true;
        }
        logUtil.d("", "shellservice 未连接");
        return false;

    }

    /**
     * 执行shell命令，无root权限需开启adb tcpip 5555
     * @param command 待执行的shell命令
     * @param timeout timeout == 0 阻塞命令直到执行结束;timeout>0 超时时间,超时后结束进程; timeout< 0 等待timeout绝对值时间，不返回命令结果
     * @return shell命令执行结果
     */
    public static String runShellCommand(String command, int timeout) {
//        logUtil.d("", command );
        if (shellFlag == 0) {
            isShellEnable();
        }
        String res = "";
        switch (shellFlag){
            case 0:
                // 不可用
                ToastUtils.showLongByHandler(AppContext.getContext(), "shell 不可用");
                break;
            case 1:
                // root
                res = executeRuntimeShell(command, timeout);
                break;
            case 3:
                // adb lib
//                long st = System.currentTimeMillis();
                res = CmdTools.execAdbCmd(command, timeout);
//                logUtil.d("adblib 耗时", (System.currentTimeMillis() - st));
                break;
            case 2:
                // shell socket
//                long stt = System.currentTimeMillis();
                res = executeSocketShell(command, timeout);
//                logUtil.d("shellsocket 耗时", res + ":"+(System.currentTimeMillis() - stt));
                break;

        }
//        logUtil.d("", "***************"+res+"***************" );
        return res;
    }

    private static String executeRuntimeShell(String command, int timeout){
        boolean waitResult = true;
        Process process = null;
        DataOutputStream os = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(EXE_PREFIX + "su");
            os = new DataOutputStream(process.getOutputStream());
            if (command.startsWith("su root ")){
                command = command.substring(8);
            }
            if (!command.endsWith("\n")) {
                command = command + "\n";
            }
            os.write(command.getBytes());
            os.writeChars("exit\n");
            os.flush();

            if (timeout > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    process.waitFor(timeout, TimeUnit.MILLISECONDS);
                } else {
                    SystemClock.sleep(timeout);
                }
            } else if (timeout == 0) {
                process.waitFor();
            } else {
                waitResult = false;
                SystemClock.sleep(Math.abs(timeout));

            }

            //输出结果
            if (waitResult) {
                BufferedReader successResult = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                BufferedReader errorResult = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                    Log.i(TAG, errorMsg.toString());
                }
            }

        } catch (Exception e) {
            logUtil.e("AdbUtils.runShellCommand", e.getMessage());
            return "-1";
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                logUtil.e("AdbUtils.runShellCommand", e.getMessage());
            }
        }
//        logUtil.d("-------", successMsg.toString());
        return successMsg.toString();
    }

    public static String executeSocketShell(String cmd, int timeout){
        if (TextUtils.isEmpty(cmd)) return "";
        SocketShellResult = "**wait**";
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                new SocketClient(cmd, timeout,new SocketClient.onServiceSend() {
                    @Override
                    public void getSend(String result) {
                        SocketShellResult = result;
                    }
                });
            }
        });
        th.start();
        SystemClock.sleep(300);
        while (th.isAlive()){
            SystemClock.sleep(100);
        }
        return SocketShellResult;
    }

    /**
     * 设置系统属性值，这些属性值可以通过getprop获取，setprop设置
     *
     * @param property 属性名
     * @param value    属性值
     * @return 是否成功设置属性值
     */
    public static String setProp(String property, String value) {
        return runShellCommand("setprop " + property + " " + value, 0);
    }

    /**
     * 启动adb服务
     *
     * @return 是否成功启动adb服务
     */
    public static boolean adbStart() {
        try {
            runShellCommand("stop adbd", 0);
            setProp("service.adb.tcp.port", ADB_PORT + "");
            runShellCommand("stop adbd", 0);
            runShellCommand("start adbd", 0);
            logUtil.d("", "开启" + ADB_PORT + "端口");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 停止adb服务
     *
     * @return 是否成功停止adb服务
     */
    public static boolean adbStop() {
        try {
            setProp("service.adb.tcp.port", "-1");
            runShellCommand("stop adbd", 0);
            runShellCommand("start adbd", 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * 检测adb服务是否正在运行
     *
     * @return 是否正在运行adb服务
     */
    public static boolean adbIfRunning() {
        final boolean[] running = {false};
        new Thread(){
            @Override
            public void run() {

                String res = runShellCommand("getprop service.adb.tcp.port", 0);
                System.out.println(res);
                if (res.contains(ADB_PORT + "")) {
                    running[0] = true;
                }
            }
        }.start();
        SystemClock.sleep(200);
        return running[0];
    }

}
