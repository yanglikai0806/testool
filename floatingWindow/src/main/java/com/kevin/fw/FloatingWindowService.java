package com.kevin.fw;

import static java.lang.Math.abs;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.kevin.share.Common;
import com.kevin.share.utils.logUtil;


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
//        mScreenHeight = windowManager.getDefaultDisplay().getHeight();
//        mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        android.graphics.Point size = new Point();
        windowManager.getDefaultDisplay().getRealSize(size);
        mScreenHeight = size.y;
        mScreenWidth = size.x;
        layoutParams.width = 100;
        layoutParams.height = 100;
        layoutParams.x = mScreenWidth;
        layoutParams.y = mScreenHeight/4;
        layoutParams.alpha = 0.5f;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getIntExtra("MODE", 1) == 0) {
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                layoutParams.alpha = 0.2f;
            }
            if (intent.getIntExtra("MODE", 1) == 1) {
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                layoutParams.alpha = 0.5f;
                logUtil.d("", "参数更新");
            }
            showFloatingWindow();
        }
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
                logUtil.e("", e.getMessage());
                windowManager.updateViewLayout(floatView, layoutParams);
            }
            floatView.setOnTouchListener(new FloatingOnTouchListener());
            floatView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!Common.getActivity().contains(getPackageName())) {
                        Common.launchApp(getApplicationContext(), getPackageName());
                    }
                }
            });

            floatView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });


        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int xView;
        private int yView;
        private int xScreen;
        private int yScreen;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    xView = (int) event.getX();
                    yView = (int) event.getY();
                    xScreen = (int) event.getRawX();
                    yScreen = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    xScreen = (int) event.getRawX();
                    yScreen = (int) event.getRawY();
//                    int movedX = nowX - x;
//                    int movedY = nowY - y;
//                        x = nowX;
//                        y = nowY;
//                        layoutParams.x = layoutParams.x + movedX;
//                        layoutParams.y = layoutParams.y + movedY;
//                    layoutParams.x = nowX;
//                    layoutParams.y = nowY;
//                    windowManager.updateViewLayout(view, layoutParams);
                    layoutParams.x = xScreen - xView;
                    layoutParams.y = yScreen - yView;
                    windowManager.updateViewLayout(view, layoutParams);

                    break;
                case MotionEvent.ACTION_UP:
                    //这里做贴边效果
//                    int upX = (int)event.getRawX();
//                    int upY = (int)event.getRawY();
//                    int xDis = abs(upX - xView);
//                    int yDis = abs(upY - yView);
//                    if (xDis >50 || yDis>50){
//                        if (upX > mScreenWidth/2) {
//                            layoutParams.x = mScreenWidth;
//                            layoutParams.y = upY;
//                        } else {
//                            layoutParams.x = 0;
//                            layoutParams.y = upY;
//                        }
//                    }
////                    layoutParams.x = xScreen - xView;
////                    layoutParams.y = yScreen - yView;
//                    windowManager.updateViewLayout(view, layoutParams);

                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
