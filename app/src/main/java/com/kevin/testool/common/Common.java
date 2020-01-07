package com.kevin.testool.common;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.kevin.testool.MyApplication;
import com.kevin.testool.checkpoint.Checkpoint;
import com.kevin.testool.utils.AdbUtils;
import com.kevin.testool.utils.AppUtils;
import com.kevin.testool.CONST;
import com.kevin.testool.MyFile;
import com.kevin.testool.utils.logUtil;
import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import android.util.Base64;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.kevin.testool.CONST.REPORT_PATH;
import static com.kevin.testool.CONST.TESSDATA;

public abstract class Common {
    public static String TAG = "";
    private static JSONArray fileList;
    public static String SCREENSHOT = "screenshot";

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;

    private static Date date = new Date();
    private static ArrayList<Node> nodeset;
    private static int n;
    private static int m;

    public static void goBackHome(int back) {
        for (int i = 0; i < back; i++) {
            AdbUtils.runShellCommand("input keyevent 4\n", 0);
            SystemClock.sleep(300);
        }
        AdbUtils.runShellCommand("input keyevent 3\n", 0);

    }

    public static void unlockScreen(String password) {
        JSONArray x_y = getScreenSize();
        int x = 0;
        int y = 0;
        try {
            x = x_y.getInt(0);
            y = x_y.getInt(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        SystemClock.sleep(500);
        if (!isScreenLocked()) {
            return;
        } else {
            Common.wakeup();
        }
        if (password.length() > 0) {
            if (get_elements(true, "content-desc", "密码区域", 0) == null) {
                swipe(0.5 * x, 0.9 * y, 0.5 * x, 0.1 * y, 200);
            }
//            SystemClock.sleep(100);
            for (int i = 0; i < password.length(); i++) {
                AdbUtils.runShellCommand("input keyevent KEYCODE_NUMPAD_" + password.substring(i, i + 1) + "\n", 0);

            }

        } else {
            swipe(0.5 * x, 0.9 * y, 0.5 * x, 0.1 * y, 500);
        }
//        SystemClock.sleep(200);
        logUtil.i(TAG, "完成解锁操作");

    }

    public static void unlock(String password) {
        click(500, 200);
        for (int i = 0; i < password.length(); i++) {
            AdbUtils.runShellCommand("input keyevent KEYCODE_NUMPAD_" + password.substring(i, i + 1) + "\n", 0);
        }
        logUtil.i(TAG, "完成解锁操作");

    }

    public static void wakeup() {
        if (!isScreenOn()) {
            pressPower();
            logUtil.i(TAG, "唤醒屏幕");
        }
    }

    public static void press(String key) {
        switch (key) {
            case "home":
                AdbUtils.runShellCommand("input keyevent 3\n", 0);
                break;
            case "back":
                AdbUtils.runShellCommand("input keyevent 4\n", 0);
                break;
            case "recent":
                AdbUtils.runShellCommand("input keyevent KEYCODE_MENU\n", 0);
                break;
            case "power":
                AdbUtils.runShellCommand("input keyevent 26\n", 0);
                break;
        }
    }

    public static void pressPower() {
        AdbUtils.runShellCommand("input keyevent 26\n", 0);
        SystemClock.sleep(100);
    }

    public static Boolean isScreenOn() {
        PowerManager pm = (PowerManager) MyApplication.getContext().getSystemService(Service.POWER_SERVICE);
        logUtil.i(TAG, "是否亮屏：" + pm.isScreenOn());
        return pm.isScreenOn();

    }

    public static Boolean isScreenLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) MyApplication.getContext().getSystemService (Context. KEYGUARD_SERVICE);
        boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
        logUtil.i(TAG, "是否锁屏：" + flag);
        return flag;

    }

    public static void lockScreen() {

        AdbUtils.runShellCommand("input keyevent 26\n", 0);
        SystemClock.sleep(200);
        logUtil.i(TAG, "锁屏");
    }

