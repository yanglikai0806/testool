package com.kevin.testool;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.kevin.share.AppContext;
import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.share.utils.BatteryManagerUtils;
import com.kevin.share.utils.CvUtils;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.MemoryManager;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.StringUtil;
import com.kevin.share.CONST;
import com.kevin.testool.activity.MainActivity;
import com.kevin.share.ErrorDetect;
import com.kevin.share.Checkpoint;
import com.kevin.share.Common;
import com.kevin.testool.common.HtmlReport;
import com.kevin.testool.service.MyService;
import com.kevin.testool.utils.MyWebSocketClient;
import com.kevin.testool.utils.MyWebSocketServer;
import com.kevin.testool.utils.WifiUtils;
import com.kevin.share.utils.ShellUtils;
import com.kevin.testool.utils.DateTimeUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static android.os.Process.killProcess;
import static com.kevin.share.CONST.ACTION_DEBUG;
import static com.kevin.share.CONST.ACTION_RUN_TASK;
import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.CONST.LOGPATH;
import static com.kevin.share.CONST.REPORT_PATH;
import static com.kevin.share.CONST.TARGET_APP;
import static com.kevin.share.CONST.TEMP_FILE;
import static com.kevin.share.CONST.SERVER_BASE_URL;
import static com.kevin.share.Common.CONFIG;
import static com.kevin.share.Common.cropeImage;
import static com.kevin.share.Common.drag;
import static com.kevin.share.Common.getActivity;
import static com.kevin.share.utils.HttpUtil.getResp;
import static com.kevin.share.Common.get_bounds;
import static com.kevin.share.Common.get_element_text_value;
import static com.kevin.share.Common.get_elements;
import static com.kevin.share.Common.inputText;
import static com.kevin.share.Common.parseBounds;
import static com.kevin.share.Common.parseRect;
import static com.kevin.share.utils.StringUtil.isNumeric;
import static com.kevin.share.utils.CvUtils.getMatchImgByTemplate;
import static com.kevin.share.utils.CvUtils.getMatchImgByFeature;
import static com.kevin.share.utils.CvUtils.getMatchImgFromVideo;
import static com.kevin.share.utils.CvUtils.getMatchTextFromVideo;
import static com.kevin.share.utils.CvUtils.isSameImage;

/**
 * 执行服务：执行器 和 检查器
 */
public class MyIntentService extends IntentService {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private SimpleDateFormat dr = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static final String TAG = "MyIntentService";
    private static final String ACTION_RUN = "com.kevin.testool.action.run";
    private static final String ACTION_RUN_ADB = "com.kevin.testool.action.adb";
    private static final String ACTION_RUN_REMOTE = "com.kevin.testool.action.remote";
    private static final String ACTION_EXECUTE_STEP = "com.kevin.testool.action.execute_step";

    private static final String ACTION_DEBUG_FINISH = "com.kevin.testool.action.debug.finish";
    private static final String ACTION_DEBUG_TOOL = "com.kevin.testool.action.debug.tool";
    private static final String ACTION_TESTCASES_SYC = "com.kevin.testool.action.testcases.syc";
    private static final String ACTION_AUDIO_RECORD= "com.kevin.testool.action.audio_record";
    private static final String DEBUG = "com.kevin.testool.DEBUG";
    //
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
    private static String SCREENIMG;
    private static String SCREEN_GIF;
    private static String SCREEN_MP4;
    private static String mp4File = "";
    private static long startRecordTime;
    private static int CASE_ID_NUM = 0;
    private static int LAST_WAIT_TIME = 0;
    private static int checkType = 2; // 0: 只检查fc/anr; 1: 只检查判断点； 2：0&1
    private static JSONObject newCheckPoint = null;  //新检测点，用于覆盖默认检测点
    public static ArrayList<Boolean> checkResults = new ArrayList<>();

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

    private static String audioFilePath = CONST.LOGPATH + "test_audio.wav";
    private static String videoFilePath = CONST.LOGPATH + "test_video.mp4";
    private static String response = ""; // post/get 请求的返回结果

    private static JSONObject VAR_SET;
    private static JSONObject VAR_CHECK;
    private static JSONObject VAR_CODE;

    private static int musicVal = 0;
    private static int aiVal = 0;

    private static MediaRecorder mediaRecorder;

