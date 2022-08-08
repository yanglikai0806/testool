package com.kevin.fw;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;

import java.io.IOException;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private View floatView;
    private Button mButton;

    @SuppressLint("RtlHardcoded")
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.float_button, null);
//        floatView.setVisibility(View.GONE);

        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int mScreenHeight = windowManager.getDefaultDisplay().getHeight();
        int mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        layoutParams.width = WRAP_CONTENT;
        layoutParams.height = WRAP_CONTENT;
        layoutParams.x = 0;
        layoutParams.y = mScreenHeight/4;
        layoutParams.alpha = 0.8f;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String btnText = intent.getStringExtra("BUTTON_TEXT");
        showFloatingWindow(btnText);
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showFloatingWindow(String btnText) {
        if (Settings.canDrawOverlays(this)) {
//            button = new Button(getApplicationContext());
//            button.setText("...");
//            button.setBackgroundColor(Color.BLUE);
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
            mButton = floatView.findViewById(R.id.button);

            mButton.setText(btnText);
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent aIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);

                    if (btnText.equals("activity")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                aIntent.putExtra("TEXT", Common.getActivity());
                                startService(aIntent);
                            }
                        }).start();


                    }
                    if (btnText.equals("dump")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Common.get_elements(true, "", "", 0);
                                try {
                                    aIntent.putExtra("TEXT", FileUtils.readFile(CONST.DUMP_PATH));
                                    startService(aIntent);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                    windowManager.removeView(floatView);

                }
            });
//            showText.setTextColor(Color.parseColor("#FF0000"));


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

    @Override
    public void onDestroy(){
        try{
            windowManager.removeView(floatView);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
