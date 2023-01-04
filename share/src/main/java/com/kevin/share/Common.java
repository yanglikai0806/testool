package com.kevin.share;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.WindowManager;

import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.share.service.RecordService;
import com.kevin.share.utils.AppUtils;
import com.kevin.share.utils.BatteryManagerUtils;
import com.kevin.share.utils.CvUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.MemoryManager;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.StringUtil;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.WifiUtils;
import com.kevin.share.utils.logUtil;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static com.kevin.share.CONST.ACCESSIBILITY_RECEIVER;
import static com.kevin.share.CONST.AUTOTEST;
import static com.kevin.share.CONST.CONFIG_FILE;
import static com.kevin.share.CONST.DRAG_RECEIVER;
import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.CONST.LOGPATH;
import static com.kevin.share.CONST.NOT_CLEAR_RECENT_APP;
import static com.kevin.share.CONST.REPORT_PATH;
import static com.kevin.share.CONST.TARGET_APP;
import static com.kevin.share.accessibility.AccessibilityHelper.dumpElementInfo;
import static com.kevin.share.accessibility.AccessibilityHelper.getNodeInfoToUse;
import static com.kevin.share.utils.StringUtil.isContainChinese;
import static com.kevin.share.utils.StringUtil.isNumeric;
import static java.lang.Math.abs;


