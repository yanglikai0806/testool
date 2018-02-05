package com.kevin.testool;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CMDUtils {

    /**
     * 生成命令
     *
     * @param pkgName 包名
     * @param clsName 类名
     * @return 返回
     */

    public static String generateCommand(String pkgName, String clsName) {
        String command = "am instrument -w -r -e debug false -e class "
                + pkgName + "." + clsName + " "
                + pkgName + ".test/android.support.test.runner.AndroidJUnitRunner";
        Log.e("Test Start", command);
        return command;
    }

    private static final String TAG = "CMDUtils";

    public static class CMD_Result {
        int resultCode;
        public String error;
        public String success;

        public CMD_Result(int resultCode, String error, String success) {
            this.resultCode = resultCode;
            this.error = error;
            this.success = success;
        }

    }

    /**
     * 执行命令
     *
     * @param command         命令
     * @param isShowCommand   是否显示执行的命令
     * @param isNeedResultMsg 是否反馈执行的结果
     */
    public static CMD_Result runCMD(String command, boolean isShowCommand,
                                    boolean isNeedResultMsg) {
        if (isShowCommand)
            Log.i(TAG, "runCMD:" + command);
        CMD_Result cmdRsult = null;
        int result;
        try {
            Process process = Runtime.getRuntime().exec(command);
            result = process.waitFor();
            if (isNeedResultMsg) {
                StringBuilder successMsg = new StringBuilder();
                StringBuilder errorMsg = new StringBuilder();
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
                }
                cmdRsult = new CMD_Result(result, errorMsg.toString(),
                        successMsg.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "run CMD:" + command + " failed");
            e.printStackTrace();
        }
        return cmdRsult;
    }
}
