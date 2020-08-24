package com.kevin.share;

import com.kevin.share.utils.AdbUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.logUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ErrorDetect extends Checkpoint {

    private static String ERROR_FILE = "error.txt";

    public static long startDetectCrashAnr(String logDir){

        FileUtils.creatDir(logDir);
        String _log = "2>" + logDir +File.separator+ ERROR_FILE +" 1> " + logDir + File.separator +"info.txt";
        String mksFile = CONST.LOGPATH + File.separator + "uicrawler.mks";
        if (!new File(mksFile).exists()){
            String content = "type=user\n" +
                    "count=10\n" +
                    "speed=1.0\n" +
                    "start data >>\n" +
                    "UserWait (20000)";
            try {
                FileUtils.writeFile(mksFile, content, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        String monkey_cmd = String.format("monkey -f %s -v 1 %s", mksFile, _log);
        AdbUtils.runShellCommand(monkey_cmd, -100);
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



}
