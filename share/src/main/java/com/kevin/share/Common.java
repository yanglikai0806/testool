package com.kevin.share;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyCharacterMap;

import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.share.accessibility.AccessibilityNodeInfoDumper;
import com.kevin.share.adblib.AdbBase64;
import com.kevin.share.adblib.AdbConnection;
import com.kevin.share.adblib.AdbCrypto;
import com.kevin.share.adblib.AdbStream;
import com.kevin.share.utils.AdbUtils;
import com.kevin.share.utils.AppUtils;
import com.kevin.share.AppContext;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.Checkpoint;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import android.util.Base64;
import android.view.WindowManager;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.content.Context.WINDOW_SERVICE;
import static com.kevin.share.CONST.ACCESSIBILITY_RECEIVER;
import static com.kevin.share.CONST.AUTOTEST;
import static com.kevin.share.CONST.DRAG_RECEIVER;
import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.CONST.LOGPATH;
import static com.kevin.share.CONST.REPORT_PATH;
import static com.kevin.share.CONST.TESSDATA;
import static com.kevin.share.accessibility.AccessibilityHelper.dumpElementInfo;
import static com.kevin.share.accessibility.AccessibilityHelper.getNodeInfoToUse;


public abstract class Common {
    public static String TAG = "";
    private static JSONArray fileList;
    private static String SCREENSHOT = "screenshot";
    private static String ALIAS;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);//日期格式;

    private static Date date = new Date();
    private static ArrayList<Node> nodeset;
    private static int n;
    private static int m;

    private static boolean isBugreportCapturing = false;
    public static boolean enableGetElementsByAccessibility = false;
    public static boolean enableGetElementsByInstrument = true;

    public static String TARGET_APP = "";

    public static JSONObject CONFIG = new JSONObject();

    public Common() {
        if (!AppUtils.isApkInstalled(AppContext.getContext(),"com.kevin.testassist.test")){
            enableGetElementsByInstrument = false;
        }
        try {
            TARGET_APP = CONFIG().getString("TARGET_APP");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public static void goBackHome(int back) {
        for (int i = 0; i < back; i++) {
            AdbUtils.runShellCommand("input keyevent 4", 0);
        }
        AdbUtils.runShellCommand("input keyevent 3", 0);

    }

    public static void openNegscreen() {

        goBackHome(3);
        JSONArray x_y = getScreenSize();
        System.out.println(x_y);
        int x = 0;
        int y = 0;
        try {
            x = x_y.getInt(0);
            y = x_y.getInt(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        swipe(0.01 * x, 0.5 * y, 0.8 * x, 0.5 * y, 500);
        SystemClock.sleep(500);
        logUtil.i(TAG, "打开负一屏");

    }

    public static void switchCardFocusEnable(String status) {

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
            if (get_elements(true, "content-desc", "密码区域", 0).size() == 0) {
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
        logUtil.d("press", "按键：" + key);
        switch (key) {
            case "home":
                AdbUtils.runShellCommand("input keyevent 3", 0);
                break;
            case "back":
                AdbUtils.runShellCommand("input keyevent 4", 0);
                break;
            case "recent":
                AdbUtils.runShellCommand("input keyevent KEYCODE_MENU", 0);
                break;
            case "AIkey":
                AdbUtils.runShellCommand("input keyevent 689", 0);
                break;
            case "power":
                AdbUtils.runShellCommand("input keyevent 26", 0);
                break;
        }
    }

    public static void pressPower() {
        AdbUtils.runShellCommand("input keyevent 26", 0);
        SystemClock.sleep(100);
    }

    public static Boolean isScreenOn() {

        PowerManager pm = (PowerManager) AppContext.getContext().getSystemService(Service.POWER_SERVICE);
        logUtil.i(TAG, "是否亮屏：" + pm.isScreenOn());
        return pm.isScreenOn();
//        String res = AdbUtils.runShellCommand("dumpsys window policy|grep mAwake\n", 0);
//        String[] _res = res.trim().split("=");
//        String result = _res[_res.length - 1];
//        logUtil.i(TAG, "是否亮屏：" + result);
//        return result.equals("true");

    }

    public static Boolean isScreenLocked() {

        KeyguardManager mKeyguardManager = (KeyguardManager) AppContext.getContext().getSystemService(Context.KEYGUARD_SERVICE);
        boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
        logUtil.i(TAG, "是否锁屏：" + flag);
        return flag;

//        String res = AdbUtils.runShellCommand("dumpsys window policy|grep mDreamingLockscreen\n", 0);
//        String[] _res = res.trim().split("mDreamingLockscreen=");
//        String result = _res[_res.length - 1].split(" ")[0];
//        logUtil.i(TAG, "是否锁屏：" + result);
//        return result.equals("true");

    }

    public static void lockScreen() {

        AdbUtils.runShellCommand("input keyevent 26", 0);
        SystemClock.sleep(200);
        logUtil.i(TAG, "锁屏");
    }

    public static JSONArray getScreenSize() {
//        String SS;
//        String[] _SS;
//        String _x;
//        String _y;
        JSONArray x_y = new JSONArray();

//        SS = AdbUtils.runShellCommand("dumpsys window displays |grep init=", 0);
//        _SS = SS.trim().split(" ");
//        _SS = _SS[0].split("=");
//        _SS = _SS[1].split("x");
//        _x = _SS[0];
//        _y = _SS[1];
//        x_y.put(Integer.valueOf(_x));
//        x_y.put(Integer.valueOf(_y));
        WindowManager windowManager = (WindowManager) AppContext.getContext().getSystemService(WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();
        x_y.put(width);
        x_y.put(height);
        logUtil.d("screenSize", x_y.toString());
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
        swipe(0.2 * x, 0.01 * y, 0.2 * x, 0.8 * y, 500);
        logUtil.i(TAG, "打开通知栏");

    }

//    public static void swipe(String x, String y, String X, String Y, String step) {
//        AdbUtils.runShellCommand("input swipe " + x + " " + y + " " + X + " " + Y + " " + step + "\n", 0);
//    }

    /**
     * 滑动
     * @param a
     * @param b
     * @param A
     * @param B
     * @param duration
     * @return
     */
    public static JSONArray swipe(double a, double b, double A, double B, int duration) {
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
        AdbUtils.runShellCommand("input swipe " + a * X + " " + b * Y + " " + A * X + " " + B * Y + " " + duration, 0);
        JSONArray xys = new JSONArray();
        try {
            xys.put(a);
            xys.put(b);
            xys.put(A);
            xys.put(B);
            xys.put(duration);
        } catch (JSONException e) {
            return null;
        }
        return xys;
    }

    /**
     * 拖拽
     * @param drag [x,y,X,Y,step]
     */

    public static void drag(JSONArray drag){
        long start = System.currentTimeMillis();
        AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.drag' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", -500);
        logUtil.d("", (System.currentTimeMillis() - start) + "");
        SystemClock.sleep(2000);
        Intent dragIntent = new Intent(DRAG_RECEIVER);
        dragIntent.putExtra("ARGS", drag.toString());
        AppContext.getContext().sendBroadcast(dragIntent);
        SystemClock.sleep(2000);
        Intent dragFIntent = new Intent(DRAG_RECEIVER);
        dragFIntent.putExtra("FINISH", true);
        AppContext.getContext().sendBroadcast(dragFIntent);


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

    public static boolean startActivityWithUri(Context context, String uri) {
        boolean success = false;
        try {
            final Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
            PackageManager manager = context.getPackageManager();
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
            Intent intent= packageManager.getLaunchIntentForPackage(activityName);
            context.startActivity(intent);
        }
    }

    public static void killApp(String pkgName) {
        AdbUtils.runShellCommand("am force-stop " + pkgName, 0);
        SystemClock.sleep(1000);
    }

    public static boolean killProcess(String process) {
        String pid = "";
        pid = AdbUtils.runShellCommand(String.format("ps -A| grep %s |awk '{print $2}'", process), 0);
        if (pid.length() == 0) {
            pid = AdbUtils.runShellCommand(String.format("ps -A| grep %s", process), 0);
            String[] item = pid.split(" ");
            for (int i = 1; i < item.length; i++) {
                if (item[i].length() > 0) {
                    pid = item[i];
                    break;
                }
            }
        }
        if (pid.length() == 0) {
            logUtil.d("", "killProcess: " + process + "失败, 未找到该进程pid");
            return false;
        }
        logUtil.d("", "killProcess: " + pid);
        AdbUtils.runShellCommand("kill -2 " + pid, 0);
        SystemClock.sleep(1000);
        return true;
    }

    public static void clearRecentApp() {
        press("home");
        press("recent");
        String _id;
        try {
            _id = CONFIG().getString("CLEAR_BUTTON_ID");
        } catch (JSONException e) {
            _id = "com.android.systemui:id/clearAnimView";
        }


        try {
            if (!CONFIG().isNull(_id)) {
                JSONArray XY = Objects.requireNonNull(CONFIG()).getJSONArray(_id);
                if (XY.length() > 0) {
                    click(Double.valueOf(XY.get(0).toString()), Double.valueOf(XY.get(1).toString()));
                    SystemClock.sleep(2000);
                    return;
                }
            }
            SystemClock.sleep(1000);
            JSONArray XY = click_element(true, "resource-id", _id, 0, 0, "false");
            JSONObject CONFIG_UPDATE = new JSONObject(String.valueOf(CONFIG()));
            CONFIG_UPDATE.put(_id, XY);
            // 坐标值缓存在配置文件，以提高执行效率
            FileUtils.writeFile(CONST.CONFIG_FILE, CONFIG_UPDATE.toString().replace(",\"", ",\n\""), false);
        } catch (FileNotFoundException | JSONException e) {
            logUtil.d("", "config.json 文件不存在");
            click_element(true, "resource-id", _id, 0, 0, "false");
        }
    }

    public static JSONObject CONFIG(){
        if (CONFIG.length() > 0) {
            return CONFIG;
        }
        if (new File(CONST.CONFIG_FILE).exists()) {
            try {
                CONFIG = new JSONObject(FileUtils.readJsonFile(CONST.CONFIG_FILE));
                return CONFIG;
            } catch (JSONException e) {
                logUtil.e("", e);
                ToastUtils.showLongByHandler(AppContext.getContext(), "config.json 文件格式错误");
            }
        } else {
            ToastUtils.showLongByHandler(AppContext.getContext(), "config.json 文件不存在");
        }
        return new JSONObject();
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

    public static void click(String bounds) {
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
        int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
        click(x, y);
    }

    public static void long_click(double x, double y, String mode) {
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
        if (mode.equals("true")){
            long_click(x, y);
        }
        if (isNumeric(mode)) {
            Common.swipe(x * X, y * Y, x * X, y * Y, Integer.valueOf(mode));
        }
    }

    public static void long_click(String bounds, String mode){
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
        int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
        long_click(x, y , mode);
    }

    public static void long_click(double x, double y) {
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
        Common.swipe(x * X, y * Y, x * X, y * Y, 1500);
    }

    public static void long_click(String bounds) {
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
        int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
        long_click(x, y);
    }

    public static JSONArray click_element(Boolean refresh, String key, String value, int nex, int index, String longClick) {
        String bounds;
        String[] x_y;
        ArrayList<Element> element = get_elements(refresh, key, value, nex);
        if (element != null) {

            bounds = element.get(index).attribute("bounds").getValue();
            x_y = bounds.replace("[", "").replace("]", ",").split(",");
            int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
            int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
            if (longClick.length() > 0){
                long_click(x, y, longClick);
                logUtil.i(TAG, "完成长按元素 " + key + ":" + value);
            } else{
                click(x, y);
                logUtil.i(TAG, "完成点击元素 " + key + ":" + value);
            }

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

    public static JSONArray click_element(Boolean refresh, String key, String value, int nex, int index) {
        String bounds;
        ArrayList<Element> element = get_elements(refresh, key, value, nex);
        if (element != null) {
            bounds = element.get(index).attribute("bounds").getValue();
//            logUtil.d("---------", bounds);
//            Intent ab = new Intent(ACCESSIBILITY_RECEIVER);
//            ab.putExtra("BOUNDS", bounds);
//            ab.putExtra("ACTION_TYPE", "click");
//            MyApplication.getContext().sendBroadcast(ab);
            click(bounds);

            logUtil.i(TAG, "完成点击元素 " + key + ":" + value);
        } else {
            logUtil.i(TAG, String.format("没有找到 %s ： %s", key, value));
        }
        return null;
    }



    public static void setGetElementsByAccessibility(boolean enable){
        if (!AccessibilityHelper.checkAccessibilityEnabled()){
            enable = false;
        }
        enableGetElementsByAccessibility = enable;
//        if (enable) {
//            Intent ab = new Intent(ACCESSIBILITY_RECEIVER);
//            ab.putExtra("ENABLE", enable);
//            MyApplication.getContext().sendBroadcast(ab);
//        }
        logUtil.d("setGetElementsByAccessibility", enable + "");
    }


    public static ArrayList<Element> get_elements_by_instrument(Boolean fresh, String key, String value, int nex) {
        ArrayList<Element> targetElements;
        File filePath = new File(DUMP_PATH);
        if (fresh) {
            try {
                if (filePath.exists()) {
                    filePath.delete();
                }
                if (enableGetElementsByInstrument) {
                    logUtil.d("get_elements", "--------------by instrument-------------");
                    AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.GetDumpTest' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", -2000);

                } else {
                    logUtil.d("get_elements", "--------------by adb-------------");
                    AdbUtils.runShellCommand("uiautomator dump", 0);
                }
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 30000) {
                    try {
                        targetElements = parserXml(filePath.toString(), key, value, nex);//.get(nex);
                        if (targetElements.size() > 0){
                            return targetElements;
                        }

                    } catch (Exception e) {
//                        logUtil.e("", e);
                    }
                    SystemClock.sleep(500);

                }
            } catch (Exception e) {
//                logUtil.e("", e);
            }
        } else {
            try {
                targetElements = parserXml(filePath.toString(), key, value, nex);//.get(nex);
                if (targetElements.size() > 0) {
                    return targetElements;
                }
//                logUtil.i("", targetElements.toString());
            } catch (Exception e) {
//                logUtil.e("", e);
                logUtil.i("", String.format("no target elements \"%s\"：\"%s\"", key, value));

            }
        }
        return null;
    }

    /**
     * 通过广播触发辅助功能获取界面元素
     */

    public static void get_elements_by_accessibility(){
        Intent ab = new Intent(ACCESSIBILITY_RECEIVER);
        ab.putExtra("ENABLE", true);
        AppContext.getContext().sendBroadcast(ab);
        logUtil.d("get_elements", "---------------by accessibility------------");
    }



    public static Element get_element(Boolean refresh, String key, String value, int nex){
        ArrayList<Element> elements = get_elements(refresh, key, value, nex);
        if (elements != null && elements.size() > 0){

            return elements.get(0);
        }
        return null;
    }

    /**
     * 根据属性获取元素
     * @param refresh
     * @param nodeInfo
     * @return
     * @throws JSONException
     */

    public static Element get_element(Boolean refresh, JSONObject nodeInfo) throws JSONException {
        int index = 0;
        int nex = 0;
        String key = "";
        if (!nodeInfo.isNull("index")){
            index = nodeInfo.getInt("index");
            nodeInfo.remove("index");
        }
        if (!nodeInfo.isNull("nex")){
            nex = nodeInfo.getInt("nex");
            nodeInfo.remove("nex");
        }
        Iterator<String> itr = nodeInfo.keys();
        while (itr.hasNext()){
            key = itr.next();
        }
        ArrayList<Element> elements = get_elements(refresh, key, nodeInfo.getString(key), nex);
        if (elements != null && elements.size() > 0){

            return elements.get(index);
        }
        return null;
    }

    /**
     * 获取元素bounds
     * @param refresh
     * @param nodeInfo
     * @return
     * @throws JSONException
     */
    public static String get_bounds(Boolean refresh, JSONObject nodeInfo) throws JSONException {
        Element e = get_element(refresh, nodeInfo);
        if (e != null){
            return e.attributeValue("bounds");
        }
        return "";
    }

    public static ArrayList<Element> get_elements(Boolean refresh, String key, String value, int nex) {
        ArrayList<Element> targetElements = new ArrayList<>();
        if (refresh) {
//            SystemClock.sleep(500);
            setGetElementsByAccessibility(true);
            if (!enableGetElementsByAccessibility) {
                return get_elements_by_instrument(refresh, key, value, nex);
            }
            if (TextUtils.isEmpty(key)){
                SystemClock.sleep(2000);
            }
            get_elements_by_accessibility();
        }
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {

            try {
                if (FileUtils.readFile(DUMP_PATH).endsWith("</hierarchy>")) {
                    targetElements = parserXml(DUMP_PATH, key, value, nex);//
                    if (targetElements.size() > 0) {
                        return targetElements;
                    } else if (refresh){
                        SystemClock.sleep(500);
                        get_elements_by_accessibility();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!refresh){
                break;
            }
            SystemClock.sleep(100);

        }
//        logUtil.d("", String.format("no target elements \"%s\"：\"%s\"", key, value));
        return targetElements;//get_elements_by_instrument(refresh, key, value, nex);

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
            for (Element elem : targetElements) {
                ArrayList<Boolean> res = new ArrayList<>();
                for (int i = 1; i < keys.size(); i++) {
                    if (elem.attributeValue(keys.get(i)).equals(values.get(i))) {
                        res.add(true);
                    } else {
                        res.add(false);
                    }
                }
                if (!res.contains(false) && res.size() > 0) {
                    targetElem.add(elem);
                }
            }
        }
        if (targetElem.size() == 1) {
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
                FileUtils.writeFile(CONST.INPUT_FILE, inputObj.toString(), false);
                AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.Input#input' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);
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
        //定义符合预期的元素容器集合
        ArrayList<Element> elem_list = new ArrayList<>();
        if (key.toLowerCase().equals("xpath")){
            List<Node> node_list = document.selectNodes(value);
            logUtil.d("", node_list.size() + "");
            for (Node nd :node_list){
//                logUtil.d("node", nd+"");
                elem_list.add((Element)nd);
            }
//            logUtil.d("xpath", elem_list+"");
            return elem_list;
        }
        //获取文档根节点
        Element root = document.getRootElement();
        //输出根标签的名字
//        System.out.println(root.getName());
        //获取根节点下面的所有子节点（不包过子节点的子节点）
//        List<Element> list = root.elements();
        //遍历List的方法

        //获得指定节点下面的子节点
        List<Element> nodeElems = root.elements();
        //定义所有
        nodeset = new ArrayList<>();
        n = 0;
        m = 0;
        for (Element nodeElem : nodeElems) {
            //调用下面获取子节点的递归函数。
            if (key.equals("bounds")){
                // 根据bounds范围获得元素
                getChildNodesByBounds(nodeElem, value, elem_list);
            } else {
                getChildNodes(nodeElem, key, value, elem_list, nex);
            }
        }
//        logUtil.i("", "element_list "+ elem_list.toString());
//        logUtil.i("", "element_list "+ elem_list.size());
        return elem_list;

    }

    //递归查询节点函数,输出节点名称
//    private static void getChildNodes(Element elem, String key, String value, List<Element> elem_list, int nex) {
//        Iterator<Node> it = elem.nodeIterator();
//        ArrayList<Node> tmpIt = new ArrayList<>();
//        while (it.hasNext()) {
//            Node node = it.next();
//            if (nex < 0) {
//                tmpIt.add(node);
//            }
//            if (node instanceof Element) {
//                Element e1 = (Element) node;
//                if (key.equals("")){
//                    elem_list.add(e1);
//                } else {
//                    if (e1.attribute(key).getValue().equals(value)) {
//                        if (nex > 0) {
//                            for (int i=0; i < nex; i++){
//                                e1 = (Element) it.next();
//                            }
//                        }
//                        if (nex < 0 && (tmpIt.size() + nex) >= 1){
//                            e1 = (Element) tmpIt.get(tmpIt.size()-1 + nex);
//
//                        }
//                        elem_list.add(e1);
//                    }
//                }
//                getChildNodes(e1,key,value, elem_list, nex);
//
//            }
//        }
//    }

    /**
     * 递归查询子节点
     * @param elem
     * @param key
     * @param value
     * @param elem_list
     * @param nex
     */

    private static void getChildNodes(Element elem, String key, String value, List<Element> elem_list, int nex) {
        Iterator<Node> it = elem.nodeIterator();
//        ArrayList<Node> tmpIt = new ArrayList<>();
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
                    if (e1.attribute(key)!= null && e1.attributeValue(key).equals(value)) {
                        if (nex == 0) {
                            elem_list.add(e1);
                        }
                        if (nex > 0) {
                            int i = 0;
                            while (i < nex) {
                                if (it.hasNext()) {
                                    Node nd = it.next();
                                    try {
                                        Element nt = (Element) nd;
                                        if (i + 1 == nex) {
                                            e1 = nt;
                                            elem_list.add(e1);
                                            break;
                                        } else {
                                            i = i + 1;
                                        }
                                    } catch (Exception e){
//                                        logUtil.i("getChildNodes", e.toString());
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
                        elem_list.add((Element) nodeset.get(m + nex - 1));
                    }
                }
                getChildNodes(e1, key, value, elem_list, nex);
//
//                System.out.println(e1.getName());
            } else {
//                logUtil.d("node is not element:::" , node.asXML());
            }
        }
    }

    /**
     * 根据bounds获得范围内的元素
     * @param elem
     * @param value bound的值
     * @param elem_list
     */

    private static void getChildNodesByBounds(Element elem, String value, List<Element> elem_list) {
        Iterator<Node> it = elem.nodeIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node instanceof Element) {

                Element e1 = (Element) node;
                if (e1.attribute("bounds") == null){
                    continue;
                }
                String abounds = e1.attribute("bounds").getValue();
                if (isInBounds(abounds, value)) {
                    elem_list.add(e1);
                }
                getChildNodesByBounds(e1, value, elem_list);
            }
        }
    }

    /**
     * 根据bounds判断a是否在b内
     * @param aBounds
     * @param bBounds
     * @return
     */

    public static boolean isInBounds(String aBounds, String bBounds){
//        String[] a_x_y = aBounds.replace("[", "").replace("]", ",").split(",");
//        String[] b_x_y = bBounds.replace("[", "").replace("]", ",").split(",");
//        int aleft, atop, aright, abottom;
//        int bleft, btop, bright, bbottom;
//        aleft = Integer.valueOf(a_x_y[0]);
//        atop = Integer.valueOf(a_x_y[1]);
//        aright = Integer.valueOf(a_x_y[2]);
//        abottom = Integer.valueOf(a_x_y[3]);
//
//        bleft = Integer.valueOf(b_x_y[0]);
//        btop = Integer.valueOf(b_x_y[1]);
//        bright = Integer.valueOf(b_x_y[2]);
//        bbottom = Integer.valueOf(b_x_y[3]);
//        if (bleft <= aleft){
//            if (btop <= atop){
//                if (bright >= aright){
//                    return bbottom >= abottom;
//                }
//            }
//        }
        Rect a = parseBounds(aBounds);
        Rect b = parseBounds(bBounds);
        if (b.left <= a.left){
            if(b.top <= a.top){
                if (b.right >= a.right){
                    return b.bottom >= a.bottom;
                }
            }
        }
        return false;
    }

    /**
     * 解析bounds string to Rect
     * @param bounds
     * @return
     */
    public static Rect parseBounds(String bounds){
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int left, top, right, bottom;
        left = Integer.valueOf(x_y[0]);
        top = Integer.valueOf(x_y[1]);
        right = Integer.valueOf(x_y[2]);
        bottom = Integer.valueOf(x_y[3]);
        Rect rt = new Rect();
        rt.left = left;
        rt.top = top;
        rt.right = right;
        rt.bottom = bottom;
        return rt;
    }

    /**
     * 计算bounds的中心坐标
     * @param bounds
     * @return
     */
    public static JSONArray parseBoundsToPoint(String bounds){
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
        int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
        return new JSONArray().put(x).put(y);
    }

    /**
     * 通过点坐标获得元素
     * @param point
     */
    public static JSONObject getElementInfoByPoint(JSONArray point) throws Exception {
        ArrayList<Element> elements = null;
        if (AdbUtils.isAdbEnable()) {
            elements = Common.get_elements(true, "", "", 0);
        } else {
            // adb 不可用时，尝试启动主app服务获取界面元素
            Intent playIntent = new Intent("com.kevin.testool.action.execute_step");
            playIntent.setPackage("com.kevin.testool");
            playIntent.setComponent(new ComponentName("com.kevin.testool", "com.kevin.testool.MyIntentService"));
            playIntent.putExtra("EXECUTE_STEP", "[{\"dump_xml\":\"\"}]");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppContext.getContext().startForegroundService(playIntent);
            } else {
                AppContext.getContext().startService(playIntent);
            }
            SystemClock.sleep(1500);
            long st = System.currentTimeMillis();
            while (System.currentTimeMillis() - st < 15000){
                if (new File(DUMP_PATH).exists()){
                    elements = Common.get_elements(false, "", "", 0);
                    break;
                }
                SystemClock.sleep(100);
            }

        }
        if (elements == null){
            return new JSONObject().put("click", point);
        }
        JSONArray elementLst = new JSONArray();
        int x = point.getInt(0);
        int y = point.getInt(1);
        String mockBounds = String.format("[%s,%s][%s,%s]", x-1, y-1, x+1, y+1);
//         elements = parserXml(DUMP_PATH, "", "", 0);
        for (Element e: elements){
            String eBounds = e.attributeValue("bounds");
            if (eBounds.startsWith("[0,0]")){
                continue;
            }

            if (Common.isInBounds(mockBounds, eBounds)){
                JSONObject eInfo = dumpElementInfo(e);
                if (eInfo.length() > 2){ //判断是否存在有效属性信息
                    logUtil.d("", eInfo + "");
                    elementLst.put(eInfo);
                }
            }
        }
        // 计算最优元素
        ArrayList<Integer> disDataLst = new ArrayList<>();
        for (int i=0;i< elementLst.length(); i++){
            JSONArray ePoint = parseBoundsToPoint(elementLst.getJSONObject(i).getString("bounds"));
            int distance = (int) Math.sqrt(Math.abs((ePoint.getInt(0) - x))+Math.abs((ePoint.getInt(1)-y)));
            disDataLst.add(distance);

        }
        if (disDataLst.size() > 0) {

            int indexMin = disDataLst.indexOf(Collections.min(disDataLst));
            logUtil.d("index is", indexMin + "");
            return getNodeInfoToUse(elementLst.getJSONObject(indexMin), true);
        }
        return new JSONObject().put("click", point);
    }

    public static String getActivity() {

//        String res = AdbUtils.runShellCommand("dumpsys input | grep FocusedApplication\n", 0);
//        String[] act = res.trim().split("name=")[1].split("u0")[1].trim().split(" ");
        String res = AdbUtils.runShellCommand("dumpsys activity top | grep ACTIVITY | tail -n 1", 0);
        String act = res.replace("ACTIVITY", "").trim().split(" ")[0];
        if (TextUtils.isEmpty(act)) {
            ActivityManager mActivityManager = (ActivityManager) AppContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
//        logUtil.d("get_activity", rti.get(0).topActivity.flattenToString());
            logUtil.d("get_activity", rti.get(0).topActivity.flattenToShortString());
            return rti.get(0).topActivity.flattenToShortString();
        }
        return act.trim();
    }

    public static void generateBugreport(final String logFileName) {
        if (isBugreportCapturing) {
            return;
        }
        isBugreportCapturing = true;
        final File bugreportFile = new File(logFileName);
        logUtil.i("", bugreportFile.getName().replace(".txt", ".zip"));
        new Thread(() -> {
            AdbUtils.runShellCommand("bugreport > " + bugreportFile, 60000);
            isBugreportCapturing = false;
            try {
                FileUtils.ZipFolder(logFileName, logFileName.replace(".txt", ".zip"));
                FileUtils.deleteFile(logFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String screenShot() {
        String img;
        try {
            String png = dateFormat.format(new Date()) + ".png";
            img = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + png;
            screenShot(img);
            logUtil.i("", png);
            return png;

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void screenShot(String path) {
        AdbUtils.runShellCommand("screencap -p " + path, 0);
    }

    public static String screenRecorder(int seconds) {
        String mp4 = dateFormat.format(new Date()) + ".mp4";
        try {
            String mp4File = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + mp4;
            if (seconds > 0) {
                AdbUtils.runShellCommand(String.format("screenrecord --size 720x1080  --time-limit %s %s", seconds, mp4File), -seconds * 1000);
            } else {
                AdbUtils.runShellCommand(String.format("screenrecord --size 720x1080 %s", mp4File), -50);
            }
            return mp4File;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

    }

    /**
     * 用于进行ocr识别的屏幕截图
     * @param refresh
     * @return
     */

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

    //    @RequiresApi(api = Build.VERSION_CODES.O)
//    @SuppressLint("MissingPermission")
    public static final String getSerialno() {
        return AdbUtils.runShellCommand("getprop ro.serialno", 0).trim();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                return AdbUtils.runShellCommand("getprop ro.serialno", 0).trim();
//            }
//            return Build.getSerial();
//        } else {
//            return AdbUtils.runShellCommand("getprop ro.serialno", 0).trim();
//        }
    }

    public static String getVersionName(Context context, String pkg){

        return AppUtils.getVersionName(context, pkg);

    }
    public static int getVersionCode(Context context, String pkg){

        return AppUtils.getVersionCode(context, pkg);

    }

    public static String getVersionName2(String pkg){
        if (pkg.contains("/")){
            pkg = pkg.split("/")[0];
        }
        String res = AdbUtils.runShellCommand("dumpsys package "+pkg+" |grep versionName", 0);
        String[] res_tmp = res.trim().split(" ")[0].split("=");
        return res_tmp[1];
    }



    public static String runMonkey(String pkg, String count, String throttle, String seed,String logDir){
        if (pkg.contains("/")){
            pkg = pkg.split("/")[0];
        }
//        String dt = dateFormat.format(date);
//        String logDir = MONKEY_PATH + getVersionName2(pkg) + File.separator + dt + File.separator;
        FileUtils.creatDir(logDir);
        String _log = "2>" + logDir + "monkey_error.txt 1> " + logDir + "monkey_info.txt";
        String monkey_cmd = String.format("monkey -p %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s ", pkg, throttle, count) + _log + "\n";
        System.out.println(monkey_cmd);
        if (seed != null && seed.length() > 0){
            monkey_cmd = String.format("monkey -p %s -s %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, seed, throttle, count) + _log + "\n";

        }
        AdbUtils.runShellCommand(monkey_cmd, 0);
        return logDir + "monkey_error.txt";
    }

    public static String getMonkeyProcess(){
        return AdbUtils.runShellCommand("ps -A| grep monkey", 0);
    }

    /**
     * 获取应用内存占用大小
     * @param pkg 应用包名
     * @return 内存占用kb, 0 表示未获得相关进程
     */
    public static String getMeminfoTotal(String pkg){
        String res = AdbUtils.runShellCommand(String.format("dumpsys meminfo %s|grep TOTAL:", pkg), 0);
        logUtil.d("meminfo", res);
        if (res.length()>0){
            String total = res.replace(" TOTAL:","").trim().split(" ")[0];
            return total;
        } else {
            return "0";
        }
    }

    /**
     * 记录应用内存数据
     * @param pkg 被采集内存的应用包名
     * @param recordFile 采集数据输出的文件
     * @param recordWait 采集间隔 ms
     * @param recordSeconds 记录时长，单位s(秒)
     */
    public static void recordMeminfo(String pkg, String recordFile, int recordWait, int recordSeconds){
        long startTime = System.currentTimeMillis();
        ArrayList<String> memData = new ArrayList<>();
        while(System.currentTimeMillis()-startTime < recordSeconds * 1000){
            memData.add(dateFormat2.format(new Date()) + "," + getMeminfoTotal(pkg));
            SystemClock.sleep(recordWait);
        }
        try {
            String content;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                content = String.join("\n", memData) + "\n";
            } else {
                content = String.valueOf(memData).replace("[","").replace("]", "\n").replace(", ", "\n");
            }
            if (!new File(recordFile).exists()){
                FileUtils.writeFile(recordFile, "Time,Meminfo(KB)\n", false);
            }
            FileUtils.writeFile(recordFile, content, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static JSONArray getAllTestCases(String dir, String _type, String flag, String tag){
        if (flag.equals("reload")){
            fileList = new JSONArray();
        }
        File casesDir = new File(dir);
        if (! casesDir.exists()){
            ToastUtils.showLongByHandler(AppContext.getContext(), "导入用例失败");
        }
        File[] files = casesDir.listFiles();

//        JSONArray fileList = new JSONArray();
//        JSONObject _fInfo = new JSONObject();
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
                getAllTestCases(_file.getAbsolutePath(), _type, "append", _file.getName());
            }
        }
        return fileList;

    }

    public static ArrayList<String> getCaseList(){
        ArrayList<String> cases_list = new ArrayList<String>();//获取测试用例
        JSONArray fileList;
        File testcasesFolder = new File(CONST.TESTCASES_PATH);
        if (! testcasesFolder.exists()){
            ToastUtils.showLongByHandler(AppContext.getContext(), "未发现用例配置文件");
            return null;
        }
        fileList = getAllTestCases(CONST.TESTCASES_PATH, "json", "reload", "");
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
//        System.out.println(cases_list);
        return cases_list;
    }


    public static void switchTestEnv(String pkg, String env){
        //todo
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

    /**
     *
     * @param url 接口url
     * @param data json格式的string
     */
    public static void postJson(String url, String data) {
        logUtil.d("postJson", url);
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
                logUtil.d("POST_RESULT", e.getMessage());
//                Looper.prepare();
//                Looper.loop();
            }

            @Override
            public void onResponse(Call call, Response response) {
//                String resp = response.toString();
//                logUtil.d("POST_RESULT",resp);
                try {
                    logUtil.d("POST_RESULT", response.body().string());
                } catch (IOException e) {
                    logUtil.d("POST_RESULT", e.toString());
                }
//                try {
//                    ToastUtils.showLongByHandler(MyApplication.getContext(), response.body().string());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                response.close();
            }
        });
    }

    public static String postResp(String url, String data){
        logUtil.d("postResp", url);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            String msg = response.body().string();
            logUtil.d("response", msg);
            return msg.trim();
        } catch (IOException e) {
            logUtil.e("postResp", e);
            return "";
        }
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
//            client.dispatcher().cancelAll();
//            client.connectionPool().evictAll();
//            try {
//                response = client.newCall(request).execute();
//            } catch (IOException e1) {
//                logUtil.i("重试失败", e.toString());
//            }
            if (AdbUtils.hasRootPermission()){
                AdbUtils.runShellCommand("killall -9 atx-agent", 0);
                SystemClock.sleep(500);
                AdbUtils.runShellCommand("/data/local/tmp/atx-agent server -d", 0);
            } else {
                try {
                    logUtil.d("shell", cmd);
                    FileUtils.writeFile(CONST.TEMP_FILE, "::"+dateFormat.format(new Date())+">"+e.toString(), true); //测试异常标志
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
            AdbStream stream = connection.open("shell:" + cmd + "\n");
            logUtil.d("",stream + "");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static String getResp(String url) {
        logUtil.d("getResp", url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            String msg = response.body().string();
//            ToastUtils.showLongByHandler(MyApplication.getContext(), "msg:"+msg);
            return msg.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static void mySocket(){
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    //1.创建监听指定服务器地址以及指定服务器监听的端口号
//                    Socket socket = new Socket("transfer.falcon.miliao.srv", 8433);
                    Socket socket = new Socket("127.0.0.1", 51835);
                    //2.拿到客户端的socket对象的输出流发送给服务器数据
                    OutputStream os = socket.getOutputStream();
                    //写入要发送给服务器的数据
                    JSONObject payload = new JSONObject();
                    long _date_time = new Date().getTime();
//                    String jsonStr = String.format("{\"endpoint\":\"production\",\"metric\":\"my_test\", \"timestamp\":%s,\"step\":60,\"value\":1,\"counterType\":\"GAUGE\", \"tags\":\"%s\"}", _date_time, tag);
                    JSONObject data = new JSONObject();
                    try {
                        data.put("host", "devices");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        payload.put("id", 1);
                        payload.put("params", new JSONArray().put(new JSONArray().put(data)));
                        payload.put("method", "Transfer.Update");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String _data = "host:devices";
                    os.write(data.toString().getBytes());
                    os.flush();
                    socket.shutdownOutput();
                    //拿到socket的输入流，这里存储的是服务器返回的数据
                    InputStream is = socket.getInputStream();
                    //解析服务器返回的数据
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader bufReader = new BufferedReader(reader);
                    String s = null;
                    final StringBuffer sb = new StringBuffer();
                    while ((s = bufReader.readLine()) != null) {
                        sb.append(s);
                    }
                    logUtil.d("SOCKET", sb.toString());
                    System.out.println("===================");
                    System.out.println(sb.toString());
                    System.out.println("-------------------");
                    //3、关闭IO资源（注：实际开发中需要放到finally中）
                    bufReader.close();
                    reader.close();
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    public static String getLastToast() throws IOException {
        String filePath = Environment.getExternalStorageDirectory() + File.separator + "toast.txt";
        File file = new File(filePath);
        if (file.exists()){
            return FileUtils.readFile(filePath);
        }
        return "";
    }

    public static boolean execute_step(JSONArray Step, JSONArray waitTime) throws JSONException {
        boolean success = true;
        logUtil.i("执行: ", Step.toString());
        for (int i = 0; i < Step.length(); i++) {
            int wait_time;
            try {
                wait_time = waitTime.getInt(i);
            } catch (Exception e) {
                wait_time = 3;
            }

            if (Step.get(i) instanceof JSONObject) {
//                String key = Step.getJSONObject(i).keys().next();
                Iterator<String> itr = Step.getJSONObject(i).keys();
                ArrayList<String> keys = new ArrayList<>();
                while (itr.hasNext()) {
                    keys.add(itr.next());
                }
                int nex = 0, index = 0;
                boolean refresh = true;
                String longClick = "";
                if (keys.contains("nex")){
                    nex = (int) Step.getJSONObject(i).get("nex");
                    keys.remove("nex");
                }
                if (keys.contains("index")) {
                    index = (int) Step.getJSONObject(i).get("index");
                    keys.remove("index");
                }
                if (keys.contains("refresh")){
                    refresh = Step.getJSONObject(i).get("refresh").equals("true");
                    keys.remove("refresh");
                }
                if (keys.contains("long")) {
                    longClick = Step.getJSONObject(i).getString("long");
                    keys.remove("long");

                }
                String key = keys.get(0);
                Object value = Step.getJSONObject(i).get(key);
                switch (key) {
                    case "uiautomator":
                        if (value instanceof JSONObject) {
                            @SuppressLint("WrongConstant") SharedPreferences shuttle = AppContext.getContext().getSharedPreferences("shuttle", 0);
                            if (!((JSONObject) value).isNull("method")) {
                                shuttle.edit().putString("method", ((JSONObject) value).getString("method")).apply();
                            } else {
                                logUtil.d("MyIntentService", "dismiss method");
                            }
                            if (!((JSONObject) value).isNull("args")){
                                if (((JSONObject) value).get("args") instanceof String){
                                    shuttle.edit().putString("args", ((JSONObject) value).getString("args")).apply();
                                }
                                else if (((JSONObject) value).get("args") instanceof Integer){
                                    shuttle.edit().putInt("args", ((JSONObject) value).getInt("args")).apply();
                                }
                                else if (((JSONObject) value).get("args") instanceof Boolean){
                                    shuttle.edit().putBoolean("args", ((JSONObject) value).getBoolean("args")).apply();
                                }

                            } else {
                                shuttle.edit().putString("args", "null").apply();
                            }
                            AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.Shuttle' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);
                        }
                        logUtil.d("dd", value.toString());
                        break;
                    case "click":
                        if (value instanceof JSONArray) {
                            if (longClick.length() > 0) {
                                Common.long_click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1), longClick);
                            } else {
                                Common.click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1));
                            }                        }
                        break;
                    case "text":
                        Common.click_element(refresh, "text", value.toString(), nex, index, longClick);
                        break;
                    case "id":
                        Common.click_element(refresh, "resource-id", value.toString(), nex, index, longClick);
                        break;
                    case "resource-id":
                        Common.click_element(refresh, "resource-id", value.toString(), nex, index, longClick);
                        break;
                    case "content":
                        Common.click_element(refresh, "content-desc", value.toString(), nex, index, longClick);
                        break;
                    case "content-desc":
                        Common.click_element(refresh, "content-desc", value.toString(), nex, index, longClick);
                        break;
                    case "clazz":
                        Common.click_element(refresh, "class", value.toString(), nex, index, longClick);
                        break;
                    case "class":
                        Common.click_element(refresh, "class", value.toString(), nex, index, longClick);
                        break;
                    case "activity":
                        Common.launchActivity(value);
                        break;
                    case "launchApp":
                        Common.launchApp(AppContext.getContext(), value.toString());
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
                    case "negscreen":
                        Common.openNegscreen();
                        break;
                    case "lock":
                        Common.lockScreen();
                        break;
                    case "unlock":
                        Common.unlock(value.toString());
                        break;
                    case "env":
                        Common.switchTestEnv(TARGET_APP, value.toString());
                        break;
                    case "focus":
                        Common.switchCardFocusEnable(value.toString());
                        break;
                    case "uri":
                        success = Common.startActivityWithUri(AppContext.getContext(), String.valueOf(value));
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
                        press(value.toString());
                        break;
                    case "wait":
                        SystemClock.sleep((Integer) value * 1000);
                        break;
                    case "shell":
                        AdbUtils.runShellCommand(value.toString(), 0);
                        break;
                    case "input":
                        if (value instanceof JSONObject) {
                            String _key = ((JSONObject) value).getString("key");
                            String _value = ((JSONObject) value).getString("value");
                            String _msg = ((JSONObject) value).getString("msg");
                            String _mode = ((JSONObject) value).getString("mode");
                            Common.inputText(_key, _value, _msg, _mode);
                        }
                        if (value instanceof String){
                            AdbUtils.runShellCommand("input text " + value, 0);
                        }
                        break;
                    case "offline":
                        closeWifi();
                        break;
                    case "online":
                        openWifi();
                        break;
                    case "if":
                        if (value instanceof JSONObject) {
                            try {
                                resultCheck((JSONObject) value, true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
//                logUtil.i(TAG, "action: " + key + ":" + value);
                SystemClock.sleep(wait_time * 1000);
            } else {

                logUtil.i("执行步骤：", Step.get(i) + "");

            }
        }
        return success;
    }

    public static boolean resultCheck(JSONObject check_point, Boolean refresh) throws IOException, JSONException {
        ArrayList<Boolean> result = Checkpoint.muitiCheck(check_point, refresh);
        //sim 卡判断
        if (!check_point.isNull("sim_card")){
            String sim = check_point.getString("sim_card");
            if (sim.length() > 0){
                result.add(String.valueOf(Common.hasSimCard(AppContext.getContext())).equals(sim));
            }
        }
        //nfc 功能判断
        if (!check_point.isNull("nfc")){
            String nfc = check_point.getString("nfc");
            if (nfc.length() > 0){
                result.add(String.valueOf(Common.hasNfc(AppContext.getContext())).equals(nfc));
            }
        }

        //app 安装判断
        if (!check_point.isNull("no_pkg")){
            String pkg = check_point.getString("no_pkg");
            if (pkg.length() > 0){
                String res = AdbUtils.runShellCommand("pm list package | grep " +pkg, 0);
                logUtil.i("package", res);
                result.add(res.length() <= 0);
            }
        }
        // device 黑白名单
        if (!check_point.isNull("dev_white_lst")){
            JSONArray dwl = check_point.getJSONArray("dev_white_lst");
            for (int n=0; n< dwl.length(); n++){
                if (dwl.getString(n).equals(ALIAS)){
                    result.add(false);
                }
            }
        }

        if (!check_point.isNull("dev_black_lst")){
            JSONArray dwl = check_point.getJSONArray("dev_black_lst");
            for (int n=0; n< dwl.length(); n++){
                if (dwl.getString(n).equals(ALIAS)){
                    result.add(true);
                }
            }
        }
        // 对依赖app的判断
        if (!check_point.isNull("app")){
            JSONObject appInfo = check_point.getJSONObject("app");
            String appPkg = appInfo.getString("pkg");
            if (!appInfo.isNull("version_name")) {
                String appVersionName = appInfo.getString("version_name");
                System.out.println(Common.getVersionName(AppContext.getContext(), appPkg));
                if (Common.getVersionName(AppContext.getContext(), appPkg).equals(appVersionName)){
                    result.add(true);
                } else {
                    result.add(false);
                }
            }
            if (!appInfo.isNull("version_code")) {
                JSONArray verScope = appInfo.getJSONArray("version_code");
                int xaVersionCode = Common.getVersionCode(AppContext.getContext(), appPkg);
                result.add(verScope.getInt(0) <= xaVersionCode && xaVersionCode <= verScope.getInt(1));
            }

        }
        //结果取 “或” 关系
        if (!check_point.isNull("or")){
            String or = check_point.getString("or");
            if (or.equals("true")){
                logUtil.i("", result.toString());
                if (result.contains(true)) {
                    result = new ArrayList<>();
                    result.add(true);
                }
//                return result.contains(true);
            }
        }
        // 测试后的必要处理
        if (!check_point.isNull("teardown")){
            JSONArray teardown = check_point.getJSONArray("teardown");
            JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
            if (teardown.length() > 0) {
                logUtil.i("", "teardown:");
                execute_step(teardown, waitTime);
            }
        }
        logUtil.i("", result.toString());

        boolean ret = !result.contains(false);
        // 结果取反
        if (!check_point.isNull("reverse")){
//            String reverse = check_point.getString("reverse");
            ret = !ret;

        }

        // 执行操作
        if (!check_point.isNull("true") && ret){
            JSONArray toDo = check_point.getJSONArray("true");
            JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
            if (toDo.length() > 0) {
                logUtil.i("", "do :");
                execute_step(toDo, waitTime);
            }
        }

        if (!check_point.isNull("false") && !ret){
            JSONArray toDo = check_point.getJSONArray("false");
            JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
            if (toDo.length() > 0) {
                logUtil.i("", "do :");
                execute_step(toDo, waitTime);
            }
        }
        return ret;

    }

    public static boolean isNumeric(String str){
        for (int i = str.length();--i>=0;){
            if (!Character.isDigit(str.charAt(i))){
                return false;
            }
        }
        return true;
    }

    public static String downloadApk(Context context, String url){
        String apkFile;
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
        apkFile = url.substring(url.lastIndexOf("/") + 1);
        request.setDestinationInExternalPublicDir(AUTOTEST, apkFile);
        FileUtils.deleteFile(CONST.LOGPATH + apkFile); // 删除旧文件
        request.setNotificationVisibility(VISIBILITY_VISIBLE);
        request.setTitle("更新");
        request.setDescription("正在下载" + apkFile);
//        request.setAllowedOverRoaming(false);
        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
        return LOGPATH + apkFile;
    }


    public static void syncTestcases(){
//        String database = "test_mp";
//        try {
//            JSONObject mysql = Common.CONFIG().getJSONObject("MYSQL");
//            database = mysql.getString("database");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        JSONArray _tables = new JSONArray();
        try {
            JSONObject resp = new JSONObject(getResp("http://"+CONFIG().getString("SERVER_IP") + ":"+CONFIG().getString("SERVER_PORT") + "/client_test/test_cases?table=" + CONFIG().getString("TABLE") +"&domain_list=1"));
            _tables = new JSONArray(resp.getString("data"));
        } catch (JSONException e) {
            logUtil.e("syncTestcases", e);
        }
//        ToastUtils.showLongByHandler(MyApplication.getContext(), _tables.toString());
        for (int i=0; i < _tables.length(); i++){
            try {
                syncTestcase(_tables.getString(i));
            } catch (JSONException e) {
                logUtil.e("syncTestcases", e);
            }
        }
    }

    public static void syncTestcase(String domain){
        try {
            JSONObject resp1 = new JSONObject(getResp("http://"+CONFIG().getString("SERVER_IP") + ":"+CONFIG().getString("SERVER_PORT") + "/client_test/test_cases?table=" + CONFIG().getString("TABLE") +"&domain=" + domain));
            JSONArray list_cases;
            list_cases = new JSONObject(resp1.getString("data")).getJSONArray(domain);
            FileUtils.writeCaseJsonFile(domain, list_cases);
        } catch (JSONException e) {
            logUtil.e("syncTestcase", e);
        }

    }

    public static void updateTestCases(String testcase){
        try {
            String url = "http://" + CONFIG().getString("SERVER_IP") +":" + CONFIG().getString("SERVER_PORT") + "/client_test/test_cases?table=" + CONFIG().getString("TABLE");
            postResp(url, testcase);
        } catch (JSONException e) {
            logUtil.e("updateTestCases", e);
        }
    }

}

