package com.kevin.fw;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevin.share.Common;
import com.kevin.share.accessibility.AccessibilityHelper;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import static android.os.Process.killProcess;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class RecordCaseService extends Service {
    public static boolean isStarted = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    public static JSONObject caseContent = new JSONObject();
    private static String rootKey = "case";
    int mScreenWidth;
    int mScreenHeight;
    BroadcastReceiver SR;

    boolean isMoveToSide = false;
    boolean isReplaying = false;

    public View floatView;

    String caseId;
    String case_desc;
    String caseFile;

    TextView item_step;
    TextView item_ui;
    TextView item_acs;
    TextView item_act;
    TextView item_key;
    TextView item_input;
    TextView item_ifelse;
    TextView item_shell;
    TextView item_check;
    TextView item_more;

    ImageButton replayBtn;


    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = false;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.record_display, null);
//        floatView.setVisibility(View.GONE);

        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mScreenHeight = windowManager.getDefaultDisplay().getHeight();
        mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        layoutParams.width = mScreenWidth;
        layoutParams.height = 200;
        layoutParams.x = mScreenWidth/2;
        layoutParams.y = 0;
        layoutParams.alpha = 0.7f;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        caseId = intent.getStringExtra("CASE_NAME");
        case_desc = intent.getStringExtra("CASE_DESC");
        caseFile = intent.getStringExtra("CASE_FILE");
        if (TextUtils.isEmpty(caseFile)){
            caseFile = "debug";
        }
        showFloatingWindow(Long.parseLong(caseId), case_desc, caseFile);
        SR = new StepActionReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.kevin.testool.MyAccessibility.Receiver");
        registerReceiver(SR, filter);

        return super.onStartCommand(intent, flags, startId);
    }
    //回放用例时，录制窗口缩小为球型
    private void showReplayBall(){
        WindowManager ballManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        View floatBallView = LayoutInflater.from(this).inflate(R.layout.record_replay_ball, null);
        ImageView replayBall = floatBallView.findViewById(R.id.replay_ball);
//        floatView.setVisibility(View.GONE);

        WindowManager.LayoutParams ballLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ballLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            ballLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        ballLayoutParams.format = PixelFormat.RGBA_8888;
        ballLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        ballLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mScreenHeight = windowManager.getDefaultDisplay().getHeight();
        mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        ballLayoutParams.width = WRAP_CONTENT;
        ballLayoutParams.height = WRAP_CONTENT;
        ballLayoutParams.x = mScreenWidth;
        ballLayoutParams.y = 200;
        ballLayoutParams.alpha = 0.7f;

        try {
            ballManager.addView(floatBallView, ballLayoutParams);
        } catch (Exception e){
            e.printStackTrace();
        }

        replayBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    //结束测试服务（结束intentservice）
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
                    isReplaying = false;
                    windowManager.addView(floatView, layoutParams);
                    ballManager.removeView(floatBallView);
                }
        });
    }


    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void showFloatingWindow(final long caseId, final String caseDesc, final String caseFile) {
        if (Settings.canDrawOverlays(this)) {
            final ImageView record_icon = floatView.findViewById(R.id.recordIcon);
            final TextView case_id = floatView.findViewById(R.id.case_id);
            case_id.setText(caseId + "/" + caseDesc);
            item_step = floatView.findViewById(R.id.select_item_step);
            item_ui = floatView.findViewById(R.id.select_item_ui);
            item_acs = floatView.findViewById(R.id.select_item_access);
            item_act = floatView.findViewById(R.id.select_item_act);
            item_key = floatView.findViewById(R.id.select_item_key);
            item_input = floatView.findViewById(R.id.select_item_input);
            item_ifelse = floatView.findViewById(R.id.select_item_if);
            item_shell = floatView.findViewById(R.id.select_item_shell);
            item_check = floatView.findViewById(R.id.select_item_check);
            item_more = floatView.findViewById(R.id.select_item_more);

            final ImageButton startBtn = floatView.findViewById(R.id.start);
            replayBtn = floatView.findViewById(R.id.replay);
            ImageButton closeBtn = floatView.findViewById(R.id.close);
            ImageButton saveBtn = floatView.findViewById(R.id.add);

            floatView.setPivotX(100);
            floatView.setPivotY(100);
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(200).start();
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Float value = (Float) animation.getAnimatedValue();
                    floatView.setScaleX(value);
                    floatView.setScaleY(value);
                }
            });
            try {
                windowManager.addView(floatView, layoutParams);
            } catch (Exception e){
                e.printStackTrace();
            }
            floatView.setOnTouchListener(new FloatingOnTouchListener());
            //贴边
            record_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isMoveToSide = !isMoveToSide;
                }
            });
            //开始录制
            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!isStarted){
                        isStarted = true;
                        rootKey = "case";
                        startBtn.setBackgroundResource(R.drawable.ic_media_stop);
                        record_icon.setBackgroundResource(R.drawable.record_busy);

                        try {
                            caseContent.put("id", caseId);
                            caseContent.put("domain", new File(caseFile).getName().replace(".json", ""));
                            caseContent.put("case", new JSONObject(String.format("{\"case_desc\":\"%s\",\"step\":[], \"wait_time\":[]}", caseDesc)));
                            caseContent.put("check_point", new JSONObject());
                            caseContent.put("skip_condition", new JSONObject());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //缓存测试用例
                        SharedPreferences rc = getSharedPreferences("record_case", 0);
                        rc.edit().putString("case", caseContent.toString()).apply();

                    } else {
                        isStarted = false;
                        startBtn.setBackgroundResource(R.drawable.ic_media_play);
                        record_icon.setBackgroundResource(R.drawable.record_idle);
                    }

                }
            });

            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isStarted) {
                        windowManager.removeView(floatView);
                        // 通知辅助服务停止监听
                        Intent ai = new Intent("com.kevin.testool.MyAccessibility.Receiver");
                        ai.putExtra("SELECT_ITEM", "");
                        sendBroadcast(ai);
                    } else {
                        Toast.makeText(getApplicationContext(), "请先结束录制", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    SharedPreferences rc = getSharedPreferences("record_case", 0);
//                    String res = rc.getString("case", "");
                    String res = SPUtils.getString("record_case", "case");
                    Intent si = new Intent(getApplicationContext(), RecordStepService.class);
                    si.putExtra("SELECT_ITEM", "save");
                    si.putExtra("STEP_MSG", res.replace("}},","}\n},").replace("},", "},\n").replace("\n\n", "\n"));
                    si.putExtra("CASE_FILE", caseFile);
                    startService(si);
                    logUtil.d("", res);

                }

            });

            replayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isReplaying) {
                        isReplaying = true;
                        windowManager.removeView(floatView); //隐藏录屏窗口
                        showReplayBall();
                        Intent playIntent = new Intent("com.kevin.testool.action.debug");
                        playIntent.setPackage("com.kevin.testool");
                        playIntent.setComponent(new ComponentName("com.kevin.testool", "com.kevin.testool.MyIntentService"));
                        String debugCase = SPUtils.getString("record_case", "case");
                        playIntent.putExtra("DEBUG_CASE", debugCase);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(playIntent);
                        } else {
                            startService(playIntent);
                        }
                        replayBtn.setRotation(0);
                        SystemClock.sleep(100);

                        ReplayFinishReceiver rfr = new ReplayFinishReceiver();
                        IntentFilter filter = new IntentFilter();
                        filter.addAction("com.kevin.testool.action.debug.finish");
                        registerReceiver(rfr, filter);
                    }

