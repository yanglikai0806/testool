package com.kevin.share.utils;

import android.os.Environment;
import android.util.Log;

import com.kevin.share.CONST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.kevin.share.Common.CONFIG;

/**
 * 将Log日志写入文件中
 */
public class logUtil {

    private static String TAG = "KEVIN_DEBUG";

    private static String logPath = null;//log日志存放路径

    private static String screenshotPath = null;//截图存放存放路径

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;

    private static Date date = new Date();//因为log日志是使用日期命名的，使用静态成员变量主要是为了在整个程序运行期间只存在一个.log文件中;


    private static final char VERBOSE = 'v';

    private static final char DEBUG = 'd';

    private static final char INFO = 'i';

    private static final char WARN = 'w';

    private static final char ERROR = 'e';
    private static final char UICRAWLER = 'u';
    private static final char TESTOOL = 't';

    public static boolean isShow = true;
    public static String test_log = "";
    public static boolean recordTestLog = false;
    private static boolean isDebug = true;

    static {
        try {
            isDebug = CONFIG().getString("DEBUG").equals("true");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean toShow(boolean isshow){
        isShow = isshow;
        return isShow;
    }

    public static void v(String tag, String msg) {

        Log.v(TAG + tag, msg);

    }

    public static void d(String tag, Object msg) {
        if(isDebug) {
            Log.d(TAG + tag, msg + "");
//            writeToFile(TESTOOL, tag, msg + "");
        }
//        writeToFile(DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        if(isShow) {
            writeToFile(INFO, tag, msg);
        }
        Log.i(TAG + tag, msg);
    }

    public static void f(String tag, String msg) {
        if (isShow) {
            writeToFile(INFO, tag, msg);
        }
    }

    public static void w(String tag, String msg) {

        Log.w(TAG + tag, msg);

//        writeToFile(WARN, tag, msg);
    }

    public static void e(String tag, String msg) {

        Log.e(TAG + tag, msg);
        writeToFile(TESTOOL, tag, msg + "");
//        writeToFile(ERROR, tag, msg);
    }

    public static void e(String tag, Throwable e) {

        Log.e(TAG + tag, Log.getStackTraceString(e));
        writeToFile(TESTOOL, tag, Log.getStackTraceString(e));
//        writeToFile(ERROR, tag, Log.getStackTraceString(e));
    }
    public static void u(String tag, String msg) {
        System.out.println(tag + ":" + msg);
        writeToFile(UICRAWLER, tag, msg);
    }

    /**
     * 将log信息写入文件中
     *
     * @param type
     * @param tag
     * @param msg
     */
    private static void writeToFile(char type, String tag, String msg) {
        try {
            if (type == UICRAWLER){
                logPath = CONST.LOGPATH + "UICrawler" + File.separator ;//获得文件储存路径
            } else if (type == TESTOOL) {
                logPath = CONST.LOGPATH;
            }else {
                logPath = CONST.REPORT_PATH + readTempFile() + File.separator ;//获得文件储存路径
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String fileName = logPath +  File.separator + dateFormat.format(new Date()) + ".txt";//log日志名，使用时间命名，保证不重复
        String fileName = logPath + "log.txt";//log日志名，使用时间命名，保证不重复
        String log = dateFormat.format(new Date()) + ":" + type + " " + tag + " " + msg + "\n";//log日志内容，可以自行定制

        //如果父路径不存在
        File Folder = new File(logPath);
        if (!Folder.exists()) {
            Folder.mkdirs();//创建父路径
            File file = new File(fileName);
            if (! file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        //记录单条case执行log
        if(tag.equals("CASEID")){
            test_log = "";
            recordTestLog = true;
        }
        if (recordTestLog){
            test_log = test_log + log;
        }
        if(tag.equals("RESULT")){
            recordTestLog = false;
        }
        FileUtils.writeFile(fileName, log, true);

    }

    public static String readTempFile() throws IOException {
        String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
        File AutoTestFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "AutoTest");
        if (!AutoTestFolder.exists()){
            AutoTestFolder.mkdirs();
        }
        StringBuilder sb = new StringBuilder();
        File file = new File(tempFile);
        if (!file.exists()){
            file.createNewFile();
        }
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = "";
        line = br.readLine();
        sb.append(line);
        br.close();
        return sb.toString().split(":")[0];
    }

}
