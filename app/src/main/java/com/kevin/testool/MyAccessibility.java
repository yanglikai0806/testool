package com.kevin.testool;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Environment;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.kevin.fw.RecordPointService;
import com.kevin.fw.RecordStepService;
import com.kevin.share.accessibility.AccessibilityNodeInfoDumper;
import com.kevin.testool.accessibility.BaseAccessibility;
import com.kevin.share.Common;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.kevin.share.CONST.ACCESSIBILITY_RECEIVER;
import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.accessibility.AccessibilityHelper.getNodeInfoToUse;
import static com.kevin.share.accessibility.AccessibilityNodeInfoDumper.dumpWindowHierarchy;
import static com.kevin.share.Common.isInBounds;
import static com.kevin.share.Common.parserXml;


public class MyAccessibility extends BaseAccessibility {
    String TAG = "MyAccessibility";
    private static boolean getElements = false;
    private static boolean uiUpdated = false;
    private static String rootKey = "case";
    public static boolean isDumping = false;
    public static String selectItem = "";
    BroadcastReceiver AR;


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AR = new AccessibilityReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(ACCESSIBILITY_RECEIVER);
        registerReceiver(AR, filter);
        logUtil.i("accessibility", "注册广播");
//        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
//        info.notificationTimeout = 100;
//        info.packageNames = new String[]{"...", "..."};
//        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
//        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType;
        eventType = event.getEventType();
//        AccessibilityNodeInfo[] nodeInfo = getWindowRoots(this);
//        logUtil.d("-----------------node----------------", nodeInfo.toString());
        //界面获取
        if (getElements && !isDumping) {
            new Thread(() -> Common.screenShot(Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.png")).start();
            dumpWindowHierarchys();
            isDumping = false;
            getElements = false;
            if (uiUpdated){
                ToastUtils.showShortByHandler(MyApplication.getContext(), "界面获取完成");
            }
        }
        // 用例录制
        if (selectItem.equals("ui")) {
            JSONObject ndinfo;
            Intent rIntent = new Intent(MyApplication.getContext(), RecordStepService.class);

            switch (eventType){
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    if (event.getSource() != null) {
                        // 过滤掉对自己的点击操作
                        if (event.getSource().getPackageName().equals(getPackageName())){
                            break;
                        }
                        ndinfo = dumpEventResourceInfo(event);
                        rIntent.putExtra("SELECT_ITEM", "ui");
                        if (ndinfo.length() > 1){
                            try {
                                rIntent.putExtra("STEP_MSG", getNodeInfoToUse(ndinfo, uiUpdated).toString());
                            } catch (Exception e) {
                                logUtil.e("", e);
                                rIntent.putExtra("STEP_MSG", e.toString());
                            }

                        } else {

                            rIntent.putExtra("STEP_MSG", "请点击acs重试");
                        }
                        rIntent.putExtra("ROOT_KEY", rootKey);
                        startService(rIntent);
                        uiUpdated = false;
                    } else {
                        logUtil.d("MyAccessibility", "event source is null");
                        Intent pIntent = new Intent(MyApplication.getContext(), RecordPointService.class);
                        pIntent.putExtra("SELECT_ITEM", "ui");
                        pIntent.putExtra("ROOT_KEY", rootKey);
                        startService(pIntent);
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    if (event.getSource().getPackageName().equals("com.kevin.testool")) {
                        // 过滤掉对自己的点击操作
                        break;
                    }
                    ndinfo = dumpEventResourceInfo(event);
                    logUtil.d("", ndinfo.toString());
//                    rIntent = new Intent(MyApplication.getContext(), RecordStepService.class);
                    rIntent.putExtra("SELECT_ITEM", "ui");
                    rIntent.putExtra("STEP_MSG", ndinfo.toString());
                    startService(rIntent);
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    break;
                case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                    logUtil.d("touch", event.getSource() + "");
            }
        }
    }

    /**
     * 生成window_dump.xml
     */

    public void dumpWindowHierarchys() {
        isDumping = true;
        try {

            String strPath = Environment.getExternalStorageDirectory().getPath();
            //创建xml文件
            File file = new File(strPath, "window_dump.xml");
            FileOutputStream fos = new FileOutputStream(file);
            dumpWindowHierarchy(this, fos);
            logUtil.d("MyAccessibility", "完成界面获取");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void setServiceInfoToTouchBlockMode() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            logUtil.e(TAG, "ServiceInfo为空");
            return;
        }
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;

        logUtil.d(TAG, "辅助功能进入触摸监控模式");
        setServiceInfo(info);
    }

    private void setServiceToNormalMode() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            logUtil.e(TAG, "ServiceInfo为空");
            return;
        }
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.DEFAULT;

        logUtil.d(TAG, "辅助功能进入正常模式");
        setServiceInfo(info);
    }

