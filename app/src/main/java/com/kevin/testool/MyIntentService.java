package com.kevin.testool;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kevin.testool.adblib.CmdTools;
import com.kevin.testool.checkpoint.Checkpoint;
import com.kevin.testool.common.Common;
import com.kevin.testool.common.DBService;
import com.kevin.testool.common.HtmlReport;
import com.kevin.testool.common.MemoryManager;
import com.kevin.testool.common.WifiHelper;
import com.kevin.testool.utils.AdbUtils;
import com.kevin.testool.utils.ToastUtils;
import com.kevin.testool.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kevin.testool.common.Common.CONFIG;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private SimpleDateFormat dr = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final String TAG = "MyIntentService";
    private static final String ACTION_RUN = "com.kevin.testool.action.run";
    private static final String ACTION_RUN_ADB = "com.kevin.testool.action.adb";
    private static final String ACTION_DEBUG = "com.kevin.testool.action.debug";
    private static final String ACTION_DEBUG_FINISH = "com.kevin.testool.action.debug.finish";
    private static final String ACTION_DEBUG_TOOL = "com.kevin.testool.action.debug.tool";
    private static final String ACTION_KEEP_PAGE = "com.kevin.testool.action.keep.page";
    private static final String ACTION_TESTCASES_SYC = "com.kevin.testool.action.testcases.syc";
    private static final String DEBUG = "com.kevin.testool.DEBUG";
    //
    private static String REPORT_TITLE = "REPORT_TITLE";
    private static String START = "START";
    private static String CASEID = "CASEID";
    private static String RESULT = "RESULT";
    private static String ERROR = "ERROR";
    public static Boolean continue_flag;
    private static Boolean skip_flag;

    private static String DEVICE;
    private static String PKG = "";
    private static String SN;
    private static String ALIAS;
    private static String APPVER;
    private static String TEST_ENV = "";
    private static String SCREENIMG;

    private static final int NOTIFICATION_ID = 0x1;

    private NotificationCompat.Builder builder;
    //配置
    private static Boolean LOG_FLAG = false;
    private static Boolean SCREENSHOT_FLAG = false;
    private static Boolean ALARM_FLAG = false;
    private static Boolean POST_FLAG = false;


    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //开启前台服务避免被杀
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "my_channel_01";
            CharSequence NAME = "my_channel_name";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NAME, NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text)).build();

            startForeground(NOTIFICATION_ID, notification);
        }else {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(getString(R.string.service_ticker))
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_text))
                    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    public boolean debugActionRun(String debug_case) throws JSONException {
        JSONObject testcase = new JSONObject(debug_case);
        String _caseid = testcase.getString("id");
        JSONObject _case = testcase.getJSONObject("case");
        JSONObject _check = testcase.getJSONObject("check_point");
        JSONArray Step = _case.getJSONArray("step");
        JSONArray waitTime = _case.getJSONArray("wait_time");
        logUtil.i(CASEID, _caseid);
        if (Common.isScreenLocked()){
            Common.unlockScreen("");
            Common.unlockScreen(CONFIG().getString("SCREEN_LOCK_PW"));  //测试手机锁屏密码
        }
        execute_xa(Step, waitTime);
        try {
            return resultCheck(_check, true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startActionRun(String caseName, String case_tag, Boolean offline, int loop) throws JSONException, IOException {
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
//        String app;
//        JSONObject _check;
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
            JSONObject testcase = (JSONObject) testcases.get(i);
            //处理音量避免扰民
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume > 1) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                }
            }
            //检查网络状态,非离线测试无网络跳过测试
            if (offline.equals(false)){
                if (!Common.isNetworkConnected(getApplicationContext())){
                    if (AdbUtils.hasRootPermission()) {
                        Common.openWifi();
                    } else {
                        Common.startActivity("com.android.settings/.wifi.WifiSettings");
                        if (!Checkpoint.checkElementStatus(true, "resource-id", "android:id/checkbox", 0, 0, "checked", "true")){
                            Common.click_element(false, "resource-id", "android:id/checkbox",0,0);
                            Common.goBackHome(1);
                        }
                    }
                    SystemClock.sleep(5000);
//                    int wifi_state_2 = new WifiHelper(MyIntentService.this).checkState();
                    if (!Common.isNetworkConnected(getApplicationContext())){
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
                    for (int j=0; j< resList.length(); j++) {
                        String[] tmpRes = resList.getString(j).split(":");
                        String _appver = tmpRes[0];
                        String _test_env = tmpRes[1];
                        String _device = tmpRes[2];
                        String _res = tmpRes[3];
                        if (_appver.equals(APPVER) && _test_env.equals(TEST_ENV) && _device.equals(DEVICE.replace(" ", "")) && _res.toLowerCase().equals(case_tag.toLowerCase())){
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
                                SCREENIMG = Common.screenShot();
                            }
                            logUtil.i("-----------LOOP-" + (l+1)+"-----------","");
                            result_loops_failed.add(false);
                        }
                        //失败率大于10%， case终止测试
                        if (result_loops_failed.size()/(float)loop > 0.1){
                            result = false;
                            break;
                        }
                        //每执行5次返回桌面一次，每21次清理后台一次
                        if (l % 2 == 0) {
                            Common.goBackHome(1);
                        }
                        if (l % 21 == 0) {
                            Common.clearRecentApp();
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
                Checkpoint.clickPopWindow();
                Common.clearRecentApp();
                retryTime++;
                logUtil.i("", "----------------------retry------------------------");
                if (case_tag.equals("ignore")) {
                    result = action(testcase, retryTime, "ignore");
                } else {
                    result = action(testcase, retryTime, "");
                }
                if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                    SCREENIMG = Common.screenShot();
                }
            }
            String res;
            if (result instanceof Boolean) {
                res = resultDic.getString(result.toString());
                if (Objects.equals(result, false)) {
                    //抓bugreport
                    if (LOG_FLAG) {
                        String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                        Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
                        logUtil.i("", bugreportFile.replace(".txt", ".zip"));
                    }
                    //实现报警
                    String alarm_tag = "alarm message";
                    MyFile.writeFile(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + "alarm.txt", alarm_tag, true);
                    if (ALARM_FLAG) {
                        Checkpoint.sendAlarm(alarm_tag);
                    }
                }
                Common.goBackHome(2);
                if (POST_FLAG) {
                    // TODO 数据上传逻辑，比如更新测试结果等
                }
            logUtil.i(RESULT, result + "");
            }
            if (!continue_flag) {
                break;
            }
        }
    }

    private Object action(JSONObject testcase, int retry, String case_tag) throws JSONException, IOException {
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
            String res = AdbUtils.runShellCommand("ls " + delta.getString("path") + "\n", 0);
            String regex = delta.getString("file_re");
            Pattern p = Pattern.compile(regex);
            Matcher matcher =p.matcher(res);
            int cbt = 0;
            while (matcher.find()) {
                cbt++;
            }
            _check.put("delta",delta.put("cbt", cbt));

        }
        //处理依赖app未安装情况
        if (!_case.isNull("app")){
            app = _case.getString("app");
            // app 名，用于判断app是否安装
            if (!Checkpoint.checkPackageExist(app)){
                _check = new JSONObject();
                _check.put("text", new JSONArray().put("你还没有|安装").put("安装|下载"));
                _case.put("wait_time", new JSONArray().put(3));
                continue_flag = false;
            }
        }
        JSONArray Step;
        // 实现uri测试， 针对case tag 为 uri的测试重新组装测试case
        if (case_tag.equals("uri")){
            Step = new JSONArray().put(new JSONObject().put("uri", _case.getString("uri")));
            _case.put("wait_time", new JSONArray().put(2));
        } else {
            Step = _case.getJSONArray("step");
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
        if (Common.isScreenLocked()){
            Common.unlockScreen(Common.CONFIG().getString("SCREEN_LOCK_PW"));  //手机锁屏密码
        }
//            Common.switchCardFocusEnable("true");
        if (!execute_xa(Step, waitTime)){
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

    public String startDubugTool(Intent intent) {
        String res;
        String TOOL;
        TOOL = intent.getStringExtra("TOOL");
        if (TOOL.equals("activity")){

            SystemClock.sleep(5000);
            res = Common.getActivity();
            return res;
        }
        if (TOOL.equals("dump")){

            SystemClock.sleep(5000);
//            AdbUtils.runShellCommand("uiautomator dump\n", 0);
            AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.github.uiautomator.GetDumpTest' com.github.uiautomator.test/android.support.test.runner.AndroidJUnitRunner\n", 0);
            res = logUtil.readToString(Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml");
            if (res != null){
                return res.replace("<node", "\n\n<node");
            }

        }
        return null;

    }


        @Override
    protected void onHandleIntent(@Nullable Intent intent) {

            try {
                PKG = CONFIG().getJSONObject("APP").getString("package");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String CASE_TAG;
        int LOOP;
        String timeTag;
        ArrayList<String> SELECTED_CASES;
        if (intent != null) {
            Log.i("kevin", "onHandleIntent方法被调用!");
            String action = intent.getAction();
            assert action != null;
            switch (action){
                case ACTION_RUN:
                    boolean isRoot = AdbUtils.hasRootPermission();
                    timeTag = MyFile.creatLogDir();
                    MyFile.createTempFile(CONST.TEMP_FILE, timeTag);
                    Common.killApp("com.github.uiautomator"); //kill uiautomator process (atx app)
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APPVER = Common.getVersionName(this, PKG);
                    TEST_ENV = intent.getStringExtra("TEST_ENV");
                    logUtil.i(REPORT_TITLE, "测试机型：" + DEVICE +"("+ ALIAS + ") SN："+ Common.getSerialno()+" 应用版本：" + APPVER + " 测试环境：" + TEST_ENV);
                    SELECTED_CASES = intent.getStringArrayListExtra("SELECTED_CASES");
                    CASE_TAG = intent.getStringExtra("CASE_TAG");
                    logUtil.i("","case_tag is :"+CASE_TAG);
                    LOOP = intent.getIntExtra("LOOP", 1);

                    System.out.println(SELECTED_CASES);
                    for(int i=0; i < SELECTED_CASES.size(); i++){
                        //判断adb连接状态
                        if (!isRoot && CmdTools.ADB_BREAK){
                            logUtil.i("", "ADB 连接异常，跳过测试");
                            break;
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
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ACTION_RUN_ADB:
                    timeTag = MyFile.creatLogDir();
                    MyFile.createTempFile(CONST.TEMP_FILE, timeTag);
                    // 判断手机存储状态
                    double leftMenory = MemoryManager.getAvailableInternalMemorySize()/1000000000.00;
                    if (leftMenory < 2){
                        //删除报告文件
                        MyFile.RecursionDeleteFile(new File(CONST.REPORT_PATH));
                        //删除照片文件
                        MyFile.RecursionDeleteFile(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator + "Camera" + File.separator));
                    }
                    if (MemoryManager.getAvailableInternalMemorySize()/1000000000.00 < 2){
                        logUtil.i("", "设备可用存储过低，停止测试");
                        ToastUtils.showShort(getApplicationContext(), "设备可用存储过低，停止测试");
                        return;
                    }
                    //同步用例
                    DBService.syncTestcases();
                    Common.goBackHome(0);
//                    Common.killApp("com.github.uiautomator"); //kill uiautomator process (atx app)
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APPVER = Common.getVersionName(getApplicationContext(), PKG);
                    TEST_ENV = intent.getStringExtra("TEST_ENV");
                    if (TEST_ENV == null){
                        TEST_ENV = "production";
                    }
                    logUtil.i(REPORT_TITLE, "测试机型：" + DEVICE +"("+ ALIAS + ") SN："+ Common.getSerialno()+" 小爱版本：" + APPVER + " 测试环境：" + TEST_ENV);
                    String select_cases = intent.getStringExtra("SELECTED_CASES");
                    if (select_cases.toLowerCase().equals("all")){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            select_cases = String.join(",", Common.getCaseList(getApplicationContext()));
                        } else {
                            select_cases = String.valueOf(Common.getCaseList(getApplicationContext())).replace("[","").replace("]", "").replace(" ", "");
                        }
                    }
                    SELECTED_CASES = new ArrayList<String>(Arrays.asList(select_cases.split(",")));
                    CASE_TAG = intent.getStringExtra("CASE_TAG");
                    if (CASE_TAG == null) {
                        CASE_TAG = "";
                    }
                    LOOP = intent.getIntExtra("LOOP", 1);
                    for(int i=0; i < SELECTED_CASES.size(); i++){
                        //判断adb连接状态
                        if (!AdbUtils.hasRootPermission()&&CmdTools.ADB_BREAK){
                            logUtil.i("", "ADB 连接异常，跳过测试");
                            break;
                        }
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
                        MyFile.writeFile(CONST.TEMP_FILE, "::finished", true); //测试结束标志
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ACTION_DEBUG:
                    String debug_case = intent.getStringExtra("DEBUG_CASE");
                    try {
                        debugActionRun(debug_case);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        // log 在调试页面显示
                        String logPath = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "report" + File.separator + logUtil.readTempFile()+ File.separator+"log.txt";
                        String debug_log = logUtil.readToString(logPath);
                        Intent intent_debug = new Intent();
                        intent_debug.setAction(ACTION_DEBUG_FINISH);
                        intent_debug.putExtra("DEBUG_LOG", debug_log);
                        sendBroadcast(intent_debug);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;

                case ACTION_DEBUG_TOOL: //调试工具
                    String res = startDubugTool(intent);
                    Intent intent_debug = new Intent();
                    intent_debug.setAction(ACTION_DEBUG_FINISH);
                    intent_debug.putExtra("DEBUG_LOG", res);
                    sendBroadcast(intent_debug);
                    break;
                case ACTION_KEEP_PAGE: //执行指定页面的monkey测试
                    String pkg = intent.getStringExtra("PKG");
                    String con = intent.getStringExtra("CON");
                    String thro = intent.getStringExtra("THRO");
                    String seed = intent.getStringExtra("SEED");
                    int KEEP_TIME = Integer.valueOf(con)*Integer.valueOf(thro);
                    int waitTime = 2000;
                    if (pkg.contains("/")){
                        Intent intent_monkey = new Intent(this,MonkeyService.class);
                        intent_monkey.putExtra("PKG", pkg);
                        intent_monkey.putExtra("CON", con);
                        intent_monkey.putExtra("THRO", thro);
                        intent_monkey.putExtra("SEED", seed);
                        startService(intent_monkey);
                        Common.startActivity(pkg);
                        while (KEEP_TIME > 0){
                            SystemClock.sleep(waitTime);
                            Common.startActivity(pkg);
                            if (Common.getMonkeyProcess().equals("")){  //判断monkey是否结束
                                break;
                            }
                            KEEP_TIME = KEEP_TIME -waitTime;
                        }
                }else{
                        Intent intent_monkey = new Intent(this,MonkeyService.class);
                        intent_monkey.putExtra("PKG", pkg);
                        intent_monkey.putExtra("CON", con);
                        intent_monkey.putExtra("THRO", thro);
                        intent_monkey.putExtra("SEED", seed);
                        startService(intent_monkey);
//                        Common.runMonkey(pkg,con,thro);
                    }

                    break;
                case ACTION_TESTCASES_SYC:
                    DBService.syncTestcases();
                    //service 内通过handler发起toast
                    Handler mHandler = new Handler(getMainLooper());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showShort(getApplicationContext(), "同步完成");
                        }
                    });
                    break;
                case DEBUG:
                    //todo 调试代码
                    break;

            }
        }
    }

    public boolean execute_xa(JSONArray Step, JSONArray waitTime) throws JSONException {
        boolean success = true;
        logUtil.i("执行: ", Step.toString());
        ArrayList<String> queryList = new ArrayList<>();
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
                            Common.click(((JSONArray) value).getInt(0), ((JSONArray) value).getInt(1));
                        }
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
                        Common.launchApp(getApplicationContext(), value.toString());
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
                            new WifiHelper(getApplicationContext()).closeWifi();
                        } else if (value.toString().equals("on")){
                            if (AdbUtils.hasRootPermission()){
                                Common.openWifi();
                            } else {
                                new WifiHelper(getApplicationContext()).openWifi();
                                Common.click_element(true, "text", "允许", 0, 0);
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
        return success;
    }

    public boolean resultCheck(JSONObject check_point, Boolean refresh) throws IOException, JSONException {
        ArrayList<Boolean> result = Checkpoint.muitiCheck(check_point, refresh);
        //sim 卡判断
        if (!check_point.isNull("sim_card")){
            String sim = check_point.getString("sim_card");
            if (sim.length() > 0){
                result.add(String.valueOf(Common.hasSimCard(getApplicationContext())).equals(sim));
            }
        }
        //nfc 功能判断
        if (!check_point.isNull("nfc")){
            String nfc = check_point.getString("nfc");
            if (nfc.length() > 0){
                result.add(String.valueOf(Common.hasNfc(getApplicationContext())).equals(nfc));
            }
        }
        //app 安装判断
        if (!check_point.isNull("no_pkg")){
            String pkg = check_point.getString("no_pkg");
            if (pkg.length() > 0){
                String res = AdbUtils.runShellCommand("pm list package | grep " +pkg + "\n", 10);
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
                System.out.println(Common.getVersionName(getApplicationContext(), appPkg));
                if (Common.getVersionName(getApplicationContext(), appPkg).equals(appVersionName)){
                    result.add(true);
                } else {
                    result.add(false);
                }
            }
            if (!appInfo.isNull("version_code")) {
                JSONArray verScope = appInfo.getJSONArray("version_code");
                int xaVersionCode = Common.getVersionCode(getApplicationContext(), appPkg);
                result.add(verScope.getInt(0) <= xaVersionCode && xaVersionCode <= verScope.getInt(1));
            }

        }
        //结果取 “或” 关系
        if (!check_point.isNull("or")){
            String or = check_point.getString("or");
            if (or.equals("true")){
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
                execute_xa(teardown, waitTime);
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
                execute_xa(toDo, waitTime);
            }
        }

        if (!check_point.isNull("false") && !ret){
            JSONArray toDo = check_point.getJSONArray("false");
            JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
            if (toDo.length() > 0) {
                logUtil.i("", "do :");
                execute_xa(toDo, waitTime);
            }
        }
        return ret;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

}
