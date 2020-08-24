package com.kevin.share;

import android.os.Environment;

import org.json.JSONObject;

import java.io.File;

public final class CONST {
     private CONST() {}
     public static final String AUTOTEST= "autotest";
     public static final String REPORT= "report";
     public static final String SCREENSHOT= "screenshot";
     public static final String TESTCASES = "testcases";
     public static final String MONKEY = "monkey";
     public static final String INPUT = "input.json";
     public static final String LOGPATH = Environment.getExternalStorageDirectory().getPath() + File.separator + AUTOTEST + File.separator;
     public static final String TESTCASES_PATH = LOGPATH + TESTCASES + File.separator;
     public static final String REPORT_PATH = LOGPATH + REPORT + File.separator;
     public static final String MONKEY_PATH = LOGPATH + MONKEY + File.separator;
     public static final String MONKEY_QUERYS_FILE = MONKEY_PATH + "querys.txt";
     public static final String DUMP_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml";
     public static final String DUMP_PNG_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.png";
     public static final String CONFIG_FILE = LOGPATH + "config.json";
     public static final String UICRAWLWER_CONFIG_FILE = LOGPATH + "uicrawler.json";
     public static final String UICRAWLWER_PATH = LOGPATH + "uicrawler" + File.separator;
     public static final String TEMP_FILE = LOGPATH +"temp.txt";
     public static final String INPUT_FILE = LOGPATH + INPUT;
     public static final String TESSDATA = LOGPATH + "tessdata";
     public static final String RESOURCE = LOGPATH + "resource";
     public static final String ACCESSIBILITY_RECEIVER = "com.kevin.testool.MyAccessibility.Receiver";
     public static final String SHUTTLE_RECEIVER = "com.kevin.testool.test.shuttle";
     public static final String DRAG_RECEIVER = "com.kevin.testool.test.drag";

     public static final String ACTION_UPDATECASELIST = "com.kevin.testool.action.updatecaselist";


     public static final String TESTOOL_SETTING = "testool_setting";
     public static final String NEED_REMIND_ACCESSIBILITY = "NEED_REMIND_ACCESSIBILITY";
     public static final String EDITE_TESTCASE = "EDITE_TESTCASE";
     public static final String DUMP_FINISHED = "DUMP_FINISHED";

     public static String SERVER_IP = "127.0.0.1";
     public static String SERVER_PORT = "9999";
     public static String URL_PORT = "http://" + SERVER_IP + ":" + SERVER_PORT + "/";

    public static JSONObject CONFIG;

}

