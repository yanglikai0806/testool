/*
 * Copyright (C) 2015-present, Ant Financial Services Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kevin.share.adblib;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.util.Base64;

import com.kevin.share.CONST;
import com.kevin.share.utils.logUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 命令行操作集合
 */
public class CmdTools {
    private static String TAG = "cmd_tool";

    private static final int MODE_APPEND = 0;

    public static final String FATAL_ADB_CANNOT_RECOVER = "fatalAdbNotRecover";
    public static boolean ADB_BREAK = false;

    public static final String ERROR_NO_CONNECTION = "ERROR_NO_CONNECTION";
    public static final String ERROR_CONNECTION_ILLEGAL_STATE = "ERROR_CONNECTION_ILLEGAL_STATE";
    public static final String ERROR_CONNECTION_COMMON_EXCEPTION = "ERROR_CONNECTION_COMMON_EXCEPTION";

    private static final SimpleDateFormat LOG_FILE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CHINA);

    private static ScheduledExecutorService mOppoKeepAliveExecutor;

    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();

    private static volatile AdbConnection connection;

    private static Boolean isRoot = null;

    private static List<Process> processes = new ArrayList<>();

    private static List<AdbStream> streams = new ArrayList<>();

//    private static PidWatcher watcher;

    private static File currentLogFile;

    private static ConcurrentLinkedQueue<String> logs = null;

    public static void forceAdb(){
        isRoot = false;
    }

    public static void cancelForceAdb(){
        isRoot = null;
    }

    public static long LAST_ADB_RETRY_TIME = 0;

    /**
     * root超时限制
     */
    private static ThreadPoolExecutor processReadExecutor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(10));

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    protected static void logcatCmd(String cmd){
        logUtil.w("ADB CMD", cmd);
    }

    /**
     * 判断当前手机是否有ROOT权限
     * @returnz
     */
    public static boolean isRooted(){
        boolean bool = false;

        // 避免重复查找文件
        if (isRoot != null) {
            return isRoot;
        }
        try{
            if (new File("/system/bin/su").exists()){
                bool = true;
            } else if (new File("/system/xbin/su").exists()) {
                bool = true;
            } else if (new File("/su/bin/su").exists()) {
                bool = true;
            }
            logUtil.d(TAG, "isRooted = " + bool);

        } catch (Exception e) {
            logUtil.e(TAG, "THrow exception: " + e.getMessage());
        }
        return bool;
    }

    /**
     * 是否已初始化
     * @return
     */
    public static boolean isInitialized() {
        return connection != null;
    }

    public static Process getRootCmd(){
        try{
            return Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            logUtil.e(TAG, e.toString());
            isRoot = false;
        }
        return null;
    }

    /**
     * 快捷执行ps指令
     * @param filter grep 过滤条件
     * @return 分行结果
     */
    public static String[] ps(String filter) {
        try {
            Process p;
            if (filter != null && filter.length() > 0) {
                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps | grep \"" + filter + "\""});
            } else {
                p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps"});
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            List<String> results = new ArrayList<>();
            while ((line = br.readLine()) != null) {
//            		logUtil.d(TAG, "ERR************" + line);
                results.add(line);
            }
            return results.toArray(new String[results.size()]);
        } catch (IOException e) {
            logUtil.e(TAG, "Read ps content failed");
            return new String[0];
        }
    }

    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeToString(arg0, 2);
            }
        };
    }

    /**
     * 运行高权限命令
     * @param cmd
     * @return
     */
    public static String execHighPrivilegeCmd(String cmd) {
        if (isRooted()) {
            return execRootCmd(cmd, null, true, null).toString();
        }
        return execAdbCmd(cmd, 0);
    }

    /**
     * 带超时的高权限命令执行
     * @param cmd shell命令（shell之后的部分）
     * @param maxTime 最长执行时间
     * @return 执行结果
     */
    public static String execHighPrivilegeCmd(final String cmd, int maxTime) {
        if (isRooted()) {
            return execRootCmd(cmd, maxTime).toString();
        }
        return execAdbCmd(cmd, maxTime);
    }

    public static String execAdbExtCmd(final String cmd, final int wait) {
        if (connection == null) {
            logUtil.d(TAG, "no connection");
            return "";
        }

        try {
            AdbStream stream = connection.open(cmd);
            logcatCmd(stream.getLocalId() + "@" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待wait毫秒后强制退出
                Thread.sleep(wait);
                stream.close();
            }

            // 获取stream所有输出
            InputStream adbInputStream = stream.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[128];
            int readCount = -1;
            while ((readCount = adbInputStream.read(buffer, 0, 128)) > -1) {
                sb.append(new String(buffer, 0, readCount));
            }

            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            logUtil.e(TAG
                    , "IllegalState, " + e.getMessage());

            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection();
            if (result) {
                return retryExecAdb(cmd, wait);
            } else {
                logUtil.d(TAG, "regenerateConnection failed");
                return "";
            }
        } catch (Exception e){
            logUtil.e(TAG, "Throw Exception: " + e.getMessage());
            return "";
        }
    }

    /**
     * 执行Adb命令，对外<br/>
     * <b>注意：主线程执行的话超时时间会强制设置为5S以内，防止ANR</b>
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String execAdbCmd(final String cmd, int wait) {
        //生成adb连接
        generateConnection();
        // 主线程的话走Callable
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (wait > 5000 || wait == 0) {
                logUtil.w(TAG, "主线程配置的等待时间过长，修改为5000ms");
                wait = 5000;
            }

            final int finalWait = wait;
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() {
                    return _execAdbCmd(cmd, finalWait);
                }
            };
            Future<String> result = cachedExecutor.submit(callable);

            // 等待执行完毕
            try {
                return result.get();
            } catch (InterruptedException e) {
                logUtil.d(TAG, "Catch java.lang.InterruptedException: " + e.getMessage());
            } catch (ExecutionException e) {
                logUtil.d(TAG, "Catch java.util.concurrent.ExecutionException: " + e.getMessage());
            }
            return null;
        }
        return _execAdbCmd(cmd, wait);
    }

    /**
     * 执行Adb命令
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String _execAdbCmd(final String cmd, final int wait) {
        if (connection == null) {
            logUtil.d(TAG, "no connection when execAdbCmd");
            return "";
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
//            logcatCmd(stream.getLocalId() + "@" + "shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else if (wait > 0){
                // 等待最长wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }

                if (!stream.isClosed()) {
                    stream.close();
                }
            } else {
                Thread.sleep(Math.abs(wait));
                return "";
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            logUtil.e(TAG, "Throw IllegalStateException: " + e.getMessage());

            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection();
            if (result) {
                return retryExecAdb(cmd, wait);
            } else {
                logUtil.d(TAG, "regenerateConnection failed");
                return "";
            }
        } catch (Exception e){
            logUtil.e(TAG, "Throw Exception: " + e.getMessage()
                    );
            return "";
        }
    }



    /**
     * 执行adb命令，在超时时间范围内
     * @param cmd
     * @param timeout 超时时间（必大于0）
     * @return
     */
    public static String execShellCmdWithTimeout(final String cmd, @IntRange(from = 1) final long timeout) {
        if (connection == null) {
            logUtil.d(TAG, "connection is null");
            return "";
        }

        try {
            long startTime = System.currentTimeMillis();
            AdbStream stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
            streams.add(stream);

            while (!stream.isClosed() && System.currentTimeMillis() - startTime < timeout) {
                Thread.sleep(10);
            }

            if (!stream.isClosed()) {
                stream.close();
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {

            logUtil.e(TAG, "IllegalState?? " + e.getMessage());
            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection();
            if (result) {
                return retryExecAdb(cmd, timeout);
            } else {
                return "";
            }
        } catch (Exception e){
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
            return "";
        }
    }


    private static String retryExecAdb(String cmd, long wait) {
        AdbStream stream = null;
        try {
            stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else if (wait > 0){
                // 等待wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }
                if (!stream.isClosed()) {
                    stream.close();
                }
            } else {
                Thread.sleep(wait);
                return "";
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IOException e) {
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
        } catch (InterruptedException e) {
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
        }

        return "";
    }


    private static String execAdbCmdWithStatus(final String cmd, final int wait) {
        if (connection == null) {
            return ERROR_NO_CONNECTION;
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }

                if (!stream.isClosed()) {
                    stream.close();
                }

            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes: results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            return ERROR_CONNECTION_ILLEGAL_STATE;
        } catch (Exception e){
            return ERROR_CONNECTION_COMMON_EXCEPTION;
        }
    }

    private static String execSafeCmd(String cmd, int retryCount) {
        String result = "";
        while (retryCount-- > 0) {
            result = execAdbCmdWithStatus(cmd, 0);
            if (ERROR_NO_CONNECTION.equals(result) || ERROR_CONNECTION_ILLEGAL_STATE.equals(result)) {
                generateConnection();
//                MiscUtil.sleep(2000);
            } else if (ERROR_CONNECTION_COMMON_EXCEPTION.equals("result")) {
//                MiscUtil.sleep(2000);
            } else {
                break;
            }
        }
        return result;
    }


    public static void clearProcesses() {
        try {
            for (Process p : processes) {
                logUtil.i(TAG, "stop process: " + p.toString());
                p.destroy();
            }
            processes.clear();
            for (AdbStream stream : streams) {
                logUtil.i(TAG, "stop stream: " + stream.toString());
                try {
                    stream.close();
                } catch (Exception e) {
                    logUtil.e(TAG, "Stop stream " + stream.toString() + " failed");
                }
            }
            streams.clear();
        } catch (Exception e) {
            logUtil.d(TAG, "抛出异常 " + e.getMessage());
        }
    }


    private static volatile long LAST_RUNNING_TIME = 0;
    /**
     * 生成Adb连接，由所在文件生成，或创建并保存到相应文件
     */
    public static synchronized boolean generateConnection() {
        if (connection != null && connection.isFine()) {
            return true;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e1) {
                logUtil.e(TAG, "Throw IOException: " + e1.getMessage());
                e1.printStackTrace();
            } finally {
                connection = null;
            }
        }

        Socket sock;
        AdbCrypto crypto;
        AdbBase64 base64 = getBase64Impl();

        // 获取连接公私钥
        File privKey = new File(CONST.LOGPATH+ File.separator +"privKey");
        File pubKey = new File(CONST.LOGPATH+ File.separator + "pubKey");

        if (!privKey.exists() || !pubKey.exists()) {
            try {
                crypto = AdbCrypto.generateAdbKeyPair(base64);
                privKey.delete();
                pubKey.delete();
                crypto.saveAdbKeyPair(privKey, pubKey);
            } catch (NoSuchAlgorithmException | IOException e) {
                logUtil.e(TAG, "抛出异常 " + e.getMessage());
                ADB_BREAK = true;
                return false;
            }
        } else {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
            } catch (Exception e) {
                logUtil.d(TAG, "抛出异常 " + e.getMessage());
                try {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    privKey.delete();
                    pubKey.delete();
                    crypto.saveAdbKeyPair(privKey, pubKey);
                } catch (NoSuchAlgorithmException | IOException ex) {
                    logUtil.d(TAG, "抛出异常 " + ex.getMessage());
                    ADB_BREAK = true;
                    return false;
                }
            }
        }

        // 开始连接adb
        logUtil.d(TAG, "Socket connecting...");
        try {
            sock = new Socket("localhost", 5555);
        } catch (IOException e) {
            logUtil.d("", "-----------------------");
            logUtil.e(TAG, e);
            logUtil.d("", "***********************");
            ADB_BREAK = true;
            return false;
        }
        logUtil.d(TAG, "Socket connected");

        AdbConnection conn;
        try {
            conn = AdbConnection.create(sock, crypto);
            logUtil.d(TAG, "ADB connecting...");

            // 10s超时
            conn.connect(10 * 1000);
        } catch (Exception e) {
            logUtil.d(TAG, "ADB connect failed");
            // socket关闭
            if (sock.isConnected()) {
                try {
                    sock.close();
                } catch (IOException e1) {
                    logUtil.e(TAG, e1.toString());
                }
            }
            ADB_BREAK = true;
            return false;
        }
        connection = conn;
        logUtil.i(TAG, "ADB connected");

        // ADB成功连接后，开启ADB状态监测
//        startAdbStatusCheck();
        ADB_BREAK = false;
        return true;
    }

    /**
     * 在maxTime内执行root命令
     * @param cmd 待执行命令
     * @param maxTime 最长执行时间
     * @return 输出
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder execRootCmd(String cmd, final int maxTime) {
        final StringBuilder result = new StringBuilder();
        DataOutputStream dos = null;
        String line = null;
        Process p;

        try {
            p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            processes.add(p);
            dos = new DataOutputStream(p.getOutputStream());
            final InputStream inputStream = p.getInputStream();

            // 写输入
//            logUtil.i(TAG, cmd);

            Future future = processReadExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 读取全部输入
                        byte[] read = new byte[1024];
                        int length = 0;
                        while ((length = inputStream.read(read, 0, 1024)) > 0) {
                            result.append(new String(read, 0, length));
                        }
                    } catch (IOException e) {
                        logUtil.e(TAG, "抛出异常 " + e.getMessage());
                    }
                }
            });

            dos.writeBytes(cmd + "\n");
            dos.flush();

            long startTime = System.currentTimeMillis();
            while (!future.isDone() && System.currentTimeMillis() - startTime < maxTime) {
                Thread.sleep(10);
            }

            if (!future.isDone()) {
                future.cancel(true);
            }


            // 关闭process
            p.destroy();
            processes.remove(p);


            isRoot = true;
        } catch (Exception e) {
            logUtil.e(TAG, "命令执行发生异常" + e.getMessage());
            isRoot = false;
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    logUtil.e(TAG, "抛出IOException " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 执行root命令
     * @param cmd 待执行命令
     * @param log 日志输出文件
     * @param ret 是否保留命令行输出
     * @param ct 上下文
     * @return 输出
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder execRootCmd(String cmd, String log, Boolean ret, Context ct) {
        StringBuilder result = new StringBuilder();
        DataOutputStream dos = null;
        DataInputStream dis = null;
        DataInputStream des = null;
        String line = null;
        Process p;

        try {
            p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            processes.add(p);
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());
            des = new DataInputStream(p.getErrorStream());

//            while ((line = des.readLine()) != null) {
//            		logUtil.d(TAG, "ERR************" + line);
//            }

            logUtil.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

            while ((line = dis.readLine()) != null) {
                if(log != null) {
                    writeFileData(log, line, ct);
                }
                if(ret) {
                    result.append(line).append("\n");
                }
            }
            p.waitFor();
            processes.remove(p);
            isRoot = true;
        } catch (Exception e) {
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
            isRoot = false;
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    logUtil.e(TAG, "抛出异常 " + e.getMessage());
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    logUtil.e(TAG, "抛出异常 " + e.getMessage());
                }
            }
        }
        return result;
    }


    public static void writeFileData(String monkeyLog, String message, Context ct) {
        String time = "";
        try {
            FileOutputStream fout = ct.openFileOutput(monkeyLog, MODE_APPEND);

            SimpleDateFormat formatter = new SimpleDateFormat("+++   HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            time = formatter.format(curDate);

            byte [] bytes = message.getBytes();
            fout.write(bytes);
            bytes = (time + "\n").getBytes();
            fout.write(bytes);
            fout.close();
        }
        catch(Exception e) {
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
        }


    }

    public static StringBuilder execCmd(String cmd) {
        InputStreamReader isr = null;
        BufferedReader br = null;
        Process p = null;
        StringBuilder ret = new StringBuilder();
        String line = "";

        try {
            p = Runtime.getRuntime().exec(cmd);// 经过Root处理的android系统即有su命令
            isr = new InputStreamReader(p.getInputStream());

            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
//            		logUtil.d(TAG, "ERR************" + line);
                ret.append(line).append("\n");
            }
            br.close();
            p.waitFor();
        } catch (Exception e) {
            logUtil.e(TAG, "抛出异常 " + e.getMessage());
            return ret;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {

                    logUtil.e(TAG, "抛出异常 " + e.getMessage());
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    logUtil.e(TAG, "抛出异常 " + e.getMessage());
                }
            }
            if(p != null) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    logUtil.e(TAG, "抛出异常 " + e.getMessage());
                }
            }
        }
        return ret;
    }


    public static String getActivityName() {
        String result = execAdbCmd("dumpsys activity top | grep ACTIVITY | grep -o /[^[:space:]]*", 0);

        if (result.length() < 2) {
            return null;
        } else if (result.endsWith("\n")) {
            return result.substring(1, result.length()-1);
        } else {
            return result.substring(1);
        }
    }

    public static String getPageUrl() {
        String result = execAdbCmd("dumpsys activity top | grep -o ' url=[^[:space:]]*'", 0).trim();

        if (result.length() < 5) {
            return null;
        } else {
            String[] tmp = result.split("\n");
            if (tmp.length < 1) {
                return null;
            }
            result = tmp[tmp.length - 1].trim();
            int index = result.indexOf('?');
            if (index > 0) {
                return result.substring(4, index);
            } else {
                index = result.indexOf(',');
                if (index > 0) {
                    return result.substring(4, index);
                } else {
                    return result.substring(4);
                }
            }
        }
    }


    public static String getTopActivity() {
        return execAdbCmd("dumpsys activity top | grep ACTIVITY", 0);
    }

    /**
     * 获取屏幕现实的View
     * @return
     */
    public static String loadTopViews(String app) {
        if (app.isEmpty()) {
            return execAdbCmd("dumpsys SurfaceFlinger --list", 0);
        } else {
            return execAdbCmd("dumpsys SurfaceFlinger --list | grep '" + app + "'", 0);
        }
    }

    /**
     * 判断文件是否存在
     * @param file shell中文件路径
     * @return
     */
    public static boolean fileExists(String file) {
        String result = execHighPrivilegeCmd("ls " + file);

        if (result.contains("No such file")) {
            return false;
        }

        // md5没对上，重新推
        return true;
    }

    private static ScheduledExecutorService scheduledExecutorService;

    /**
     * 开始检查ADB状态
     */
    private static void startAdbStatusCheck() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // 防止重复运行，14s内只能执行一次
                if (currentTime - LAST_RUNNING_TIME < 14 * 1000) {
                    return;
                }

                LAST_RUNNING_TIME = currentTime;
                String result = null;
                try {
                    result = execAdbCmd("echo '1'", 5000);
                } catch (Exception e) {
                    logUtil.e(TAG, "Check adb status throw :" + e.getMessage());
                }

                if (result==null || !result.trim().equals("1")) {
                    // 等2s再检验一次
                    MiscUtil.sleep(2000);

                    boolean genResult = false;

                    // double check机制，防止单次偶然失败带来重连
                    String doubleCheck = null;
                    try {
                        doubleCheck = execAdbCmd("echo '1'", 5000);
                    } catch (Exception e) {
                        logUtil.e(TAG, "Check adb status throw :" + e.getMessage());
                    }
                    if (doubleCheck==null || !doubleCheck.trim().equals("1")) {
                        // 尝试恢复3次
                        for (int i = 0; i < 3; i++) {
                            // 关停无用连接
                            if (connection != null && connection.isFine()) {
                                try {
                                    connection.close();
                                } catch (IOException e) {
                                    logUtil.e(TAG, "Catch java.io.IOException: " + e.getMessage());
                                } finally {
                                    connection = null;
                                }
                            }

                            // 清理下当前已连接进程
                            clearProcesses();

                            // 尝试重连
                            genResult = generateConnection();
                            if (genResult) {
                                break;
                            }
                        }
                    }
                    if (!genResult){
                        ADB_BREAK = true;
                    }


                }

                // 15S 检查一次
                scheduledExecutorService.schedule(this, 15, TimeUnit.SECONDS);
            }
        }, 15, TimeUnit.SECONDS);
    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}