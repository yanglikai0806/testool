package com.kevin.fw;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import android.widget.ScrollView;

import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.BitmapUtil;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.ToastUtils;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Iterator;

import static com.kevin.share.CONST.CROPPER_IMG_PATH;
import static com.kevin.share.CONST.CROPPER_IMG_SCALE;


public class RecordStepService extends Service {
    public static boolean isRunning = false;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    public View floatView;
    JSONArray cs = new JSONArray(); // case step
    JSONArray wt = new JSONArray(); // wait_time
    JSONObject cp = new JSONObject(); // check_point
    JSONObject ca = new JSONObject(); // check_add

    String caseFile = "debug.json";


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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String item = "";
        String msg = "";
        String rootKey = "case";
        if (intent.hasExtra("SELECT_ITEM")) {
            item = intent.getStringExtra("SELECT_ITEM");
        }
        if (intent.hasExtra("STEP_MSG")){
            msg = intent.getStringExtra("STEP_MSG");
        }
        if (intent.hasExtra("ROOT_KEY")){
            rootKey = intent.getStringExtra("ROOT_KEY");
        }
        if (intent.hasExtra("CASE_FILE")){
            caseFile = intent.getStringExtra("CASE_FILE");
        }
        showFloatingWindow(item, msg, rootKey);
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showFloatingWindow(String item, String msg, String rootKey) {
        if (Settings.canDrawOverlays(this)) {
            if (isRunning && floatView != null){
                windowManager.removeView(floatView);
            }
            isRunning = true;
            initView(item, rootKey);

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

            final EditText stepMsg = floatView.findViewById(R.id.text_content);
            ImageButton closeBtn = floatView.findViewById(R.id.cancle);
            Button editeBtn = floatView.findViewById(R.id.edite);
            Button actionBtn = floatView.findViewById(R.id.action);
            Button addBtn = floatView.findViewById(R.id.add);
            if (stepMsg != null) {
                stepMsg.setText(msg);
            }
            CropImageView crop = null;
            if (item.equals("image")){

                Common.screenShot(CROPPER_IMG_PATH);//截图
                crop = (CropImageView) floatView.findViewById(R.id.dialog_action_crop_view);
                Bitmap bitmap = BitmapFactory.decodeFile(CROPPER_IMG_PATH);
                Matrix matrix = new Matrix();
                float mScale = 0.8f;
                matrix.postScale(mScale, mScale);
                Bitmap newBM = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, false);

                final Rect defaultBound;
                defaultBound = new Rect(newBM.getWidth() / 5,
                        newBM.getHeight() / 3, newBM.getWidth() / 5 * 4,
                        newBM.getHeight() / 3 * 2);


                crop.setImageBitmap(newBM);
                crop.setCropRect(defaultBound);
                layoutParams.alpha = 1.0f;
                windowManager.updateViewLayout(floatView, layoutParams);
                if (!bitmap.isRecycled()){
                    bitmap.recycle();
                }
            }

            if (item.equals("toast") && stepMsg != null){
                stepMsg.setHint("点击“编辑”按钮，输入你要说的话...");
            }
            if (item.equals("key")){
                Button recentKey = floatView.findViewById(R.id.recentKey);
                Button homeKey = floatView.findViewById(R.id.homeKey);
                Button backKey = floatView.findViewById(R.id.backKey);
                Button powerKey = floatView.findViewById(R.id.powerKey);
                setKeyButtonListener(recentKey, stepMsg);
                setKeyButtonListener(homeKey, stepMsg);
                setKeyButtonListener(backKey, stepMsg);
                setKeyButtonListener(powerKey, stepMsg);

            }
            if (item.equals("save")){
                Button saveBtn = floatView.findViewById(R.id.save);
                Button uploadCase = floatView.findViewById(R.id.upload);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (caseFile.split("\\.").length > 1){
                            FileUtils.editCaseJsonFile(caseFile.split("\\.",2)[1].trim(), stepMsg.getText().toString());
                        } else {
                            FileUtils.editCaseJsonFile(caseFile.trim(), stepMsg.getText().toString());
                        }
                        ToastUtils.showShortByHandler(getApplicationContext(), "已保存");
                    }
                });

                uploadCase.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (Common.updateTestCases( "["+stepMsg.getText().toString()+"]")){
                                    ToastUtils.showShortByHandler(getApplicationContext(), "上传完成");
                                } else {
                                    ToastUtils.showShortByHandler(getApplicationContext(), "上传失败");
                                }
                            }
                        }).start();
                    }
                });
            }
            if (item.equals("check")){
                Button checkAdd = floatView.findViewById(R.id.checkAdd);
                Button checkPoint = floatView.findViewById(R.id.checkPoint);
                checkAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecordCaseService.setRootKey("check_add");
                        checkAdd.setTextColor(Color.parseColor("#ff4081"));
                        checkPoint.setTextColor(Color.parseColor("#000000"));
                    }
                });

                checkPoint.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecordCaseService.setRootKey("check_point");
                        checkPoint.setTextColor(Color.parseColor("#ff4081"));
                        checkAdd.setTextColor(Color.parseColor("#000000"));
                    }
                });


                setButtonTouchStyle(checkAdd);
                setButtonTouchStyle(checkPoint);

            }


            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        windowManager.removeView(floatView);
                        // 通知辅助服务停止监听
                        if (item.equals("ui")) {
                            Intent ai = new Intent("com.kevin.testool.MyAccessibility.Receiver");
                            ai.putExtra("SELECT_ITEM", "");
                            sendBroadcast(ai);
                        }

                        RecordCaseService.setRootKey("case");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isRunning = false;
                }
            });
            if (editeBtn != null){
                editeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item.equals("save")){
                            if (editeBtn.getText().toString().equals("编辑")) {
                                editeBtn.setText("完成");
                            } else {
                                //实现缓存后，继续录制上一次未完成的测试case
                                editeBtn.setText("编辑");
                                String caseStr = stepMsg.getText().toString();
                                SPUtils.putString("record_case", "case", caseStr);
                                try {
                                    JSONObject caseJson = new JSONObject(caseStr);
                                    cs = caseJson.getJSONObject("case").getJSONArray("step");
                                    wt = caseJson.getJSONObject("case").getJSONArray("wait_time");
                                    cp = caseJson.getJSONObject("check_point");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                        windowManager.updateViewLayout(floatView, layoutParams);
                    }
                });
            }

            if (actionBtn != null) {

                CropImageView finalCrop = crop;
                actionBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item.equals("image")){
                            String cropperBounds = Common.parseRect(finalCrop.getCropRect(), CROPPER_IMG_SCALE, 50);
                            Log.d("KEVIN_DEBUG bounds", cropperBounds);

                            Bitmap result = finalCrop.getCroppedImage();
                            String bs64 = BitmapUtil.bitmapToBase64(result);
                            FileUtils.writeFile(CONST.LOGPATH + "image_base64.txt", bs64,false);

                            if (!result.isRecycled()){
                                result.recycle();
                            }

                            Intent img = new Intent(getApplicationContext(), RecordStepService.class);
                            img.putExtra("SELECT_ITEM", "more");
                            img.putExtra("ROOT_KEY", rootKey);
                            img.putExtra("STEP_MSG", String.format("{\"bounds\":\"%s\",\"image\":\"%s\"}", cropperBounds,bs64));
                            startService(img);

                            return;
                        }

                        JSONArray step = new JSONArray();
                        String action = stepMsg.getText().toString();
                        if (action.startsWith("{")) {
                            try {
                                step.put(new JSONObject(action));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            step.put(action);
                        }

                        Intent playIntent = new Intent("com.kevin.testool.action.execute_step");
                        playIntent.setPackage("com.kevin.testool");
                        playIntent.setComponent(new ComponentName("com.kevin.testool", "com.kevin.testool.MyIntentService"));
                        playIntent.putExtra("EXECUTE_STEP", step.toString());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(playIntent);
                        } else {
                            startService(playIntent);
                        }
