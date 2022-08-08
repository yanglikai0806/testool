package com.kevin.share;

import android.os.SystemClock;

import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ErrorDetect extends Checkpoint {

    public static String ERROR_FILE = "monkey_error.txt";
    public static int USER_WAIT_TIME = 1800000;
    private static int errorCount = 0;
    private static List<String> errorList = new ArrayList<>();

    public static long startDetectCrashAnr(String logDir){
        FileUtils.creatDir(logDir);

        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(1000);
                monitorFcAndAnr(logDir, CONST.TARGET_APP);
            }
        }).start();

        String _log = "2>>" + logDir +File.separator+ ERROR_FILE +" 1>/dev/null ";
        String mksFile = CONST.LOGPATH + "uicrawler.mks";
        if (!new File(mksFile).exists()){
            String content = "type=user\n" +
                    "count=10\n" +
                    "speed=1.0\n" +
                    "start data >>\n" +
                    "UserWait ("+USER_WAIT_TIME+")";

            FileUtils.writeFile(mksFile, content, false);

        }
        String monkey_cmd = String.format("monkey -f %s -v 1 %s", mksFile, _log);
        ShellUtils.runShellCommand(monkey_cmd, -100);
        return System.currentTimeMillis();
    }

    public static boolean isDetectCrash(String logDir){
        try {
            String logContent = FileUtils.readFile(logDir + File.separator + ERROR_FILE);
            if (logContent.contains("// CRASH")){
                logUtil.u("ErrorDetect", "-------------发现crash--------------");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isDetectAnr(String logDir){
        try {
            String logContent = FileUtils.readFile(logDir + File.separator + ERROR_FILE);
            if (logContent.contains("NOT RESPONDING:")){
                logUtil.u("ErrorDetect", "-------------发现anr--------------");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void monitorFcAndAnr(String logDir, String pkg){
        logUtil.d("", "正在监听crash&anr异常");
        while (true) {
            try {
                int merrorcont = errorCount;
                int nerrorCount = readMonkeyLog(logDir + ERROR_FILE, pkg);
                if (nerrorCount - merrorcont > 0) {
                    Common.generateBugreport(logDir + File.separator + "bugreport_" + dateFormat.format(new Date()) + ".txt");
                    errorCount = nerrorCount;
                }
            } catch (IOException e) {
                logUtil.e("", e);
            }
            SystemClock.sleep(60000);
        }
    }

    private static int readMonkeyLog(String filePath, String pkg) throws IOException {
        int _errorCount = 0;
//        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        boolean isRealError = false;
        while((line = br.readLine()) != null){
//            logUtil.d("readMonkeyLog:", line);
            if (line.contains("CRASH:") | line.contains("NOT RESPONDING:") && line.contains(pkg)){
                isRealError = true;
                continue;
            }
            if (isRealError){
                if (line.contains("Long Msg:")){
                    String errorMsg = line.split("Long Msg:")[1].trim();
                    if (!errorList.contains(errorMsg)){
                        _errorCount += 1;
                        errorList.add(errorMsg);
                        isRealError = false;
                    }
                }
                if (line.contains("Reason:")){
                    String errorMsg = line.split("Reason:")[1].trim();
                    if (!errorList.contains(errorMsg)){
                        _errorCount += 1;
                        errorList.add(errorMsg);
                        isRealError = false;
                    }
                }
            }
        }
        br.close();
        return _errorCount;
    }



}
