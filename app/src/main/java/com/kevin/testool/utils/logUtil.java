package com.kevin.testool.utils;

import android.os.Environment;
import android.util.Log;

import com.kevin.testool.CONST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 将Log日志写入文件中
 * <p>
 * 使用单例模式是因为要初始化文件存放位置
 * <p>
 */
public class logUtil {

    private static String TAG = "";

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

    public static boolean isShow = true;

    public static boolean toShow(boolean isshow){
        isShow = isshow;
        return isShow;
    }

    public static void v(String tag, String msg) {
        if(isShow) {
            Log.v(TAG + tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if(isShow) {
            Log.d(TAG + tag, msg);
        }
//        writeToFile(DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        if(isShow) {
            Log.i(TAG + tag, msg);
            writeToFile(INFO, tag, msg);
        }
    }

    public static void f(String tag, String msg) {
        if (isShow) {
            writeToFile(INFO, tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if(isShow) {
            Log.w(TAG + tag, msg);
        }
//        writeToFile(WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        if(isShow) {
            Log.e(TAG + tag, msg);
            writeToFile(ERROR, tag, msg);
        }
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
                logPath = CONST.LOGPATH + "UICrawler";//获得文件储存路径
            } else {
                logPath = CONST.REPORT_PATH + readTempFile();//获得文件储存路径
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String fileName = logPath +  File.separator + dateFormat.format(new Date()) + ".txt";//log日志名，使用时间命名，保证不重复
        String fileName = logPath +  File.separator + "log.txt";//log日志名，使用时间命名，保证不重复
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

        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {

            fos = new FileOutputStream(fileName, true);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);
//            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
    public static void editCaseJsonFile(String Name, String msg) throws IOException {
//        logPath = Environment.getExternalStorageDirectory() + File.separator + "AutoTest";//获得文件储存路径
        String fileName = CONST.TESTCASES_PATH  + Name +".json";

        File Folder = new File(CONST.LOGPATH);
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

        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {
            String content = readToString(fileName);
//            System.out.println(content);

            fos = new FileOutputStream(fileName, false);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            assert content != null;
            if (content.isEmpty()) {
                bw.write("[\n" + msg + "]");
            } else {
                bw.write(content.trim().substring(0,content.length()-1).trim() + ",\n" + msg + "]");
//                System.out.println("["+content.trim().substring(1,content.length()-1) + ",\n" + msg + "]");
            }

//            bw.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String readToString(String fileName) {
        String encoding = "UTF-8";
        File file = new File(fileName);
        if (!file.exists()){
            return null;
        }
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

}