//                        new Thread(() -> {
//                            try {
//                                Common.execute_step(step, new JSONArray().put(1));
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }).start();

                    }
                });
            }

            if (addBtn != null) {
                addBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String sm = stepMsg.getText().toString();

                        if (TextUtils.isEmpty(sm)){
                            ToastUtils.showShortByHandler(getApplicationContext(), "内容不能为空");
                            return;
                        }

                        if (rootKey.equals("case")) {
                            if (sm.contains("{") && sm.contains("}")) {
                                try {
                                    setCaseStep(new JSONObject(sm));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                setCaseStep(sm);
                            }
                        }
                        if (rootKey.equals("check_point")) {
                            try {
                                setCheckpoint(new JSONObject(sm));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (rootKey.equals("check_add")) {
                            try {
                                setCheckadd(new JSONObject(sm));
                                try {
                                    JSONArray caseSteps = getCaseStep();
                                    int l = caseSteps.length();
                                    if (l > 0){
                                        if (caseSteps.get(l-1) instanceof JSONObject){
                                            JSONObject _step = caseSteps.getJSONObject(l-1);
                                            Iterator<String> itKeys1 = _step.keys();
                                            String key;
                                            boolean isUpdateCheckadd = false;
                                            while(itKeys1.hasNext()){
                                                key = itKeys1.next();
                                                if (key.equals("check_add")){
                                                    caseSteps.remove(l-1);
                                                    cs = caseSteps.put(new JSONObject().put("check_add", ca));
                                                    isUpdateCheckadd = true;
                                                }
                                            }
                                            if (!isUpdateCheckadd){
                                                setCaseStep(new JSONObject().put("check_add", new JSONObject(sm)));
                                            }
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            setCaseContent(new JSONObject().put("case_desc", getCaseDesc()).put("step", cs).put("wait_time", wt), cp);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        stepMsg.setText("");

                    }
                });
            }


        }
    }

    private void initView(String item, String rootKey){
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.record_step, null);

        if (item.equals("check") || rootKey.contains("check")){
            floatView = LayoutInflater.from(this).inflate(R.layout.record_check, null);
        }
        if (item.equals("key")){
            floatView = LayoutInflater.from(this).inflate(R.layout.record_key, null);
        }
        if (item.equals("save")){
            floatView = LayoutInflater.from(this).inflate(R.layout.record_save, null);
        }
        if (item.equals("image")){
            floatView = LayoutInflater.from(this).inflate(R.layout.record_cropper, null);
        }

        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.CENTER|Gravity.RIGHT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int mScreenHeight = windowManager.getDefaultDisplay().getHeight();
        int mScreenWidth = windowManager.getDefaultDisplay().getWidth();
        layoutParams.width = mScreenWidth;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.x = mScreenWidth/2;
        layoutParams.y = 0;
        layoutParams.alpha = 0.7f;
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

    public JSONObject getCaseContent(){
        String res = SPUtils.getString("record_case", "case");
        try {
            return new JSONObject(res);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public void setCaseContent(JSONObject caseStep, JSONObject checkpoint){
        try {
            JSONObject cc = getCaseContent().put("case", caseStep).put("check_point", checkpoint);
            SPUtils.putString("record_case", "case", cc.toString());
//            SharedPreferences rc = getSharedPreferences("record_case", 0);
//            rc.edit().putString("case", cc.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public String getCaseDesc(){
        try {
            return getCaseContent().getJSONObject("case").getString("case_desc");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    public JSONArray getCaseStep(){
        try {
            return getCaseContent().getJSONObject("case").getJSONArray("step");
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    public void setCaseStep(String text){
        cs = getCaseStep().put(text);
        wt = getWaitTime().put(2);

    }

    public void setCaseStep(JSONObject JO){
        cs = getCaseStep().put(JO);
        wt = getWaitTime().put(2);

    }

    public JSONArray getWaitTime(){
        try {
            wt = getCaseContent().getJSONObject("case").getJSONArray("wait_time");
        } catch (JSONException e) {
            e.printStackTrace();
            wt = new JSONArray();
        }
        return wt;
    }

    public JSONObject getCheckpoint(){
        try {
            return getCaseContent().getJSONObject("check_point");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public JSONObject getCheckadd(){
        try {
            JSONArray caseSteps = getCaseStep();
            int l = caseSteps.length();
            if (l > 0){
                if (caseSteps.get(l-1) instanceof JSONObject){
                    JSONObject _step = caseSteps.getJSONObject(l-1);
                    Iterator<String> itKeys1 = _step.keys();
                    String key;
                    while(itKeys1.hasNext()){
                        key = itKeys1.next();
                        if (key.equals("check_add")){
                            return _step.getJSONObject("check_add");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
    public void setCheckpoint(JSONObject JO){
        try {
            cp = getCheckpoint();
            Iterator<String> itr = JO.keys();
//            ArrayList<String> keys = new ArrayList<>();
            while (itr.hasNext()){
                String key = itr.next();
                Log.d("KEVIN_DEBUG", "setCheckpoint: " + key);
                cp = cp.put(key, JO.get(key));
            }
//            String key = JO.keys().next();
//            cp = getCheckpoint().put(key, JO.get(key));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setCheckadd(JSONObject JO){
        try {
            String key = JO.keys().next();
            ca = getCheckadd().put(key, JO.get(key));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setKeyButtonListener(Button btn, EditText stepMsg){
        if (btn == null){
            return;
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                stepMsg.setText(String.format("{\"press\":\"%s\"}", btn.getContentDescription().toString()));
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setButtonTouchStyle(final Button btn){
        btn.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btn.setBackgroundColor(R.color.colorAccent);
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    btn.setBackgroundResource(android.R.drawable.btn_default);
                }

                return false;
            }
        });
    }
}
