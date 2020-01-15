package com.kevin.testool;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.NotificationCompat;

import com.kevin.testool.aw.Checkpoint2;
import com.kevin.testool.aw.Common2;
import com.kevin.testool.stub.Automator;
import com.kevin.testool.adblib.CmdTools;
import com.kevin.testool.common.DBService;
import com.kevin.testool.common.HtmlReport;
import com.kevin.testool.common.WifiHelper;
import com.kevin.testool.utils.FileUtils;
import com.kevin.testool.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kevin.testool.aw.Common2.CONFIG;
import static com.kevin.testool.stub.Automator.executeShellCommand;

@RunWith(AndroidJUnit4.class)
public class Run {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private static SimpleDateFormat dr = new SimpleDateFormat("yyyyMMdd", Locale.US);
    //
    private static String VOICEASSIST = "com.miui.voiceassist";
    private static String REPORT_TITLE = "REPORT_TITLE";
    private static String START = "START";
    private static String CASEID = "CASEID";
    private static String RESULT = "RESULT";
    private static String ERROR = "ERROR";
    public static Boolean continue_flag;
    private static Boolean skip_flag;

    private static String DEVICE;
    private static String SN;
    private static String ALIAS;
    private static String XIAOAIVER;
    private static String TEST_ENV = "";
    private static String SCREENIMG;
    private static String QUERY;


    private static String MIUI;

    private AlarmReceiver alarmReceiver;

    private static final int NOTIFICATION_ID = 0x1;

    private NotificationCompat.Builder builder;
    //配置
    private static Boolean LOG_FLAG = false;
    private static Boolean SCREENSHOT_FLAG = false;
    private static Boolean ALARM_FLAG = false;
    private static Boolean POST_FLAG = false;

