package com.kevin.share;

import android.os.Environment;
import android.text.TextUtils;

import org.json.JSONException;

import java.io.File;

public final class CONST {
     private CONST() {}
     public static final String AUTOTEST= "autotest";
     public static final String DATA_LOCAL_TMP= "/data/local/tmp";
     public static final String REPORT= "report";
     public static final String SCREENSHOT= "screenshot";
     public static final String TESTCASES = "testcases";
     public static final String MONKEY = "monkey";
     public static final String INPUT = "input.json";
     public static final String REFLECT = "reflect.json";
     public static final String LOGPATH = Environment.getExternalStorageDirectory().getPath() + File.separator + AUTOTEST + File.separator;
//     public static final String LOGPATH = "/data/data/com.kevin.testool/autotest/";
     public static final String TESTCASES_PATH = LOGPATH + TESTCASES + File.separator;
     public static final String REPORT_PATH = LOGPATH + REPORT + File.separator;
     public static final String MONKEY_PATH = LOGPATH + MONKEY + File.separator;
     public static final String DUMP_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml";
     public static final String DUMP_PNG_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.png";
     public static final String DUMP_JPG_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.jpg";
     public static final String TO_SPEAK_FILE = Environment.getExternalStorageDirectory().getPath() + File.separator + "last_to_speak.txt";
     public static final String CONFIG_FILE = LOGPATH + "config.json";
     public static final String SHELL_SERVER_DEX = LOGPATH + "shellserver.dex";
     public static final String UICRAWLWER_CONFIG_FILE = LOGPATH + "uicrawler.json";
     public static final String UICRAWLWER_PATH = LOGPATH + "uicrawler" + File.separator;
     public static final String TEMP_FILE = LOGPATH +"temp.txt";
     public static final String INPUT_FILE = LOGPATH + INPUT;
     public static final String REFLECT_FILE = LOGPATH + REFLECT;
     public static final String TESSDATA = LOGPATH + "tessdata";
     public static final String RESOURCE = LOGPATH + "resource";
     public static final String ACCESSIBILITY_RECEIVER = "com.kevin.testool.MyAccessibility.Receiver";
     public static final String SHUTTLE_RECEIVER = "com.kevin.testool.test.shuttle";
     public static final String DRAG_RECEIVER = "com.kevin.testool.test.drag";

     public static final String ACTION_UPDATECASELIST = "com.kevin.testool.action.updatecaselist";
     public static final String ACTION_POWER_RECORD_FINISH = "com.kevin.testool.action.power.record.finish";

     public static final String CROPPER_IMG_PATH = CONST.LOGPATH + "cropper.jpg";
     public static final float CROPPER_IMG_SCALE = 0.8f;


     public static final String TESTOOL_SETTING = "testool_setting";
     public static final String NEED_REMIND_ACCESSIBILITY = "NEED_REMIND_ACCESSIBILITY";
     public static final String EDITE_TESTCASE = "EDITE_TESTCASE";
     public static final String DUMP_FINISHED = "DUMP_FINISHED";
     public static final String[] TEST_RESULT_URL = {};
     public static final String[] TEST_TASK_URL = {};
     public static final String[] NOT_CLEAR_RECENT_APP = {};
     public static final int ADB_PORT = 5555; //无线adb使用端口

     public static String TARGET_APP = "";

     static {
          try {
               TARGET_APP = Common.CONFIG().getString("TARGET_APP");
          } catch (JSONException e) {
               e.printStackTrace();
          }
     }


     public static String SERVER_IP = "";

     static {
          try {
               SERVER_IP = Common.CONFIG().getString("SERVER_IP");
          } catch (JSONException e) {
               e.printStackTrace();
          }
     }

     public static String SERVER_PORT = "9999";

     static {
          try {
               SERVER_PORT = Common.CONFIG().getString("SERVER_PORT");
          } catch (JSONException e) {
               e.printStackTrace();
          }
     }

     public static String SERVER_BASE_URL = "";

     static {
          try {
               if (!TextUtils.isEmpty(Common.CONFIG().getString("SERVER_BASE_URL"))){
                    SERVER_BASE_URL = Common.CONFIG().getString("SERVER_BASE_URL");
               }
          } catch (JSONException e) {
               e.printStackTrace();
          }
     }

     public static String CONFIG_JSON = "{\n" +
             "    \"ALARM_MSG\": \"false\",\n" +
             "    \"CASE_TAG\": \"\",\n" +
             "    \"CHECK_TYPE\": 2,\n" +
             "    \"GET_ELEMENT_BY\": -1,\n" +
             "    \"DEBUG\": \"true\",\n" +
             "    \"MUTE\": \"true\",\n" +
             "    \"LOG\": \"false\",\n" +
             "    \"MP42GIF\": \"false\",\n" +
             "    \"OFFLINE\": \"false\",\n" +
             "    \"POP_WINDOW_LIST\": [\n" +
             "        \"同意并继续\",\n" +
             "        \"允许\",\n" +
             "        \"始终允许\",\n" +
             "        \"仅在使用中允许\",\n" +
             "        \"确定\",\n" +
             "        \"同意\",\n" +
             "        \"继续\",\n" +
             "        \"好\",\n" +
             "        \"暂不升级\",\n" +
             "        \"跳过\",\n" +
             "        \"立即体验\",\n" +
             "        \"知道了\",\n" +
             "        \"我知道了\",\n" +
             "        \"立即开通\",\n" +
             "        \"我同意\",\n" +
             "        \"继续安装\",\n" +
             "        \"接受\",\n" +
             "        \"以后再说\",\n" +
             "        \"同意并使用\",\n" +
             "        \"您已阅读并同意\",\n" +
             "        \"同意并加入\"\n" +
             "    ],\n" +
             "    \"POST_RESULT\": \"true\",\n" +
             "    \"RECORD_CURRENT\": \"false\",\n" +
             "    \"RECORD_MEMINFO\": \"false\",\n" +
             "    \"RETRY\": \"1\",\n" +
             "    \"SCREENSHOT\": \"true\",\n" +
             "    \"SCREEN_LOCK_PW\": \"0000\",\n" +
             "    \"SCREEN_RECORD\": \"false\",\n" +
             "    \"SERVER_BASE_URL\": \"\",\n" +
             "    \"TABLE\": \"\",\n" +
             "    \"TARGET_APP\": \"\",\n" +
             "    \"TEST_ENV\": \"production\",\n" +
             "    \"TEST_TAG\": \"\",\n" +
             "    \"DISABLE_CLEAR_RECENT_APP\": \"false\",\n" +
             "    \"REMOTE_DEVICE_IP\": \"\"\n" +
             "}";



}

