package com.kevin.testool;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.testool.receiver.AlarmReceiver;
import com.kevin.share.CONST;
import com.kevin.share.ErrorDetect;
import com.kevin.share.Checkpoint;
import com.kevin.share.Common;
import com.kevin.testool.common.DBService;
import com.kevin.testool.common.HtmlReport;
import com.kevin.testool.service.MonkeyService;
import com.kevin.testool.service.MyService;
import com.kevin.testool.utils.MemoryManager;
import com.kevin.testool.utils.WifiUtils;
import com.kevin.share.utils.AdbUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.Process.killProcess;
import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.Common.CONFIG;
import static com.kevin.share.Common.getActivity;
import static com.kevin.share.Common.getRomName;
import static com.kevin.share.Common.get_elements;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyIntentService extends IntentService {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private SimpleDateFormat dr = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final String TAG = "MyIntentService";
    private static final String ACTION_RUN = "com.kevin.testool.action.run";
    private static final String ACTION_RUN_RETRY = "com.kevin.testool.action.run.retry";
    private static final String ACTION_RUN_ADB = "com.kevin.testool.action.adb";
    private static final String ACTION_EXECUTE_STEP = "com.kevin.testool.action.execute_step";
    private static final String ACTION_DEBUG = "com.kevin.testool.action.debug";
    private static final String ACTION_DEBUG_FINISH = "com.kevin.testool.action.debug.finish";
    private static final String ACTION_DEBUG_TOOL = "com.kevin.testool.action.debug.tool";
    private static final String ACTION_KEEP_PAGE = "com.kevin.testool.action.keep.page";
    private static final String ACTION_TESTCASES_SYC = "com.kevin.testool.action.testcases.syc";
    private static final String ACTION_STOP = "com.kevin.testool.action.stop";
    private static final String ACTION_POWER_TEST = "com.kevin.testool.action.powertest";
    private static final String ACTION_POWER_TEST_FINISH = "com.kevin.testool.action.powertest.finish";
    private static final String ACTION_WAKEUP_TEST = "com.kevin.testool.action.wakeuptest";
    private static final String ACTION_WAKEUP_TEST_RESULT = "com.kevin.testool.action.wakeuptest.result";
    private static final String DEBUG = "com.kevin.testool.DEBUG";
    //
    private static String TARGET_APP = "";
    private static String REPORT_TITLE = "REPORT_TITLE";
    private static String START = "START";
    private static String CASEID = "CASEID";
    private static String RESULT = "RESULT";
    private static String ERROR = "ERROR";
    public static Boolean continue_flag;
    private static Boolean skip_flag;

    private static String failMsg = "";
    private static String postScreen = "";

    private static String DEVICE;
    private static String SN;
    private static String ALIAS;
    private static String APP_VER;
    private static String TEST_ENV = "production";
    private static String SCREEN_GIF;
    private static String SCREEN_MP4;
    private static String SCREENIMG;
    private static String mp4File = "";
    private static long startRecordTime;
    private static String STEP;
    private static int LAST_WAIT_TIME = 0;
    private static String ROM = getRomName();
    private static int checkType = 1;
    private static JSONObject newCheckPoint = null;  //新检测点，用于覆盖默认检测点
    private static ArrayList<Boolean> checkResults = new ArrayList<>();

    private AlarmReceiver alarmReceiver;

    private static final int NOTIFICATION_ID = 0x1;

    private NotificationCompat.Builder builder;
    //配置
    private static Boolean LOG_FLAG = false;
    private static Boolean SCREENSHOT_FLAG = false;
    private static Boolean SCREENRECORD_FLAG = false;
    private static Boolean MP42GIF_FLAG = false;
    private static Boolean ALARM_FLAG = false;
    private static Boolean POST_FLAG = false;
    private static int RETRY = 2;

    private long sdTime = System.currentTimeMillis();//记录开始时间
    private static String timeTag;

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //
        try {
            TARGET_APP = CONFIG().getString("TARGET_APP");
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    public Object debugActionRun(String debug_case) throws JSONException {
        JSONObject testcase = new JSONObject(debug_case);
        logUtil.d("debug_case", debug_case);
        try {
            return action(testcase, 0, "");
        } catch (IOException e) {
            e.printStackTrace();
            logUtil.d("", e.toString());
            return false;
        }
    }

    /**
     *
     * @param testcases 重试case集合，如：[{"id":"test","case":{"step":[{"text":"天气"}],"wait":[2]},"check_point":{"text":["天气"]}}]
     * @throws JSONException
     * @throws IOException
     */
    public void startRetryRun(final JSONArray testcases) throws JSONException, IOException {
        // 读取配置
        JSONObject CFG = CONFIG();
        if (!CFG.isNull("LOG") && CFG.getString("LOG").equals("true")){
            LOG_FLAG = true;
        }
        if (!CFG.isNull("SCREENSHOT") && CFG.getString("SCREENSHOT").equals("true")){
            SCREENSHOT_FLAG = true;
        }
        if (!CFG.isNull("ALARM_MSG") && CFG.getString("ALARM_MSG").equals("true")){
            ALARM_FLAG = true;
        }
        if (!CFG.isNull("POST_RESULT") && CFG.getString("POST_RESULT").equals("true")){
            POST_FLAG = true;
        }
        if (!CFG.isNull("SCREEN_RECORD") && CFG.getString("SCREEN_RECORD").equals("true")){
            SCREENRECORD_FLAG = true;
        }
        if (!CFG.isNull("MP42GIF") && CFG.getString("MP42GIF").equals("true")){
            MP42GIF_FLAG = true;
        }
        if (!CFG.isNull("RETRY") && CFG.getString("RETRY").length() > 0) {
            RETRY = Integer.valueOf(CFG.getString("RETRY"));
        }
        //result 命名
        JSONObject resultDic = new JSONObject("{\"true\":1,\"false\":0,\"null\":-1, \"break\":-1, \"continue\":-1}");

        continue_flag = true;
//        String test_date = dateFormat2.format(new Date());
//        String test_date = timeTag.substring(0, 10) + " " + timeTag.substring(11).replace("-", ":");
        for (int i = 0; i < testcases.length(); i++){
            int retryTime = 0;
            JSONObject testcase = (JSONObject) testcases.get(i);
            String test_date = testcase.getString("test_date");
            //切换测试环境
            String test_env = testcase.getString("environment");
            Common.switchTestEnv(TARGET_APP, test_env); //切换测试环境
            TEST_ENV = test_env;
            if (i == 0){
                timeTag = FileUtils.creatLogDir(test_date.replace(":", "-").replace(" ", "_"));
                FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE +"("+ ALIAS + ") SN："+ Common.getSerialno()+" App版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
            }
//            logUtil.i("", testcase.toString());
            int origin_id = 0;
            int is_retry = 0;
            if (!testcase.isNull("origin_id")){
                origin_id = testcase.getInt("origin_id");
                if (origin_id > 0){
                    is_retry = 1;
                }
            }
            String case_domain = "";
            if (!testcase.isNull("case_domain")){

                case_domain = testcase.getString("case_domain").toLowerCase();
            }
            //处理音量避免扰民
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVolume > 1) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                }
            }
            //
            Object result = null;
            String jsonStr = testcase.getString("test_case");
            JSONObject mTestcase = new JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}")+1));
            result = action(mTestcase, retryTime, ""); //执行测试
            // 对跳过测试情况的判断
            if (result == "break"){
                JSONObject retryCaseInfo = generateCaseDetail(mTestcase, resultDic.getInt(String.valueOf(result)),testcase.getString("test_tag"), postScreen, test_date, is_retry, case_domain,origin_id);
                Common.postResp(CONST.URL_PORT+"client_test/test_submit", retryCaseInfo.toString());
                break;
            }
            if (result == "continue"){
                JSONObject retryCaseInfo = generateCaseDetail(mTestcase, resultDic.getInt(String.valueOf(result)),testcase.getString("test_tag"), postScreen, test_date, is_retry, case_domain,origin_id);
                Common.postResp(CONST.URL_PORT+"client_test/test_submit", retryCaseInfo.toString());
                continue;
            }
            boolean record_flag = true;
            while (Objects.equals(result, false) & (retryTime < RETRY)) {
                Checkpoint.clickPopWindow(false);
                Common.clearRecentApp();
                retryTime++;
                logUtil.i("", "----------------------retry------------------------");
                if(SCREENRECORD_FLAG && record_flag && retryTime == RETRY) {
                    Common.clearRecentApp();
                    mp4File = Common.screenRecorder(0); //录制屏幕
                    record_flag = false;
                    SCREEN_MP4 = new File(mp4File).getName();
                    SCREEN_GIF = SCREEN_MP4.replace(".mp4", ".gif");
                    startRecordTime = System.currentTimeMillis();
                }
                result = action(mTestcase, retryTime, "");
                if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                    SCREENIMG = Common.screenShot();
                }
            }
            if (!record_flag && Common.killProcess("screenrecord")){

                if (MP42GIF_FLAG) {
//                    AVUtils.mp4ToGif(mp4File, (int) (System.currentTimeMillis() - startRecordTime) / 1000, true);
                    Intent mp42gif = new Intent(this, MyService.class);
                    mp42gif.putExtra("mp4File", mp4File);
                    mp42gif.putExtra("mp4Time", (int) (System.currentTimeMillis() - startRecordTime) / 1000);
                    mp42gif.putExtra("toDeleteMp4", true);
                    startService(mp42gif);
                    logUtil.i("", SCREEN_GIF);
                }
                if (mp4File.contains(".mp4") && new File(mp4File).exists()){
                    logUtil.i("", SCREEN_MP4);
                }
            }

            if (result instanceof Boolean) {
                if (Objects.equals(result, false)) {
                    //抓bugreport
                    if (LOG_FLAG) {
                        String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                        Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
                        logUtil.i("", bugreportFile.replace(".txt", ".zip"));
                    }
                    if (ALARM_FLAG) {
                        //todo 实现报警逻辑
                    }
                }
                Common.goBackHome(1);
                logUtil.i(RESULT, result + "");
            }
            if (POST_FLAG) {
                //上传case信息到服务端
                JSONObject retryCaseInfo = generateCaseDetail(mTestcase, resultDic.getInt(String.valueOf(result)),testcase.getString("test_tag"), postScreen, test_date, is_retry, case_domain, origin_id);
                String resp = Common.postResp(CONST.URL_PORT+"client_test/test_submit", retryCaseInfo.toString());
                logUtil.d("", resp);
            }

            if (!continue_flag) {
                break;
            }
        }
    }


    /**
     * 执行测试集合内的测试case
     * @param caseName  测试集合json文件的文件名
     * @param case_tag 执行测试的用例标签
     * @param offline 是否执行离线功能测试
     * @param loop 执行循环次数
     */

    public void startActionRun(final String caseName, String case_tag, Boolean offline, int loop) throws JSONException, IOException {
        // 读取配置
        JSONObject CFG = CONFIG();
        if (!CFG.isNull("LOG") && CFG.getString("LOG").equals("true")){
            LOG_FLAG = true;
        }
        if (!CFG.isNull("SCREENSHOT") && CFG.getString("SCREENSHOT").equals("true")){
            SCREENSHOT_FLAG = true;
        }
        if (!CFG.isNull("ALARM_MSG") && CFG.getString("ALARM_MSG").equals("true")){
            ALARM_FLAG = true;
        }
        if (!CFG.isNull("POST_RESULT") && CFG.getString("POST_RESULT").equals("true")){
            POST_FLAG = true;
        }
        if (!CFG.isNull("SCREEN_RECORD") && CFG.getString("SCREEN_RECORD").equals("true")){
            SCREENRECORD_FLAG = true;
        }
        if (!CFG.isNull("MP42GIF") && CFG.getString("MP42GIF").equals("true")){
            MP42GIF_FLAG = true;
        }
        if (!CFG.isNull("RETRY") && CFG.getString("RETRY").length() > 0) {
            RETRY = Integer.valueOf(CFG.getString("RETRY"));
        }
        //result 命名
        JSONObject resultDic = new JSONObject("{\"true\":1,\"false\":0,\"null\":-1, \"break\":-1, \"continue\":-1}");

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
        String case_domain = caseName.toLowerCase();

        JSONArray testcases; // 测试用例集合
        JSONObject tmpResult; // 测试结果
        continue_flag = true;
        testcases = new JSONArray(content);
//        String test_date = dateFormat2.format(new Date());
        String test_date = timeTag.substring(0, 10) + " " + timeTag.substring(11).replace("-", ":");
//        Common.clearRecentApp();
        for (int i = 0; i < testcases.length(); i++){
            String resDate = dr.format(new Date());
            int retryTime = 0;

            String domain = "";
            JSONObject testcase = (JSONObject) testcases.get(i);
            JSONObject _case = testcase.getJSONObject("case");
            JSONArray Step = _case.getJSONArray("step");
            if (!_case.isNull("domain")){
                domain = _case.getString("domain");
                if (domain.length()>0){
                    case_domain = domain.toLowerCase();
                }
            }
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
            if (case_tag.toLowerCase().contains("fail")){
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
                        if (_appver.equals(APP_VER) && _test_env.equals(TEST_ENV) && _device.equals((DEVICE+"|"+ ROM).replace(" ", "")) && _res.toLowerCase().equals(case_tag.toLowerCase())){
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
//                            if (LOG_FLAG) {
//                                String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
//                                Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
//                                logUtil.i("", bugreportFile.replace(".txt", ".zip"));
//                                LOG_FLAG = false;
//                            }
                            result = false;
                            break;
                        }
                        //压力测试每执行5次返回桌面一次，每21次清理后台一次
//                        if (l % 2 == 0) {
//                            Common.goBackHome(1);
//                        }
//                        if (l % 21 == 0) {
//                            Common.clearRecentApp();
//                        }

                    }
                }
            }
            // 对跳过测试情况的判断
            if (result == "break"){
                JSONObject caseInfo = generateCaseDetail(testcase, resultDic.getInt(String.valueOf(result)),case_tag, postScreen, test_date, 0, case_domain, 0);
                Common.postJson(CONST.URL_PORT+"client_test/test_submit", caseInfo.toString());
                break;
            }
            if (result == "continue"){
                JSONObject caseInfo = generateCaseDetail(testcase, resultDic.getInt(String.valueOf(result)),case_tag, postScreen, test_date, 0, case_domain, 0);
                Common.postJson(CONST.URL_PORT+"client_test/test_submit", caseInfo.toString());
                continue;
            }
            boolean record_flag = true;
            while (Objects.equals(result, false) & (retryTime < RETRY)) {
                Checkpoint.clickPopWindow(false);  // 对异常弹框处理
//                Common.switchCardFocusEnable("false");
                Common.clearRecentApp();
                retryTime++;
                logUtil.i("", "----------------------retry------------------------");
                if(SCREENRECORD_FLAG && record_flag && retryTime == RETRY) {
                    mp4File = Common.screenRecorder(0); //录制屏幕
                    record_flag = false;
                    SCREEN_MP4 = new File(mp4File).getName();
                    SCREEN_GIF = SCREEN_MP4.replace(".mp4", ".gif");
                    startRecordTime = System.currentTimeMillis();
                }
                if (case_tag.equals("ignore")) {
                    result = action(testcase, retryTime, "ignore");
                } else {
                    result = action(testcase, retryTime, "");
                }
                if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                    SCREENIMG = Common.screenShot();
                }
            }
            if (!record_flag && Common.killProcess("screenrecord")){

                if (MP42GIF_FLAG) {
//                    AVUtils.mp4ToGif(mp4File, (int) (System.currentTimeMillis() - startRecordTime) / 1000, true);
                    Intent mp42gif = new Intent(this, MyService.class);
                    mp42gif.putExtra("mp4File", mp4File);
                    mp42gif.putExtra("mp4Time", (int) (System.currentTimeMillis() - startRecordTime) / 1000);
                    mp42gif.putExtra("toDeleteMp4", true);
                    startService(mp42gif);
                    logUtil.i("", SCREEN_GIF);
                }
                if (mp4File.contains(".mp4") && new File(mp4File).exists()){
                    logUtil.i("", SCREEN_MP4);
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
//                        logUtil.i("", bugreportFile.replace(".txt", ".zip"));
                    }

                    if (ALARM_FLAG) {
                        //todo 实现报警逻辑
                    }
                }
                Common.goBackHome(1);
                logUtil.i(RESULT, result + "");
                if (POST_FLAG) {
                    //上传case信息到服务端
                    JSONObject caseInfo = generateCaseDetail(testcase, resultDic.getInt(String.valueOf(result)),case_tag, postScreen, test_date, 0, case_domain, 0);
                    Common.postJson(CONST.URL_PORT+"client_test/test_submit", caseInfo.toString());
                    //更新测试结果到数据库
                    String resDetail = APP_VER + ":" + TEST_ENV + ":" + DEVICE.replace(" ", "")+"|"+ ROM + ":" + res;
                    tmpResult = DBService.selectCaseResult(caseName, i);
                    JSONArray resLst = new JSONArray();
                    boolean needAdd = true;
                    if (!tmpResult.isNull(resDate)){
                        if (tmpResult.get(resDate) instanceof JSONArray) {
                            resLst = tmpResult.getJSONArray(resDate);
                            for (int j=0; j< resLst.length(); j++) {
                                String[] tmpRes = resLst.getString(j).split(":");
                                String _appver = tmpRes[0];
                                String _test_env = tmpRes[1];
                                String _device = tmpRes[2];
                                String _res = tmpRes[3];
                                //结果不区分设备
                                if (_appver.equals(APP_VER) && _test_env.equals(TEST_ENV) && _res.equals(res)){
                                    needAdd = false;
                                }
                                //重试成功，覆盖结果
                                if (_appver.equals(APP_VER) && _test_env.equals(TEST_ENV) && !_res.equals(res)){
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
                            DBService.updateCaseResult(caseName, updateResult, finalI);
                        }
                    }).start();
                }
            }
            if (!continue_flag) {
                break;
            }
        }
    }

    private JSONObject generateCaseDetail(JSONObject testcase, int test_result, String case_tag, String postScreenFile, String test_date, int is_retry, String case_domain, int origin_id) throws JSONException {
        JSONObject caseDetail = new JSONObject();
        JSONArray data = new JSONArray();
        JSONObject dt = new JSONObject();
        dt.put("test_case", testcase.toString());
        dt.put("test_log",logUtil.test_log);
        dt.put("fail_msg", failMsg);
        dt.put("test_result",test_result );
        dt.put("case_domain", case_domain);
        if (case_tag.contains("monitor") && test_result == 0 && is_retry == 0){
            dt.put("retry", 1);
        } else {
            dt.put("retry", 0);
        }
        if (origin_id == 0){
            dt.put("is_retry", 0);
        } else {
            dt.put("is_retry", is_retry);
        }
        if (is_retry == 1) {
            dt.put("origin_id", origin_id);
        }
        dt.put("img_url", new JSONArray().put(timeTag+"/screenshot/"+ postScreenFile));
        caseDetail.put("data",data.put(dt));
        caseDetail.put("test_date", test_date);
        caseDetail.put("device_name",Common.getDeviceAlias());
        caseDetail.put("device_id", Common.getSerialno());
        caseDetail.put("environment", TEST_ENV);
        if (case_tag.equals("")){
            caseDetail.put("test_tag", "default");
        } else {
            caseDetail.put("test_tag", case_tag);
        }
        String targetApp = "";
        if (!Common.CONFIG().isNull("TARTGET_APP") && Common.CONFIG().getString("TARTGET_APP").length() > 0) {
            targetApp = Common.CONFIG().getString("TARTGET_APP");
        }
        caseDetail.put("apk", targetApp);
        caseDetail.put("apk_version", Common.getVersionName(MyApplication.getContext(), targetApp));
        return caseDetail;
    }


    private Object action(JSONObject testcase, int retry, String case_tag) throws JSONException, IOException {
        //判断adb连接状态
        if (!AdbUtils.isAdbEnable()){
            logUtil.i("action", "ADB server 连接异常，跳过测试");
            return "null";
        }
        checkResults = new ArrayList<>(); //初始化checkResults
        newCheckPoint = null;
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
                    return "null";
                }
            } else {
                return "null";
            }
        } else {
            // case_tag 为 ignore的用例，跳过测试
            if (!_case.isNull("case_tag")){
                _case_tag = _case.getString("case_tag");

                if (_case_tag.contains("ignore")){
                    return "null";
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
        //处理三方app未安装情况, 只适用于smartApp相关功能
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
            waitTime.put(3);
        }
        if (Common.isScreenLocked()){
            Common.unlockScreen(Common.CONFIG().getString("SCREEN_LOCK_PW"));  //手机锁屏密码
        }
        // 开启fc anr 检测
        String fcAnrLogDir = CONST.REPORT_PATH + File.separator + timeTag;
        if (System.currentTimeMillis() - sdTime > 10000){
            ErrorDetect.startDetectCrashAnr(fcAnrLogDir);
            sdTime = System.currentTimeMillis();
        }
        // 界面获取初始化
        FileUtils.deleteFile(DUMP_PATH); // 删除缓存的window_dump.xml
        Common.setGetElementsByAccessibility(AccessibilityHelper.checkAccessibilityEnabled()); // 设置辅助功能获取界面信息
        // 执行测试步骤
        if (!execute_xa(Step, waitTime)){
            return "null";
        }
        Object result = true;
        Boolean refresh = true;
        if (checkType == 0 || checkType == 2) {

            // 检查fc anr
            if (ErrorDetect.isDetectCrash(fcAnrLogDir) || ErrorDetect.isDetectAnr(fcAnrLogDir)) {
                logUtil.i("", " ----------发现FC|ANR---------");
                String savePath = fcAnrLogDir + File.separator + "error_" + dateFormat.format(new Date()) + ".txt";
                FileUtils.copyFile(new File(fcAnrLogDir + File.separator + "error.txt"), savePath);
                logUtil.i("", savePath);
                result = false;
            } else {
                logUtil.i("", "未发现FC/ANR");

            }
        }

        if (checkType == 1 || checkType == 2) {
            // skip condition
            if (!testcase.isNull("skip_condition")) {
                JSONObject skipCondition = testcase.getJSONObject("skip_condition");
                if (skipCondition.length() > 0) {
                    logUtil.i("skip condition:", skipCondition.toString());
                    String scope = skipCondition.getString("scope");
                    if (resultCheck(skipCondition, refresh, System.currentTimeMillis(), false, false)) {
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
//            SystemClock.sleep(1000);
            if (newCheckPoint != null){
                _check = newCheckPoint;
            }
            logUtil.f("check_point", _check.toString()); //打印检测点
            try {
                resultCheck(_check, refresh, System.currentTimeMillis(), false);
                logUtil.d("", checkResults.toString());
                result = !checkResults.contains(false);
            } catch (Exception e) {
                logUtil.i("check_point", e.toString());
                logUtil.e("check_point", e);
                result = "null";
            }
        }
        LAST_WAIT_TIME = 0; // 初始化
        logUtil.i("测试结果", result.equals(true) ? "：Pass": "：Fail");
        return result;

    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // 判断检查类型 0:只检查fc anr ，1: 只检查界面 ， 2: 全部检查

        try {
            checkType = CONFIG().getInt("CHECK_TYPE");
        } catch (Exception e){
            e.printStackTrace();
            checkType = 1;
        }

        String CASE_TAG;
        int LOOP;
//        String timeTag;
        String select_cases;
//        SharedPreferences taskInfo ;
        ArrayList<String> SELECTED_CASES;
        if (intent != null) {
            Log.i("kevin", "onHandleIntent方法被调用!");
            String action = intent.getAction();
            assert action != null;
            switch (action){
                case ACTION_RUN:
                    timeTag = FileUtils.creatLogDir();
                    FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    TEST_ENV = intent.getStringExtra("TEST_ENV");
                    Common.switchTestEnv(TARGET_APP, TEST_ENV); //切换测试环境
                    logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE +"（"+ ALIAS + "） SN："+ Common.getSerialno()+" App版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
                    SELECTED_CASES = intent.getStringArrayListExtra("SELECTED_CASES");


                    CASE_TAG = intent.getStringExtra("CASE_TAG");
                    logUtil.i("","case_tag is :"+CASE_TAG);
                    LOOP = intent.getIntExtra("LOOP", 1);
                    for(int i=0; i < SELECTED_CASES.size(); i++){
                        //判断adb连接状态
                        if (!AdbUtils.isAdbEnable()){
                            logUtil.i("", "ADB 连接异常，跳过测试");
                            break;
                        }
                        logUtil.i(START, SELECTED_CASES.get(i));
                        try {
                            startActionRun(SELECTED_CASES.get(i), CASE_TAG, false, LOOP);
                        } catch (Exception e) {
                            logUtil.e(ERROR, e);
                            ToastUtils.showLongByHandler(getApplicationContext(), e.toString());
                        }

//                        SystemClock.sleep(1000);
                    }
                    logUtil.i("FINISH", "测试完成～");
                    try {
                        FileUtils.writeFile(CONST.TEMP_FILE, timeTag + "::finished", false); //测试结束标志
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        logUtil.e("tmp", e.getMessage());
                    }
                    try {
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ACTION_RUN_ADB:
                    timeTag = FileUtils.creatLogDir();
                    FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                    // 判断手机存储状态
                    double leftMemory = MemoryManager.getAvailableInternalMemorySize()/1000000000.00;
                    if (leftMemory < 2){
                        //删除报告文件
                        FileUtils.RecursionDeleteFile(new File(CONST.REPORT_PATH));
                        //删除相册文件
                        FileUtils.RecursionDeleteFile(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator));
                    }
                    if (MemoryManager.getAvailableInternalMemorySize()/1000000000.00 < 2){
                        logUtil.i("", "设备可用存储过低，停止测试");
                        ToastUtils.showShortByHandler(getApplicationContext(), "设备可用存储过低，停止测试");
                        return;
                    }
                    //同步用例
                    DBService.syncTestcases();
                    Common.goBackHome(0);
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    TEST_ENV = intent.getStringExtra("TEST_ENV");
                    if (TEST_ENV == null){
                        TEST_ENV = "production";
                    }
                    Common.switchTestEnv(TARGET_APP, TEST_ENV); //切换测试环境
                    logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE +"("+ ALIAS + ") SN："+ Common.getSerialno()+" App版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
                    select_cases = intent.getStringExtra("SELECTED_CASES");
                    if (select_cases.toLowerCase().equals("all")){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            select_cases = String.join(",", Common.getCaseList());
                        } else {
                            select_cases = String.valueOf(Common.getCaseList()).replace("[","").replace("]", "").replace(" ", "");
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
                        if (!AdbUtils.isAdbEnable()){
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
                            logUtil.e(ERROR, e);
                        }


                    }
                    logUtil.i("","bugreport.zip");
                    logUtil.i("FINISH", "测试完成～");
                    try {
                        FileUtils.writeFile(CONST.TEMP_FILE, timeTag + "::finished", false); //测试结束标志
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        logUtil.i("tmp", e.getMessage());
                    }
                    try {
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        logUtil.i("report", e.getMessage());
                    }
                    break;
                case ACTION_RUN_RETRY:
                    Common.goBackHome(0);
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    String retryCases = intent.getStringExtra("RETRY_CASES");
                    try {
                        startRetryRun(new JSONArray(retryCases));
                    } catch (Exception e) {
                        e.printStackTrace();
                        logUtil.i("",e.toString());
                    }

                    break;
                case ACTION_EXECUTE_STEP:
                    try {
                        execute_xa(new JSONArray(intent.getStringExtra("EXECUTE_STEP")), new JSONArray().put(2));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case ACTION_DEBUG:
                    String debug_case = intent.getStringExtra("DEBUG_CASE");
                    Object debug_res = "";
                    try {
                        debug_res = debugActionRun(debug_case);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // log 在调试页面显示
                    Intent intent_debug = new Intent();
                    intent_debug.setAction(ACTION_DEBUG_FINISH);
                    intent_debug.putExtra("DEBUG_LOG", logUtil.test_log);
                    intent_debug.putExtra("RESULT", debug_res.toString());
                    sendBroadcast(intent_debug);

                    break;

                case ACTION_DEBUG_TOOL: //调试工具
                    logUtil.d("", "--------------------------------");
                    String res = "";
                    if (intent.hasExtra("TOOL")){
                        if (intent.getStringExtra("TOOL").equals("dump")){
                            FileUtils.deleteFile(DUMP_PATH);
                            get_elements(true, "", "", 0);
                            try {
                                res = FileUtils.readFile(DUMP_PATH);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (intent.getStringExtra("TOOL").equals("activity")){
                            res = getActivity();
                        }
                    }
                    Intent intent_tool = new Intent();
                    intent_tool.setAction(ACTION_DEBUG_FINISH);
                    intent_tool.putExtra("DEBUG_LOG", res);
                    sendBroadcast(intent_tool);
                    break;
                case ACTION_KEEP_PAGE: //执行指定页面的monkey测试
                    String pkg = intent.getStringExtra("PKG");
                    String con = intent.getStringExtra("CON");
                    String thro = intent.getStringExtra("THRO");
                    String seed = intent.getStringExtra("SEED");
                    int KEEP_TIME = Integer.valueOf(con)*Integer.valueOf(thro);
                    int waitTime = 2000;
                    if (pkg.contains("/")){
                        Intent intent_monkey = new Intent(this, MonkeyService.class);
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
                    Common.syncTestcases();
                    sendBroadcast(new Intent(CONST.ACTION_UPDATECASELIST));
                    break;
                case DEBUG:

//                    BatteryManagerUtils.recordCurrentAverage(CONST.LOGPATH + "current.csv", 100, 5);
//                    int c = BatteryManagerUtils.getBatteryCurrentAverage();
//                    logUtil.d("------------------", c+"");
//                    ToastUtils.showLongByHandler(MyApplication.getContext(), "mA");
                    break;

            }
        }
    }

    public boolean execute_xa(JSONArray Step, JSONArray waitTime) throws JSONException {
        boolean success = true;
        logUtil.i("执行: ", Step.toString());
        logUtil.i("时间: ", waitTime.toString());
//        ArrayList<String> queryList = new ArrayList<>();
        int wait_time = waitTime.getInt(waitTime.length() - 1);
        for (int i = 0; i < Step.length(); i++) {
            try{
                wait_time = waitTime.getInt(i);
            } catch (Exception e){
                logUtil.d("等待时间: ", wait_time + "s");
            }

            if (Step.get(i) instanceof JSONObject){
//                String key = Step.getJSONObject(i).keys().next();
                Iterator<String> itr = Step.getJSONObject(i).keys();
                ArrayList<String> keys = new ArrayList<>();
                while (itr.hasNext()){
                    keys.add(itr.next());
                }
                int nex = 0, index = 0;
                boolean refresh = true;
                String longClick = "";
                if (keys.contains("nex")){
                    nex = (int) Step.getJSONObject(i).get("nex");
                    keys.remove("nex");
                }
                if (keys.contains("index")){
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
                            SharedPreferences shuttle = getSharedPreferences("shuttle", MODE_WORLD_WRITEABLE);
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
                        success = Common.startActivityWithUri(getApplicationContext(), String.valueOf(value));
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
                    case "drag":
                        if (value instanceof JSONArray){
                            if (((JSONArray) value).length() == 2){
                                String boundsStart;
                                String boundsEnd;
                                JSONObject elementStart = ((JSONArray) value).getJSONObject(0);
                                int width, height;
                                if (!elementStart.isNull("point")){
                                    JSONArray sPoint = elementStart.getJSONArray("point");
                                    double sx = sPoint.getDouble(0);
                                    double sy = sPoint.getDouble(1);
                                    if (sx <1 && sy < 1){
                                        JSONArray screenSize = Common.getScreenSize();
                                        width = screenSize.getInt(0);
                                        height = screenSize.getInt(1);
                                        boundsStart = String.format("[%s,%s][%s,%s]",(int)(sx * width), (int)(sy * height), (int)(sx*width+1), (int)(sy*height+1));
                                    } else {
                                        boundsStart = String.format("[%s,%s][%s,%s]", sx, sy , sx + 1, sy + 1);
                                    }
                                } else {
                                    boundsStart = Common.get_bounds(refresh, elementStart);
                                }

                                JSONObject elementEnd = ((JSONArray) value).getJSONObject(1);
                                if (!elementEnd.isNull("point")){
                                    JSONArray ePoint = elementEnd.getJSONArray("point");
                                    double ex = ePoint.getDouble(0);
                                    double ey = ePoint.getDouble(1);
                                    logUtil.d("", ex+"," + ey);
                                    if (ex < 1 && ey < 1){
                                        JSONArray screenSize = Common.getScreenSize();
                                        width = screenSize.getInt(0);
                                        height = screenSize.getInt(1);
                                        logUtil.d("wh", width+"," + height);
                                        boundsEnd = String.format("[%s,%s][%s,%s]",(int)(ex * width), (int)(ey*height), (int)(ex*width+1), (int)(ey*height+1));
                                    } else {
                                        boundsEnd = String.format("[%s,%s][%s,%s]", ex, ey, ex + 1, ey + 1);
                                    }
                                } else {
                                    boundsEnd = Common.get_bounds(false, elementEnd);
                                }

                                logUtil.d("", boundsStart);
                                logUtil.d("", boundsEnd);
                                if (!TextUtils.isEmpty(boundsStart) && !TextUtils.isEmpty(boundsEnd) && !TextUtils.isEmpty(boundsEnd)){
                                    JSONArray bs = Common.parseBoundsToPoint(boundsStart);
                                    JSONArray be = Common.parseBoundsToPoint(boundsEnd);
                                    JSONArray dragArgs = new JSONArray().put(bs.getInt(0)).put(bs.getInt(1)).put(be.getInt(0)).put(be.getInt(1));
                                    Common.drag(dragArgs);

                                }
                            } else {
                                logUtil.d("drag", "元素数目不对");
                            }
                        }
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
                    case "dump_xml":
                        get_elements(true, "", "", 0);
                        break;
                    case "input":
                        if (value instanceof JSONObject){
//                            Iterator<String> itrInput = ((JSONObject) value).keys();
//                            ArrayList<String> keysInput = new ArrayList<>();
//                            while (itrInput.hasNext()){
//                                keysInput.add(itrInput.next());
//                            }
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
                        new WifiUtils(getApplicationContext()).closeWifi();
                        break;
                    case "online":
                        if (AdbUtils.hasRootPermission()){
                            Common.openWifi();
                        } else {
                            new WifiUtils(getApplicationContext()).openWifi();
                            Common.click_element(true, "text", "允许", 0, 0);
                        }
                        break;
                    case "check_point":
                        if (value instanceof JSONObject) {
                            newCheckPoint = (JSONObject) value;
                            logUtil.d("", "check_point已更新：" + value.toString());
                        }
                        break;
                    case "if":
                        if (value instanceof JSONObject) {
                            try {
                                resultCheck((JSONObject) value, true, System.currentTimeMillis(), false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    case "check_add":
                        try {
                            boolean ret = resultCheck(Step.getJSONObject(i).getJSONObject("check_add"), refresh, System.currentTimeMillis(), false);
                            checkResults.add(ret);
                        } catch (IOException e) {
                            logUtil.e(TAG, e);
                        }
                        break;
                }
//                logUtil.i(TAG, "action: " + key + ":" + value);
                if (i == waitTime.length()-1){
                    LAST_WAIT_TIME = waitTime.getInt(i) + 3;
                } else {
                    SystemClock.sleep(wait_time * 1000);
                }
            }else{
                String step = Step.getString(i);
                if (step.contains("|")){

                    String[] querys = step.split("\\|");
                    Random random=new Random();
                    int j = random.nextInt(querys.length);
                    step = querys[j];

                }

                logUtil.i("执行步骤：", step);
//                queryList.add(step);
                if (i == waitTime.length()-1){
                    LAST_WAIT_TIME = waitTime.getInt(i);
                    SystemClock.sleep(1000);
                } else {
                    SystemClock.sleep(wait_time * 1000);
                }
            }
//            if (!success){
//                return false;
//            }

        }
        STEP = Step.toString().replace(",", "->");  //用于报警
        return success;
    }

    public boolean resultCheck(JSONObject check_point, Boolean refresh, long rTime, boolean showLog, boolean... addResult) throws IOException, JSONException {
        logUtil.toShow(showLog); // 由于动态检测结果，默认不打印log
        Checkpoint.failInfoDetail = new JSONObject(); // 初始化failInfoDetail
        //refresh重载
        if (!check_point.isNull("refresh")){
            refresh = check_point.getString("refresh").equals("true");
            if (refresh) {
                Common.get_elements(true, "", "", 0);
                refresh = false;
            }
        }

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
                String res = AdbUtils.runShellCommand("pm list package | grep " +pkg + "\n", 10000);
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
            JSONArray dbl = check_point.getJSONArray("dev_black_lst");
            for (int n=0; n< dbl.length(); n++){
                if (dbl.getString(n).equals(ALIAS)){
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
                logUtil.i("", Common.getVersionName(getApplicationContext(), appPkg));
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
            }
        }
        boolean ret = !result.contains(false);
        // 结果取反
        if (!check_point.isNull("reverse")){
//            String reverse = check_point.getString("reverse");
            ret = !ret;

        }
//      动态检测测试结果
        boolean flag = (System.currentTimeMillis() - rTime) < LAST_WAIT_TIME * 1000;
        if (!ret & flag & Checkpoint.isCheckPageElem) {
            SystemClock.sleep(1500);
            ret = resultCheck(check_point, true, rTime, false, addResult);
        } else {
            if(!showLog) {
                // 打印最后一次检测的结果
                ret = resultCheck(check_point, false, rTime, true, addResult);
            }
        }
//      通过showLog的状态判断是否为最终检测，并执行下面的操作逻辑
        if(showLog) {
            // 测试后的必要处理
            if (!check_point.isNull("teardown")) {
                JSONArray teardown = check_point.getJSONArray("teardown");
                JSONArray waitTime = new JSONArray().put(3);
                if (teardown.length() > 0) {
                    logUtil.i("", "teardown:");
                    execute_xa(teardown, waitTime);
                }
            }
            // 对step中"if" 字段的执行结果，执行操作
            if (!check_point.isNull("true") && ret) {
                JSONArray toDo = check_point.getJSONArray("true");
                JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                if (toDo.length() > 0) {
                    logUtil.i("", "条件判断true,执行操作:");
                    execute_xa(toDo, waitTime);
                }
            }

            if (!check_point.isNull("false") && !ret) {
                JSONArray toDo = check_point.getJSONArray("false");
                JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                if (toDo.length() > 0) {
                    logUtil.i("", "条件判断false,执行操作:");
                    execute_xa(toDo, waitTime);
                }
            }
        }
        if (addResult.length > 0 && !addResult[0]) {
            logUtil.d("", "skip add result");
        } else{
            checkResults.add(ret);
        }
        return ret;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (alarmReceiver != null){
            unregisterReceiver(alarmReceiver);
        }
        //杀死进程
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
        while(iter.hasNext()){
            ActivityManager.RunningAppProcessInfo next = iter.next();
            String mProcess = getPackageName() + ":MyIntentService"; //需要在manifest文件内定义android:process=":MyIntentService"
            if(next.processName.equals(mProcess)){
                killProcess(next.pid);
                break;
            }
        }
    }

    public static void uploadVideoFile(String id, String filePath, String appName, String server){
        File videoFile = new File(filePath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", videoFile.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), videoFile))
                .addFormDataPart("id", id)
                .addFormDataPart("appName", appName)
                .build();

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url("http://statisfaction-evaluation-staging.ai.srv/statisfaction/app/uploadVideoFile")
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logUtil.e("", e);
                final String errorMsg = e.getMessage();
                ToastUtils.showLongByHandler(MyApplication.getContext(), "文件上传失败：" + errorMsg);

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.body() != null){
                    String content= response.body().string();
                    logUtil.d("POST_body", content);
                    try {
                        if (!new JSONObject(content).getJSONObject("data").getBoolean("success")) {
                            ToastUtils.showLongByHandler(MyApplication.getContext(), "文件上传，失败请重试！！");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        ToastUtils.showLongByHandler(MyApplication.getContext(), "文件上传失败："+e.getMessage());
                    }
                }

                response.close();

            }
        });
    }

}