    @Test
    public void runTest(){
        MIUI = "MIUI" + Common2.getRomName();
        DEVICE = Common2.getDeviceName();//.replace(" ", "");
        ALIAS = Common2.getDeviceAlias();
        XIAOAIVER = Common2.getVersionName(VOICEASSIST);
        SharedPreferences testArgs = MyApplication.getContext().getSharedPreferences("testArgs", 0);
        String CASE_NAME = testArgs.getString("CASE_NAME", "");
        String CASE_TAG = testArgs.getString("CASE_TAG", "");
        String TIME_TAG = testArgs.getString("TIME_TAG", "");
        TEST_ENV = testArgs.getString("TEST_ENV", "production");
        int LOOP = testArgs.getInt("LOOP", 1);
        ArrayList<String> SELECTED_CASES = new ArrayList<String>(Arrays.asList(CASE_NAME.split(",")));

        for(int i=0; i < SELECTED_CASES.size(); i++){
            //跳过debug case
            if (SELECTED_CASES.get(i).equals("debug")){
                continue;
            }
            logUtil.i(START, SELECTED_CASES.get(i));
            try {
                startActionRun(SELECTED_CASES.get(i), CASE_TAG, false, LOOP);
            } catch (Exception e) {
                e.printStackTrace();
                logUtil.i(ERROR, e.toString());
            }

//                        SystemClock.sleep(1000);

        }
        logUtil.i("FINISH", "测试完成～");
        try {
            FileUtils.writeFile(CONST.TEMP_FILE, "::finished", true); //测试结束标志
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            HtmlReport.generateReport(CONST.REPORT_PATH + TIME_TAG + File.separator +"log.txt");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }



    public static void startActionRun(final String caseName, String case_tag, Boolean offline, int loop) throws JSONException, IOException {
        // 读取配置
        if (CONFIG().getString("LOG").equals("true")){
            LOG_FLAG = true;
        }
        if (CONFIG().getString("SCREENSHOT").equals("true")){
            SCREENSHOT_FLAG = true;
        }
        if (CONFIG().getString("ALARM_MSG").equals("true")){
            ALARM_FLAG = true;
        }
        if (CONFIG().getString("POST_RESULT").equals("true")){
            POST_FLAG = true;
        }
        //result 命名
        JSONObject resultDic = new JSONObject("{\"true\":\"Pass\",\"false\":\"Fail\",\"null\":\"None\", \"break\":\"None\", \"continue\":\"None\"}");

        File tcJson = new File(CONST.TESTCASES_PATH + caseName + ".json");
        Long fileLengthLong = tcJson.length();
        byte[] fileContent = new byte[fileLengthLong.intValue()];

        try {
            FileInputStream inputStream = new FileInputStream(tcJson);
            inputStream.read(fileContent);
            inputStream.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
        String content = new String(fileContent);

        JSONArray testcases; // 测试用例集合
        JSONObject tmpResult; // 测试结果
        continue_flag = true;
        testcases = new JSONArray(content);
        for (int i = 0; i < testcases.length(); i++){
            String resDate = dr.format(new Date());

            int retryTime = 0;
            // post result 数据
            String setup = "";
            String act = "";
            String feature = "";
            String third_app = "default";
            JSONObject testcase = (JSONObject) testcases.get(i);
            JSONObject _case = testcase.getJSONObject("case");
            JSONArray Query = _case.getJSONArray("query");
            if (!_case.isNull("setup")){
                setup = _case.getString("setup");
            }
            if (!_case.isNull("action")){
                act = _case.getString("action");
            }
            if (!_case.isNull("feature")){
                feature = _case.getString("feature");
            }
            if (!_case.isNull("app")){
                third_app = _case.getString("app");
            }
            //处理音量避免扰民
            AudioManager audioManager = (AudioManager) MyApplication.getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume > 1) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                }
            }
            //检查网络状态,非离线测试无网络跳过测试
            if (offline.equals(false)){
                if (!Common2.isNetworkConnected()){
                    Common2.openWifi();
                    SystemClock.sleep(5000);
//                    int wifi_state_2 = new WifiHelper(MyIntentService.this).checkState();
                    if (!Common2.isNetworkConnected()){
                        logUtil.i("", "无网络，测试skip");
                        return;
                    }
                }
            }
            //
            Object result = null;
            if (case_tag.toLowerCase().equals("fail")){
                //失败用例复测逻辑
                boolean findFailCase = false;
                tmpResult = DBService.selectCaseResult(caseName, i);
                if (!tmpResult.isNull(resDate)){
                    JSONArray resList = tmpResult.getJSONArray(resDate);
                    System.out.println(resList);
                    for (int j=0; j< resList.length(); j++) {
                        String[] tmpRes = resList.getString(j).split(":");
                        String _xiaoaiver = tmpRes[0];
                        String _test_env = tmpRes[1];
                        String _device = tmpRes[2];
                        String _res = tmpRes[3];
                        if (_xiaoaiver.equals(XIAOAIVER) && _test_env.equals(TEST_ENV) && _device.equals((DEVICE+"|"+MIUI).replace(" ", "")) && _res.toLowerCase().equals(case_tag.toLowerCase())){
                            result = action(testcase, retryTime, ""); //执行测试
                            findFailCase = true;
                            break;
                        } else {
                            findFailCase = false;
                        }
                    }
                }
                if (!findFailCase){
                    continue;
                }
            } else {
                ArrayList<Boolean> result_loops_failed = new ArrayList<>();
                for (int l=0; l<loop; l++) { // 压力测试重复测试
                    result = action(testcase, retryTime, case_tag); //执行测试
                    if (l>0) {
                        if (Objects.equals(result, false)) {
                            if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                                SCREENIMG = Common2.screenShot();
                            }
                            logUtil.i("-----------LOOP-" + (l+1)+"-----------","");
                            result_loops_failed.add(false);
                        }
                        //失败率大于10%， case终止测试
                        if (result_loops_failed.size()/(float)loop > 0.1){
//                            if (LOG_FLAG) {
//                                String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
//                                Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
//                                logUtil.i("", bugreportFile.replace(".txt", ".zip"));
//                                LOG_FLAG = false;
//                            }
                            result = false;
                            break;
                        }
                        //每执行5次返回桌面一次，每21次清理后台一次
                        if (l % 2 == 0) {
                            Common2.goBackHome(1);
                        }
                        if (l % 21 == 0) {
                            Common2.clearRecentApp();
                        }

                    }
                }
            }
            // 对跳过测试情况的判断
            if (result == "break"){
                break;
            }
            if (result == "continue"){
                continue;
            }
            while (Objects.equals(result, false) & (retryTime < Integer.valueOf(CONFIG().getString("RETRY")))) {
                Checkpoint2.clickPopWindow();
//                Common.switchCardFocusEnable("false");
                Common2.clearRecentApp();
                retryTime++;
                logUtil.i("", "----------------------retry------------------------");
                if (case_tag.equals("ignore")) {
                    result = action(testcase, retryTime, "ignore");
                } else {
                    result = action(testcase, retryTime, "");
                }
                if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                    SCREENIMG = Common2.screenShot();
                }
            }
            String res;
            if (result instanceof Boolean) {
                res = resultDic.getString(result.toString());
                if (Objects.equals(result, false)) {
                    //抓bugreport
                    if (LOG_FLAG) {
                        String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                        Common2.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
                        logUtil.i("", bugreportFile.replace(".txt", ".zip"));
                    }
                    //实现报警
                    String alarm_tag = String.format("app=xiaoaiApp,device=%s,result=Failed,env=%s,xiaoai=%s,query=%s,screenshot=%s\n", DEVICE.replace(" ", ""), TEST_ENV, XIAOAIVER, QUERY, SCREENIMG);
                    FileUtils.writeFile(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + "alarm.txt", alarm_tag, true);
                    if (ALARM_FLAG) {
//                    String alarm_tag = String.format("app=xiaoaiApp,device=%s,result=Failed,env=%s,xiaoai=%s,query=%s\n", DEVICE, TEST_ENV, XIAOAIVER, QUERY);
//                        Checkpoint2.sendAlarm(alarm_tag);
                    }
                }
                Common2.goBackHome(2);
                if (POST_FLAG) {
                    //更新测试结果到数据库
                    String resDetail = XIAOAIVER + ":" + TEST_ENV + ":" + DEVICE.replace(" ", "")+"|"+MIUI + ":" + res;
                    tmpResult = DBService.selectCaseResult(caseName, i);
                    JSONArray resLst = new JSONArray();
                    boolean needAdd = true;
                    if (!tmpResult.isNull(resDate)){
                        if (tmpResult.get(resDate) instanceof JSONArray) {
                            resLst = tmpResult.getJSONArray(resDate);
                            for (int j=0; j< resLst.length(); j++) {
                                String[] tmpRes = resLst.getString(j).split(":");
                                String _xiaoaiver = tmpRes[0];
                                String _test_env = tmpRes[1];
                                String _device = tmpRes[2];
                                String _res = tmpRes[3];
                                //结果不区分设备
                                if (_xiaoaiver.equals(XIAOAIVER) && _test_env.equals(TEST_ENV) && _res.equals(res)){
                                    needAdd = false;
                                }
                                //重试成功，覆盖结果
                                if (_xiaoaiver.equals(XIAOAIVER) && _test_env.equals(TEST_ENV) && !_res.equals(res)){
                                    resLst.put(j, resDetail);
                                    needAdd = false;
                                }
                            }
                        }
                    }
                    if (needAdd){
                        resLst.put(resDetail);
                    }
                    final JSONObject updateResult = new JSONObject().put(resDate, resLst);
                    final int finalI = i;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            DBService.updateCaseResult(caseName, updateResult, finalI);
                        }
                    }).start();
                }
                logUtil.i(RESULT, result + "");
            }
            if (!continue_flag) {
                break;
            }
        }
    }

    private static Object action(JSONObject testcase, int retry, String case_tag) throws JSONException, IOException {
        //判断adb连接状态
        if (CmdTools.ADB_BREAK){
            logUtil.i("", "ADB 连接异常，跳过测试");
            return null;
        }
        String app;
        JSONObject _check;
        String _case_tag;
        JSONObject _case = testcase.getJSONObject("case");
        if (case_tag.length() > 0){
            if (!_case.isNull("case_tag")){
                _case_tag = _case.getString("case_tag");

                if (_case_tag.contains(case_tag)){
                    logUtil.i("", case_tag);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            // case_tag 为 ignore的用例，跳过测试
            if (!_case.isNull("case_tag")){
                _case_tag = _case.getString("case_tag");

                if (_case_tag.contains("ignore")){
                    return null;
                }
            }
        }
        if (0 == retry){
            logUtil.i(CASEID, testcase.getString("id"));
        }
        _check = testcase.getJSONObject("check_point");
        if (!_check.isNull("delta")){
            JSONObject delta = _check.getJSONObject("delta");
            String res = Automator.executeShellCommand("ls " + delta.getString("path"));
            String regex = delta.getString("file_re");
            Pattern p = Pattern.compile(regex);
            Matcher matcher =p.matcher(res);
            int cbt = 0;
            while (matcher.find()) {
                cbt++;
            }
            _check.put("delta",delta.put("cbt", cbt));

        }
        //处理三方app未安装情况, 只适用于smartApp相关功能
        if (!_case.isNull("app")){
            app = _case.getString("app");
            // app 名，用于判断app是否安装
            if (!Checkpoint2.checkPackageExist(app)){
                _check = new JSONObject();
                _check.put("text", new JSONArray().put("你还没有|安装").put("安装|下载"));
                _case.put("wait_time", new JSONArray().put(3));
                continue_flag = false;
            }
        }
        JSONArray Query;
        // 实现uri测试， 针对case tag 为 uri的测试重新组装测试case
        if (case_tag.equals("uri")){
            Query = new JSONArray().put(new JSONObject().put("uri", _case.getString("uri")));
            _case.put("wait_time", new JSONArray().put(2));
        } else {
            Query = _case.getJSONArray("query");
        }
        JSONArray waitTime;
        // 兼容wait_time类型
        try {
            waitTime = _case.getJSONArray("wait_time");
            JSONArray _waitTime = new JSONArray();
            for (int i=0; i < waitTime.length(); i++){
                _waitTime.put(waitTime.getInt(i) + retry);
            }
            waitTime = _waitTime;
        } catch (Exception e){
            waitTime = new JSONArray();
            waitTime.put(1);
        }
        if (Common2.isScreenLocked()){
            Common2.unlockScreen(CONFIG().getString("SCREEN_LOCK_PW"));  //手机锁屏密码
        }
        if (!execute_step(Query, waitTime)){
            return null;
        }
        Object result;
        Boolean refresh = true;
        // skip condition
        if (!testcase.isNull("skip_condition")){
            JSONObject skipCondition = testcase.getJSONObject("skip_condition");
            if (skipCondition.length() > 0) {
                logUtil.i("skip condition:", skipCondition.toString());
                String scope = skipCondition.getString("scope");
                if (resultCheck(skipCondition, refresh)) {
                    if (scope.equals("all")) {
                        result = "break";
                    } else {
                        result = "continue";
                    }
                    return result;
                } else {
                    refresh = false;
                }
            }
        }

        logUtil.f("check_point", _check.toString()); //打印检测点
        try {
            result = resultCheck(_check, refresh);
        } catch (Exception e){
            logUtil.f("check_point", e.toString());
            result = null;
        }
        return result;
    }


    public static boolean execute_step(JSONArray Query, JSONArray waitTime) throws JSONException {
        boolean success = true;
        logUtil.i("执行: ", Query.toString());
        ArrayList<String> queryList = new ArrayList<>();
        for (int i = 0; i < Query.length(); i++) {
            int wait_time;
            try{
                wait_time = waitTime.getInt(i);
            } catch (Exception e){
                wait_time = 5;
            }

            if (Query.get(i) instanceof JSONObject){
//                String key = Query.getJSONObject(i).keys().next();
                Iterator<String> itr = Query.getJSONObject(i).keys();
                ArrayList<String> keys = new ArrayList<>();
                while (itr.hasNext()){
                    keys.add(itr.next());
                }
                int nex = 0, index = 0;
                if (keys.contains("nex")){
                    nex = (int) Query.getJSONObject(i).get("nex");
                    keys.remove("nex");
                }
                if (keys.contains("index")){
                    index = (int) Query.getJSONObject(i).get("index");
                    keys.remove("index");
                }
                String key = keys.get(0);
                if (key.equals("class")){
                    key = "clazz";
                }
                Object value = Query.getJSONObject(i).get(key);
                switch (key) {
                    case "automator":
                        if (value instanceof JSONObject) {
                            String func = "null";
                            String args = "null";
                            if (!((JSONObject) value).isNull("method")) {
                                func = ((JSONObject) value).getString("method");
                            }
                            if (!((JSONObject) value).isNull("args")){
                                args = ((JSONObject) value).getString("args");

                            }

                            Automator automator = new Automator();
                            try {

                                if (args.equals("null")) {
                                    Method method = automator.getClass().getDeclaredMethod(func);
                                    method.setAccessible(true);
                                    method.invoke(automator);
                                }
                                else {
                                    Method method = automator.getClass().getDeclaredMethod(func, String.class);
                                    method.setAccessible(true);
                                    method.invoke(automator, args);
                                }
                            } catch (Exception NoSuchMethodException){

                                try {
                                    Method toast = automator.getClass().getDeclaredMethod("makeToast", String.class, int.class);
                                    toast.setAccessible(true);
                                    toast.invoke(automator, func + "方法不存在", 1);
                                } catch (java.lang.NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                        break;
                    case "click":
                        if (value instanceof JSONArray) {
                            Common2.click(((JSONArray) value).getDouble(0), ((JSONArray) value).getDouble(1));
                        }
                        break;
                    case "text":
                        Common2.click_element(true, "text", value.toString(), nex, index);
                        break;
                    case "id":
                        Common2.click_element(true, "resource-id", value.toString(), nex, index);
                        break;
                    case "content":
                        Common2.click_element(true, "content-desc", value.toString(), nex, index);
                        break;
                    case "clazz":
                        Common2.click_element(true, "class", value.toString(), nex, index);
                        break;
                    case "activity":
                        Common2.launchActivity(value);
                        break;
                    case "launchApp":
                        Common2.launchApp(MyApplication.getContext(), value.toString());
                        break;
                    case "kill":
                        Common2.killApp(value.toString());
                        break;
                    case "uninstall":
                        Common2.uninstallApp(value.toString());
                        break;
                    case "notification":
                        Common2.openNotification();
                        break;
                    case "negscreen":
                        Common2.openNegscreen();
                        break;
                    case "lock":
                        Common2.lockScreen();
                        break;
                    case "unlock":
                        Common2.unlock(value.toString());
                        break;
                    case "uri":
                        success = Common2.startActivityWithUri(MyApplication.getContext(), String.valueOf(value));
                        break;
                    case "voiceassist":
                        Intent intent_w = new Intent("android.intent.action.ASSIST");
                        intent_w.setPackage(VOICEASSIST);
//                        startService(intent_w);
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
                        Common2.swipe(x1 , y1, x2, y2, step);
                        break;
                    case "press":
                        Common2.pressKey(value.toString());
                        break;
                    case "wait":
                        SystemClock.sleep((Integer)value * 1000);
                        break;
                    case "shell":
                        executeShellCommand(value.toString());
                        break;
                    case "input":
                        if (value instanceof JSONObject){
                            String _key = ((JSONObject) value).getString("key");
                            String _value = ((JSONObject) value).getString("value");
                            String _msg = ((JSONObject) value).getString("msg");
                            String _mode = ((JSONObject) value).getString("mode");
                            Common2.inputText(_key, _value, _msg, _mode);
                        }
                        break;
                    case "offline":
                        new WifiHelper(MyApplication.getContext()).closeWifi();
                        break;
                    case "online":
                        Common2.openWifi();
                        break;

                    case "scanner_src":
                        Common2.postJson("http://10.221.225.163:9999/scanner", String.format("{\"img_src\":\"%s\"}", value.toString()));
                        break;
                    case "check_point":
                        if (value instanceof JSONObject) {
                            try {
                                resultCheck((JSONObject) value, true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
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
                SystemClock.sleep(wait_time *1000);
            }else{

                SystemClock.sleep(wait_time *1000);
            }

        }
        QUERY = queryList.toString(); //用于报警
        return success;
    }

    public static boolean resultCheck(JSONObject check_point, Boolean refresh) throws IOException, JSONException {
        ArrayList<Boolean> result = Checkpoint2.muitiCheck(check_point, refresh);
        //sim 卡判断
        if (!check_point.isNull("sim_card")){
            String sim = check_point.getString("sim_card");
            if (sim.length() > 0){
                result.add(String.valueOf(Common2.hasSimCard()).equals(sim));
            }
        }
        //nfc 功能判断
        if (!check_point.isNull("nfc")){
            String nfc = check_point.getString("nfc");
            if (nfc.length() > 0){
                result.add(String.valueOf(Common2.hasNfc()).equals(nfc));
            }
        }
        //app 安装判断
        if (!check_point.isNull("no_pkg")){
            String pkg = check_point.getString("no_pkg");
            if (pkg.length() > 0){
                String res = Common2.executeShellCommand("pm list package | grep " +pkg);
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
                System.out.println(Common2.getVersionName(appPkg));
                if (Common2.getVersionName(appPkg).equals(appVersionName)){
                    result.add(true);
                } else {
                    result.add(false);
                }
            }
            if (!appInfo.isNull("version_code")) {
                JSONArray verScope = appInfo.getJSONArray("version_code");
                int xaVersionCode = Common2.getVersionCode(MyApplication.getContext(), appPkg);
                result.add(verScope.getInt(0) <= xaVersionCode && xaVersionCode <= verScope.getInt(1));
            }

        }
        //结果取 “或” 关系
        if (!check_point.isNull("or")){
            String or = check_point.getString("or");
            if (or.equals("true")){
                logUtil.i("----------------", result.toString());
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


}
