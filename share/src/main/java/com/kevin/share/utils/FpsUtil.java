package com.kevin.share.utils;

import android.os.Build;

import com.kevin.share.adblib.CmdTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FpsUtil {
    private static final String TAG = "FpsUtil";

    public static final String FPS_DATA_EVENT = "fpsData";

    private static FpsUtil _instance;

    /**
     * 尝试初始化
     */
    public static synchronized void initIfNotInited() {
        if (_instance == null) {
            _instance = new FpsUtil();
        }
    }

    /**
     * 添加监听器
     *
     * @param
     */
    public FpsUtil() {

    }


    /**
     * 获取FPS相关数据执行器
     */
    private String appName = "";

    private String topActivity = "";

    private List<ProcessInfo> childrenPids = new ArrayList<>();

    private String proc = "";

    private String previousTopActivity = null;

    private String previousProc = null;

    private AtomicInteger reloadCount = new AtomicInteger(-1);

    /**
     * 是否正在执行
     */
    private static volatile boolean runningFlag = false;

    /**
     * 起始时间所在列
     */
    private int startPos = -1;

    /**
     * 结束时间所在列
     */
    private int endPos = -1;

    /**
     * 帧标准间隔
     */
    private static Double FPS_PERIOD = null;

    /**
     * 标准帧数
     */
    private static int FRAME_PER_SECOND = 60;


    public void setApp(String app) {
        this.appName = app;
    }

    public void setPids(List<ProcessInfo> children) {
        this.childrenPids = children;
    }

    /**
     * 非Root环境获取Fps，jank，maxJank（也支持root环境）
     * @param app 应用名称
     * @return Fps,Jank,MaxJank,
     */
    private List<FpsDataWrapper> countUnrootFPS(String app, boolean requestPrevious) {
        long startTime = System.currentTimeMillis();
        List<FpsDataWrapper> fpsDatas = new ArrayList<>();


        // 获取顶层Activity
        String[] actAndProc = getTopActivityAndProcess(app, childrenPids);
        logUtil.w(TAG, "Fps get top Activity cost: " + (System.currentTimeMillis() - startTime) + "ms");

        String tmpActivity, tmpProcessName;
        if (actAndProc != null && actAndProc.length == 2) {
            tmpActivity = actAndProc[0];
            tmpProcessName = actAndProc[1];
        } else {
            tmpActivity = app;
            tmpProcessName = app;
        }

        // 发生进程切换
        if (!StringUtil.equals(tmpProcessName, proc)) {
            previousProc = proc;
            previousTopActivity = topActivity;
            reloadCount.set(4);
        }

        proc = tmpProcessName;
        topActivity = tmpActivity;

        if (requestPrevious && reloadCount.get() > 0) {
            fpsDatas.add(loadFpsDataForProc(previousTopActivity, previousProc));

            reloadCount.decrementAndGet();
        }

        fpsDatas.add(loadFpsDataForProc(topActivity, proc));

        return fpsDatas;
    }

    /**
     * 在顶层Activity列表中查找应用的Activity
     * @param app 应用
     * @return 应用在顶层的Activity，不存在返回空字符串
     */
    private static String[] getTopActivityAndProcess(String app, List<ProcessInfo> childrenPids) {
        String[] topActivityAndPackage;
        String topActivity = "";
        if (Build.VERSION.SDK_INT < 29) {
            String cmd = "dumpsys activity top | grep \"ACTIVITY " + app + "\"";
            // 每行一个Activity，切换界面时可能存在多个Activity，无法用上一行的task，可能是自定义的
            topActivityAndPackage = CmdTools.execHighPrivilegeCmd(cmd, 500).split("\n");

            // 当找到了数据
            if (topActivityAndPackage.length > 0 && !StringUtil.isEmpty(topActivityAndPackage[0])) {
                String[] contents = topActivityAndPackage[0].trim().split("\\s+");
                String activity = contents[1];

                String[] appAndAct = activity.split("/");
                if (appAndAct.length > 1 && StringUtil.startWith(appAndAct[1], ".")) {
                    activity = appAndAct[0] + "/" + appAndAct[0] + appAndAct[1];
                }

                String packageName = app;

                // 确定下pid
                String pidInfo = contents[contents.length - 1];
                int pid = 0;
                if (StringUtil.startWith(pidInfo, "pid=")) {
                    pid = Integer.parseInt(pidInfo.substring(4));
                }

                // 通过pid找下实际的子进程，没找到就直接用主进程
                if (childrenPids != null && childrenPids.size() > 0) {
                    for (ProcessInfo process : childrenPids) {
                        if (process.getPid() == pid) {
                            packageName = app + (StringUtil.equals(process.getProcessName(), "main") ? "" : ":" + process.getProcessName());
                            break;
                        }
                    }
                }


                topActivityAndPackage = new String[]{activity, packageName};
                topActivity = activity;

            } else {
                topActivityAndPackage = new String[0];
            }
        } else {
            String cmd = "dumpsys window windows | grep \"ACTIVITY " + app + "\"";
            // 每行一个Activity，切换界面时可能存在多个Activity，无法用上一行的task，可能是自定义的
            String trimmed = CmdTools.execHighPrivilegeCmd(cmd, 500).trim();

            String[] pidContent = trimmed.split("\\s*\n+\\s*");
            if (pidContent.length > 1 && pidContent[1].contains("Session{")) {
                String[] originActivityName = pidContent[0].split("\\s+");
                String[] topActivityOrigin = originActivityName[originActivityName.length - 1].split("/");

                logUtil.i(TAG, "Activity:" + Arrays.toString(topActivityOrigin));
                // 针对Activity是以"."开头的相对定位路径
                String mActivity = topActivityOrigin[1];
                // 尾缀fix
                if (mActivity.contains("}")) {
                    mActivity = mActivity.split("\\}")[0];
                }

                if (StringUtil.startWith(mActivity, ".")) {
                    mActivity = topActivityOrigin[0] + mActivity;
                }
                String activity = topActivityOrigin[0] + "/" + mActivity;
                String packageName = app;

                String pidStr = pidContent[1].split("\\s+")[3];
                if (pidStr.contains(":")) {
                    pidStr = pidStr.split("\\:")[0];
                }
                logUtil.i(TAG, "Get pid info：" + pidStr) ;
                // 记录过滤PID
                // 确定下pid
                int pid = Integer.parseInt(pidStr);
                // 通过pid找下实际的子进程，没找到就直接用主进程
                if (childrenPids != null && childrenPids.size() > 0) {
                    for (ProcessInfo process : childrenPids) {
                        if (process.getPid() == pid) {
                            packageName = app + (StringUtil.equals(process.getProcessName(), "main") ? "" : ":" + process.getProcessName());
                            break;
                        }
                    }
                }

                // 拼接会完整名称
                topActivity = activity;
                topActivityAndPackage = new String[]{activity, packageName};
            } else {
                topActivityAndPackage = new String[0];
            }
        }

        if (topActivityAndPackage.length == 2) {
            // 特殊处理
            switch (app) {
            }
        }

        logUtil.d(TAG, "指定应用在当前界面上的top activity是：" + topActivity);
        return topActivityAndPackage;
    }


    /**
     * 加载特定进程与Activity的fps数据
     * @param activity 顶层Activity
     * @param processName 进程名
     * @return
     */
    private FpsDataWrapper loadFpsDataForProc(String activity, String processName) {
        String result;
        long startTime = System.currentTimeMillis();
        if (FPS_PERIOD == null) {
            loadDeviceScreenInfo();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // 如果topActivity存在，查找Activity对应的Profile Data
                result = CmdTools.execAdbCmd("dumpsys gfxinfo " + processName + " | grep '" + activity + "' -A129 | grep Draw -B1 -A128", 1000);
            } else {
                // 如果topActivity存在，并且版本在5.0及以上，可以通过visibility进行过滤，查找Activity对应的Profile Data
                result = CmdTools.execAdbCmd("dumpsys gfxinfo " + processName + " | grep '" + activity + ".*visibility=0' -A129 | grep Draw -B1 -A128", 1000);
            }
            logUtil.w(TAG, "Fps get gfxinfo cost: " + (System.currentTimeMillis() - startTime) + "ms");

            /**
             * 结果样式：
             * XXXX/XXXX.AAA/VVVV (visibility=0) 对应Activity
             * Draw	    Prepare	Process	Execute
             * 7.31	    5.07	6.63	0.99 每一行数据加起来，为该帧耗时
             * 50.00	21.64	44.89	6.06
             * 50.00	10.52	6.58	2.79
             * 20.21	2.36	6.78	2.51
             * 33.46	0.62	13.44	1.24
             * 10.30	0.21	6.07	1.55
             * 50.00	11.42	10.51	3.61
             * 0.84	    7.79	15.48	32.71
             * 7.56	    0.80	11.23	1.56
             * 46.13	2.58	7.29	1.16
             * 50.00	3.66	12.06	1.49
             * 12.26	0.31	5.29	0.84
             * 2.97	    1.14	8.17	1.62
             * 6.26	    0.84	9.47	2.72
             * ......
             */
            //Log.e(TAG, result);
            String[] draws = result.split("\n");
            int start = 1;
            //Log.i(TAG, "receive response: " + Arrays.toString(draws));
            if (draws.length < 3) {
                return new FpsDataWrapper(processName, activity, 0, 0, 0, 0, null, null);
            }

            int currentState = 1;

            // 状态码
            // 1: 查找Draw
            // 2: 进入其他进程
            // 如果第二行不是4列数字，说明该Activity无数据，查找下一个有数据的Activity对应数据
            while (!draws[start + 1].contains(".")) {
                int previousStart = start;
                for (int i = start + 1; i < draws.length; i++) {
                    // 跳入其他pid
                    if (StringUtil.startWith(draws[i], "**")) {
                        if (!StringUtil.contains(draws[i], appName)) {
                            currentState = 2;
                        } else {
                            currentState = 1;
                        }
                        continue;
                    }
                    if (currentState == 1 && draws[i].contains("Draw")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // 对于5.0及以上设备，需保证ACTIVITY为可见状态
                            if (!draws[i - 1].contains("visibility=0")) {
                                continue;
                            }
                        }
                        start = i;
                        break;
                    }
                }

                if (start == previousStart) {
                    break;
                }
            }

            // 没有找到数据
            if (start == draws.length - 1) {
                return new FpsDataWrapper(processName, activity, 0, 0, 0, 0, null, null);
            }

            String[] packageNameSplit = draws[start - 1].split("/");
            activity = draws[start - 1];
            if (packageNameSplit.length > 2) {
                activity = packageNameSplit[0] + "/" + packageNameSplit[1];
            } else if (activity.length() == 2) {
                activity = packageNameSplit[0];
            }

            List<Float> jankList = new ArrayList<>();
            try {
                for (int i = start + 1; i < draws.length; i++) {
                    String draw = draws[i];
                    String[] contents = draw.split("\\s+");
                    // 循环到不存在数据
                    if (contents.length < 3) {
                        break;
                    }
                    float jank = 0;

                    // 总耗时为一行数据的和
                    for (String content : contents) {
                        if (StringUtil.isEmpty(content)) {
                            continue;
                        }

                        jank += Float.parseFloat(content);
                    }
                    logUtil.d(TAG, "jank: " + jank);
                    jankList.add(jank);
                }
            } catch (NumberFormatException e) {
                logUtil.e(TAG,  e);
            }
            float maxJank = 0;
            int leftFrame = FRAME_PER_SECOND;
            int jankFrame = 0;
            int totalCount = 0;
            int jankCount = 0;

            // 从最后一位向上计数，直到耗时满足满帧
            for (int position = jankList.size() - 1; position > -1; position--) {
                float jankTime = jankList.get(position);
                totalCount++;
                int count = (int) Math.ceil(jankTime * 1000 / FPS_PERIOD);
                if (jankTime > maxJank) {
                    maxJank = jankTime;
                }
                if (count > 1) {
                    jankCount++;
                }
                if (leftFrame > count) {
                    jankFrame += count - 1;
                    leftFrame -= count;
                } else {
                    jankFrame += leftFrame;
                    break;
                }
            }

            logUtil.w(TAG, "Fps result cost: " + (System.currentTimeMillis() - startTime) + "ms");

            logUtil.d(TAG, "Total period: " + totalCount + "/Expect period: " + jankList.size());
            if (jankList.size() == 0) {
                return new FpsDataWrapper(processName, activity, 0, 0, 0, 0, null, null);
            }

            return new FpsDataWrapper(processName, activity, FRAME_PER_SECOND - jankFrame, jankCount, (int) maxJank, jankCount / (float) totalCount * 100, null, null);
        } else {
            result = CmdTools.execAdbCmd("dumpsys gfxinfo " + processName + " framestats| grep '" + activity + "' -A280", 1000);

            //Log.e(TAG, result);
            String[] draws = result.split("\n");
            int start = 0;

            if (draws.length < 3) {
                return new FpsDataWrapper(processName, activity, 0, 0, 0, 0, null, null);
            }

            int currentState = 1;

            // 状态码
            // 1: 查找Activity
            // 2: 查找---PROFILEDATA---开始
            // 3: 查找---PROFILEDATA---结束
            // 如果第二行不是4列数字，说明该Activity无数据，查找下一个有数据的Activity对应数据
            while (!draws[start].contains("---PROFILEDATA---")) {
                int previousStart = start;
                for (int i = start; i < draws.length; i++) {
                    // 跳入其他pid
                    if (currentState == 1 && draws[i].contains(activity)) {
                        currentState = 2;
                        continue;
                    }
                    if (currentState == 2 && draws[i].contains("---PROFILEDATA---")) {
                        start = i;
                        currentState = 3;
                        break;
                    }
                }

                if (start == previousStart) {
                    break;
                }
            }

            if (startPos == -1 || endPos == -1) {
                String[] titleLine = draws[start + 1].split(",");
                for (int i = 0; i < titleLine.length; i++) {
                    if (StringUtil.equals(titleLine[i], "IntendedVsync")) {
                        startPos = i;
                    } else if (StringUtil.equals(titleLine[i], "FrameCompleted")) {
                        endPos = i;
                    }
                }
            }

            List<Long> startRenderTimes = new ArrayList<>();
            List<Long> endRenderTimes = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            for (int i = start + 2; i < draws.length; i++) {
                String currentLine = draws[i];

                String[] splitted = StringUtil.split(currentLine, ",");

                if (splitted == null || splitted.length < 5) {
                    break;
                }

                if (!StringUtil.startWith(currentLine, "0")) {
                    continue;
                }

                long startRenderTime = Long.parseLong(splitted[startPos]) / 1000;
                long endRenderTime = Long.parseLong(splitted[endPos]) / 1000;

                startRenderTimes.add(startRenderTime);
                endRenderTimes.add(endRenderTime);
            }

            if (startRenderTimes.size() == 0) {
                return new FpsDataWrapper(processName, activity, 0, 0, 0, 0, startRenderTimes, endRenderTimes);
            }

            int lastPos = startRenderTimes.size() - 1;
            long filter = endRenderTimes.get(lastPos) - 1000000L;

            int totalCount = 0;
            long maxJank = 0;
            int jankCount = 0;
            int jankVsyncCount = 0;

            int position;
            // 从最后一位向上计数，直到耗时满足满帧
            for (position = lastPos; position > -1 && startRenderTimes.get(position) > filter; position--) {
                long jankTime = endRenderTimes.get(position) - startRenderTimes.get(position);
                totalCount++;
                int count = (int) Math.ceil(jankTime / FPS_PERIOD);
                if (jankTime > maxJank) {
                    maxJank = jankTime;
                }
                if (count > 1) {
                    jankCount++;
                }
                jankVsyncCount += count;
            }

            // 可能存在只有一部分数据的情况
            int fps = jankVsyncCount < FRAME_PER_SECOND?  FRAME_PER_SECOND - jankVsyncCount + totalCount: totalCount;

            return new FpsDataWrapper(processName, activity, fps, jankCount, (int) Math.ceil(maxJank / 1000F), jankCount / (float) totalCount * 100, startRenderTimes, endRenderTimes);
        }
    }

    /**
     * 加载设备屏幕信息
     */
    private static void loadDeviceScreenInfo() {
        String result = CmdTools.execHighPrivilegeCmd("dumpsys SurfaceFlinger --latency com.alipay.hulu");
        if (!StringUtil.isEmpty(result)) {
            String firstLine = result.split("\n")[0];
            try {
                long frameTime = Long.parseLong(firstLine.trim());
                FPS_PERIOD = frameTime / 1000D;
                FRAME_PER_SECOND = (int) (1000000000 / frameTime);
            } catch (NumberFormatException e) {
                logUtil.e(TAG, e);
                FPS_PERIOD = 16666D;
                FRAME_PER_SECOND = 60;
            }
        } else {
            FPS_PERIOD = 16666D;
            FRAME_PER_SECOND = 60;
        }
    }

    public static class FpsDataWrapper {
        public String proc;
        public String activity;
        public int fps;
        public int junkCount;
        public int maxJunk;
        public float junkPercent;

        public List<Long> startRenderTime;
        public List<Long> finishRenderTime;

        public FpsDataWrapper(String proc, String activity, int fps, int junkCount, int maxJunk, float junkPercent, List<Long> startRenderTime, List<Long> finishRenderTime) {
            this.proc = proc;
            this.activity = activity;
            this.fps = fps;
            this.junkCount = junkCount;
            this.maxJunk = maxJunk;
            this.junkPercent = junkPercent;
            this.startRenderTime = startRenderTime;
            this.finishRenderTime = finishRenderTime;
        }
    }
}
