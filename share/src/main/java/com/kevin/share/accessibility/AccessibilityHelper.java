package com.kevin.share.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import com.kevin.share.AppContext;
import com.kevin.share.Common;
import com.kevin.share.utils.logUtil;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.kevin.share.CONST.DUMP_PATH;
import static com.kevin.share.Common.isInBounds;
import static com.kevin.share.Common.parserXml;


public final class AccessibilityHelper {

    private AccessibilityHelper() {}


    /**
     * Check当前辅助服务是否启用
     *
     * @param
     * @return 是否启用
     */
    public static boolean checkAccessibilityEnabled() {

        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(AppContext.getContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        String settingValue = "";
        try {
            if (accessibilityEnabled == 1) {
                settingValue = Settings.Secure.getString(AppContext.getContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                logUtil.d("checkAccessibility", settingValue + "");
            }
            return settingValue.contains("com.kevin.testool");
        } catch (Exception e){
            return false;
        }


    }

    /**
     * 前往开启辅助服务界面
     */
    public static void goAccess() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        AppContext.getContext().startActivity(intent);
    }


    /** 通过id查找*/
    public static AccessibilityNodeInfo findNodeInfosById(AccessibilityNodeInfo nodeInfo, String resId) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
        if(list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /** 通过文本查找*/
    public static AccessibilityNodeInfo findNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if(list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /** 通过关键字查找*/
    public static AccessibilityNodeInfo findNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String... texts) {
        for(String key : texts) {
            AccessibilityNodeInfo info = findNodeInfosByText(nodeInfo, key);
            if(info != null) {
                return info;
            }
        }
        return null;
    }

    /** 通过组件名字查找*/
    public static AccessibilityNodeInfo findNodeInfosByClassName(AccessibilityNodeInfo nodeInfo, String className) {
        if(TextUtils.isEmpty(className)) {
            return null;
        }
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo node = nodeInfo.getChild(i);
            if(className.contentEquals(node.getClassName())) {
                return node;
            }
        }
        return null;
    }

    /** 找父组件*/
    public static AccessibilityNodeInfo findParentNodeInfosByClassName(AccessibilityNodeInfo nodeInfo, String className) {
        if(nodeInfo == null) {
            return null;
        }
        if(TextUtils.isEmpty(className)) {
            return null;
        }
        if(className.equals(nodeInfo.getClassName())) {
            return nodeInfo;
        }
        return findParentNodeInfosByClassName(nodeInfo.getParent(), className);
    }

    private static final Field sSourceNodeField;

    static {
        Field field = null;
        try {
            field = AccessibilityNodeInfo.class.getDeclaredField("mSourceNodeId");
            field.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sSourceNodeField = field;
    }

    public static long getSourceNodeId (AccessibilityNodeInfo nodeInfo) {
        if(sSourceNodeField == null) {
            return -1;
        }
        try {
            return sSourceNodeField.getLong(nodeInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getViewIdResourceName(AccessibilityNodeInfo nodeInfo) {
        return nodeInfo.getViewIdResourceName();
    }

    /** 返回主界面事件*/
    public static void performHome(AccessibilityService service) {
        if(service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    /** 返回事件*/
    public static void performBack(AccessibilityService service) {
        if(service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /** 点击事件*/
    public static void performClick(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return;
        }
        if(nodeInfo.isClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            performClick(nodeInfo.getParent());
        }
    }

    /** 长按事件*/
    public static void performLongClick(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return;
        }
        if(nodeInfo.isLongClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        } else {
            performLongClick(nodeInfo.getParent());
        }
    }


    public static JSONObject dumpElementInfo(Element element) throws JSONException {
        JSONObject eInfo = new JSONObject();
        if (!TextUtils.isEmpty(element.attributeValue("text"))){
            eInfo.put("text", element.attributeValue("text"));
        }
        if (!TextUtils.isEmpty(element.attributeValue("resource-id"))){
            eInfo.put("resource-id", element.attributeValue("resource-id"));
        }
        if (!TextUtils.isEmpty(element.attributeValue("content-desc"))){
            eInfo.put("content-desc", element.attributeValue("content-desc"));
        }
        if (!TextUtils.isEmpty(element.attributeValue("class"))){
            eInfo.put("class", element.attributeValue("class"));
        }
        if (!TextUtils.isEmpty(element.attributeValue("bounds"))){
            eInfo.put("bounds", element.attributeValue("bounds"));
        }
//        logUtil.d("dumpElementInfo", eInfo.toString());
        return eInfo;
    }


    /**
     * 生成可用于测试case的nodeInfo属性
     * @param ndInfo
     * @return
     */
    public static JSONObject getNodeInfoToUse(JSONObject ndInfo, boolean uiUpdated) throws Exception {
        if (ndInfo.isNull("bounds") || !uiUpdated) {
            return ndInfo;
        }
        String bounds = ndInfo.getString("bounds");
        ArrayList<Element> elements;
        JSONObject target;
        JSONArray targetLst = new JSONArray();

        try {
            if (!ndInfo.isNull("text")){
                elements = parserXml(DUMP_PATH, "text", ndInfo.getString("text"), 0);
                target = getTargetNode(elements, "text", bounds);
                if (target.length() == 1){
                    return target;
                }
                if (target.length() > 1){
                    targetLst.put(target);
                }
            }
            if (!ndInfo.isNull("resource-id")){
                elements = parserXml(DUMP_PATH, "resource-id", ndInfo.getString("resource-id"), 0);
                target = getTargetNode(elements, "resource-id", bounds);
                if (target.length() == 1){
                    return target;
                }
                if (target.length() > 1){
                    targetLst.put(target);
                }
            }
            if (!ndInfo.isNull("content-desc")){
                elements = parserXml(DUMP_PATH, "content-desc", ndInfo.getString("content-desc"), 0);
                target = getTargetNode(elements, "content-desc", bounds);
                if (target.length() == 1){
                    return target;
                }
                if (target.length() > 1){
                    targetLst.put(target);
                }
            }
            if (!ndInfo.isNull("bounds")) {
                elements = parserXml(DUMP_PATH, "bounds", bounds, 0);
                logUtil.d("elements", elements.size() + "");
                for (Element e : elements) {
                    JSONObject eInfo = dumpElementInfo(e);

                    target = getTargetNode(eInfo);
                    if (target.length() == 1){
                        return target;
                    }
                    if (target.length() > 1){
                        targetLst.put(target);
                    }
                }
            }
            if (!ndInfo.isNull("class")){
                elements = parserXml(DUMP_PATH, "class", ndInfo.getString("class"), 0);
                target = getTargetNode(elements, "class", bounds);
                if (target.length() == 1){
                    return target;
                }
                if (target.length() > 1){
                    targetLst.put(target);
                }
            }
            ArrayList<Integer> indexList = new ArrayList<>();
            logUtil.d("targetLst----------", targetLst + "");
            for (int i = 0; i< targetLst.length(); i++){
                try {
                    indexList.add(targetLst.getJSONObject(i).getInt("index"));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            logUtil.d("indexlist----------", indexList + "");
            logUtil.d("indexlist----------", Collections.min(indexList) + "");
            return targetLst.getJSONObject(indexList.indexOf(Collections.min(indexList)));

        } catch (Exception e) {
            logUtil.e("", e);

        }
        return ndInfo;
    }

    public static JSONObject getTargetNode(ArrayList<Element> elst, String attr,String targetBounds) throws JSONException {
        JSONObject targetNode = new JSONObject();
        if (elst.size() == 1){
            targetNode.put(attr, elst.get(0).attributeValue(attr));
        }
        if (elst.size() > 1){
            for (int i=0;i < elst.size();i++){
                String aBounds = elst.get(i).attributeValue("bounds");
                if (isInBounds(aBounds, targetBounds)){
                    targetNode.put(attr, elst.get(i).attributeValue(attr));
                    if (i > 0){
                        targetNode.put("index", i);
                    }
                    logUtil.d("getTargetNode", targetNode.toString());
                    break;
                }
            }
        }
        return targetNode;
    }

    public static JSONObject getTargetNode(JSONObject ndInfo) throws Exception {
        JSONObject targetNode = new JSONObject();
        Iterator<String> itr = ndInfo.keys();
        ArrayList<String> keys = new ArrayList<>();
        while (itr.hasNext()){
            String key = itr.next();
            if (key.equals("bounds") || key.equals("class")){
                continue;
            }
            String value = ndInfo.getString(key);
            ArrayList<Element> elementList = parserXml(DUMP_PATH, key, value, 0);
            int n = elementList.size();
            if (n == 1){
                targetNode.put(key, value);
            }
            if (n > 1){
                for (int i=0; i < n; i++){
                    String aBounds = elementList.get(i).attributeValue("bounds");
                    String bBounds = ndInfo.getString("bounds");
                    if (isInBounds(aBounds, bBounds)){
                        targetNode.put(key, value);
                        if (i > 0){
                            targetNode.put("index", i);
                        }
                        break;
                    }
                }
            }
        }
        if (targetNode.length() > 0){
            return targetNode;
        }
        return ndInfo;
    }

}