//                    Intent textDisplayIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);
//                    textDisplayIntent.putExtra("TEXT", logUtil.test_log);
//                    startService(textDisplayIntent);
                }


            });

            item_step.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_step, true);
                        Intent qi = new Intent(getApplicationContext(), RecordStepService.class);
                        qi.putExtra("SELECT_ITEM", "step");
                        qi.putExtra("ROOT_KEY", "case");
                        startService(qi);
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            item_ui.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_ui, true);
                        Intent pIntent = new Intent(getApplicationContext(), RecordPointService.class);
                        pIntent.putExtra("SELECT_ITEM", "ui");
                        pIntent.putExtra("ROOT_KEY", rootKey);
                        startService(pIntent);

                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            item_acs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_acs, true);
                        //如果辅助功能不可用，需要先开启辅助功能
                        if (AccessibilityHelper.checkAccessibilityEnabled()) {
                            Intent ui = new Intent("com.kevin.testool.MyAccessibility.Receiver");
                            ui.putExtra("SELECT_ITEM", "ui");
                            ui.putExtra("ROOT_KEY", rootKey);
                            ui.putExtra("ENABLE", true);
                            sendBroadcast(ui);
                        } else {
                            Toast.makeText(getApplicationContext(), "请先开启辅助功能", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            item_act.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_act, true);
                        Intent ai = new Intent(getApplicationContext(), RecordStepService.class);
                        ai.putExtra("SELECT_ITEM", "activity");
                        ai.putExtra("ROOT_KEY", rootKey);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String res = null;
                                try {
                                    res = new JSONObject().put("activity", Common.getActivity()).toString();
                                } catch (JSONException e) {
                                    res = e.toString();
                                }
                                ai.putExtra("STEP_MSG", res);
                                startService(ai);
                            }
                        }).start();

                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });
            item_shell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_shell, true);
                        Intent si = new Intent(getApplicationContext(), RecordStepService.class);
                        si.putExtra("SELECT_ITEM", "shell");
                        si.putExtra("ROOT_KEY", rootKey);
                        try {
                            si.putExtra("STEP_MSG", new JSONObject().put("shell", "input your cmd").toString());
                        } catch (JSONException e) {
                            si.putExtra("STEP_MSG", e.toString());
                        }
                        startService(si);
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });
            item_key.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_key, true);
                        Intent ki = new Intent(getApplicationContext(), RecordStepService.class);
                        ki.putExtra("SELECT_ITEM", "key");
                        ki.putExtra("ROOT_KEY", rootKey);
                        startService(ki);
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });
            item_input.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_input, true);
                        Intent ii = new Intent(getApplicationContext(), RecordStepService.class);
                        ii.putExtra("SELECT_ITEM", "input");
                        ii.putExtra("ROOT_KEY", rootKey);
                        ii.putExtra("STEP_MSG", "{\"input\":\"输入内容，支持英文\"}");
                        startService(ii);
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });
            item_ifelse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_ifelse, true);
                        Intent ifelse = new Intent(getApplicationContext(), RecordStepService.class);
                        ifelse.putExtra("SELECT_ITEM", "ifelse");
                        ifelse.putExtra("ROOT_KEY", rootKey);
                        ifelse.putExtra("STEP_MSG", "{\"if\":{}, \"true\":[], \"false\":[]}");
                        startService(ifelse);
                    }else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });
            item_check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted ) {
                        setTextColor(item_check, true);
                        Intent ci = new Intent(getApplicationContext(), RecordStepService.class);
                        ci.putExtra("SELECT_ITEM", "check");
                        ci.putExtra("ROOT_KEY", rootKey);
                        startService(ci);
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            item_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStarted) {
                        setTextColor(item_more, true);
                        startService(new Intent(getApplicationContext(), RecordStepService.class));
                    } else {
                        Toast.makeText(getApplicationContext(), "请按规范流程操作", Toast.LENGTH_SHORT).show();
                    }

                }
            });

            setTextViewTouchEvent(item_step);
            setTextViewTouchEvent(item_ui);
            setTextViewTouchEvent(item_act);
            setTextViewTouchEvent(item_shell);
            setTextViewTouchEvent(item_key);
            setTextViewTouchEvent(item_input);
            setTextViewTouchEvent(item_ifelse);
            setTextViewTouchEvent(item_check);
            setTextViewTouchEvent(item_more);



        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    public void setTextColor(TextView tv, boolean setFlag){
        item_step.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_ui.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_acs.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_act.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_key.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_input.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_ifelse.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_shell.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_check.setTextColor(Color.parseColor("#FFA3A1A1"));
        item_more.setTextColor(Color.parseColor("#FFA3A1A1"));
        if (setFlag) {
            String tx = tv.getText().toString();
            logUtil.d("", "setColor: " + tx);
            if (rootKey.contains("check")){
//                Log.d("KEVIN_DEBUG", "setTextColor: " + rootKey);
                if (tx.equals("check") || tx.equals("UI++") || tx.equals("acs") || tx.equals("activity") || tx.equals("to_speak") || tx.equals("ocr") || tx.equals("status")) {
                    tv.setTextColor(Color.parseColor("#D81B60"));
                    item_check.setTextColor(Color.parseColor("#D81B60"));
                }
            } else {
                tv.setTextColor(Color.parseColor("#D81B60"));
            }

        }
    }

    private boolean isRecordStepServiceRunning(){
        Log.d("KEVIN_DEBUG", "isRecordStepServiceRunning: "+RecordStepService.isRunning);
        return RecordStepService.isRunning;
    }

    public static void setRootKey(String rk){
        rootKey = rk;
        Log.d("KEVIN_DEBUG", "setRootKey: " + rk);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTextViewTouchEvent(final TextView tv){
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    tv.setBackgroundColor(Color.RED);
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    tv.setBackgroundColor(Color.WHITE);
                }else{
                    tv.setBackgroundColor(Color.WHITE);
                }

                return false;
            }
        });
    }

    /**
     * 广播接收器, 用于初始化item显示
     */

    public class StepActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra("SELECT_ITEM") && TextUtils.isEmpty(intent.getStringExtra("SELECT_ITEM"))){
                setTextColor(item_step, false);
            }
        }
    }

    public class ReplayFinishReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent_replay) {
            if (intent_replay.hasExtra("RESULT")) {
                Intent textDisplayIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);
                textDisplayIntent.putExtra("TEXT", intent_replay.getStringExtra("DEBUG_LOG"));
                startService(textDisplayIntent);
                replayBtn.setRotation(180);
                unregisterReceiver(this);
            }
        }
    }

    @Override
    public void onDestroy(){
        if (SR != null){
            unregisterReceiver(SR);
        }
    }
}
