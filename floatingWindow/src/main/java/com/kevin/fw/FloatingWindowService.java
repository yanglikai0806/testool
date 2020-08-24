package com.kevin.fw;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

import static android.os.Process.killProcess;


public class FloatingWindowService extends Service {
    public static boolean isStarted = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    int mScreenHeight;
    int mScreenWidth;

    public View floatView;

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.image_display, null);
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
        layoutParams.width = 100;
        layoutParams.height = 100;
        layoutParams.x = mScreenWidth;
        layoutParams.y = mScreenHeight/4;
        layoutParams.alpha = 0.1f;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
//            button = new Button(getApplicationContext());
//            button.setText("...");
//            button.setBackgroundColor(Color.BLUE);
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
            floatView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                    Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                    while(iter.hasNext()){
                        ActivityManager.RunningAppProcessInfo next = iter.next();
                        String pricessName = getPackageName() + ":MonitorService"; //需要在manifest文件内定义android:process=":MyIntentService"
                        if(next.processName.equals(pricessName)){
                            Toast.makeText(getApplicationContext(), "监听服务已启动...", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
            });

            floatView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    isStarted = false;
                    Toast.makeText(getApplicationContext(), "监听服务已终止！", Toast.LENGTH_LONG).show();
                    SystemClock.sleep(500);
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                    Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();
                    while(iter.hasNext()){
                        ActivityManager.RunningAppProcessInfo next = iter.next();
                        String pricessName = getPackageName() + ":MonitorService"; //需要在manifest文件内定义android:process=":MyIntentService"
                        if(next.processName.equals(pricessName)){
                            killProcess(next.pid);
                            Intent intent_monitor = new Intent();
                            intent_monitor.setAction("com.kevin.testool.action.monitor.finish");
                            sendBroadcast(intent_monitor);
                            break;
                        }
                    }
                    windowManager.removeView(floatView);
                    return true;
                }
            });


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
                case MotionEvent.ACTION_UP:
                    //这里做贴边效果
                    if (x >= mScreenWidth/2){
                        layoutParams.x = mScreenWidth;
                        layoutParams.y = y;
                    } else {
                        layoutParams.x = 0;
                        layoutParams.y = y;
                    }
                    windowManager.updateViewLayout(view, layoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
