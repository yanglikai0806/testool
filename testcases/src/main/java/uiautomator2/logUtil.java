package uiautomator2;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 将Log日志写入文件中
 * <p>
 * 使用单例模式是因为要初始化文件存放位置
 * <p>
 * Created by waka on 2016/3/14.
 */
public class logUtil {

    private static String TAG = "LogToFile";

    private static String logPath = null;//log日志存放路径

    private static String screenshotPath = null;//截图存放存放路径

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;

    private static Date date = new Date();//因为log日志是使用日期命名的，使用静态成员变量主要是为了在整个程序运行期间只存在一个.log文件中;

//    public static void init() {
//        try {
//            logPath = Environment.getExternalStorageDirectory() + File.separator + "AutoTest"+File.separator +readTempFile();//获得文件储存路径
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static final char VERBOSE = 'v';

    private static final char DEBUG = 'd';

    private static final char INFO = 'i';

    private static final char WARN = 'w';

    private static final char ERROR = 'e';

    public static void v(String tag, String msg) {
        writeToFile(VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        writeToFile(DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        writeToFile(INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        writeToFile(WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        writeToFile(ERROR, tag, msg);
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
            logPath = Environment.getExternalStorageDirectory() + File.separator + "AutoTest"+File.separator +readTempFile();//获得文件储存路径

        } catch (IOException e) {
            e.printStackTrace();
        }

//        String fileName = logPath +  File.separator + dateFormat.format(new Date()) + ".txt";//log日志名，使用时间命名，保证不重复
        String fileName = logPath +  File.separator + "log.txt";//log日志名，使用时间命名，保证不重复
        String log = dateFormat.format(date) + ":" + type + " " + tag + " " + msg + "\n";//log日志内容，可以自行定制

        //如果父路径不存在
        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdirs();//创建父路径
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

    private static String readTempFile() throws IOException {
        String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
        StringBuilder sb = new StringBuilder();
        File file = new File(tempFile);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = "";
        line = br.readLine();
        sb.append(line);
        br.close();
        return sb.toString();
    }

}