    /**
     * 根据event获得nodeinfo属性信息
     * @param event
     * @return JSONObject
     */
    private JSONObject dumpEventResourceInfo(AccessibilityEvent event) {
        JSONObject ndInfo = new JSONObject();
        JSONObject childNodeInfo = new JSONObject();
        AccessibilityNodeInfo nd = event.getSource();
        if (nd == null){
            return ndInfo;
        }
        ndInfo = dumpNodeInfo(nd);

        logUtil.d("parentNodeInfo", ndInfo + "");
        int n = nd.getChildCount();
        if (n > 0){
            try {
                String parentBounds = ndInfo.getString("bounds");
                childNodeInfo = dumpChildInfo(nd, parentBounds, childNodeInfo);
                logUtil.d("childNodeInfo", childNodeInfo + "");
                if (childNodeInfo.length() > 0){
                    childNodeInfo.put("bounds", parentBounds);
                    return childNodeInfo;
                }
            } catch (JSONException e) {
                logUtil.e("", e);
            }
        }

        return ndInfo;

    }

    private JSONObject dumpChildInfo(AccessibilityNodeInfo nd, String parentBounds, JSONObject ndInfo){
//        JSONObject ndInfo = new JSONObject();
        if (nd == null){
            return new JSONObject();
        }
        int n = nd.getChildCount();

        for (int i = 0; i < n; i++){
            AccessibilityNodeInfo childNode = nd.getChild(i);
            JSONObject childInfo = dumpNodeInfo(childNode);
            logUtil.d("childNode", childInfo.toString());

            try {
                if (!childInfo.isNull("text") && childInfo.getString("text").length() > 0){
                    if (ndInfo.isNull("text")){
                        ndInfo.put("text", childInfo.getString("text"));
                    }
                }
                if (!childInfo.isNull("resource-id")){
                    if (ndInfo.isNull("resource-id")) {
                        ndInfo.put("resource-id", childInfo.getString("resource-id"));
                    }
                }
                if (!childInfo.isNull("content-desc")){
                    if (ndInfo.isNull("content-desc")) {
                        ndInfo.put("content-desc", childInfo.getString("content-desc"));
                    }
                }
//                if (ndInfo.length() > 0){
//                    ndInfo.put("bounds", parentBounds);
////                    return ndInfo;
//                }
            } catch (JSONException e) {
                logUtil.e("", e);
            }
            if (childNode != null && childNode.getChildCount() > 0){
                dumpChildInfo(childNode, parentBounds, ndInfo);
            }
        }
        return ndInfo;
    }

    /**
     * 整合node属性信息
     * @param nd
     * @return
     */
    private JSONObject dumpNodeInfo(AccessibilityNodeInfo nd){
        JSONObject ndInfo = new JSONObject();
        if (nd == null){
            return ndInfo;
        }
        try {

            if (nd.getText() != null){
                ndInfo.put("text", nd.getText());
            }
            if (nd.getViewIdResourceName() != null){
                ndInfo.put("resource-id", nd.getViewIdResourceName());
            }
            if (nd.getContentDescription() != null){
                ndInfo.put("content-desc", nd.getContentDescription().toString());
            }
            if (nd.getClassName() != null) {
                ndInfo.put("class", nd.getClassName());
            }
            Rect rect = new Rect();
            nd.getBoundsInScreen(rect);
            ndInfo.put("bounds",String.format("[%s,%s][%s,%s]", rect.left, rect.top, rect.right, rect.bottom));


        } catch (JSONException e) {
            logUtil.e("MyAccessibility", e);
        }
        return ndInfo;
    }


    /**
     * 广播接收器
     */

    public class AccessibilityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            getElements = intent.getBooleanExtra("ENABLE", false);
            if (intent.hasExtra("BOUNDS")){
                getElements = true;
                AccessibilityNodeInfoDumper.setTargetBounds(intent.getStringExtra("BOUNDS"));
            }
            if (intent.hasExtra("ACTION_TYPE")){
                String actionType = intent.getStringExtra("ACTION_TYPE");
                if (actionType.contains("click")){
                    AccessibilityNodeInfoDumper.setClickType(actionType);
                }
            }
            if (intent.hasExtra("SELECT_ITEM")){
                selectItem = intent.getStringExtra("SELECT_ITEM");
                uiUpdated = intent.getBooleanExtra("ENABLE", false);
                rootKey = intent.getStringExtra("ROOT_KEY");
                logUtil.d("MyAccessibility", "select itme :"+selectItem);
            }
        }
    }
    @Override
    public void onDestroy(){
        if (AR != null){
            unregisterReceiver(AR);
        }
    }
}