    public MyIntentService() {
        super("MyIntentService");
    }
    static{
        System.loadLibrary("opencv_java4");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        if (!OpenCVLoader.initDebug()) {
            logUtil.d(getClass().getName(), "Internal OpenCV library not found. Using OpenCV manger for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            logUtil.d(getClass().getName(), "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


        //
        try {
            TARGET_APP = CONFIG().getString("TARGET_APP");
            DEVICE = Common.getDeviceName();
        } catch (Exception e) {
            logUtil.e("", e);
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
//        logUtil.d("调试用例", debug_case);
        timeTag = FileUtils.creatLogDir("debug");
        FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
        SystemClock.sleep(100);
        logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE + "SN：" + Common.getSerialno() + " App版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
        logUtil.i(START, "DEBUG");
        try {
            return action(testcase, 0, "");
        } catch (Exception e) {
            logUtil.e("", e);
            logUtil.d("", e.toString());
            return false;
        }
    }

    /**
     * 任务执行
     * @param testcases case集合，如：[{"id":"test","case":{"step":[{"text":"相机"}],"wait":[2]},"check_point":{"text":["天气"]}}]
     * @param flag 标记测试类型，默认 0： 为 监控任务； 1： 为测试任务
     * @throws JSONException
     * @throws IOException
     */
    public void startTaskRun(JSONArray testcases, int flag, int taskId) throws JSONException, IOException {
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
            RETRY = Integer.parseInt(CFG.getString("RETRY"));
        }
        //result 命名
        JSONObject resultDic = new JSONObject("{\"true\":1,\"false\":0,\"null\":-1, \"break\":-1, \"continue\":-1}");

        continue_flag = true;
        logUtil.d("", "任务类型 0:监控, 1:测试。当前任务为:" +flag);
        if (flag == 1){
            // 从缓存中读取未执行的测试用例
            String _testcases = SPUtils.getString(SPUtils.TASK_CASE, taskId + "");
            testcases = new JSONArray(_testcases);
            if (testcases.length() > 0) {
                Common.goBackHome(0);
                logUtil.d("***********从缓存中读取case**********", testcases.length() + "条");
            } else {
                // 用例执行完成,取消进程守护
                ShellUtils.executeSocketShell("###testool", 0);
                return;
            }

        }
        JSONArray caseDetailInfoCach = new JSONArray();
        for (int i = 0; i < testcases.length(); i++){
            int retryTime = 0;
            JSONObject testcase = (JSONObject) testcases.get(i);
            String test_date = testcase.getString("test_date");
            if (i == 0) {
                //切换测试环境
                String test_env = testcase.optString("test_env");
                Common.switchTestEnv(TARGET_APP, test_env); //切换测试环境 todo

                TEST_ENV = test_env;
                timeTag = FileUtils.creatLogDir(test_date.replace(":", "-").replace(" ", "_"));
                FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE + "(" + ALIAS + ") SN：" + Common.getSerialno() + " App版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
                if (flag == 0) {
                    logUtil.i(START, "MONITOR_TASK");
                } else {
                    logUtil.i(START, "TEST_TASK");
                }
            }
            int origin_id = 0;
            int is_retry = 0;
            if (!testcase.isNull("origin_id")){
                origin_id = testcase.getInt("origin_id");
                if (origin_id > 0){
                    is_retry = 1;
                }
            }
            String case_domain = testcase.optString("domain", "");

            //处理音量避免扰民
            Common.muteSound();
            //
            Object result = null;
            String jsonStr = testcase.getString("test_case");
            JSONObject mTestcase = new JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}")+1));

            // 执行前更新测试用例缓存，避免测试中特定步骤导致工具被杀后陷入循环
            JSONArray testcasesCach;
            if (flag ==1){
                testcasesCach = new JSONArray(SPUtils.getString(SPUtils.TASK_CASE, taskId + ""));
                if (testcasesCach.length() > 0){
                    testcasesCach.remove(0);
                }
                SPUtils.putString(SPUtils.TASK_CASE, taskId + "", testcasesCach.toString());
//                logUtil.d("缓存用例数", SPUtils.getString(SPUtils.TASK_CASE, taskId+""));
            }
            //执行测试
            result = action(mTestcase, retryTime, "", 1);
            //测试结果
            boolean record_flag = true;
            while (Objects.equals(result, false)) {
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
                result = action(mTestcase, retryTime, "", 1);
                if (SCREENSHOT_FLAG & Objects.equals(result, false)) {
                    SCREENIMG = Common.screenShot();
                }
                if (retryTime == RETRY){
                    break;
                }
            }
            if (!record_flag && Common.killProcess("screenrecord")){
                // 录屏文件转化为gif格式
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
                }
                Common.goBackHome(1);
                logUtil.i(RESULT, result + "");
            }
            if (POST_FLAG) {
                //上传截图
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        File mFile = new File(REPORT_PATH + timeTag + File.separator + CONST.SCREENSHOT + File.separator + postScreen);
//                        if (mFile.exists()) {
//                            HttpUtil.uploadFile(mFile, CONST.SERVER_BASE_URL + "api/upload", Common.getDeviceId() + "_" + mFile.getName(), "img");
//                        }
//                    }
//                }).start();
                //上传case执行结果到服务端
                String test_tag = testcase.optString("test_tag");
                JSONObject caseDetailInfo = generateCaseDetail(mTestcase, resultDic.getInt(String.valueOf(result)),test_tag, postScreen, test_date, is_retry, case_domain, origin_id, taskId);
                String resp = HttpUtil.postResp(CONST.SERVER_BASE_URL+CONST.TEST_RESULT_URL[flag], caseDetailInfo.toString());
                logUtil.d("上传测试结果", resp);
                //测试结果上传失败时，对测试结果进行缓存
                if (!resp.contains("测试结果已写入")){
                    caseDetailInfoCach.put(caseDetailInfo);
                }
            }

            // 更新测试用例缓存
//            if (flag ==1){
//                JSONArray testcasesCach = new JSONArray(SPUtils.getString(SPUtils.TASK_CASE, taskId + ""));
//                if (testcasesCach.length() > 0){
//                    testcasesCach.remove(0);
//                }
//                SPUtils.putString(SPUtils.TASK_CASE, taskId + "", testcasesCach.toString());
////                logUtil.d("缓存用例数", SPUtils.getString(SPUtils.TASK_CASE, taskId+""));
//            }

            // 对跳过测试情况的判断
            if (result.equals("break")){
                break;
            }
            if (result.equals("continue")){
                continue;
            }

            if (!continue_flag) {
                break;
            }
        }
        // 测试完成,检查是否有未上传的结果缓存
        JSONArray caseDetailInfoCach2 = new JSONArray();
        for (int i=0; i< caseDetailInfoCach.length(); i++){
            if (!HttpUtil.postResp(CONST.SERVER_BASE_URL +CONST.TEST_RESULT_URL[flag], caseDetailInfoCach.get(i).toString()).contains("测试结果已写入")){
                caseDetailInfoCach2.put(caseDetailInfoCach.get(i));
            }

        }
        if (caseDetailInfoCach2.length() > 0){
            SPUtils.putString(SPUtils.TASK_RESULT, "result", caseDetailInfoCach2.toString()); //缓存
        }
        // 上传error异常
        if (checkType == 0 || checkType == 2){
            String uploadLogFolder = CONST.REPORT_PATH + timeTag + File.separator + APP_VER;
            if (ErrorDetect.isDetectCrash(uploadLogFolder)){
                logUtil.i("", "检测到 anr/crash 异常 ");
//                String zipFilePath = uploadLogFolder + ".zip";
//                ErrorDetect.uploadErrorLog(uploadLogFolder, zipFilePath);
//                FileUtils.deleteFile(uploadLogFolder + File.separator + ErrorDetect.ERROR_FILE);
            }
        }

    }


    /**
     * 本地执行
     * @param caseName  测试集合json文件的文件名
     * @param case_tag 执行测试的用例标签
     * @param offline 是否执行离线功能测试
     * @param loop 执行循环次数
     */

