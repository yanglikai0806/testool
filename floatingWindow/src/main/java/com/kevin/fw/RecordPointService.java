package com.kevin.fw;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.icu.util.ValueIterator;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.kevin.share.Common;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.kevin.share.Common.getElementInfoByPoint;


public class RecordPointService extends Service {

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    int mScreenHeight;
    int mScreenWidth;

    public View floatView;
    String item = "";
    String msg = "";
    String rootKey = "case";

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.hasExtra("SELECT_ITEM")) {
            item = intent.getStringExtra("SELECT_ITEM");
        }
        if (intent.hasExtra("ROOT_KEY")){
            rootKey = intent.getStringExtra("ROOT_KEY");
        }
        showFloatingWindow();
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ClickableViewAccessibility")
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            initView();

//            floatView.setPivotX(100);
//            floatView.setPivotY(100);
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

            ImageButton closeBtn = floatView.findViewById(R.id.cancle);
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        windowManager.removeView(floatView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    private void initView(){
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.record_point, null);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.CENTER|Gravity.RIGHT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mScreenHeight = windowManager.getDefaultDisplay().getHeight();
        mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        logUtil.d("", mScreenWidth + "," + mScreenHeight);
        layoutParams.width = mScreenWidth;
        layoutParams.height = mScreenHeight;
//        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.x = mScreenWidth/2;
        layoutParams.y = 0;
        layoutParams.alpha = 0.2f;
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;
        private long touchStart;
        private long touchEnd;
        private long duration;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart = System.currentTimeMillis();
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();

                    break;
                case MotionEvent.ACTION_UP:
                    touchEnd = System.currentTimeMillis();
                    duration = touchEnd - touchStart;
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    windowManager.removeView(floatView);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Intent sIntent = new Intent(getApplicationContext(), RecordStepService.class);
                            sIntent.putExtra("SELECT_ITEM", item);
                            JSONObject stepMsg= new JSONObject();
                            try {
                                if (Math.abs(nowX - x) < 10 && Math.abs(nowY - y) < 10) { //click 时间
                                    stepMsg = getElementInfoByPoint(new JSONArray().put(x).put(y));
                                    if (stepMsg.toString().contains("com.kevin.testool")){ //过滤对工具的点击操作
                                        Common.click(x, y);
                                        Intent pIntent = new Intent(getApplicationContext(), RecordPointService.class);
                                        pIntent.putExtra("SELECT_ITEM", "ui");
                                        pIntent.putExtra("ROOT_KEY", rootKey);
                                        startService(pIntent);
                                        return;
                                    }
                                    if (duration > 500){
                                        stepMsg.put("long", (int)duration/100*100 + "");
                                    }
                                } else if (duration > 500){ //swipe 事件
                                    stepMsg = new JSONObject().put("swipe", new JSONArray().put(x).put(y).put(nowX).put(nowY).put((int)duration/100*100));
                                }
                                sIntent.putExtra("STEP_MSG", stepMsg.toString());
                            } catch (Exception e) {
                                sIntent.putExtra("STEP_MSG", e.toString());
                                logUtil.e("", e);
                            }

                            sIntent.putExtra("ROOT_KEY", rootKey);
                            startService(sIntent);
                        }
                    }).start();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