public class Common {
    private static final String ALIAS = "";
    public static String TAG = "";
    private static JSONArray fileList;
    private static String SCREENSHOT = "screenshot";

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;
    private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);//日期格式;

    private static Date date = new Date();
    private static ArrayList<Node> nodeset;
    private static int n;
    private static int m;

    private static boolean isBugreportCapturing = false;
    public static boolean enableGetElementsByAccessibility = false;
    public static int getElementBy = -1; // 0:adb; 1: instrument; 2: accessibility
    public static int waitTimeForElement = 5000;

    public static JSONObject CONFIG = new JSONObject();
    private static String DEVICE_TYPE = "";
    private static String DEVICE_NAME = "";
    private static String DEVICE_ALIAS = "";
    private static boolean setToIdle = false;

    public static int getElementBy(){
        if (getElementBy > -1){
            return getElementBy;
        }
        if (CONFIG().optInt("GET_ELEMENT_BY", -1) > 0){
            getElementBy =  CONFIG().optInt("GET_ELEMENT_BY");
        } else {
            if (AppUtils.isApkInstalled(AppContext.getContext(),"com.kevin.testassist.test")) getElementBy = 1;
            else if (enableGetElementsByAccessibility) getElementBy = 2;
            else getElementBy = 0;
        }
        return getElementBy;
    }

    // 配置信息相关↓
    public static JSONObject CONFIG(){
        if (CONFIG.length() > 0) {
            return CONFIG;
        }
        return reloadCONFIG();
    }

    public static JSONObject reloadCONFIG(){
        try {
            if (new File(CONST.CONFIG_FILE).exists()) {
                CONFIG = new JSONObject(FileUtils.readJsonFile(CONST.CONFIG_FILE));
                logUtil.d("", "重载config文件");

            } else {
                logUtil.d("", "config.json 文件不存在");
                CONFIG = new JSONObject(CONST.CONFIG_JSON);
            }
        } catch (Exception e){
            logUtil.e("reloadCONFIG", e);
        }
        return CONFIG;
    }

    public static void updateCONFIG(String content){
        try {
            CONFIG = new JSONObject(content);
            FileUtils.writeFile(CONFIG_FILE, content, false);
        } catch (JSONException e) {
            logUtil.e("", e);
        }

    }

    public static String SERVER_BASE_URL() {
        return CONFIG().optString("SERVER_BASE_URL");

    }

    // 配置信息相关↑






    // 设备相关信息获取↓
    public static String getActivity() {

//        String res = AdbUtils.runShellCommand("dumpsys input | grep FocusedApplication\n", 0);
//        String[] act = res.trim().split("name=")[1].split("u0")[1].trim().split(" ");
        String res = ShellUtils.runShellCommand("dumpsys activity top | grep ACTIVITY | tail -n 1", 0);
        String act = res.replace("ACTIVITY", "").trim().split(" ")[0];
        if (TextUtils.isEmpty(act)) {
            ActivityManager mActivityManager = (ActivityManager) AppContext.getContext().getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
//        logUtil.d("get_activity", rti.get(0).topActivity.flattenToString());
            logUtil.d("get_activity", rti.get(0).topActivity.flattenToShortString());
            return rti.get(0).topActivity.flattenToShortString();
        }
        return act.trim();
    }

    public static String getDeviceName() {
        if (!TextUtils.isEmpty(DEVICE_NAME)){
            return DEVICE_NAME;
        }
        String res = ShellUtils.runShellCommand("getprop ro.product.marketname",1000);
        if (!TextUtils.isEmpty(res.trim())){
            DEVICE_NAME = res.trim();
        } else {
            DEVICE_NAME = Build.MODEL;
        }
        return DEVICE_NAME;
    }

    public static String getDeviceAlias() {
        if (!TextUtils.isEmpty(DEVICE_ALIAS)){
            return DEVICE_ALIAS;
        }
        String res = ShellUtils.runShellCommand("getprop ro.product.name",0);
        if (!TextUtils.isEmpty(res.trim())){
            DEVICE_ALIAS = res.trim();
        } else {
            DEVICE_ALIAS = Build.DEVICE;
        }
        return DEVICE_ALIAS;
    }

    public static String getDeviceType() {
        if (!TextUtils.isEmpty(DEVICE_TYPE)){
            return DEVICE_TYPE;
        }
        String dn = getDeviceName();
        if (dn.toLowerCase().contains("mitv")){
            DEVICE_TYPE = "TV";
        } else if (dn.toLowerCase().contains("pad")){
            DEVICE_TYPE = "PAD";
        } else {
            DEVICE_TYPE = "PHONE";
        }
        return DEVICE_TYPE;
    }

    public static String getRomName() {
        return Build.VERSION.INCREMENTAL;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getDeviceManufacturer(){
        return Build.MANUFACTURER;
    }

    public static JSONObject getDeviceInfo(){
        JSONObject info = new JSONObject();
        try {
            info.put("厂商", getDeviceManufacturer());
            info.put("名称", getDeviceName());
            info.put("代号", getDeviceAlias());
            info.put("Android版本", getAndroidVersion());
            info.put("ROM", getRomName());
            info.put("可用存储", MemoryManager.getAvailableInternalMemorySize()/(1024*1024*1024) + "GB");
            info.put("剩余电量", BatteryManagerUtils.getBatteryCapacity());
            info.put("测试工具", getVersionName(AppContext.getContext(), AppContext.getContext().getPackageName()));
            info.put("ROOT", ShellUtils.hasRootPermission() + "");
            info.put(TARGET_APP, getVersionName(AppContext.getContext(), TARGET_APP));
        } catch (JSONException e) {
            logUtil.e("", e);
        }
        return info;
    }

    public static String getSerialno() {
        return ShellUtils.runShellCommand("getprop ro.serialno", 0).trim();
    }

    public static String getDeviceId(){
        String deviceId = SPUtils.getString("mock_data", "device_id");
        if (TextUtils.isEmpty(deviceId)){
            deviceId = getSerialno();
        }
        logUtil.d("设备ID", deviceId);
        return deviceId;
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

    public static void setDeviceId(String deviceId){
        SPUtils.putString("mock_data", "device_id", deviceId);
    }

    public static String getVersionName(Context context, String pkg){
        String versionName = "";
        try {
            versionName = AppUtils.getVersionName(context, pkg);
            if (TextUtils.isEmpty(versionName)) {
                versionName = getVersionName2(pkg);
            }
        } catch (Exception ignore){

        }
//        logUtil.d("", pkg + " 版本：" + versionName);
        return versionName;

    }

    public static int getVersionCode(Context context, String pkg){

        return AppUtils.getVersionCode(context, pkg);

    }

    public static String getVersionName2(String pkg){
        if (pkg.contains("/")){
            pkg = pkg.split("/")[0];
        }
        if (TextUtils.isEmpty(pkg)){
            return "";
        }
        String res = ShellUtils.runShellCommand("dumpsys package "+pkg+" |grep versionName", 0);
        logUtil.d("获取应用版本号：", res);
        String[] res_tmp = res.trim().split(" ")[0].split("=");
        return res_tmp[1];
    }

    public static int getSoundVal(int chanel){
        try {
            AudioManager audioManager = (AudioManager) AppContext.getContext().getSystemService(Context.AUDIO_SERVICE);
            return audioManager.getStreamVolume(chanel);
        } catch (Exception e){
            return -1;
        }
    }

    /**
     * 统计所装app数量
     */

    public static int appSum(){
        String res = ShellUtils.runShellCommand("pm list package | wc -l", 0);
        return !TextUtils.isEmpty(res) ? Integer.parseInt(res.trim()): 0;
    }

    /**
     * 设备是否处于空闲状态，根据任务进程及monkey进程是否运行判断
     * @return
     */

    public static boolean isIdle(){
        boolean isIdle = true;
        ActivityManager am = (ActivityManager) AppContext.getContext().getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
        while(iter.hasNext()){
            ActivityManager.RunningAppProcessInfo next = iter.next();
            String mProcess = AppContext.getContext().getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
            if(next.processName.equals(mProcess)){
                isIdle = false;
                break;
            }
        }
        return (getMonkeyProcess().length() == 0 & isIdle) || setToIdle;
    }

    public static void setIdle(boolean idle) {
        setToIdle = idle;
    }

    /**
     *
     * @return JSONObject 获取设备状态信息
     */
    public static JSONObject getDeviceStatusInfo(){
        JSONObject deviceStatusInfo = new JSONObject();
        try {
            deviceStatusInfo.put("idle", String.valueOf(isIdle()));
            deviceStatusInfo.put("remote", 0);
            deviceStatusInfo.put("device_name",getDeviceAlias());
            deviceStatusInfo.put("device_type", getDeviceType());
            deviceStatusInfo.put("device_id", getSerialno());
            deviceStatusInfo.put("device_ip", new WifiUtils().getIPAddress());
            deviceStatusInfo.put("device_info", getDeviceInfo().toString());
            deviceStatusInfo.put("battery", BatteryManagerUtils.getBatteryCapacity() + "");
            deviceStatusInfo.put("apk_version", getVersionName(AppContext.getContext(), TARGET_APP));
        } catch (JSONException e) {
            logUtil.e("", e);
        }
//        logUtil.d("设备信息", deviceStatusInfo.toString());
        return deviceStatusInfo;
    }

    /**
     *
     * @return JSONObject 获取设备状态信息
     */
    public static JSONObject getDeviceStatusInfo(String idle){
        JSONObject deviceStatusInfo = getDeviceStatusInfo();
        try {
            deviceStatusInfo.put("idle", idle);
        } catch (JSONException e) {
            logUtil.e("", e);
        }
        logUtil.d("设备信息", deviceStatusInfo.toString());
        return deviceStatusInfo;
    }


    public static Point getScreenSize() {
        WindowManager windowManager = (WindowManager) AppContext.getContext().getSystemService(WINDOW_SERVICE);
        android.graphics.Point size = new Point();
        windowManager.getDefaultDisplay().getRealSize(size);
        return size;
    }

    public static int getScreenWidth() {
        return getScreenSize().x;
    }

    public static int getScreenHeight() {
        return getScreenSize().y;
    }

    public static void setWaitTimeForElement(int ms){
        waitTimeForElement = ms;
    }

    public static void setGetElementsByAccessibility(boolean enable){
        if (!AccessibilityHelper.checkAccessibilityEnabled()){
            enable = false;
        }
        enableGetElementsByAccessibility = enable;
        logUtil.d("setGetElementsByAccessibility", enable + "");
    }


    public static ArrayList<Element> get_elements_by_instrument(Boolean fresh, String key, String value, int nex) {
        ArrayList<Element> targetElements = new ArrayList<>();
        File filePath = new File(DUMP_PATH);
        if (fresh) {
            try {
                if (filePath.exists()) {
                    filePath.delete();
                }
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < waitTimeForElement) {
                    if (getElementBy() == 1) {
                        logUtil.d("get_elements", "--------------by instrument-------------");
                        ShellUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.dumpWindow' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);

                    } else {
                        logUtil.d("get_elements", "--------------by adb-------------");
                        ShellUtils.runShellCommand("uiautomator dump", 0);
                    }
                    try {
                        if (FileUtils.readFile(DUMP_PATH).endsWith("</hierarchy>")){

                            targetElements = parserXml(filePath.toString(), key, value, nex);//.get(nex);
                            if (targetElements.size() > 0) {
                                return targetElements;
                            } else {
                                break;
                            }
                        }
                    } catch (Exception e) {
//                        logUtil.e("", e);
                    }
                    SystemClock.sleep(1000);

                }
            } catch (Exception e) {
                logUtil.e("", e);
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
        return targetElements;
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
        if (elements.size() > 0){
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
        ArrayList<Element> elements = new ArrayList<>();
        if (!nodeInfo.isNull("index")){
            index = nodeInfo.getInt("index");
            nodeInfo.remove("index");
        }
        if (!nodeInfo.isNull("nex")){
            nex = nodeInfo.getInt("nex");
            nodeInfo.remove("nex");
        }
        if (!nodeInfo.isNull("id")){
            elements = get_elements(refresh, "resource-id", nodeInfo.getString("id"), nex);
        }
        if (!nodeInfo.isNull("resource-id")){
            elements = get_elements(refresh, "resource-id", nodeInfo.getString("resource-id"), nex);
        }
        if (!nodeInfo.isNull("text")){

            elements = get_elements(refresh, "text", nodeInfo.getString("text"), nex);
        }
        if (!nodeInfo.isNull("content")){
            elements = get_elements(refresh, "content-desc", nodeInfo.getString("content"), nex);
        }
        if (!nodeInfo.isNull("content-desc")){
            elements = get_elements(refresh, "content-desc", nodeInfo.getString("content-desc"), nex);
        }
        if (!nodeInfo.isNull("class")){
            elements = get_elements(refresh, "class", nodeInfo.getString("class"), nex);
        }
        if (elements.size() > index){

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

    public static String get_element_text_value(boolean refresh, JSONObject nodeInfo) throws JSONException {
        Element element = get_element(refresh, nodeInfo);
        if (element != null){
            if (element.attributeValue("text").length() > 0){
                return element.attributeValue("text");
            } else {
                return element.attributeValue("content-desc");
            }
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
        while (System.currentTimeMillis() - startTime < waitTimeForElement) {

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
            SystemClock.sleep(1000);

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

    public static String getProcessId(String process){
        String pid = ShellUtils.runShellCommand(String.format("ps -A| grep %s |awk '{print $2}'", process), 0);
        if (!StringUtil.isNumeric(pid)) {
            pid = ShellUtils.runShellCommand(String.format("ps | grep %s |awk '{print $2}'", process), 0);
        }
        if (!StringUtil.isNumeric(pid)) {
            pid = ShellUtils.runShellCommand(String.format("ps -A| grep %s", process), 0);
            String[] item = pid.split(" ");
            for (int i = 1; i < item.length; i++) {
                if (item[i].length() > 0) {
                    pid = item[i];
                    break;
                }
            }
        }
        logUtil.d("", process+" 进程pid:"+pid);
        return pid;
    }

    public static int getProcessPid(String processName){
        int processPid = 0;
        ActivityManager am = (ActivityManager) AppContext.getContext().getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
        while(iter.hasNext()){
            ActivityManager.RunningAppProcessInfo next = iter.next();
            if(next.processName.equals(processName)){
                processPid = next.pid;
                break;

            }
        }
        return processPid;
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
        return elem_list;

    }

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
        if (aBounds.length()==0 || bBounds.length() == 0) return false;
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
     * 根据bounds判断a是否在b内
     * @param aBounds 目标
     * @param bBounds 源
     * @param extend 源扩展量
     * @return
     */
    public static boolean isInBounds(String aBounds, String bBounds, int extend){
        if (aBounds.length()==0 || bBounds.length() == 0) return false;
        Rect a = parseBounds(aBounds);
        Rect b = parseBounds(bBounds);
        if (b.left - extend <= a.left){
            if(b.top - extend <= a.top){
                if (b.right + extend >= a.right){
                    return b.bottom + extend >= a.bottom;
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
        if (TextUtils.isEmpty(bounds)){
            return null;
        }
        String[] x_y = bounds.replace("，", ",").replace(" ","").replace("[", "").replace("]", ",").split(",");
        double left, top, right, bottom;
        left = Double.valueOf(x_y[0]);
        top = Double.valueOf(x_y[1]);
        right = Double.valueOf(x_y[2]);
        bottom = Double.valueOf(x_y[3]);
        if (left <= 1 && top <= 1 && right <= 1 && bottom <= 1){ //支持相对值表示bounds
            Point size = getScreenSize();
            int w = size.x;
            int h = size.y;

            left = left * w;
            top = top * h;
            right = right * w;
            bottom = bottom * h;

        }
        Rect rt = new Rect();
        rt.left = (int) left;
        rt.top = (int) top;
        rt.right = (int) right;
        rt.bottom = (int) bottom;
        return rt;
    }


    /**
     * 解析Rect to bounds string
     * @param rect
     * @param scale 缩放比例
     * @param ext 扩展值
     * @return
     */
    public static String parseRect(Rect rect, float scale, int ext){
        int left = rect.left;
        int top = rect.top;
        int right = rect.right;
        int bottom = rect.bottom;
        return String.format("[%s,%s][%s,%s]", (int)(left/scale - ext), (int)(top/scale - ext), (int)(right/scale + ext), (int)(bottom/scale + ext));
    }

    public static String parseRect(Rect rect){
        if (rect == null){
            return "";
        }
        int left = rect.left;
        int top = rect.top;
        int right = rect.right;
        int bottom = rect.bottom;
        return String.format("[%s,%s][%s,%s]", (int)(left), (int)(top), (int)(right), (int)(bottom));
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
        ArrayList<Element> elements = new ArrayList<>();
        if (ShellUtils.isShellEnable()) {
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
        if (elements.size() == 0){
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
            int distance = (int) Math.sqrt(abs((ePoint.getInt(0) - x))+ abs((ePoint.getInt(1)-y)));
            disDataLst.add(distance);

        }
        if (disDataLst.size() > 0) {

            int indexMin = disDataLst.indexOf(Collections.min(disDataLst));
            logUtil.d("index is", indexMin + "");
            return getNodeInfoToUse(elementLst.getJSONObject(indexMin), true);
        }
        return new JSONObject().put("click", point);
    }
    // 设备相关信息获取↑





    // 手机操作↓
    public static void goBackHome(int back) {
        for (int i = 0; i < back; i++) {
            ShellUtils.runShellCommand("input keyevent 4", 0);
        }
        ShellUtils.runShellCommand("input keyevent 3", 0);

    }

    public static void unlockScreen(String password) {
        Point size = getScreenSize();
        int x = size.x;
        int y = size.y;
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
                ShellUtils.runShellCommand("input keyevent KEYCODE_NUMPAD_" + password.substring(i, i + 1), 0);

            }

        } else {
            swipe(0.5 * x, 0.9 * y, 0.5 * x, 0.1 * y, 200);
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
        if (StringUtil.isNumeric(key)){
            ShellUtils.runShellCommand("input keyevent " + key, 0);
            return;
        }
        switch (key.toLowerCase()) {
            case "home":
                ShellUtils.runShellCommand("input keyevent 3", 0);
                break;
            case "back":
                ShellUtils.runShellCommand("input keyevent 4", 0);
                break;
            case "返回":
                ShellUtils.runShellCommand("input keyevent 4", 0);
                break;
            case "recent":
                ShellUtils.runShellCommand("input keyevent KEYCODE_MENU", 0);
                break;
            case "多任务":
                ShellUtils.runShellCommand("input keyevent KEYCODE_MENU", 0);
                break;
            case "power":
                ShellUtils.runShellCommand("input keyevent 26", 0);
                break;
            case "电源":
                ShellUtils.runShellCommand("input keyevent 26", 0);
                break;
            case "enter": // 电视遥控器enter键
                ShellUtils.runShellCommand("input keyevent KEYCODE_ENTER", 0);
                break;
        }
    }

    public static void press(String key, int time){
        for (int i=0; i<=time;i++){
            press(key);
            SystemClock.sleep(100);
        }
    }

    public static void pressPower() {
        press("power");
        SystemClock.sleep(100);
    }

    public static Boolean isScreenOn() {

        PowerManager pm = (PowerManager) AppContext.getContext().getSystemService(Service.POWER_SERVICE);
        logUtil.i(TAG, "是否亮屏：" + pm.isScreenOn());
        return pm.isScreenOn();

    }

    public static Boolean isScreenLocked() {

        KeyguardManager mKeyguardManager = (KeyguardManager) AppContext.getContext().getSystemService(Context.KEYGUARD_SERVICE);
        boolean flag = mKeyguardManager.inKeyguardRestrictedInputMode();
        logUtil.i(TAG, "是否锁屏：" + flag);
        return flag;

    }

    public static void lockScreen() {

        ShellUtils.runShellCommand("input keyevent 26", 0);
        SystemClock.sleep(200);
        logUtil.i(TAG, "锁屏");
    }


    public static void openNotification(String mode) {
        Point size = getScreenSize();
        int x = size.x;
        int y = size.y;
        if (mode.equals("left")) {
            swipe(0.2 * x, 0.01 * y, 0.2 * x, 0.8 * y, 500);
        } else if (mode.equals("right")){
            swipe(0.8 * x, 0.01 * y, 0.8 * x, 0.8 * y, 500);
        } else {
            swipe(0.2 * x, 0.01 * y, 0.2 * x, 0.8 * y, 500);
        }
        logUtil.i(TAG, "打开通知栏");

    }

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
            Point size = Common.getScreenSize();
            X = size.x;
            Y = size.y;

        }
        ShellUtils.runShellCommand("input swipe " + a * X + " " + b * Y + " " + A * X + " " + B * Y + " " + duration, 0);
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

    public static void swipe(Object value){
        double x1 = 0;
        double x2 = 0;
        double y1 = 0;
        double y2 = 0;
        int duration = 500;
        try {
            if (value instanceof JSONArray) {
                x1 = ((JSONArray) value).getDouble(0);
                y1 = ((JSONArray) value).getDouble(1);
                x2 = ((JSONArray) value).getDouble(2);
                y2 = ((JSONArray) value).getDouble(3);
                duration = ((JSONArray) value).optInt(4, 500);
            } else {
                if (value.toString().equalsIgnoreCase("left")){
                    x1 = 0.3;
                    x2 = 0.7;
                    y1 = 0.5;
                    y2 = 0.5;

                } else if (value.toString().equalsIgnoreCase("right")) {
                    x1 = 0.7;
                    x2 = 0.3;
                    y1 = 0.5;
                    y2 = 0.5;

                } else if (value.toString().equalsIgnoreCase("up")) {
                    x1 = 0.5;
                    x2 = 0.5;
                    y1 = 0.3;
                    y2 = 0.7;

                } else if (value.toString().equalsIgnoreCase("dowm")) {
                    x1 = 0.5;
                    x2 = 0.5;
                    y1 = 0.7;
                    y2 = 0.3;

                } else {
                    String[] deta = value.toString().split(",");
                    x1 = Double.parseDouble(deta[0]);
                    y1 = Double.parseDouble(deta[1]);
                    x2 = Double.parseDouble(deta[2]);
                    y2 = Double.parseDouble(deta[3]);
                    duration = Integer.parseInt(deta[4]);
                }
            }
        } catch (Exception e){
//            logUtil.e("",e);
        }
        swipe(x1 , y1, x2, y2, duration);
    }

    /**
     * 拖拽
     * @param drag [x,y,X,Y,duration]
     */

    public static void drag(JSONArray drag){
        ShellUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.drag' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", -500);
        SystemClock.sleep(500);
        Intent dragIntent = new Intent(DRAG_RECEIVER);
        dragIntent.putExtra("ARGS", drag.toString());
        AppContext.getContext().sendBroadcast(dragIntent);
    }

    public static void uninstallApp(String pkgName) {
        ShellUtils.runShellCommand("pm uninstall " + pkgName, 0);
        SystemClock.sleep(5000);
    }

    public static String startActivity(String activityName) {
//        logUtil.i(TAG, "打开activity：" + activityName);
        if (!getActivity().contains(activityName)) {
            String res = ShellUtils.runShellCommand("am start -n " + activityName, 0);
            SystemClock.sleep(1000);
            return res;
        } else {
            return "Starting: Intent { " + activityName + " }";
        }
    }

    public static String startActivity(String activityName, String mode) {
//        logUtil.i(TAG, "打开activity：" + activityName);
        if (!getActivity().contains(activityName)) {
            String res;
            if (mode.equals("restart")){
                res = ShellUtils.runShellCommand("am start -S " + activityName, 0);
            } else {
                res = ShellUtils.runShellCommand("am start -n " + activityName, 0);
            }
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
                ShellUtils.runShellCommand(String.format("am start \"%s\"", uri), 0);
                success = true;
            } else {
                logUtil.d(TAG, "No Application can handle your intent");
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static String launchActivity(Object activity, String mode) throws JSONException {
        if (activity instanceof JSONArray) {
            Random random = new Random();
            int i = random.nextInt(((JSONArray) activity).length());
            return startActivity(((JSONArray) activity).get(i).toString(), mode);

        } else {
            return startActivity(activity.toString(), mode);
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
            if (isContainChinese(activityName)){
                try {
                    activityName = CONFIG().getJSONObject("APP").getString(activityName);
                } catch (JSONException e) {
                    logUtil.e("", e);
                }
            }
            Intent intent= packageManager.getLaunchIntentForPackage(activityName);
            context.startActivity(intent);
        }
    }

    public static void killApp(String pkgName) {
        ShellUtils.runShellCommand("am force-stop " + pkgName, 0);
        SystemClock.sleep(1000);
    }

    public static boolean killProcess(String process) {
        String pid = "";
        pid = getProcessId(process);

        if (pid.length() == 0) {
            logUtil.d("", "killProcess: " + process + " 失败, 未找到该进程pid");
            return false;
        }
        logUtil.d("", "killProcess: " + pid);
        ShellUtils.runShellCommand("kill -2 " + pid, 0);
        SystemClock.sleep(1000);
        return true;
    }

    public static void clearRecentApp() {
        if (CONFIG().optString("DISABLE_CLEAR_RECENT_APP").equals("true")) return;
        if (Arrays.asList(NOT_CLEAR_RECENT_APP).contains(TARGET_APP)) return;
        press("home", 2);
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
            if (get_element(true, "resource-id", _id, 0) == null){
                _id = "com.miui.home:id/clearAnimView";
            }
            JSONArray XY = click_element(false, "resource-id", _id, 0, 0, "");
            JSONObject CONFIG_UPDATE = new JSONObject(String.valueOf(CONFIG()));
            CONFIG_UPDATE.put("CLEAR_BUTTON_ID", _id);
            CONFIG_UPDATE.put(_id, XY);
            // 坐标值缓存在配置文件，以提高执行效率
            FileUtils.writeFile(CONST.CONFIG_FILE, CONFIG_UPDATE.toString().replace(",\"", ",\n\""), false);
        } catch (Exception e) {
            logUtil.d("", "config.json 文件不存在");
            click_element(true, "resource-id", _id, 0, 0, "");
        }
    }

    public static void clearAppData(String pkg){
        ShellUtils.runShellCommand("pm clear " + pkg, 0);
    }



    public static void click(double x, double y) {
        int X = 1;
        int Y = 1;
        if (x < 1 & y < 1) {
            Point size = getScreenSize();
            X = size.x;
            Y = size.y;
        }
        logUtil.d("", "input tap " + x * X + " " + y * Y);
        ShellUtils.runShellCommand("input tap " + x * X + " " + y * Y, 0);
    }

    public static void click(String bounds) {
        Rect rt = parseBounds(bounds);
        int x = (rt.left + rt.right)/2;
        int y = (rt.top + rt.bottom)/2;
        logUtil.d("", x + "," + y);
        click(x, y);
    }

    public static void long_click(double x, double y, String mode) {
        int X = 1;
        int Y = 1;
        if (x < 1 & y < 1) {
            Point size = getScreenSize();
            X = size.x;
            Y = size.y;
        }
        if (mode.equals("true")){
            long_click(x, y);
        }
        if (isNumeric(mode)) {
            swipe(x * X, y * Y, x * X, y * Y, Integer.valueOf(mode));
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
            Point size = getScreenSize();
            X = size.x;
            Y = size.y;
        }
        swipe(x * X, y * Y, x * X, y * Y, 1500);
    }

    public static void long_click(String bounds) {
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
        int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
        long_click(x, y);
    }

    public static JSONArray click_element(boolean refresh, String key, String value, int nex, int index, String longClick, String... param) {
        String bounds = "";
        String mode = "";
        String checked = "";
        if (param.length > 0){
            mode = param[0];
            if (mode.equals("true") || mode.equals("false")){
                checked = mode;
            }
        }
        if (key.equals("ocr")){ // 通过ocr获取界面文本
            bounds = CvUtils.getTextFromScreenshot(value, index, true);
        } else if (key.equals("txt")) {
            ArrayList<Element> element = get_elements(refresh, "text", value, nex);
            if (element.size() > 0) {
                bounds = element.get(index).attribute("bounds").getValue();
                // 控件状态判断，当控件状态符合预期时不做操作（用于开关状态转换）
                if (checked.equals(element.get(index).attribute("checked").getValue())){
                    return null;
                }
                if (mode.equals("random")){
                    int randomIndex = new Random().nextInt(element.size());
                    bounds = element.get(randomIndex).attribute("bounds").getValue();
                    logUtil.i("", "随机操作第" + randomIndex + "个元素");
                }
            } else {
                // 如果通过dump未获得界面文本，通过ocr尝试获取
                bounds = CvUtils.getTextFromScreenshot(value, index, false);
            }

        } else {
            ArrayList<Element> element = get_elements(refresh, key, value, nex);
            if (element.size() > 0) {
                bounds = element.get(index).attribute("bounds").getValue();
                // 控件状态判断，当控件状态符合预期时不做操作（用于开关状态转换）
                if (checked.equals(element.get(index).attribute("checked").getValue())){
                    return null;
                }
                if (mode.equals("random")){
                    int randomIndex = new Random().nextInt(element.size());
                    bounds = element.get(randomIndex).attribute("bounds").getValue();
                    logUtil.i("", "随机操作第" + randomIndex + "个元素");
                }
            }
        }
        if (bounds.length() > 0) {
            Rect rt = parseBounds(bounds);
            int x = (rt.left + rt.right) / 2;
            int y = (rt.top + rt.bottom) / 2;
            if (longClick.length() > 0) {
                long_click(x, y, longClick);
                logUtil.i(TAG, "完成长按元素 " + key + ":" + value);
            } else {
                click(x, y);
                logUtil.i(TAG, "完成点击元素 " + key + ":" + value);
            }

            try {
                return new JSONArray().put(0, x).put(1, y);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        logUtil.i(TAG, String.format("没有找到 %s ： %s", key, value));
        return null;
    }


    public static boolean click_element(Boolean refresh, String key, String value, int nex, int index) {
        String bounds;
        ArrayList<Element> element = get_elements(refresh, key, value, nex);
        if (element.size() > 0) {
            bounds = element.get(index).attribute("bounds").getValue();
            click(bounds);
            logUtil.i(TAG, "完成点击元素 " + key + ":" + value);
            return true;
        } else {
            logUtil.i(TAG, String.format("没有找到 %s ： %s", key, value));
            return false;
        }
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
                ShellUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.Input#input' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void inputText(JSONObject inputJO) throws JSONException {

        Iterator<String> itrInput = inputJO.keys();
        ArrayList<String> keys = new ArrayList<>();
        while (itrInput.hasNext()){
            keys.add(itrInput.next());
        }
        String key = "resource-id";
        String value;
        String msg = "";
        String mode = "w";
        int nex = 0, index = 0;
        if (keys.contains("nex")){
            nex = (int) inputJO.get("nex");
            keys.remove("nex");
        }
        if (keys.contains("index")){
            index = (int) inputJO.get("index");
            keys.remove("index");
        }
        if (keys.contains("msg")){
            msg = inputJO.getString("msg");
            keys.remove("msg");
        }
        if (keys.contains("mode")){
            mode = inputJO.getString("mode");
            keys.remove("mode");
        }
        if (keys.size() > 0){
            key = keys.get(0);
        }
        value = inputJO.getString(key);
        inputText(key, value, msg, mode);
    }


    public static void generateBugreport(final String logFileName) {
        if (isBugreportCapturing) {
            return;
        }
        isBugreportCapturing = true;
        final File bugreportFile = new File(logFileName);
        logUtil.i("", bugreportFile.getName().replace(".txt", ".zip"));
        new Thread(() -> {
            ShellUtils.runShellCommand("bugreport > " + bugreportFile, 0);
            isBugreportCapturing = false;
            try {
                FileUtils.ZipFolder(logFileName, logFileName.replace(".txt", ".zip"));
                FileUtils.deleteFile(logFileName);
            } catch (Exception e) {
                logUtil.e("", e);
            }
        }).start();
    }

    public static void generateBugreport2(final String logFileName) {
        if (isBugreportCapturing) {
            return;
        }
        isBugreportCapturing = true;
        final File bugreportFile = new File(logFileName);
        logUtil.i("", bugreportFile.getName().replace(".txt", ".zip"));

        ShellUtils.runShellCommand("bugreport > " + bugreportFile, 0);
        isBugreportCapturing = false;
        try {
            FileUtils.ZipFolder(logFileName, logFileName.replace(".txt", ".zip"));
            FileUtils.deleteFile(logFileName);
        } catch (Exception e) {
            logUtil.e("", e);
        }
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

    public static String screenShot2() {
        String img;
        try {
            String jpg = dateFormat.format(new Date()) + ".png";
            img = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + jpg;
            screenShot(img);
            logUtil.d("", img);
            return img;

        } catch (Exception e) {
            logUtil.e("", e);
            return "";
        }
    }

    /**
     * 先通过RecordService服务实现截图，如果未能成功截图会通过adb命令进行兜底截图操作
     * @param path
     * @return
     */

    public static String screenShot(String path){
        File imgFile = new File(path);
        if (imgFile.exists()){
            imgFile.delete();
        }
        return screenShot(path, 1, 70);
    }

    public static String screenShot(String path, int zoom, int quality) {
        // MediaProjection截图
        File imgFile = new File(path);
        if (imgFile.exists()){
            imgFile.delete();
        }
        Intent sIntent = new Intent(AppContext.getContext(), RecordService.class);
        sIntent.putExtra("ZOOM", zoom);
        sIntent.putExtra("QUALITY", quality);
        sIntent.putExtra("FILE_PATH", path);
        AppContext.getContext().startService(sIntent);
        SystemClock.sleep(50);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 1000){
            if (imgFile.exists()){
                SystemClock.sleep(50);
                return path;
            }
            SystemClock.sleep(50);
        }
        // shell截图（兜底策略）
        long st = System.currentTimeMillis();
        ShellUtils.runShellCommand("screencap -p " + path, 0);
        logUtil.d("shell截图耗时", (System.currentTimeMillis() - st) + "");
        return path;
    }

    /**
     * 屏幕录制， 自定义画质720*1080
     * @param seconds
     * @return
     */
    public static String screenRecorder(int seconds) {
        String mp4 = dateFormat.format(new Date()) + ".mp4";
        try {
            String mp4File = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + mp4;
            if (seconds > 0) {
                ShellUtils.runShellCommand(String.format("screenrecord --bit-rate 3000000 --size 720x1080  --time-limit %s %s", seconds, mp4File), -50);
            } else {
                ShellUtils.runShellCommand(String.format("screenrecord --bit-rate 3000000 --size 720x1080 %s", mp4File), -50);
            }
            return mp4File;
        } catch (IOException e) {
            logUtil.e("Common.screenRecorder", e.toString());
            return "";
        }
    }

    /**
     * 屏幕录制， 自定义画质，录制mic声音
     * @param seconds
     * @return
     */

    public static String screenRecorder(String mp4File, int seconds, int zoom) {
        if (seconds == 0) seconds = 1800;
        boolean ok = RecordService.recordScreen(mp4File, getScreenWidth()/zoom, getScreenHeight()/zoom, seconds);
        if (ok){
            return mp4File;
        }

        File file = new File(mp4File);
        if (file.exists()){
            file.delete();
        }
        Intent sIntent = new Intent(AppContext.getContext(), RecordService.class);
        sIntent.putExtra("ZOOM", zoom);
        sIntent.putExtra("SECONDS", seconds);
        sIntent.putExtra("FILE_PATH", mp4File);
        AppContext.getContext().startService(sIntent);

        return mp4File;
    }

    /**
     * adb屏幕录制，默认画质,无声音
     * @param seconds
     * @return
     */
    public static String screenRecorder2(int seconds) {
        String mp4 = dateFormat.format(new Date()) + ".mp4";
        try {
            String mp4File = REPORT_PATH + logUtil.readTempFile() + File.separator + SCREENSHOT + File.separator + mp4;
            if (seconds > 0) {
                ShellUtils.runShellCommand(String.format("screenrecord --time-limit %s %s", seconds, mp4File), -50);
            } else {
                ShellUtils.runShellCommand(String.format("screenrecord %s", mp4File), -50);
            }
            return mp4File;
        } catch (IOException e) {
            logUtil.e("", e);
            return "";
        }
    }

    public static String runMonkey(String pkg, String count, String throttle, String seed, String logDir){
        if (pkg.contains("/.")){
            pkg = pkg.split("/.")[0];
        }

        FileUtils.creatDir(logDir);
//        String _log = "2>>" + logDir + "monkey_error.txt 1>> " + logDir + "monkey_info.txt";
        String _log = "2>>" + logDir + "monkey_error.txt 1>/dev/null";
        String monkey_cmd = String.format("monkey -p %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s ", pkg, throttle, count) + _log ;
        logUtil.d("MONKEY",monkey_cmd);
        if (seed != null && seed.length() > 0){
            monkey_cmd = String.format("monkey -p %s -s %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, seed, throttle, count) + _log;
        }
        if (pkg.endsWith("whitelist.txt")){
            monkey_cmd = String.format("monkey --pkg-whitelist-file %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, throttle, count) + _log;

        }
        if (pkg.endsWith("blacklist.txt")){
            monkey_cmd = String.format("monkey --pkg-blacklist-file %s --throttle %s --pct-nav 0 --pct-majornav 0 --ignore-crashes --ignore-security-exceptions --ignore-timeouts --monitor-native-crashes -v -v %s " ,pkg, throttle, count) + _log;

        }
        logUtil.d("", pkg);
        logUtil.d("", monkey_cmd);
        ShellUtils.runShellCommand(monkey_cmd, -10000);
        return logDir + "monkey_error.txt";
    }

    public static String getMonkeyProcess(){
        return getProcessId("com.android.commands.monkey");
    }

    /**
     * 获取应用内存占用大小
     * @param pkg 应用包名
     * @return 内存占用kb, 0 表示未获得相关进程
     */
    public static String getMeminfoTotal(String pkg){
        String res = ShellUtils.runShellCommand(String.format("dumpsys meminfo %s|grep TOTAL:", pkg), 0);
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
        } catch (Exception e) {
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
//            ToastUtils.showLongByHandler(AppContext.getContext(), "未发现用例文件");
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
        return cases_list;
    }


    public static void switchTestEnv(String pkg, String env){
        //todo
    }


    public static void openWifi(){
        ShellUtils.runShellCommand("su root svc wifi enable", 0);
        logUtil.i("", "开启WIFI");
    }

    public static void closeWifi(){
        ShellUtils.runShellCommand("su root svc wifi disable", 0);
        logUtil.i("", "关闭WIFI");
    }

    public static void startToastMonitor(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ShellUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.getLastToast#getToast' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);
            }
        }).start();
        logUtil.d("", "监听toast");
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
        String stepInfo = Step.toString();
        if (stepInfo.length() > 500){
            //执行内容过长，不予打印
        } else {
            logUtil.i("执行: ", stepInfo);
            logUtil.i("时间: ", waitTime.toString());
        }

        int wait_time = 3;
        for (int i = 0; i < Step.length(); i++) {
            try{
                wait_time = waitTime.getInt(i);
            } catch (Exception e){
                logUtil.d("等待时间: ", wait_time + "s");
            }

            if (Step.get(i) instanceof JSONObject) {
                Iterator<String> itr = Step.getJSONObject(i).keys();
                ArrayList<String> keys = new ArrayList<>();
                while (itr.hasNext()) {
                    keys.add(itr.next());
                }
                int nex = 0, index = 0;
                boolean refresh = true;
                String longClick = "";
                String mode = "";
                String checked = "";
                if (keys.contains("checked")) {
                    checked = Step.getJSONObject(i).getString("checked");
                    keys.remove("checked");
                }
                if (keys.contains("nex")) {
                    nex = (int) Step.getJSONObject(i).get("nex");
                    keys.remove("nex");
                }
                if (keys.contains("index")) {
                    index = (int) Step.getJSONObject(i).get("index");
                    keys.remove("index");
                }
                if (keys.contains("refresh")) {
                    refresh = Step.getJSONObject(i).get("refresh").equals("true");
                    keys.remove("refresh");
                }
                if (keys.contains("long")) {

                    longClick = Step.getJSONObject(i).getString("long");
                    keys.remove("long");
                }

                String key = "";
                if (keys.size() <= 1) {
                    key = keys.get(0);
                    Object value = Step.getJSONObject(i).get(key);
                    switch (key) {
                        case "click":
                            if (value instanceof JSONArray) {
                                if (longClick.length() > 0) {
                                    Common.long_click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1), longClick);
                                } else {
                                    Common.click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1));
                                }
                            }
                            break;
                        case "text":
                            Common.click_element(refresh, "text", value.toString(), nex, index, longClick, checked);
                            break;
                        case "ocr":
                            ocrStep(value, index, longClick, refresh);
                            break;
                        case "id":
                            Common.click_element(refresh, "resource-id", value.toString(), nex, index, longClick, checked);
                            break;
                        case "resource-id":
                            Common.click_element(refresh, "resource-id", value.toString(), nex, index, longClick, checked);
                            break;
                        case "content":
                            Common.click_element(refresh, "content-desc", value.toString(), nex, index, longClick, checked);
                            break;
                        case "content-desc":
                            Common.click_element(refresh, "content-desc", value.toString(), nex, index, longClick, checked);
                            break;
                        case "clazz":
                            Common.click_element(refresh, "class", value.toString(), nex, index, longClick, checked);
                            break;
                        case "class":
                            Common.click_element(refresh, "class", value.toString(), nex, index, longClick, checked);
                            break;
                        case "activity":
                            Common.launchActivity(value, mode);
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
                            Common.openNotification(value.toString());
                            break;
                        case "lock":
                            Common.lockScreen();
                            break;
                        case "unlock":
                            Common.unlockScreen(value.toString());
                            break;
                        case "clear":
                            Common.clearAppData(value.toString());
                            break;
                        case "clearRecentApp":
                            Common.clearRecentApp();
                            break;
                        case "monitor_toast":
                            Common.startToastMonitor();
                            break;
                        case "uri":
                            success = Common.startActivityWithUri(AppContext.getContext(), String.valueOf(value));
                            break;
                        case "swipe":
                            Common.swipe(value);
                            break;
                        case "swipe_click":
                            break;
                        case "drag":
                            drag((JSONArray) value);
                            break;
                        case "press":
                            Common.press(value.toString());
                            break;
                        case "wait":
                            SystemClock.sleep((Integer) value * 1000);
                            break;
                        case "shell":
                            ShellUtils.runShellCommand(value.toString(), 0);
                            break;
                        case "dump_xml":
                            get_elements(true, "", "", 0);
                            break;
                        case "input":
                            if (value instanceof JSONObject) {
                                inputText((JSONObject) value);
                            }
                            if (value instanceof String) {
                                ShellUtils.runShellCommand("input text " + value, 0);
                            }
                            break;
                        case "offline":
                            if (ShellUtils.hasRootPermission()) {
                                Common.closeWifi();
                            }
                            break;
                        case "online":
                            if (ShellUtils.hasRootPermission()) {
                                Common.openWifi();
                            }
                            break;
                        case "wifi":
                            if (value.equals("on")) {
                                if (ShellUtils.hasRootPermission()) {
                                    Common.openWifi();
                                }
                            } else {
                                if (ShellUtils.hasRootPermission()) {
                                    Common.closeWifi();
                                }

                            }
                            break;
                        case "if":
                            logUtil.i("", "----------执行if判断-----------");
                            if (value instanceof JSONObject) {
                                try {
                                    resultCheck((JSONObject) value, true);
                                } catch (Exception e) {
                                    logUtil.e("", e);
                                }
                            }
                            logUtil.i("", "-----------------------------");
                            break;

                        case "action_map":
                            String _value = value.toString();
                            JSONArray theAction;
                            try {
                                if (StringUtil.isNumeric(_value)) {
                                    theAction = new JSONArray(HttpUtil.getResp(Common.SERVER_BASE_URL() + "action?id=" + _value));

                                } else {
                                    theAction = new JSONArray(HttpUtil.getResp(Common.SERVER_BASE_URL() + "action?name=" + _value));
                                }
                                execute_step(theAction, new JSONArray());
                            } catch (Exception e) {
                                logUtil.i("", "基础操作库中未找到id/name为'" + _value + "'的操作数据");
                                logUtil.e("", e);
                            }
                            break;

                        case "install":
                            // 通过命令静默安装
                            String targetAppVersion = getVersionName(AppContext.getContext(), TARGET_APP);
                            String apk_url = value.toString();
                            String apkFilePath = Common.downloadResource(AppContext.getContext(), apk_url, apk_url.substring(apk_url.lastIndexOf("/") + 1));
                            SystemClock.sleep(2000);
                            File apkFile = new File(apkFilePath);
                            long fileLongth = -1;
                            // 等待文件下载完成
                            while (fileLongth < apkFile.length()|| fileLongth == 0){
                                fileLongth = apkFile.length();
                                logUtil.d("", "安装包正在下载");
                                SystemClock.sleep(10000);

                            }
                            logUtil.d("", "下载完成：" + apk_url);
                            String cpFilePath = "/data/local/tmp/" + apkFilePath.substring(apkFilePath.lastIndexOf("/")+1);
                            ShellUtils.runShellCommand(String.format("cp -f %s %s", apkFilePath, cpFilePath),0);
                            SystemClock.sleep(2000);
                            String res = ShellUtils.runShellCommand("pm install -r -d " + cpFilePath,0);
                            SystemClock.sleep(30000);
                            logUtil.d("", "开始安装应用:" + res);
                            String targetAppVersion2 = getVersionName(AppContext.getContext(), TARGET_APP);
                            logUtil.d("shell type is", ShellUtils.shellFlag);
                            if (ShellUtils.shellFlag == 2){
                                logUtil.d("", "应用安装完成:" + targetAppVersion2);
                                success = true;
                            } else {
                                if (res.toLowerCase().contains("success") || !targetAppVersion2.equals(targetAppVersion)) {
                                    logUtil.d("", "应用安装完成:" + targetAppVersion2);
                                    success = true;
                                } else {
                                    success = false;
                                }
                            }
                            break;
                    }
                } else {
                    loopEx((JSONObject) Step.get(i));
                }

                SystemClock.sleep(wait_time * 1000);

            }else{
                logUtil.i("对小爱说：", Step.getString(i));

            }

        }
        return success;
    }

    public static void loopEx(JSONObject param) throws JSONException {
        int loop = 10;
        if (!param.isNull("loop")){
            loop = (int) param.get("loop");
        }

        for (int i=0; i < loop; i++) {
            boolean isBreak = false;
            if (!param.isNull("break")){
                JSONObject breakCondition = param.getJSONObject("break");
                isBreak = resultCheck(breakCondition, true);
            }
            if (!isBreak) {
                if (!param.isNull("do")) {
                    JSONArray ex = param.getJSONArray("do");
                    execute_step(ex, new JSONArray().put(1));
                }
            } else{
                break;
            }
        }
    }

    private static void ocrStep(Object value, int index, String longClick, boolean refresh){
        String res_bounds = CvUtils.getTextFromScreenshot(value.toString(), index, refresh);
        if (res_bounds.length() > 0) {
            Rect rt = Common.parseBounds(res_bounds);
            int x = (rt.left + rt.right) / 2;
            int y = (rt.top + rt.bottom) / 2;
            if (longClick.length() > 0) {
                Common.long_click(x, y, longClick);
                logUtil.i(TAG, "完成长按元素text:" + value);
            } else {
                Common.click(x, y);
                logUtil.i(TAG, "完成点击元素text:" + value);
            }
        }
    }


    public static boolean resultCheck(JSONObject check_point, Boolean refresh) {
        try {
            ArrayList<Boolean> result = Checkpoint.muitiCheck(check_point, refresh);
            //sim 卡判断
            if (!check_point.isNull("sim_card")) {
                String sim = check_point.getString("sim_card");
                if (sim.length() > 0) {
                    result.add(String.valueOf(Common.hasSimCard(AppContext.getContext())).equals(sim));
                }
            }
            //nfc 功能判断
            if (!check_point.isNull("nfc")) {
                String nfc = check_point.getString("nfc");
                if (nfc.length() > 0) {
                    result.add(String.valueOf(Common.hasNfc(AppContext.getContext())).equals(nfc));
                }
            }
            //app 安装判断
            if (!check_point.isNull("no_pkg")) {
                String pkg = check_point.getString("no_pkg");
                if (pkg.length() > 0) {
                    String res = ShellUtils.runShellCommand("pm list package | grep " + pkg, 0);
                    logUtil.i("package", res);
                    result.add(res.length() <= 0);
                }
            }
            // device 黑白名单
            if (!check_point.isNull("dev_white_lst")) {
                JSONArray dwl = check_point.getJSONArray("dev_white_lst");
                for (int n = 0; n < dwl.length(); n++) {
                    if (dwl.getString(n).equals(ALIAS)) {
                        result.add(false);
                    }
                }
            }

            if (!check_point.isNull("dev_black_lst")) {
                JSONArray dwl = check_point.getJSONArray("dev_black_lst");
                for (int n = 0; n < dwl.length(); n++) {
                    if (dwl.getString(n).equals(ALIAS)) {
                        result.add(true);
                    }
                }
            }
            // 对依赖app的判断
            if (!check_point.isNull("app")) {
                JSONObject appInfo = check_point.getJSONObject("app");
                String appPkg = appInfo.getString("pkg");
                if (!appInfo.isNull("version_name")) {
                    String appVersionName = appInfo.getString("version_name");
                    System.out.println(Common.getVersionName(AppContext.getContext(), appPkg));
                    if (Common.getVersionName(AppContext.getContext(), appPkg).equals(appVersionName)) {
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
            if (!check_point.isNull("or")) {
                String or = check_point.getString("or");
                if (or.equals("true")) {
                    logUtil.i("", result.toString());
                    if (result.contains(true)) {
                        result = new ArrayList<>();
                        result.add(true);
                    }
//                return result.contains(true);
                }
            }
            // 测试后的必要处理
            if (!check_point.isNull("teardown")) {
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
            if (!check_point.isNull("reverse")) {
//            String reverse = check_point.getString("reverse");
                ret = !ret;

            }

            // 执行操作
            if (!check_point.isNull("true") && ret) {
                JSONArray toDo = check_point.getJSONArray("true");
                JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                if (toDo.length() > 0) {
                    logUtil.i("", "do :");
                    execute_step(toDo, waitTime);
                }
            }

            if (!check_point.isNull("false") && !ret) {
                JSONArray toDo = check_point.getJSONArray("false");
                JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                if (toDo.length() > 0) {
                    logUtil.i("", "do :");
                    execute_step(toDo, waitTime);
                }
            }
            return ret;

        } catch (Exception e){
            return false;
        }

    }

    /**
     * 裁剪图片指定区域
     * @param imageFile
     * @param bounds
     * @return
     */
    public static boolean cropeImage(String imageFile, String bounds){

        try {
            Mat img = Imgcodecs.imread(imageFile);
            Rect rt = parseBounds(bounds);
            Imgcodecs.imwrite(imageFile, img.submat(rt.top, rt.bottom, rt.left,rt.right));
            return true;
        } catch (Exception e){
            logUtil.e("", e);
            return false;
        }
    }

    public static MediaRecorder recordAudio(String outFile, int duration) {

        if (ContextCompat.checkSelfPermission(AppContext.getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            logUtil.i("", "未开启录音权限，无法录音");
            return null;
        }
        logUtil.d("", "录音文件：" + outFile);

        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(outFile);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            logUtil.d("", "-----------------start audio record------------------");
            SystemClock.sleep(duration * 1000);
            stopAudioRecord(mediaRecorder);
        } catch (IOException e) {
            e.printStackTrace();
            logUtil.e("", e);
        }
        return mediaRecorder;
    }

    public static boolean stopAudioRecord(MediaRecorder mediaRecorder) {
        if (mediaRecorder == null){
            return true;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            logUtil.d("", "-----------------stop audio record------------------");
            return true;
        } catch (Exception e){
            logUtil.e("", e);
            return false;
        }
    }


    /**
     * 将系统的音乐，铃声，闹钟音量关闭
     */

    public static void muteSound(){
        try {
            if (!CONFIG().isNull("MUTE") && CONFIG().getString("MUTE").equals("true")){
                setSound(AudioManager.STREAM_MUSIC,0);
                setSound(AudioManager.STREAM_ALARM,0);
                setSound(AudioManager.STREAM_RING,0);
            }
        } catch (JSONException e) {
            logUtil.e("", e);
        }

    }

    /** AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM, AudioManager.STREAM_RING, 11 (手机小爱独立声道)
     * 设置系统的音乐，铃声，闹钟音量
     */

    public static void setSound(int chanel, int value){
        try {
            AudioManager audioManager = (AudioManager) AppContext.getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(chanel, value, AudioManager.FLAG_PLAY_SOUND);
            }
        } catch (Exception e){
            logUtil.e("", e.toString());
        }
    }




    // 其他通用功能
    /**
     * 下载文件
     * @param context
     * @param url
     * @param saveSubPath
     * @return
     */
    public static String downloadResource(Context context, String url, String saveSubPath){
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir(AUTOTEST, saveSubPath);
        FileUtils.deleteFile(CONST.LOGPATH + saveSubPath); // 删除旧文件
        request.setNotificationVisibility(VISIBILITY_VISIBLE);
        request.setTitle(saveSubPath.substring(0, saveSubPath.lastIndexOf(".")));
        request.setDescription("正在下载" + saveSubPath);
//        request.setAllowedOverRoaming(false);
        //获取下载管理器
        DownloadManager downloadManager= (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        downloadManager.enqueue(request);
        return LOGPATH + saveSubPath;
    }


    /**
     * 同步所有用例
     */
    public static void syncTestcases(){
        JSONArray _tables = new JSONArray();
        try {
            JSONObject resp = new JSONObject(HttpUtil.getResp(CONFIG().getString("SERVER_BASE_URL") + "cases?table=" + CONFIG().getString("TABLE") +"&domain_list=1"));
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

    /**
     * 同步某个集合的测试用例
     * @param domain
     */
    public static void syncTestcase(String domain){
        try {
            JSONObject resp1 = new JSONObject(HttpUtil.getResp(CONFIG().getString("SERVER_BASE_URL") + "cases?table=" + CONFIG().getString("TABLE") +"&domain=" + domain));
            JSONArray list_cases;
            list_cases = new JSONObject(resp1.getString("data")).getJSONArray(domain);
            FileUtils.writeCaseJsonFile(domain, list_cases);
        } catch (JSONException e) {
            logUtil.e("syncTestcase", e);
        }

    }

    public static boolean updateTestCases(String testcase){
        try {
            String url = CONFIG().getString("SERVER_BASE_URL") + "cases?table=" + CONFIG().getString("TABLE");
            return !TextUtils.isEmpty(HttpUtil.postResp(url, testcase));
        } catch (JSONException e) {
            logUtil.e("updateTestCases", e);
        }
        return false;
    }

    /**
     * 通过asr服务接口解析音频文件内容
     * @param audioFile
     * @return
     */
    public static String asrService(String audioFile){
//        String url = CONST.SERVER_BASE_URL + "speechrecognize";
//        File af = new File(audioFile);
//        String res = HttpUtil.uploadFile(af, url, af.getName(), "file");
//        String txt;
//        try {
//            txt = new JSONObject(res).getJSONArray("alternative").getJSONObject(0).getString("transcript");
//        } catch (JSONException e) {
//            logUtil.e("", e);
//            txt = res;
//        }
//        logUtil.i("音频识别结果",txt);
        return "TO DO 适配ASR服务";
    }

}