    public void startLocalRun(final String caseName, String case_tag, Boolean offline, int loop) throws JSONException, IOException {
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
            RETRY = Integer.parseInt(CFG.getString("RETRY"));
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
            // post result 数据
            String domain = "";
            JSONObject testcase = (JSONObject) testcases.get(i);
            JSONObject _case = testcase.getJSONObject("case");
            if (!_case.isNull("domain")){
                domain = _case.getString("domain");
                if (domain.length()>0){
                    case_domain = domain.toLowerCase();
                }
            }
            //处理音量避免扰民
            Common.muteSound();
            //检查网络状态,非离线测试无网络跳过测试
            if (offline.equals(false)){
                if (!Common.isNetworkConnected(getApplicationContext())){
                    if (ShellUtils.hasRootPermission()) {
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

            Object result = null;

            if (loop > 1) {
                ArrayList<Boolean> result_loops_failed = new ArrayList<>();
                int passNum = 0;
                for (int l=0; l<loop; l++) { // 多伦测试
                    result = action(testcase, retryTime, case_tag); //执行测试
                    if (!Objects.equals(result, true)) {
                        if (SCREENSHOT_FLAG) {
                            SCREENIMG = Common.screenShot();
                        }
                        result_loops_failed.add(false);
                    } else {
                        passNum += 1;
                    }
                    logUtil.i("-----------LOOP-" + (l+1)+"-----------","");
                }
                result = result_loops_failed.size() == 0;
                logUtil.i("", String.format("多伦执行成功率：%s/%s = %s", passNum, loop, Math.round(passNum * 100.00 / loop)) + "%");
            } else {
                result = action(testcase, retryTime, case_tag); //执行测试
            }

            // 对跳过测试情况的判断
            if (result == "break"){
                JSONObject caseInfo = generateCaseDetail(testcase, resultDic.getInt(String.valueOf(result)),case_tag, postScreen, test_date, 0, case_domain, 0, 0);
                HttpUtil.postJson(CONST.SERVER_BASE_URL+"test_submit", caseInfo.toString());
                break;
            }
            if (result == "continue"){
                JSONObject caseInfo = generateCaseDetail(testcase, resultDic.getInt(String.valueOf(result)),case_tag, postScreen, test_date, 0, case_domain, 0, 0);
                HttpUtil.postJson(CONST.SERVER_BASE_URL+"test_submit", caseInfo.toString());
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
//            String res;
            if (result instanceof Boolean) {
//                res = resultDic.getString(result.toString());
                if (Objects.equals(result, false)) {
                    //抓bugreport
                    if (LOG_FLAG) {
                        String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                        Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
//                        logUtil.i("", bugreportFile.replace(".txt", ".zip"));
                    }
                }
                Common.goBackHome(1);
                logUtil.i(RESULT, result + "");
            }
            //
            if (!continue_flag) {
                break;
            }
        }
    }

    private JSONObject generateCaseDetail(JSONObject testcase, int test_result, String case_tag, String postScreenFile, String test_date, int is_retry, String case_domain, int origin_id, int task_id) throws JSONException {
        JSONObject caseDetail = new JSONObject();
        JSONArray data = new JSONArray();
        JSONObject dt = new JSONObject();
        dt.put("test_case", testcase.toString());
        dt.put("test_log",logUtil.test_log);
        dt.put("fail_msg", failMsg);
        dt.put("test_result",test_result );
        dt.put("case_domain", case_domain);
        dt.put("case_id", testcase.getInt("id"));
        // 监控case重试字段设置
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
//        dt.put("img_url", new JSONArray().put(timeTag+"/screenshot/"+ postScreenFile));
        if (TextUtils.isEmpty(postScreenFile)){
            dt.put("img_url", new JSONArray().put(""));
        } else {
            dt.put("img_url", new JSONArray().put(SN + "_"+ postScreenFile));
        }
        caseDetail.put("data",data.put(dt));
        caseDetail.put("test_date", test_date);
        caseDetail.put("device_name",Common.getDeviceAlias());
        caseDetail.put("device_id", Common.getDeviceId());
        caseDetail.put("test_env", TEST_ENV);
        if (task_id > 0){
            caseDetail.put("task_id", task_id);
        }
        if (case_tag.equals("")){
            caseDetail.put("test_tag", "default");
        } else {
            caseDetail.put("test_tag", case_tag);
        }
        String targetApp = "";
        if (!Common.CONFIG().isNull("TARGET_APP") && Common.CONFIG().getString("TARGET_APP").length() > 0) {
            targetApp = Common.CONFIG().getString("TARGET_APP");
        }
        caseDetail.put("apk_version", Common.getVersionName(MyApplication.getContext(), targetApp));

        caseDetail.put("apk", targetApp);
        return caseDetail;
    }

    /**
     * 执行器实现
     * @param testcase a JsonObject like {"id":123, "case":{"step":["你好"], "wait_time":[1]}, "check_point":{}, "skip_condition":{}}
     * @param retry 是否重试 0 : no, 1: yes
     * @param case_tag case tag
     * @param flag 执行类型标记 0：常规执行；1：流畅执行（"skip_condition"条件中scope：all被屏蔽，相当于single）
     * @return
     * @throws JSONException
     */

    private Object action(JSONObject testcase, int retry, String case_tag, int... flag) throws JSONException{
        ShellUtils.runShellCommand("logcat -c", 0); // 清空系统日志
        postScreen = ""; // 恢复初始化
        int actionFlag = 0;
        if (flag.length > 0){
            actionFlag = flag[0];
        }
        //判断adb连接状态
        if (!ShellUtils.isShellEnable()){
            logUtil.i("action", "shell 不可用，跳过测试");
            return "null";
        }

        checkResults = new ArrayList<>(); //初始化checkResults
        newCheckPoint = null;
        String app;
        JSONObject _check = null;
        String _case_tag;
        CASE_ID_NUM = testcase.optInt("id", 0);
        JSONObject _case = testcase.optJSONObject("case");
        JSONArray Step = null;
        JSONArray waitTime = null;
        if ( _case != null ) {
            if (case_tag.length() > 0) {
                if (!_case.isNull("case_tag")) {
                    _case_tag = _case.getString("case_tag");

                    if (_case_tag.contains(case_tag)) {
                        logUtil.i("", case_tag);
                    } else {
                        return "null";
                    }
                } else {
                    return "null";
                }
            } else {
                // case_tag 为 ignore的用例，跳过测试
                if (!_case.isNull("case_tag")) {
                    _case_tag = _case.getString("case_tag");

                    if (_case_tag.contains("ignore")) {
                        return "null";
                    }
                }
            }
            if (0 == retry) {
                logUtil.i(CASEID, CASE_ID_NUM + "");
            }
            _check = testcase.optJSONObject("check_point");
            if (_check != null) {
                if (!_check.isNull("delta")) {
                    JSONObject delta = _check.getJSONObject("delta");
                    String res = ShellUtils.runShellCommand("ls " + delta.getString("path") + "\n", 0);
                    String regex = delta.getString("file_re");
                    Pattern p = Pattern.compile(regex);
                    Matcher matcher = p.matcher(res);
                    int cbt = 0;
                    while (matcher.find()) {
                        cbt++;
                    }
                    _check.put("delta", delta.put("cbt", cbt));

                }
            }

            Step = _case.optJSONArray("step");

            // 兼容wait_time类型
            try {
                waitTime = _case.getJSONArray("wait_time");
                JSONArray _waitTime = new JSONArray();
                for (int i = 0; i < waitTime.length(); i++) {
                    _waitTime.put(waitTime.getInt(i) + retry);
                }
                waitTime = _waitTime;
            } catch (Exception e) {
                waitTime = new JSONArray();
                waitTime.put(3);
            }
            if (Common.isScreenLocked()) {
                Common.unlockScreen(Common.CONFIG().getString("SCREEN_LOCK_PW"));  //手机锁屏密码
            }
        } else {
            logUtil.d("", "case 内容为空");
        }
            // 开启fc anr 检测
            String fcAnrLogDir = CONST.REPORT_PATH + timeTag + File.separator + APP_VER;
            long _monitor_gap = System.currentTimeMillis() - sdTime;
            if ((checkType == 0 || checkType == 2) &&  (_monitor_gap > ErrorDetect.USER_WAIT_TIME)) {
                Common.killProcess("com.android.commands.monkey"); // 先结束monkey脚本
                ErrorDetect.startDetectCrashAnr(fcAnrLogDir); // 执行monkey脚本进行异常检测
                sdTime = System.currentTimeMillis();
            }
            // 界面获取初始化
            FileUtils.deleteFile(DUMP_PATH); // 删除缓存的window_dump.xml
            Common.setGetElementsByAccessibility(AccessibilityHelper.checkAccessibilityEnabled()); // 设置辅助功能获取界面信息
            // 执行测试步骤
            if (!execute_step(Step, waitTime)) {
                return "null";
            }

            Object result = true;
            Boolean refresh = true;

        if (checkType == 1 || checkType == 2) {
            // skip condition
            if (!testcase.isNull("skip_condition")) {
                JSONObject skipCondition = testcase.getJSONObject("skip_condition");
                if (skipCondition.length() > 0) {
                    logUtil.i("skip condition:", skipCondition.toString());
                    String scope = skipCondition.getString("scope");
                    if (resultCheck(skipCondition, refresh, System.currentTimeMillis(), false, false)) {
                        if (scope.equals("all")) {
                            if (actionFlag == 1){
                                result = "continue";
                            } else {
                                result = "break";
                            }
                        } else {
                            result = "continue";
                        }
                        return result;
                    } else {
                        refresh = false;
                    }
                }
            }
            if (newCheckPoint != null){
                _check = newCheckPoint;
            }
            String checkPointInfo = _check + "";
            if (checkPointInfo.length() < 500){
                //文本过长，不予打印
                logUtil.f("check_point", checkPointInfo); //打印检测点

            }
            try {
                Checkpoint.failInfoDetail = new JSONObject(); // 初始化failInfoDetail
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
            ShellUtils.runShellCommand("pidof atx-agent| xargs kill -9", 0); //kill atx-agent
            Common.killApp("com.github.uiautomator"); //kill uiautomator process (atx app)
            checkType = CONFIG().getInt("CHECK_TYPE");
        } catch (Exception e){
            logUtil.e("", e);
            checkType = 2;
        }
        SN = Common.getDeviceId();
        String CASE_TAG;
        int LOOP;
//        String timeTag;
        String select_cases;
//        SharedPreferences taskInfo ;
        JSONArray SELECTED_CASES = new JSONArray();
        if (intent != null) {
            Log.i("kevin", "onHandleIntent方法被调用!");
            String action = intent.getAction();
            if (action == null){
                return;
            }

            switch (action){
                case ACTION_RUN:
                    timeTag = FileUtils.creatLogDir();
                    FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    TEST_ENV = intent.getStringExtra("TEST_ENV");
                    Common.switchTestEnv(TARGET_APP, TEST_ENV); //切换测试环境
                    logUtil.i(REPORT_TITLE, ("测试机型：" + DEVICE +"（"+ ALIAS + "） SN："+ Common.getSerialno()+" 应用版本：" + APP_VER + " 测试环境：" + TEST_ENV).replace("\n", ""));
                    try {
                        SELECTED_CASES = new JSONArray(intent.getStringExtra("SELECTED_CASES"));
                        CASE_TAG = intent.getStringExtra("CASE_TAG");
                        logUtil.i("","case_tag is :"+CASE_TAG);
                        LOOP = intent.getIntExtra("LOOP", 1);
                        logUtil.d(TAG,SELECTED_CASES+"");
                        // 缓存本地执行用例
                        SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, "-1");
                        SPUtils.putString(SPUtils.TASK_CASE, "-1", SELECTED_CASES.toString());
                        // 通知shellserver 守护进程
                        ShellUtils.executeSocketShell("###testool.MyIntentService", 0);

                        for(int i=0; i < SELECTED_CASES.length(); i++){
                            //判断adb连接状态
                            if (!ShellUtils.isShellEnable()){
                                logUtil.i("", "shell 不可用，跳过测试");
                                break;
                            }
                            logUtil.i(START, SELECTED_CASES.get(i) + "");
                            try {
                                startLocalRun(SELECTED_CASES.get(i) + "", CASE_TAG, false, LOOP);
                            } catch (Exception e) {
                                logUtil.e(ERROR, e);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    logUtil.i("FINISH", "测试完成～");
                    // 通知shellserver 守护进程
                    ShellUtils.executeSocketShell("###testool", 0);

                    try {
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        logUtil.e("", e);
                    }
                    break;

                case ACTION_RUN_ADB:
                    timeTag = FileUtils.creatLogDir();
                    FileUtils.createTempFile(CONST.TEMP_FILE, timeTag);
                    // 判断手机存储状态
                    double leftMemory = MemoryManager.getAvailableInternalMemorySize()/1000000000.00;
                    if (leftMemory < 1){
                        //删除报告文件
                        FileUtils.RecursionDeleteFile(new File(CONST.REPORT_PATH));
                        //删除相册文件
                        FileUtils.RecursionDeleteFile(new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "DCIM" + File.separator));
                    }
                    if (MemoryManager.getAvailableInternalMemorySize()/1000000000.00 < 1){
                        logUtil.i("", "设备可用存储过低，停止测试");
                        return;
                    }
                    //同步用例
                    Common.syncTestcases();
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
                    try {
                        SELECTED_CASES = new JSONArray(select_cases.split(","));

                    // 缓存本地执行用例
                    SPUtils.putString(SPUtils.MY_DATA, SPUtils.TASK_ID, "-1");
                    SPUtils.putString(SPUtils.TASK_CASE, "-1", SELECTED_CASES.toString());
                    // 通知shellserver 守护进程
                    ShellUtils.executeSocketShell("###testool.MyIntentService", 0);

                    CASE_TAG = intent.getStringExtra("CASE_TAG");
                    if (CASE_TAG == null) {
                        CASE_TAG = "";
                    }
                    LOOP = intent.getIntExtra("LOOP", 1);
                    for(int i=0; i < SELECTED_CASES.length(); i++){
                        //判断adb连接状态
                        if (!ShellUtils.isShellEnable()){
                            logUtil.i("", "shell 不可用，跳过测试");
                            break;
                        }
                        //跳过debug case
                        if (SELECTED_CASES.get(i).equals("debug")){
                            continue;
                        }
                        logUtil.i(START, SELECTED_CASES.get(i) + "");
                        try {
                            startLocalRun(SELECTED_CASES.get(i) + "", CASE_TAG, false, LOOP);
                        } catch (Exception e) {
                            logUtil.e("", e);
                            logUtil.e(ERROR, e);
                        }


                    }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    logUtil.i("","bugreport.zip");
                    logUtil.i("FINISH", "测试完成～");
                    // 通知shellserver 守护进程
                    ShellUtils.executeSocketShell("###testool", 0);


                    try {
                        HtmlReport.generateReport(CONST.REPORT_PATH + timeTag + File.separator +"log.txt");
                    } catch (IOException | JSONException e) {
                        logUtil.e("", e);
                        logUtil.i("report", e.getMessage());
                    }
                    break;
                case ACTION_RUN_TASK:
                    DEVICE = Common.getDeviceName();//.replace(" ", "");
                    ALIAS = Common.getDeviceAlias();
                    APP_VER = Common.getVersionName(getApplicationContext(), TARGET_APP);
                    String taskCases = intent.getStringExtra("TASK_CASES");
                    int flag = intent.getIntExtra("TASK_FLAG", 0);
                    int taskId = intent.getIntExtra("TASK_ID", 0);
                    try {
//                        logUtil.i(START, TARGET_APP);
                        startTaskRun(new JSONArray(taskCases), flag, taskId);
                    } catch (Exception e) {
                        logUtil.e("", e);
                        logUtil.i("",e.toString());
                    }
                    Common.killProcess("com.kevin.testool:MyIntentService");

                    break;
                case ACTION_RUN_REMOTE:
                    String case_remote = intent.getStringExtra("REMOTE");
                    Object actionRes = null;
                    try {
                        actionRes = action(new JSONObject(case_remote), 0, "", 0);
                    } catch (JSONException e) {
                        logUtil.e("", e);
                    }
                    MyWebSocketServer.WS.send(actionRes + "");
                    break;
                case ACTION_EXECUTE_STEP:
                    try {
                        String exe_step = intent.getStringExtra("EXECUTE_STEP");
                        logUtil.d("*****************", exe_step);
                        if (exe_step.startsWith("[")){
                            execute_step(new JSONArray(exe_step), new JSONArray().put(3));
                        } else if (exe_step.startsWith("{")) {
                            execute_step(new JSONArray().put(new JSONObject(exe_step)), new JSONArray().put(3));
                        } else {
                            logUtil.d("EXECUTE_STEP", "数据类型不正确");
                        }
                    } catch (Exception e) {
                        logUtil.e("", e);
                    }
                    break;

                case ACTION_DEBUG:
                    //单case执行调试，多case执行测试逻辑
                    String debug_case = intent.getStringExtra("DEBUG_CASE").trim();
                    Object debug_res = "";
                    boolean isDisplayLog = false;
                    try {
                        if (debug_case.startsWith("[")) {
                            JSONArray testcases = new JSONArray(debug_case);
                            if (testcases.length() == 1) {
                                isDisplayLog = true;
                                String jsonStr = testcases.getJSONObject(0).getString("test_case");
                                JSONObject mTestcase = new JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1));
                                debug_res = debugActionRun(mTestcase.toString());
                                logUtil.i(RESULT, debug_res + "");
                            }
                        } else if (debug_case.startsWith("{")){
                            isDisplayLog = true;
                            JSONObject testcases1 = new JSONObject(debug_case);
                            debug_res = debugActionRun(testcases1.toString());
                        }

                    } catch (Exception e) {
                        logUtil.e("", e);
                    }

                    // log 在调试页面显示
                    if (isDisplayLog) {
                        Intent intent_debug = new Intent();
                        intent_debug.setAction(ACTION_DEBUG_FINISH);
                        intent_debug.putExtra("DEBUG_LOG", logUtil.test_log);
                        intent_debug.putExtra("RESULT", debug_res.toString());
                        sendBroadcast(intent_debug);
                    }


                    break;

                case ACTION_DEBUG_TOOL: //调试工具
                    String res = "";
                    if (intent.hasExtra("TOOL")){
                        if (intent.getStringExtra("TOOL").equals("dump")){
                            FileUtils.deleteFile(DUMP_PATH);
                            get_elements(true, "", "", 0);
                            try {
                                res = FileUtils.readFile(DUMP_PATH);
                            } catch (IOException e) {
                                logUtil.e("", e);
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

                case ACTION_TESTCASES_SYC:
                    Common.syncTestcases();
                    sendBroadcast(new Intent(CONST.ACTION_UPDATECASELIST));
                    break;
                case ACTION_AUDIO_RECORD:
                    int seconds = intent.getIntExtra("SECOND", 20);
                    String filePath = intent.getStringExtra("PATH");
                    if (TextUtils.isEmpty(filePath)){
                        filePath = LOGPATH + "audio_record.mp3";
                    }
                    audioStep(seconds, filePath);
                    SystemClock.sleep(seconds*1000 + 2000);
                    break;
                case DEBUG:

                    break;

            }
            Common.setIdle(true);
        }
    }

    public boolean execute_step(JSONArray Step, JSONArray waitTime) throws JSONException {
        if (Step == null) {
            return true;
        }
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
//                wait_time = waitTime.getInt(waitTime.length() - 1);
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
                int nex = 0, index = 0, mIndex = -1;
                boolean refresh = true;
                String longClick = "";
                String bounds = "";
                double limit = 0.98;
                double similarity = 0;
                String mode = "";
                String method = "";
                String checked = "";
                String outPath = ""; // 用于指定输出文件路径
                int timeout = 5000; // 设置查找元素超时时间
                if (keys.contains("timeout")) {
                    keys.remove("timeout");
                    Common.setWaitTimeForElement(Step.getJSONObject(i).optInt("timeout"));
                }
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
                    mIndex = index;
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
                if (keys.contains("bounds")) {

                    bounds = Step.getJSONObject(i).getString("bounds");
                    keys.remove("bounds");
                }
                if (keys.contains("limit")) {

                    limit = (double) Step.getJSONObject(i).get("limit");
                    keys.remove("limit");
                }
                if (keys.contains("similarity")) {

                    similarity = (double) Step.getJSONObject(i).get("similarity");
                    keys.remove("similarity");
                }
                if (keys.contains("mode")) {
                    mode = Step.getJSONObject(i).getString("mode");
                    keys.remove("mode");
                }
                if (keys.contains("method")) {
                    method = Step.getJSONObject(i).getString("method");
                    keys.remove("method");
                }
                if (keys.contains("out_path")) {
                    outPath = Step.getJSONObject(i).getString("out_path");
                    keys.remove("out_path");
                }

                String key = "";
                if (keys.size() == 1) {
                    key = keys.get(0);
                    Object value = Step.getJSONObject(i).get(key);
                    //动态参数设置
                    if (key.startsWith("$")) {
                        VAR_CODE = (JSONObject) value;
                        VAR_SET = setActionVar(VAR_CODE, refresh);
                        logUtil.i("", "设置参数：" + VAR_SET);
                        continue;
                    }
                    switch (key) {
                        case "uiautomator":
                            if (value instanceof JSONObject) {
                                FileUtils.writeFile(CONST.REFLECT_FILE, ((JSONObject) value).toString(), false);
                                String res = ShellUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.kevin.testassist.Reflect' com.kevin.testassist.test/android.support.test.runner.AndroidJUnitRunner", 0);
                                logUtil.d("uiautomator", res);
                            }

                            break;
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
                        case "txt":
                            Common.click_element(refresh, "txt", value.toString(), nex, index, longClick, checked);
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
                        case "image":
                            imageStep(value, method, bounds, mIndex, limit, similarity, longClick);
                            break;
                        case "audio_record":
                            // 获取当前设备音量
                            musicVal = Common.getSoundVal(AudioManager.STREAM_MUSIC);
                            logUtil.d("", musicVal + "");
                            aiVal = Common.getSoundVal(11);
                            logUtil.d("", aiVal + "");
                            // 设置为较大音量
                            if (musicVal < 15) {
                                Common.setSound(AudioManager.STREAM_MUSIC, 15);
                            }
                            if (aiVal < 15 && aiVal >= 0) {
                                Common.setSound(11, 15);
                            }
                            audioStep(value, outPath);
                            break;
                        case "video_record":
                            videoFilePath = Common.screenRecorder2((Integer) value);
                            break;
                        case "activity":
                            Common.launchActivity(value, mode);
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
                            Common.openNotification(value.toString());
                            break;
                        case "lock":
                            Common.lockScreen();
                            break;
                        case "unlock":
                            Common.unlockScreen(value.toString());
                            break;
                        case "env":
                            Common.switchTestEnv(TARGET_APP, value.toString());
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
                            success = Common.startActivityWithUri(getApplicationContext(), String.valueOf(value));
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
                            Common.press(value.toString(), nex);
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
                        case "wifi":
                            if (value.equals("on")) {
                                if (ShellUtils.hasRootPermission()) {
                                    Common.openWifi();
                                } else {
                                    new WifiUtils(getApplicationContext()).openWifi();
                                    Common.click_element(true, "text", "允许", 0, 0);
                                }
                            } else {
                                if (ShellUtils.hasRootPermission()) {
                                    Common.closeWifi();
                                } else {
                                    new WifiUtils(getApplicationContext()).closeWifi();
                                }

                            }
                            break;

                        case "check_point":
                            if (value instanceof JSONObject) {
                                newCheckPoint = (JSONObject) value;
                                logUtil.d("", "check_point已更新：" + value.toString());
                            }
                            break;
                        case "if":
                            logUtil.i("", "----------执行if判断-----------");
                            if (value instanceof JSONObject) {
                                try {
                                    resultCheck((JSONObject) value, true, System.currentTimeMillis(), false, false);
                                } catch (Exception e) {
                                    logUtil.e("", e);
                                }
                            }
                            logUtil.i("", "-----------------------------");
                            break;

                        case "check_add":
                            try {
                                boolean ret = resultCheck(Step.getJSONObject(i).getJSONObject("check_add"), refresh, System.currentTimeMillis(), false);
                                checkResults.add(ret);
                            } catch (Exception e) {
                                logUtil.e(TAG, e);
                            }
                            break;
                        case "power_monitor":
                            powerMonitor(value.toString());
                            break;

                        case "QNET":
                            QNET(value);
                            break;

                        case "bugreport":
                            String bugreportFile = "bugreport_" + DateTimeUtils.getCurrentDateTimeString() + ".txt";
                            try {
                                Common.generateBugreport(CONST.REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + bugreportFile);
                            } catch (IOException e) {
                                logUtil.e("", e);
                            }

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
                            String apk_url = value.toString();
                            String apkFilePath = Common.downloadResource(AppContext.getContext(), apk_url, apk_url.substring(apk_url.lastIndexOf("/") + 1));
                            SystemClock.sleep(2000);
                            File apkFile = new File(apkFilePath);
                            long fileLongth = -1;
                            // 等待文件下载完成
                            long st = System.currentTimeMillis();
                            while ((fileLongth != apkFile.length()|| fileLongth == 0) && System.currentTimeMillis() - st < 60000){
                                SystemClock.sleep(5000);
                                fileLongth = apkFile.length();
                                logUtil.d("", "安装包正在下载");
                            }
                            String cpFilePath = "/data/local/tmp/" + apkFilePath.substring(apkFilePath.lastIndexOf("/")+1);
                            ShellUtils.runShellCommand(String.format("cp -f %s %s", apkFilePath, cpFilePath),0);
                            String res = ShellUtils.runShellCommand("pm install -r -d " + cpFilePath,0);
                            logUtil.d("", "开始安装应用");
                            if (res.toLowerCase().contains("success")){
                                logUtil.d("", "应用安装完成");
                            } else {
                                success = false;
                            }
                            break;

                        case "remote":
                            // 两个设备协同操作，需先在设备的config文件中配置对应设备的IP,对应字段 REMOTE_DEVICE_IP，且对应设备开启可见
                            String wsUrl = "ws://" + CONFIG().getString("REMOTE_DEVICE_IP") + ":" + CONST.SOCKET_PORT;
                            MyWebSocketClient msc = new MyWebSocketClient();
                            JSONObject sendMsg = new JSONObject();
                            sendMsg.put("mode", "case");
                            sendMsg.put("param", value);
                            msc.init(wsUrl);
                            MyWebSocketClient.sendMsg(sendMsg + "");
                            if (limit < 10) limit = 10; // 限制limit最小等待时间为10s
                            String resRemote = MyWebSocketClient.waitResult(limit*1000); // limit此时表示最长等待时间，单位秒
                            logUtil.i("", "远程执行步骤:" + value);
                            logUtil.i("", "结果：" + resRemote);
                            if (resRemote.equals("false")) {
                                checkResults.add(false);
                            } else {
                                checkResults.add(true);
                            }
                            break;
                        case "post":
                            response = ""; // 先初始化化
                            if (value instanceof JSONObject){
                                String url = ((JSONObject) value).optString("url");
                                JSONObject data = ((JSONObject) value).optJSONObject("data");
                                JSONObject headers = ((JSONObject) value).optJSONObject("headers");
                                if (data != null && data.length() > 0){
                                    if (headers != null) {
                                        response = HttpUtil.postResp(url, data.toString(), headers.toString());
                                    } else {
                                        response = HttpUtil.postResp(url, data.toString());
                                    }
//                                    ToastUtils.showShortByHandler(AppContext.getContext(), response +"");
                                    break;
                                }
                                if (!((JSONObject) value).isNull("video")){
                                    File videoFile = new File(videoFilePath);
                                    if (videoFile.exists()){
                                        response = HttpUtil.postFile(url,videoFile, videoFile.getName(), ((JSONObject) value).getJSONObject("video"));
                                        logUtil.d("", response);
                                    }
                                    break;
                                }

                            }
                            break;
                        case "get":
                            response = ""; // 先初始化化
                            if (value instanceof JSONObject){
                                String url = ((JSONObject) value).optString("url");
                                JSONObject headers = ((JSONObject) value).optJSONObject("headers");
                                if (headers != null) {
                                    response = HttpUtil.getResp(url, headers.toString());
                                } else {
                                    response = HttpUtil.getResp(url);
                                }
                            }
                            break;


                    }
                    Common.setWaitTimeForElement(timeout);
                } else {
                    loopEx((JSONObject) Step.get(i));
                }
//                logUtil.i(TAG, "action: " + key + ":" + value);
                if (i == waitTime.length()-1){
                    LAST_WAIT_TIME = waitTime.getInt(i) + 3;
                } else {
                    SystemClock.sleep(wait_time * 1000L);
                }
            } else {
                String stepDesc = Step.getString(i);
                logUtil.i("", stepDesc);

                if (i == waitTime.length()-1){
                    LAST_WAIT_TIME = waitTime.getInt(i);
                    SystemClock.sleep(1000);
                } else {
                    SystemClock.sleep(wait_time * 1000);
                }
            }

        }
        return success;
    }

    /**
     * 检查器实现
     * @param check_point
     * @param refresh
     * @param rTime
     * @param showLog
     * @param addResult
     * @return
     */
    public boolean resultCheck(JSONObject check_point, Boolean refresh, long rTime, boolean showLog, boolean... addResult){
        if (check_point == null || check_point.length() == 0){
            return true;
        }
        try {
            logUtil.toShow(showLog); // 由于动态检测结果，默认不打印log
            //refresh重载
            if (!check_point.isNull("refresh")) {
                refresh = check_point.getString("refresh").equals("true");
                if (refresh) {
                    Common.get_elements(true, "", "", 0);
                    refresh = false;
                }
            }
            ArrayList<Boolean> result = Checkpoint.muitiCheck(check_point, refresh);

            if (!check_point.isNull("$var")) {
                Object value = check_point.get("$var");
                String res = "=";
                if (value instanceof String) {
                    res = value.toString();
                    VAR_CHECK = setActionVar(VAR_CODE, refresh);
                } else {
                    JSONObject VALUE = (JSONObject) value;
                    if (!VALUE.isNull("res")) {
                        res = VALUE.getString("res");
                        VALUE.remove("res");
                    }
                    VAR_CHECK = setActionVar(VALUE, refresh);
                }
                if (checkActionVar(VAR_SET, VAR_CHECK, res)) {
                    logUtil.i("", "true|" + VAR_SET.toString() + "  vs  " + VAR_CHECK);
                    result.add(true);
                } else {
                    logUtil.i("", "false|" + VAR_SET.toString() + "  vs  " + VAR_CHECK);
                    result.add(false);
                }

            }

            if (!check_point.isNull("audio") && showLog) {
                // 结束录音
                if (mediaRecorder == null){
                    result.add(false);
                } else {
                    Common.stopAudioRecord(mediaRecorder);
                    SystemClock.sleep(2000);
                    String asrRes = Common.asrService(audioFilePath);
                    JSONArray targetTxt = check_point.getJSONArray("audio");
                    for (int i = 0; i < targetTxt.length(); i++) {
                        result.add(Checkpoint.checkAsrResult(asrRes, targetTxt.getString(i)));
                    }

                    // 恢复音量
                    Common.setSound(AudioManager.STREAM_MUSIC, musicVal);
                    Common.setSound(11, aiVal);
                }
            }


            if (!check_point.isNull("video") && showLog) {
                String targetBounds = "";
                if (!check_point.isNull("bounds")) {
                    targetBounds = check_point.getString("bounds");
                }
                Common.killProcess("screenrecord");
                JSONObject video = check_point.getJSONObject("video");
                int gap = 500;
                if (!video.isNull("bounds")) {
                    targetBounds = video.getString("bounds");
                }
                if (!video.isNull("gap")) {
                    gap = (int) video.get("gap");
                }
                if (!video.isNull("src")) {
                    // 检查视频中的图片
                    String image_src = video.getString("src");
                    if (image_src.length() < 20) {
                        if (isNumeric(image_src)) {
                            image_src = getResp(SERVER_BASE_URL + "image?id=" + image_src);
                        } else {
                            image_src = getResp(SERVER_BASE_URL + "image?tag=" + image_src.toString());// 标签匹配
                        }
                    }
                    logUtil.d("", image_src);
                    try {
                        String templateFile = FileUtils.base64ToFile(image_src, videoFilePath.replace(".mp4", "_m.jpg"));
                        result.add(getMatchImgFromVideo(videoFilePath, templateFile, Imgproc.TM_CCOEFF_NORMED, targetBounds, gap));
                    } catch (Exception e) {
                        logUtil.e("", e.toString());
                        result.add(false);
                    }
                }
                if (!video.isNull("text")) {
                    // 检测视频中的文字
                    Object txt = video.get("text");
                    String language = "chi_sim";
                    if (!video.isNull("language")) {
                        language = video.getString("language");
                    }
                    if (txt instanceof JSONArray) {
                        for (int i = 0; i < ((JSONArray) txt).length(); i++) {
                            result.add(getMatchTextFromVideo(videoFilePath, ((JSONArray) txt).getString(0), language, gap, targetBounds, refresh));
                        }
                    } else {
                        result.add(getMatchTextFromVideo(videoFilePath, txt.toString(), language, gap, targetBounds, refresh));
                    }
                }
            }
            // 判断response结果
            if (!check_point.isNull("response")){
                Object _response = check_point.get("response");
                if (_response instanceof JSONObject){
                    result.add(new JSONObject(response).equals(_response));
                }
                if (_response instanceof String){
                    result.add(new JSONObject(response).toString().contains(_response+""));
                }
            }

            //sim 卡判断
            if (!check_point.isNull("sim_card")) {
                String sim = check_point.getString("sim_card");
                if (sim.length() > 0) {
                    result.add(String.valueOf(Common.hasSimCard(getApplicationContext())).equals(sim));
                }
            }
            //nfc 功能判断
            if (!check_point.isNull("nfc")) {
                String nfc = check_point.getString("nfc");
                if (nfc.length() > 0) {
                    result.add(String.valueOf(Common.hasNfc(getApplicationContext())).equals(nfc));
                }
            }
            //app 安装判断
            if (!check_point.isNull("no_pkg")) {
                String pkg = check_point.getString("no_pkg");
                if (pkg.length() > 0) {
                    String res = ShellUtils.runShellCommand("pm list package | grep " + pkg + "\n", 10000);
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
                JSONArray dbl = check_point.getJSONArray("dev_black_lst");
                for (int n = 0; n < dbl.length(); n++) {
                    if (dbl.getString(n).equals(ALIAS)) {
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
                    logUtil.i("", Common.getVersionName(getApplicationContext(), appPkg));
                    if (Common.getVersionName(getApplicationContext(), appPkg).equals(appVersionName)) {
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
            if (!check_point.isNull("or")) {
                String or = check_point.getString("or");
                if (or.equals("true")) {
                    if (result.contains(true)) {
                        result = new ArrayList<>();
                        result.add(true);
                        logUtil.i("", "取或后结果为：true");
                    } else {
                        logUtil.i("", "取或后结果为：false");
                    }
                }
            }
            boolean ret = (!result.contains(false) & result.contains(true)) || (result.size() == 0);
            // 结果取反
            if (!check_point.isNull("reverse")) {
//            String reverse = check_point.getString("reverse");
                ret = !ret;
                logUtil.i("", "取反后结果为：" + ret);

            }
//      动态检测测试结果
            boolean flag = (System.currentTimeMillis() - rTime) < LAST_WAIT_TIME * 1000;
            if (!ret && flag && Checkpoint.isCheckPageElem) {
                SystemClock.sleep(1500);
                ret = resultCheck(check_point, true, rTime, false, addResult);
            } else {
                if (!showLog) {
                    // 打印最后一次检测的结果
                    ret = resultCheck(check_point, false, rTime, true, addResult);
                }
            }
//      通过showLog的状态判断是否为最终检测，并执行下面的操作逻辑
            if (showLog) {
                // 测试后的必要处理
                if (!check_point.isNull("teardown")) {
                    JSONArray teardown = check_point.getJSONArray("teardown");
                    JSONArray waitTime = new JSONArray().put(3);
                    if (teardown.length() > 0) {
                        logUtil.i("", "teardown:");
                        execute_step(teardown, waitTime);
                    }
                }
                // 对step中"if" 字段的执行结果，执行操作
                if (!check_point.isNull("true") && ret) {
                    JSONArray toDo = check_point.getJSONArray("true");
                    JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                    if (toDo.length() > 0) {
                        logUtil.i("", "if判断结果为true:");
                        execute_step(toDo, waitTime);
                    }
                }

                if (!check_point.isNull("false") && !ret) {
                    JSONArray toDo = check_point.getJSONArray("false");
                    JSONArray waitTime = new JSONArray().put(3);
//            String teardown = check_point.getString("teardown");
                    if (toDo.length() > 0) {
                        logUtil.i("", "if判断结果为false:");
                        execute_step(toDo, waitTime);
                    }
                }
            }
            if (addResult.length > 0 && !addResult[0]) {
                logUtil.d("", "skip add result");
            } else {
                checkResults.add(ret);
            }
            return ret;
        } catch (Exception e){
            logUtil.i("", Log.getStackTraceString(e));
            return false;
        }
    }

    /**
     * 设置变量 {"$var":{"txt":{"resource-id":"xxxx"}}}
     * @param varObj
     * @return JSONObject {"$var":"xxxx", "type":"txt"}
     * @throws JSONException
     */
    public JSONObject setActionVar(JSONObject varObj, boolean refresh) throws JSONException {
        JSONObject varRes = new JSONObject();
        Iterator<String> itr = varObj.keys();
        ArrayList<String> keys = new ArrayList<>();
        while (itr.hasNext()) {
            keys.add(itr.next());
        }
        if (keys.size() == 1){
            String var = keys.get(0);
            JSONObject varContent = varObj.getJSONObject(var);
            if (var.equals("txt")){
                String txt = get_element_text_value(refresh, varContent);
                varRes.put(var, txt);
                varRes.put("type","txt");
            }
            if (var.equals("img")){
                String bounds = get_bounds(refresh, varContent);
                if (!varContent.getJSONObject("img").isNull("bounds")){
                    bounds = varContent.getJSONObject("img").getString("bounds");
                }
                if (refresh) {
                    String inFile = Common.screenShot2();
                    cropeImage(inFile, bounds);
                    varRes.put(var, inFile);
                    varRes.put("type", "img");
                }

            }
        }
        return varRes;
    }

    public boolean checkActionVar(JSONObject varSet, JSONObject varCheck, String mode) throws JSONException {
        logUtil.i("", varSet + "");
        logUtil.i("", varCheck + "");
        String type = varSet.getString("type");
        if (mode.equals("!=")){
            if (type.equals("txt")){
                return !varSet.getString("txt").equals(varCheck.getString("txt"));
            }
            if (type.equals("img")){
                return !isSameImage(varSet.getString("img"), varCheck.getString("img"), null, 1, 0.98);
            }
        }
        if (mode.equals("==")){
            if (type.equals("txt")){
                return varSet.getString("txt").equals(varCheck.getString("txt"));
            }
            if (type.equals("img")){
                return isSameImage(varSet.getString("img"), varCheck.getString("img"), null, 1, 0.98);
            }
        }
        if (mode.equals("contain") || mode.equals("~")){
            if (type.equals("txt")){
                return varSet.getString("txt").contains(varCheck.getString("txt"));
            }

        }
        return false;
    }


    private void powerMonitor(String value){
        final String _value = value.toLowerCase();
        String recordFile = null;
        try {
            recordFile = REPORT_PATH + FileUtils.readFile(TEMP_FILE) + File.separator+ "power" + File.separator + CASE_ID_NUM + ".csv";

            final String theFile = recordFile;
            if (_value.equals("start")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BatteryManagerUtils.recordCurrentAverage(theFile, 100);
                    }
                }).start();
            } else if (StringUtil.isNumeric(_value)){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BatteryManagerUtils.recordCurrentAverage(theFile, 100, Integer.parseInt(_value));
                    }
                }).start();
            }else if (_value.equals("stop")){
                BatteryManagerUtils.setRecordFlag(false);

            }

        } catch (Exception e) {
            logUtil.e("", e);
        }
    }

    private void imageStep(Object value, String method, String bounds, int mIndex, double limit, double similarity, String longClick){
        String inFile = Common.screenShot2();
        String templateFile;
        try {
            if (value.toString().length() < 50 ){
                if (isNumeric(value.toString())) {
                    value = getResp(SERVER_BASE_URL + "image?id=" + value.toString());
                } else {
                    value = getResp(SERVER_BASE_URL + "image?tag=" + value.toString());// 标签匹配
                    logUtil.d("标签匹配", value.toString());
                }
            }
            templateFile = FileUtils.base64ToFile(value.toString(), inFile.replace(".png", "_m.png"));
            long startTime = System.currentTimeMillis();
            String resBounds = null;

            String outFile = inFile.replace(".png", "_out.png");
            // 通过图像识别服务进行图像匹配
            if (Common.isNetworkConnected(getApplicationContext())) {
                JSONObject param = new JSONObject();
                param.put("src_base64", FileUtils.xToBase64(inFile));
                param.put("sch_base64", value);
                param.put("target_bounds", parseRect(parseBounds(bounds)));
                param.put("resize", 1);
                param.put("method", method);
                param.put("threshold", limit);
                param.put("similarity", similarity);
                param.put("index", mIndex);
                JSONObject res = CvUtils.getMatchImgByApi(param);
                if (res.length() > 0) {
                    if (res.getInt("code") == 200) {
                        if (res.getBoolean("success")) {
                            resBounds = res.getString("res_bounds");
                            logUtil.d("", "使用图像识别服务");
                        }
                        FileUtils.base64ToFile(res.getString("image"), outFile);
                    }
                }
            }
            // 本地图像识别方法
            if (resBounds == null) {
                if (method.length() == 0 || method.equals("template")) {
                    resBounds = getMatchImgByTemplate(inFile, templateFile, outFile, 5, bounds, mIndex, limit, similarity);
                    logUtil.d("模板匹配耗时", (System.currentTimeMillis() - startTime) + "");
                } else if (method.equals("sift")) {
                    resBounds = getMatchImgByFeature(inFile, templateFile, outFile, method, bounds, mIndex, limit, similarity);
                    logUtil.d("特征匹配耗时", (System.currentTimeMillis() - startTime) + "");
                }
            }
            //
            if (resBounds != null) {
                if (longClick.length() > 0) {
                    Common.long_click(resBounds);
                } else {
                    Common.click(resBounds);
                }
                logUtil.i("点击", resBounds + "");
            }
        } catch (Exception e) {
            logUtil.e("", e);
        }
    }

    private void ocrStep(Object value, int index, String longClick, boolean refresh){
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

    private void audioStep(Object value, String filePath){
        String fileName = dateFormat.format(new Date()) + ".mp3";
        try {
            if (TextUtils.isEmpty(filePath)) {
                audioFilePath = REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + fileName;
            } else {
                audioFilePath = filePath;
            }
        } catch (IOException e) {
            logUtil.e("", e);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaRecorder = Common.recordAudio(audioFilePath, (Integer) value);
            }
        }).start();
    }

    private void QNET(Object value) throws JSONException {
        if (value instanceof JSONObject){
            Iterator<String> itr = ((JSONObject) value).keys();
            ArrayList<String> keys = new ArrayList<>();
            while (itr.hasNext()){
                keys.add(itr.next());
            }

            if (((JSONObject) value).isNull("command")){
                String cmdS = "am start --es package_name " + TARGET_APP;
                StringBuilder cmdM = new StringBuilder();
                String cmdE = " com.tencent.qnet/.Component.AdbStartActivity";
                String key;
                Object param;
                for (int i=0; i< keys.size(); i++){
                    key = keys.get(i);
                    param = ((JSONObject) value).get(key);
                    if (param instanceof Integer){
                        cmdM.append(" --ei ").append(key).append(" ").append(param);
                    }
                    if (param instanceof String){
                        cmdM.append(" --es ").append(key).append(" ").append(param);
                    }
                    if (param instanceof Boolean){
                        cmdM.append(" --ez ").append(key).append(" ").append(param);
                    }
                }
                logUtil.d("弱网命令", cmdS + cmdM.toString() + cmdE);
                Common.killApp("com.tencent.qnet");
                logUtil.d("", cmdS + cmdM.toString() + cmdE);
                ShellUtils.runShellCommand(cmdS + cmdM.toString() + cmdE, 0);
            } else {
                ShellUtils.runShellCommand("am broadcast -a qnet.boradcast.drive --include-stopped-packages --es command stop_service com.tencent.qnet", 0);
            }

        } else {
            ShellUtils.runShellCommand("am broadcast -a qnet.boradcast.drive --include-stopped-packages --es command stop_service com.tencent.qnet", 0);
            logUtil.d("弱网命令", "结束弱网");
        }
    }

    /**
     * 执行循环操作
     * @param param {"loop":10, "break":{}, "do":[] }  loop为最大循环次数， break为循环截止条件， "do"为循环中执行的操作
     * @throws JSONException
     */

    public void loopEx(JSONObject param) throws JSONException {
        int loop = 10;
        if (!param.isNull("loop")){
            loop = (int) param.get("loop");
        }

        for (int i=0; i < loop; i++) {
            boolean isBreak = false;
            if (!param.isNull("break")){
                JSONObject breakCondition = param.getJSONObject("break");
                isBreak = resultCheck(breakCondition, true, System.currentTimeMillis(), false);
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        SystemClock.sleep(2000);
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


    /**
     * opencv load
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    logUtil.d("", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

}
