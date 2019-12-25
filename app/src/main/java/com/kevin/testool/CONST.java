package com.kevin.testool;

import android.os.Environment;
import com.kevin.testool.common.Common;

import org.json.JSONException;
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
     public static final String CONFIG_FILE = LOGPATH + "config.json";
     public static final String TEMP_FILE = LOGPATH +"temp.txt";
     public static final String INPUT_FILE = LOGPATH + INPUT;
     public static final String TESSDATA = LOGPATH + "tessdata";
    public static final String UICRAWLWER_CONFIG_FILE = LOGPATH + "uicrawler.json";




    public static JSONObject CONFIG;

    static {
        try {
            CONFIG = new JSONObject(MyFile.readJsonFile(CONFIG_FILE));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

