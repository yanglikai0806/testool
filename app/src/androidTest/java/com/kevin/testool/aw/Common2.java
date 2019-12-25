package com.kevin.testool.aw;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
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
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiObject2;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.kevin.testool.adblib.CmdTools;
import com.kevin.testool.stub.Automator;
import com.kevin.testool.stub.DeviceInfo;
import com.kevin.testool.utils.AppUtils;
import com.kevin.testool.CONST;
import com.kevin.testool.MyFile;
import com.kevin.testool.utils.logUtil;

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
import java.net.URISyntaxException;
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
import static com.kevin.testool.MyApplication.getContext;


public abstract class Common2 extends Automator{
    public static String TAG = "";
    private static JSONArray fileList;
    public static String SCREENSHOT = "screenshot";
    private static String ALIAS;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;

    private static Date date = new Date();
    private static ArrayList<Node> nodeset;
    private static int n;
    private static int m;

    public Common2(){

    }

    public static void goBackHome(int back){
        for (int i = 0; i < back; i++) {
            pressKey("back");
            SystemClock.sleep(300);
        }
        pressKey("home");

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
        if (!isScreenLocked()) {
            return;
        } else {
            try {
                wakeUp();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (password.length() > 0) {
            if (get_elements(true, "content-desc", "密码区域", 0) == null) {
                swipe(0.5 * x, 0.9 * y, 0.5 * x, 0.1 * y, 200);
            }

//            SystemClock.sleep(100);
            for (int i = 0; i < password.length(); i++) {
//                click_element(false, "text", password.substring(i, i + 1), 0, 0);
                executeShellCommand("input keyevent KEYCODE_NUMPAD_" + password.substring(i, i + 1));

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

            executeShellCommand("input keyevent KEYCODE_NUMPAD_" + password.substring(i, i + 1) );
        }
        logUtil.i(TAG, "完成解锁操作");

    }


    public static Boolean isScreenLocked() {

        KeyguardManager mKeyguardManager = (KeyguardManager) getContext().getSystemService (Context. KEYGUARD_SERVICE);
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

        pressKey("power");
        SystemClock.sleep(200);
        logUtil.i(TAG, "锁屏");
    }

    public static JSONArray getScreenSize() {

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
        x_y.put(DeviceInfo.getDeviceInfo().getDisplaySizeDpX());
        x_y.put(DeviceInfo.getDeviceInfo().getDisplaySizeDpY());
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

    public static boolean openNotification() {
        logUtil.i(TAG, "打开通知栏");
        return Automator.openNotification();

    }

//    public static void swipe(String x, String y, String X, String Y, String step) {
//        AdbUtils.runShellCommand("input swipe " + x + " " + y + " " + X + " " + Y + " " + step + "\n", 0);
//    }

    public static JSONArray swipe(double a, double b, double A, double B, int step) {
        int X = 1;
        int Y = 1;
        if (a < 1 & b < 1 & A < 1 & B < 1) {
            JSONArray x_y = Common2.getScreenSize();
            try {
                X = x_y.getInt(0);
                Y = x_y.getInt(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        executeShellCommand("input swipe " + a * X + " " + b * Y + " " + A * X + " " + B * Y + " " + step);

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
        executeShellCommand("pm uninstall " + pkgName);
    }

    public static String startActivity(String activityName) {
//        logUtil.i(TAG, "打开activity：" + activityName);
        if (!getActivity().contains(activityName)) {
            String res = executeShellCommand("am start -n " + activityName);
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
                executeShellCommand(String.format("am start \"%s\"", uri));
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
        String pkg_name = activityName.split("/")[0];
        String act_name = activityName.split("/")[1];
        Intent it = new Intent();
        it.setComponent(new ComponentName(pkg_name, act_name)); //包名和类名
        context.startActivity(it);
    }

    public static void killApp(String pkgName) {
        executeShellCommand("am force-stop " + pkgName);
        SystemClock.sleep(1000);
    }

    public static void clearRecentApp() {
        pressKey("home");
        pressKey("recent");
        String _id = "com.android.systemui:id/clearAnimView"; //小米手机
        try {
            if (!Objects.requireNonNull(CONFIG()).isNull(_id)) {
                JSONArray XY = Objects.requireNonNull(CONFIG()).getJSONArray(_id);
                if (XY.length() > 0) {
                    SystemClock.sleep(300);
                    click(Double.valueOf(XY.get(0).toString()), Double.valueOf(XY.get(1).toString()));
                    SystemClock.sleep(100);
                    return;
                }
            }
            SystemClock.sleep(300);
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
            JSONArray x_y = Common2.getScreenSize();
            try {
                X = x_y.getInt(0);
                Y = x_y.getInt(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        logUtil.d("click", "点击坐标：" + x + "," + y);
        Automator.click((int)(x * X) , (int)(y * Y));
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
//                String res = AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.github.uiautomator.GetDumpTest' com.github.uiautomator.test/android.support.test.runner.AndroidJUnitRunner\n", -1800);
                Automator.dumpWindowHierarchys(false);
//                boolean complete = false;
//                int wait = 0;
//                while (!complete){
//                    if (MyFile.readFile(filePath.toString()).contains("</hierarchy>")){
//                        complete = true;
//                    } else {
//                        SystemClock.sleep(100);
//                        wait += 100;
//                    }
//                    if (wait > 15000){
//                        complete = true;
//                    }
//                }
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
        UiObject2 minput = null;
        try {
            inputObj.put("key", key);
            inputObj.put("value", value);
            inputObj.put("msg", msg);
            inputObj.put("mode", mode);

            switch (key){
                case "id":
                    minput = findObject(By.res(inputObj.getString("value")));
                    break;
                case "resource-id":
                    minput = findObject(By.res(inputObj.getString("value")));
                    break;
                case "resourceId":
                    minput = findObject(By.res(inputObj.getString("value")));
                    break;
                case "text":
                    minput = findObject(By.text(inputObj.getString("value")));
                    break;
                case "content":
                    minput = findObject(By.desc(inputObj.getString("value")));
                    break;
                case "clazz":
                    minput = findObject(By.clazz(inputObj.getString("value")));
                    break;
            }
            if (minput != null) {
                if (mode.equals("a")){
                    inputObj.put("msg", minput.getText() + inputObj.getString("msg"));
                }
                minput.setText(inputObj.getString("msg"));
            } else {
                System.out.println("控件没有找到");
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
//                    node_list.add(e1.attribute("bounds").getValue());
                    }

                }

                if (m > 0) {
                    if (nodeset.size() == (m + nex)) {
                        System.out.println(nodeset.get(m + nex - 1));
                        elem_list.add((Element) nodeset.get(m + nex - 1));
                    }
                }
                getChildNodes(e1, key, value, elem_list, nex);
//
//                System.out.println(e1.getName());
            }
        }
    }

    public static String getActivity() {
        String activity = getCurrentActivity();
        if (activity.contains("\\.")){
            return activity;
        }
        String res = executeShellCommand("dumpsys activity top | grep ACTIVITY | tail -n 1");
        String act = res.trim().split("ACTIVITY ")[1].split(" ")[0];
        return act.trim();
    }

    public static void generateBugreport(final String logFileName) {
        final File bugreportFile = new File(logFileName);
        new Thread() {
            @Override
            public void run() {
                executeShellCommand("bugreport > " + bugreportFile + "\n");
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
            if (takeScreenShot(new File(img), 1.0f, 50)){
                return png;
            } else {
                executeShellCommand("screencap -p " + img + "\n");
                logUtil.i("", png);
                return png;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
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
            executeShellCommand("screencap -p " + img);
        }
        logUtil.i("", png);
        return img;

    }

    public static String getDeviceName() {
        return Build.MODEL;
    }


    public static String getDeviceAlias() {
        return Build.DEVICE;
    }


    public static String getRomName() {
        return Build.VERSION.INCREMENTAL;
    }

    public static String getSerialno() {
        return executeShellCommand("getprop ro.serialno");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public static String getSerialno(Context context) {
        return Build.getSerial();
    }

    public static String getVersionName(String pkg){

        return AppUtils.getVersionName(getContext(), pkg);

    }
    public static int getVersionCode(Context context, String pkg){

        return AppUtils.getVersionCode(context, pkg);

    }

    public static String getVersionName2(String pkg){
        if (pkg.contains("/")){
            pkg = pkg.split("/")[0];
        }
        String res = executeShellCommand("dumpsys package "+pkg+" |grep versionName");
        String[] res_tmp = res.trim().split(" ")[0].split("=");
        return res_tmp[1];
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
        System.out.println(monkey_cmd);
        if (seed != null && seed.length() > 0){
            monkey_cmd = String.format("monkey -p %s -s %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, seed, throttle, count) + _log + "\n";

        }
        executeShellCommand(monkey_cmd);
        return logDir + "monkey_error.txt";
    }

    public static String getMonkeyProcess(){
        return executeShellCommand("ps -A| grep monkey");
    }

    private static JSONArray getAllTestCases(String dir, String _type, String flag, String tag){
        if (flag.equals("reload")){
            fileList = new JSONArray();
        }
        File casesDir = new File(dir);
        if (! casesDir.exists()){
            Toast.makeText(getContext(), "导入用例失败", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "未发现用例配置文件", Toast.LENGTH_SHORT).show();
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
        System.out.println(cases_list);
        return cases_list;
    }


    public static boolean hasSimCard() {
//        Context context = App.getAppContext();
        TelephonyManager telMgr = (TelephonyManager)
                getContext().getSystemService(Context.TELEPHONY_SERVICE);
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

    public static boolean hasNfc(){
        boolean result=false;
        if(getContext()==null)
            return false;
        NfcManager manager = (NfcManager) getContext().getSystemService(Context.NFC_SERVICE);
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
        executeShellCommand("svc wifi enable");
    }

    public static void closeWifi(){
        executeShellCommand("svc wifi disable");
    }



    public static boolean isNetworkConnected() {
        if (getContext() != null) {
            // 获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
            ConnectivityManager manager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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


//        String j = String.format("{\"branch\": \"%s\", \"query\": \"讲个笑话|讲个冷笑话\", \"attach_file\": \"\", \"manager\": \"韩伟(hanwei)\", \"result\": \"Pass\", \"message\": \"\", \"dimensions\": {\"action\": \"笑话\", \"hardware\": \"MI 6\", \"miui\": \"MIUI9.3.18\", \"xiaoai\": \"4.6.0-201904300143-internal_test\", \"third_app\": \"default_default\", \"domain\": \"joke\", \"feature\": \"笑话\", \"time\": \"2019-05-05T11:44:32Z\", \"setup\": \"任意界面\"}}", branch);
//        JSONObject json = new JSONObject(j);

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url) //"http://qp.ai.srv/v1/xiaoai/testcase"
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

        final OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient  = httpBuilder
                //设置超时
                .connectTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(150, TimeUnit.SECONDS)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        responeCode = response.code();

//        okHttpClient.newCall(request).enqueue(new Callback() {
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                System.out.println(response.body().string());
//                responeCode[0] = response.code();
//            }
//
//            @Override
//            public void onFailure(Call arg0, IOException e) {
//                // TODO Auto-generated method stub
//                System.out.println(e.toString());
//
//            }
//
//        });
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

    public static String getLastToast() throws IOException {
        String filePath = Environment.getExternalStorageDirectory() + File.separator + "toast.txt";
        File file = new File(filePath);
        if (file.exists()){
            return MyFile.readFile(filePath);
        }
        return "";
    }

}