    public static JSONArray getScreenSize() {
        String SS;
        String[] _SS;
        String _x;
        String _y;
        String screen_size = "screen_size";
        JSONArray x_y = new JSONArray();
        try {
            if (!CONFIG().isNull(screen_size)) {
                x_y = CONFIG().getJSONArray(screen_size);
                if (x_y.length() > 0) {
                    return x_y;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SS = AdbUtils.runShellCommand("dumpsys window displays |grep init=\n", 0);
        _SS = SS.trim().split(" ");
        _SS = _SS[0].split("=");
        _SS = _SS[1].split("x");
        _x = _SS[0];
        _y = _SS[1];
        x_y.put(Integer.valueOf(_x));
        x_y.put(Integer.valueOf(_y));
        JSONObject screenSize = null;
        try {
            screenSize = new JSONObject(String.valueOf(CONFIG()));
            screenSize.put(screen_size, x_y);
            try {
                MyFile.writeFile(CONST.CONFIG_FILE, screenSize.toString().replace(",\"", ",\n\""), false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return x_y;
    }

    public static void openNotification() {
        JSONArray x_y = getScreenSize();
        int x = 0;
        int y = 0;
        try {
            x = x_y.getInt(0);
            y = x_y.getInt(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        swipe(0.5 * x, 0.01 * y, 0.5 * x, 0.8 * y, 500);
        logUtil.i(TAG, "打开通知栏");

    }

//    public static void swipe(String x, String y, String X, String Y, String step) {
//        AdbUtils.runShellCommand("input swipe " + x + " " + y + " " + X + " " + Y + " " + step + "\n", 0);
//    }

    public static JSONArray swipe(double a, double b, double A, double B, int step) {
        int X = 1;
        int Y = 1;
        if (a < 1 & b < 1 & A < 1 & B < 1) {
            JSONArray x_y = Common.getScreenSize();
            try {
                X = x_y.getInt(0);
                Y = x_y.getInt(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        AdbUtils.runShellCommand("input swipe " + a * X + " " + b * Y + " " + A * X + " " + B * Y + " " + step, 0);
        JSONArray xys = new JSONArray();
        try {
            xys.put(a);
            xys.put(b);
            xys.put(A);
            xys.put(B);
            xys.put(step);
        } catch (JSONException e) {
            return null;
        }
        return xys;
    }

    public static void uninstallApp(String pkgName) {
        AdbUtils.runShellCommand("pm uninstall " + pkgName, 0);
        SystemClock.sleep(5000);
    }

    public static String startActivity(String activityName) {
//        logUtil.i(TAG, "打开activity：" + activityName);
        if (!getActivity().contains(activityName)) {
            String res = AdbUtils.runShellCommand("am start -n " + activityName, 0);
            SystemClock.sleep(1000);
            return res;
        } else {
            return "Starting: Intent { " + activityName + " }";
        }
    }

    public static boolean startActivityWithUri(String uri) {
        boolean success = false;
        try {
            final Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            PackageManager manager = MyApplication.getContext().getPackageManager();
            List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
            if (infos.size() > 0) {
                //Then there is an Application(s) can handle your intent
                AdbUtils.runShellCommand(String.format("am start \"%s\"", uri), 0);
                success = true;
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intent);
            } else {
                //No Application can handle your intent
                logUtil.d(TAG, "No Application can handle your intent");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static String launchActivity(Object activity) throws JSONException {
        if (activity instanceof JSONArray) {
            Random random = new Random();
            int i = random.nextInt(((JSONArray) activity).length());
            return startActivity(((JSONArray) activity).get(i).toString());

        } else {
            return startActivity(activity.toString());
        }
    }

    public static void launchApp(Context context, String activityName) {
//        logUtil.i(TAG, "打开activity：" + activityName);
        if (activityName.contains("/")) {
            String pkg_name = activityName.split("/")[0];
            String act_name = activityName.split("/")[1];
            Intent it = new Intent();
            it.setComponent(new ComponentName(pkg_name, act_name)); //包名和类名
            context.startActivity(it);
        } else {
            PackageManager packageManager = context.getPackageManager();
            Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(activityName);
            if (launchIntentForPackage != null) {
                context.startActivity(launchIntentForPackage);
            } else {
                logUtil.d("", "应用未安装：" + activityName);
            }

        }
    }

    public static void killApp(String pkgName) {
        AdbUtils.runShellCommand("am force-stop " + pkgName, 0);
        SystemClock.sleep(1000);
    }

    public static void clearRecentApp() {
        press("home");
        press("recent");
        String _id = "com.android.systemui:id/clearAnimView"; //清理后台控件id，默认为小米手机miui系统
        try {
            if (!Objects.requireNonNull(CONFIG()).isNull(_id)) {
                JSONArray XY = Objects.requireNonNull(CONFIG()).getJSONArray(_id);
                if (XY.length() > 0) {
                    click(Double.valueOf(XY.get(0).toString()), Double.valueOf(XY.get(1).toString()));
                    SystemClock.sleep(2000);
                    return;
                }
            }
            SystemClock.sleep(1000);
            JSONArray XY = click_element(true, "resource-id", _id, 0, 0);
            JSONObject CONFIG_UPDATE = new JSONObject(String.valueOf(CONFIG()));
            CONFIG_UPDATE.put(_id, XY);
            // 坐标值缓存在配置文件，以提高执行效率
            MyFile.writeFile(CONST.CONFIG_FILE, CONFIG_UPDATE.toString().replace(",\"", ",\n\""), false);
        } catch (FileNotFoundException | JSONException e) {
            logUtil.d("", "config.json 文件不存在");
            click_element(true, "resource-id", _id, 0, 0);
        }
    }

    public static JSONObject CONFIG() throws JSONException {

        return new JSONObject(MyFile.readJsonFile(CONST.CONFIG_FILE));
    }

    public static void click(double x, double y) {
        int X = 1;
        int Y = 1;
        if (x < 1 & y < 1) {
            JSONArray x_y = Common.getScreenSize();
            try {
                X = x_y.getInt(0);
                Y = x_y.getInt(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        AdbUtils.runShellCommand("input tap " + x * X + " " + y * Y, 0);
    }

    public static JSONArray click_element(Boolean refresh, String key, String value, int nex, int index) {
        String points;
        String[] x_y;
        ArrayList<Element> element = get_elements(refresh, key, value, nex);
        if (element != null) {

            points = element.get(index).attribute("bounds").getValue();
//            System.out.println(points);
            x_y = points.replace("[", "").replace("]", ",").split(",");
            int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
            int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
            click(x, y);
//            AdbUtils.runShellCommand("input tap "+ x +" "+ y + "\n", 0);

            logUtil.i(TAG, "完成点击元素 " + key + ":" + value);
            try {
                return new JSONArray().put(0, x).put(1, y);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            logUtil.i(TAG, String.format("没有找到 %s ： %s", key, value));
        }
        return null;
    }

    public static ArrayList<Element> get_elements(Boolean fresh, String key, String value, int nex) {
//        Element targetElement = null;
        ArrayList<Element> targetElements;
        File filePath = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml");
        if (fresh) {
            try {
                if (filePath.exists()){
                    filePath.delete();
                }
//                String res = AdbUtils.runShellCommand("uiautomator dump\n", 0);
                String res = AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.github.uiautomator.GetDumpTest' com.github.uiautomator.test/android.support.test.runner.AndroidJUnitRunner", -1800);
                boolean complete = false;
                int wait = 0;
                while (!complete){
                    if (MyFile.readFile(filePath.toString()).contains("</hierarchy>")){
                        complete = true;
                    } else {
                        SystemClock.sleep(100);
                        wait += 100;
                    }
                    if (wait > 60000){
                        complete = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            targetElements = parserXml(filePath.toString(), key, value, nex);//.get(nex);
            if (targetElements.size() > 0) {
                return targetElements;
            }
//                logUtil.i("", targetElements.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logUtil.i("", String.format("no target elements \"%s\"：\"%s\"", key, value));

        }
        return null;

    }

    public static Element get_element_xml(String filePath, ArrayList<String> keys, ArrayList<String> values, int nex) {
        ArrayList<Element> targetElements = new ArrayList<>();
        ArrayList<Element> targetElem = new ArrayList<>();

        try {
            targetElements = parserXml(filePath, keys.get(0), values.get(0), nex);//.get(nex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (targetElements.size() > 0) {
                for (Element elem:targetElements){
                    ArrayList<Boolean> res = new ArrayList<>();
                    for (int i=1; i < keys.size(); i++){
                        if (elem.attributeValue(keys.get(i)).equals(values.get(i))){
                            res.add(true);
                        } else {
                            res.add(false);
                        }
                    }
                    if (!res.contains(false) && res.size() > 0){
                        targetElem.add(elem);
                    }
                }
            }
        if (targetElem.size() == 1){
            return targetElem.get(0);
        }
        return null;

    }

    public static void inputText(String key, String value, String msg, String... mode) {
        JSONObject inputObj = new JSONObject();
        try {
            inputObj.put("key", key);
            inputObj.put("value", value);
            inputObj.put("msg", msg);
            inputObj.put("mode", mode);
            try {
                MyFile.writeFile(CONST.INPUT_FILE, inputObj.toString(), false);
                AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.github.uiautomator.Input#input' com.github.uiautomator.test/android.support.test.runner.AndroidJUnitRunner", 0);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Element> parserXml(String filename, String key, String value, int nex) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(filename));

        //获取文档根节点
        Element root = document.getRootElement();
        //输出根标签的名字
//        System.out.println(root.getName());
        //获取根节点下面的所有子节点（不包过子节点的子节点）
//        List<Element> list = root.elements();
        //遍历List的方法

        //获得指定节点下面的子节点
        List<Element> nodeElems = root.elements("node");//首先要知道自己要操作的节点。
        //定义符合预期的元素容器集合
        ArrayList<Element> elem_list = new ArrayList<>();
        //定义所有
        nodeset = new ArrayList<>();
        n = 0;
        m = 0;
        for (Element nodeElem : nodeElems) {
            //调用下面获取子节点的递归函数。
            getChildNodes(nodeElem, key, value, elem_list, nex);
        }
//        Log.d("debug", "parserXml: "+ elem_list.size());
        return elem_list;

    }

    //递归查询节点函数,输出节点名称
    private static void getChildNodes(Element elem, String key, String value, List<Element> elem_list, int nex) {
        Iterator<Node> it = elem.nodeIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node instanceof Element) {

                Element e1 = (Element) node;
//                if (nex < 0) {
//                    tmpIt.add(e1);
//                }
                if (nex != 0) {
                    nodeset.add(e1);
                    n = n + 1;
                }

                if (key.equals("")) {
                    elem_list.add(e1);
                } else {
                    if (e1.attribute(key).getValue().equals(value)) {
                        if (nex == 0) {
                            elem_list.add(e1);
                        }
                        if (nex > 0) {
                            int i = 0;
                            while (i < nex) {
                                if (it.hasNext()) {
                                    if (it.next() instanceof Element) {
                                        if (i + 1 == nex) {
                                            e1 = (Element) it.next();
                                        } else {
                                            i = i + 1;
                                        }
                                    }
                                } else {
                                    m = n;
                                    break;
                                }
                            }
                        }
                        if (nex < 0 && (nodeset.size() + nex) >= 1) {
                            e1 = (Element) nodeset.get(nodeset.size() + nex - 1);
                            elem_list.add(e1);

                        }
                    }

                }

                if (m > 0) {
                    if (nodeset.size() == (m + nex)) {
                        System.out.println(nodeset.get(m + nex - 1));
                        elem_list.add((Element) nodeset.get(m + nex - 1));
                    }
                }
                getChildNodes(e1, key, value, elem_list, nex);
            }
        }
    }

    public static String getActivity() {
//        String res = AdbUtils.runShellCommand("dumpsys input | grep FocusedApplication\n", 0);
//        String[] act = res.trim().split("name=")[1].split("u0")[1].trim().split(" ");
        String res = AdbUtils.runShellCommand("dumpsys activity top | grep ACTIVITY | tail -n 1", 0);
        String act = res.trim().split("ACTIVITY ")[1].split(" ")[0];
        return act.trim();
    }

    public static void generateBugreport(final String logFileName) {
        final File bugreportFile = new File(logFileName);
        new Thread() {
            @Override
            public void run() {
                AdbUtils.runShellCommand("bugreport > " + bugreportFile, 600000);
                try {
                    MyFile.ZipFolder(logFileName, logFileName.replace(".txt", ".zip"));
                    MyFile.deleteFile(logFileName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static String screenShot() {
        String img;
        try {
            String png = dateFormat.format(new Date()) + ".png";
            img = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + png;
            AdbUtils.runShellCommand("screencap -p " + img + "\n", 0);
            logUtil.i("", png);
            return png;

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void screenRecorder(String time, String file){
        AdbUtils.runShellCommand(String.format("screenrecord  --time-limit %s %s", time, file), -15000);
    }

    public static String screenImage(boolean refresh) {
        String img;
        String png = "screen_image.png";
        img = TESSDATA + File.separator + png;
        File file = new File(TESSDATA);
        if (!file.exists()) {
            file.mkdirs();
        }
        if (refresh) {
            AdbUtils.runShellCommand("screencap -p " + img + "\n", 0);
        }
        logUtil.i("", png);
        return img;

    }

    public static String getDeviceName() {
        return android.os.Build.MODEL;
//        String res = AdbUtils.runShellCommand("getprop ro.product.model\n",0);
//        return res.trim();
    }


    public static String getDeviceAlias() {
        return Build.DEVICE;
    }


    public static String getRomName() {
        return Build.VERSION.INCREMENTAL;
    }

    public static String getSerialno() {
        return AdbUtils.runShellCommand("getprop ro.serialno", 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public static String getSerialno(Context context) {
        return Build.getSerial();
    }

    public static String getVersionName(Context context, String pkg){

        return AppUtils.getVersionName(context, pkg);

    }
    public static int getVersionCode(Context context, String pkg){

        return AppUtils.getVersionCode(context, pkg);

    }


    public static String runMonkey(String pkg, String count, String throttle, String seed,String logDir){
        if (pkg.contains("/")){
            pkg = pkg.split("/")[0];
        }
//        String dt = dateFormat.format(date);
//        String logDir = MONKEY_PATH + getVersionName2(pkg) + File.separator + dt + File.separator;
        MyFile.creatDir(logDir);
        String _log = "2>" + logDir + "monkey_error.txt 1> " + logDir + "monkey_info.txt";
        String monkey_cmd = String.format("monkey -p %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s ", pkg, throttle, count) + _log + "\n";

        if (seed != null && seed.length() > 0){
            monkey_cmd = String.format("monkey -p %s -s %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, seed, throttle, count) + _log + "\n";

        }
        AdbUtils.runShellCommand(monkey_cmd, 0);
        return logDir + "monkey_error.txt";
    }

    public static String getMonkeyProcess(){
        return AdbUtils.runShellCommand("ps -A| grep monkey\n", 0);
    }

    private static JSONArray getAllTestCases(Context context, String dir, String _type, String flag, String tag){
        if (flag.equals("reload")){
            fileList = new JSONArray();
        }
        File casesDir = new File(dir);
        if (! casesDir.exists()){
            Toast.makeText(context.getApplicationContext(), "导入用例失败", Toast.LENGTH_SHORT).show();
        }
        File[] files = casesDir.listFiles();
        for (File _file : files) {//遍历目录
            if(_file.isFile() && _file.getName().endsWith(_type)){
                String _name=_file.getName();
                String filePath = _file.getAbsolutePath();//获取文件路径
                String fileName = _file.getName().substring(0,_name.length()-5);//获取文件名

                try {
                    JSONObject _fInfo = new JSONObject();
                    _fInfo.put(fileName, filePath);
                    if (tag.length()>0){
                        _fInfo.put("name", tag+"/"+fileName);
                    }else{
                        _fInfo.put("name", fileName);
                    }
                    _fInfo.put("path", filePath);
                    fileList.put(_fInfo);
                }catch (Exception ignored){

                }
            } else if(_file.isDirectory()){//查询子目录
                logUtil.d("", _file.getName());
                getAllTestCases(context,_file.getAbsolutePath(), _type, "append", _file.getName());
            }
        }
        return fileList;

    }

    public static ArrayList<String> getCaseList(Context context){
        ArrayList<String> cases_list = new ArrayList<String>();//获取测试用例
        JSONArray fileList;
        File testcasesFolder = new File(CONST.TESTCASES_PATH);
        if (! testcasesFolder.exists()){
            Toast.makeText(context.getApplicationContext(), "未发现用例配置文件", Toast.LENGTH_SHORT).show();
            return null;
        }
        fileList = getAllTestCases(context, CONST.TESTCASES_PATH, "json", "reload", "");
        for (int i=0; i < fileList.length(); i++ ){
            JSONObject caseItem;
            try {
                caseItem = fileList.getJSONObject(i);

                try{
                    cases_list.add(caseItem.get("name") + "");
                } catch (Exception ignored){

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(cases_list, String.CASE_INSENSITIVE_ORDER);
        System.out.println(cases_list);
        return cases_list;
    }

    public static boolean hasSimCard(Context context) {
//        Context context = App.getAppContext();
        TelephonyManager telMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        logUtil.i(TAG, result ? "有SIM卡" : "无SIM卡");
        return result;
    }

    public static boolean hasNfc(Context context){
        boolean result=false;
        if(context==null)
            return false;
        NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        if (manager != null) {
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                // adapter存在，能启用
                result = true;
            }
        }
        logUtil.i(TAG, result ? "NFC已开启" : "不支持NFC/NFC未开启");
        return result;
    }

    public static void openWifi(){
        AdbUtils.runShellCommand("svc wifi enable\n", 0);
    }

    public static void closeWifi(){
        AdbUtils.runShellCommand("svc wifi disable\n", 0);
    }



    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            // 获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // 获取NetworkInfo对象
            NetworkInfo networkInfo = null;
            if (manager != null) {
                networkInfo = manager.getActiveNetworkInfo();
            }
            //判断NetworkInfo对象是否为空
            if (networkInfo != null)
                return networkInfo.isAvailable();
        }
        return false;
    }

    public static void postJson(String url, String data) throws JSONException {

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logUtil.d("POST_RESULT","数据上传失败");
                logUtil.e("postJson", e.getMessage());
//                Looper.prepare();
//                Looper.loop();
            }

            @Override
            public void onResponse(Call call, Response response) {
                logUtil.d("POST_RESULT",response.toString());
//                Looper.prepare();
//                Looper.loop();
                response.close();
            }
        });
    }

    public static int postFile(String url, String filePath, String filename) throws IOException{
        int responeCode;
        File file = new File(filePath);
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("application/octet-stream", filename, fileBody)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient  = httpBuilder
                //设置超时
                .connectTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(150, TimeUnit.SECONDS)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        responeCode = response.code();
        return responeCode;
    }

    public static void JsonRpc(String data){
        logUtil.d("JsonRpc", data);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        final Request request = new Request.Builder()
                .url("http://127.0.0.1:7912/jsonrpc/0")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logUtil.d("JsonRpc",e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                logUtil.d("JsonRpc",response.toString());
                response.close();
            }
        });
    }
    // 通过atx实现shell命令，不稳定，弃用～～
    public static String shell(String cmd, int timeout){
        String result = "";
        FormBody formBody = new FormBody.Builder()
                .add("command", cmd).add("timeout", "60").build();
        final Request request = new Request.Builder()
                .addHeader("Connection","close")
                .url("http://127.0.0.1:7912/shell")
                .post(formBody)
                .build();

        OkHttpClient client=new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90,TimeUnit.SECONDS)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
//            response = new OkHttpClient().newCall(request).execute();
        } catch (IOException e) {
            logUtil.i("shell", e.toString());
            if (AdbUtils.hasRootPermission()){
                AdbUtils.runShellCommand("killall -9 atx-agent", 0);
                SystemClock.sleep(500);
                AdbUtils.runShellCommand("/data/local/tmp/atx-agent server -d", 0);
            } else {
                try {
                    logUtil.d("shell", cmd);
                    MyFile.writeFile(CONST.TEMP_FILE, "::"+dateFormat.format(new Date())+">"+e.toString(), true); //测试异常标志
                } catch (IOException e1) {
                    logUtil.d("shell", e1.toString());
                }
            }
        }
        try {
            if (response != null && response.body() != null) {
                JSONObject resp = new JSONObject(response.body().string());
                response.close();
                result = resp.getString("output").trim();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        SystemClock.sleep(timeout*1000);
        return result;
    }
    // socket 实现 adb shell 命令， 相同功能迁移到adblib.CmdTools中实现
    public static void adbSocket(String cmd){
        Socket socket = null; // put phone IP address here
        try {
            socket = new Socket("localhost", 5555);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AdbCrypto crypto = null;
        try {
            crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
                @Override
                public String encodeToString(byte[] data) {
                    return Base64.encodeToString(data, 2);
                }
            });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        AdbConnection connection = null;
        try {
            connection = AdbConnection.create(socket, crypto);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connection.connect();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            System.out.println(stream);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getLastToast() throws IOException {
        String filePath = Environment.getExternalStorageDirectory() + File.separator + "toast.txt";
        File file = new File(filePath);
        if (file.exists()){
            return MyFile.readFile(filePath);
        }
        return "";
    }

    public static boolean execute_step(JSONArray Step, JSONArray waitTime) throws JSONException {
        boolean success = true;
        logUtil.i("执行: ", Step.toString());
        for (int i = 0; i < Step.length(); i++) {
            int wait_time;
            try{
                wait_time = waitTime.getInt(i);
            } catch (Exception e){
                wait_time = 5;
            }

            if (Step.get(i) instanceof JSONObject){
                Iterator<String> itr = Step.getJSONObject(i).keys();
                ArrayList<String> keys = new ArrayList<>();
                while (itr.hasNext()){
                    keys.add(itr.next());
                }
                String key = keys.get(0);
                Object value = Step.getJSONObject(i).get(key);
                int nex = 0, index = 0;
                if (keys.contains("nex")){
                    nex = (int) Step.getJSONObject(i).get("nex");
                }
                if (keys.contains("index")){
                    index = (int) Step.getJSONObject(i).get("index");
                }
                switch (key) {
                    case "click":
                        if (value instanceof JSONArray) {
                            Common.click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1));
                        }
                        break;
                    case "text":
                        Common.click_element(true, "text", value.toString(), nex, index);
                        break;
                    case "id":
                        Common.click_element(true, "resource-id", value.toString(), nex, index);
                        break;
                    case "resource-id":
                        Common.click_element(true, "resource-id", value.toString(), nex, index);
                        break;
                    case "content":
                        Common.click_element(true, "content-desc", value.toString(), nex, index);
                        break;
                    case "class":
                        Common.click_element(true, "class", value.toString(), nex, index);
                        break;
                    case "clazz":
                        Common.click_element(true, "class", value.toString(), nex, index);
                        break;
                    case "activity":
                        Common.launchActivity(value.toString());
                        break;
                    case "launchApp":
                        Common.launchApp(MyApplication.getContext(), value.toString());
                        break;
                    case "kill":
                        Common.killApp(value.toString());
                        break;
                    case "uninstall":
                        Common.uninstallApp(value.toString());
                        break;
                    case "notification":
                        Common.openNotification();
                        break;
                    case "lock":
                        Common.lockScreen();
                        break;
                    case "unlock":
                        Common.unlock(value.toString());
                        break;
                    case "uri":
                        success = Common.startActivityWithUri(String.valueOf(value));
                        break;

                    case "swipe":
                        double x1 = 0;
                        double x2 = 0;
                        double y1 = 0;
                        double y2 = 0;
                        int step = 100;
                        try {
                            if (value instanceof JSONArray) {
                                x1 = ((JSONArray) value).getDouble(0);
                                y1 = ((JSONArray) value).getDouble(1);
                                x2 = ((JSONArray) value).getDouble(2);
                                y2 = ((JSONArray) value).getDouble(3);
                                step = ((JSONArray) value).getInt(4);
                            } else {
                                String[] deta = value.toString().split(",");

                                x1 = Double.valueOf(deta[0]);
                                y1 = Double.valueOf(deta[1]);
                                x2 = Double.valueOf(deta[2]);
                                y2 = Double.valueOf(deta[3]);
                                step = Integer.valueOf(deta[4]);
                            }
                        } catch (Exception Ignore){
//                            logUtil.d("Common.swipe", "执行滑动操作出错:\n" + e.getMessage());
                        }
                        Common.swipe(x1 , y1, x2, y2, step);
                        break;
                    case "press":
                        Common.press(value.toString());
                        break;
                    case "wait":
                        SystemClock.sleep((Integer)value * 1000);
                        break;
                    case "shell":
                        AdbUtils.runShellCommand(value.toString(), 0);
                        break;
                    case "input":
                        if (value instanceof JSONObject){
                            String _key = ((JSONObject) value).getString("key");
                            String _value = ((JSONObject) value).getString("value");
                            String _msg = ((JSONObject) value).getString("msg");
                            String _mode = ((JSONObject) value).getString("mode");
                            Common.inputText(_key, _value, _msg, _mode);
                        }
                        break;
                    case "wifi":
                        if (value.toString().equals("off")) {
                            new WifiHelper(MyApplication.getContext()).closeWifi();
                        } else if (value.toString().equals("on")){
                            if (AdbUtils.hasRootPermission()){
                                Common.openWifi();
                            } else {
                                new WifiHelper(MyApplication.getContext()).openWifi();
                                Common.click_element(true, "text", "允许", 0, 0);
                            }
                        }
                        break;

                }
                SystemClock.sleep(wait_time *1000);
            }

        }
        return success;
    }

}

