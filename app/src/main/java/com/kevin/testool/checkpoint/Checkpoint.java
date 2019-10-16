package com.kevin.testool.checkpoint;
import android.os.Environment;
import com.kevin.testool.utils.AdbUtils;
import com.kevin.testool.MyFile;
import com.kevin.testool.common.Common;
import com.kevin.testool.logUtil;
import com.kevin.testool.ocr.Imagett;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.regex.*;

public abstract class Checkpoint extends Common {

    public static Boolean checkIfExist(Boolean refresh,String key, String value, int nex) {
        if (value.contains("|")){
            ArrayList<Boolean> res_lst = new ArrayList<>();
            for (String item: value.split("\\|")){
                res_lst.add(checkIfExist(refresh, key, item, nex));
                refresh = false;
            }
            return res_lst.contains(true);

        }
            if (key.length() == 0){
                if (refresh) {
                    AdbUtils.runShellCommand("am instrument -w -r   -e debug false -e class 'com.github.uiautomator.GetDumpTest' com.github.uiautomator.test/android.support.test.runner.AndroidJUnitRunner\n", 0);
                }
                try {
                    String content = MyFile.readFile(Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml");
                    if (content.contains(value)){
                        logUtil.i(TAG, "Pass|当前界面存在元素:" + value);
                        return true;
                    } else{
                        logUtil.i(TAG, "Fail|当前界面不存在元素:" + value);
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
//
                if (get_elements(refresh, key, value, nex) == null) {
                    logUtil.i(TAG, "Fail|当前界面不存在元素 " + key + ":" + value);
                    clickPopWindow();
                    return false;
                } else {
                    logUtil.i(TAG, "Pass|当前界面存在元素 " + key + ":" + value);
                    return true;
                }
            }
            return false;
    }

    public static Boolean checkIfExistByImage(String text, String language, boolean refreshImg){
        ArrayList<Boolean> result=new ArrayList<>();
        if (text.contains("|")) {
            ArrayList<Boolean> res_lst = new ArrayList<>();
            for (String item : text.split("\\|")) {
                res_lst.add(Imagett.imageToText(Common.screenImage(refreshImg), language, refreshImg).contains(item));
                refreshImg = false;
            }
            result.add(res_lst.contains(true));
        } else {
            result.add(Imagett.imageToText(Common.screenImage(refreshImg), language, refreshImg).contains(text));

        }
        return !result.contains(false);
    }

    public static Boolean checkIfNotExist(Boolean refresh ,String key, String value, int nex){
        if (!checkIfExist(refresh, key, value, nex)){
            logUtil.i(TAG,"Pass|当前界面不存在元素 "+ key + ":" +value);
            return true;
        }else {
            logUtil.i(TAG,"Fail|当前界面存在元素 "+ key + ":" +value);
            return false;
        }
    }

    public static boolean checkActivity(String targetAct){
        targetAct = targetAct.trim();
        logUtil.i(TAG, "期望activity：" + targetAct);
        String currentAct = Common.getActivity();
        logUtil.i(TAG, "当前activity：" + currentAct);
        if (targetAct.contains("|")) {
            ArrayList<Boolean> res = new ArrayList<>();
            String[] targetActs = targetAct.split("\\|");
            for (String item: targetActs) {
                res.add(checkActivity(item));
            }
            return res.contains(true);
        }
        if (currentAct.contains(targetAct)){
            logUtil.i(TAG, "Pass|当前activity符合预期");
            return true;
        } else {
            logUtil.i(TAG, "Fail|当前activity不符合预期");
            return false;

        }
    }

    public static boolean checkElementStatus(boolean refresh, String key, String value, int nex, int index, String exp_attr, String exp_sts){

        if (value.contains("|")) {
            ArrayList<Boolean> res_lst = new ArrayList<>();
            for (String item : value.split("\\|")) {
                res_lst.add(checkElementStatus(refresh, key, item, nex, index, exp_attr, exp_sts));
                refresh = false;
            }
            return res_lst.contains(true);
        }
        String status;
        ArrayList<Element> element = Common.get_elements(refresh, key, value, nex);
        if (element == null){
            return false;
        }else {
            try {
                status = element.get(index).attribute(exp_attr).getValue();
                if (status.equals(exp_sts)) {
                    logUtil.i(TAG, "Pass|控件状态：" + exp_sts + "|符合预期");
                    return true;
                } else {
                    logUtil.i(TAG, "Fail|控件状态：" + exp_sts + "|不符合预期");
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return false;
            }

            }

    }

    public static ArrayList<Boolean> muitiCheck(JSONObject check_point, Boolean refresh) throws IOException, JSONException {
        ArrayList<Boolean> result=new ArrayList<>();
//        if (checkNetwork()){
//            return true;
//        }

        String id = "";
//        Boolean refresh = true;

        if (!check_point.isNull("text")) {
            try {
                JSONArray text = check_point.getJSONArray("text");
                if (text.length() > 0){
                    result.add(checkIfExist(true,"", text.getString(0), 0));
                    for (int i = 1; i < text.length(); i++) {
                        result.add(checkIfExist(false,"", text.getString(i), 0));
                    }
                    refresh = false;
                }

            } catch (Exception e) {
                String text = check_point.getString("text");
//                System.out.println(text);
                if (text.length() > 0){
                    result.add(checkIfExist(true,"", text, 0));
                    refresh = false;
                }

            }
        }

        if (!check_point.isNull("resource-id")){
            id = check_point.getString("resource-id");
            if (id.length()>0){
                result.add(checkIfExist(refresh,"", id, 0));
                refresh = false;
            }

        }

        if (!check_point.isNull("id")){
            id = check_point.getString("id");
            if (id.length()>0){
                result.add(checkIfExist(refresh,"", id, 0));
                refresh = false;
            }

        }

        if (!check_point.isNull("activity")){
            String activity = check_point.getString("activity");
            if (activity.length() > 0){
                result.add(checkActivity(activity));
            }
        }
        if (!check_point.isNull("nd")){
            String nd = check_point.getString("nd");
            if (nd.length() > 0){
                result.add(checkIfNotExist(refresh,"", nd,  0));
                refresh = false;
            }
        }
        if (!check_point.isNull("status")){
            String sText = "";
            String sContent = "";
            JSONObject status = check_point.getJSONObject("status");
            Iterator<String> itr = status.keys();
            ArrayList<String> keys = new ArrayList<>();
            while (itr.hasNext()){
                keys.add(itr.next());
            }
            int nex = 0, index = 0;
            if (keys.contains("nex")){
                nex = (int) status.get("nex");
                keys.remove("nex");
            }
            if (keys.contains("index")){
                index = (int) status.get("index");
                keys.remove("index");
            }
            if (keys.contains("s_id")){
                id = status.getString("s_id");
                keys.remove("s_id");
            }
            if (keys.contains("s_text")){
                sText = status.getString("s_text");
                keys.remove("s_text");
            }
            if (keys.contains("s_content")){
                sContent = status.getString("s_content");
                keys.remove("s_content");
            }
//            for (int i = 0; i < status.length(); i++) {
//                String key = status.keys().next();
            for (String key:keys){
                String value = (String) status.get(key);
                if (id.length() > 0) {
                    result.add(checkElementStatus(refresh, "resource-id", id, nex, index, key, value));
                }
                if (sText.length() > 0){
                    result.add(checkElementStatus(true, "text", sText, nex, index, key, value));
                }
                if (sContent.length() > 0){
                    result.add(checkElementStatus(refresh, "content", sContent, nex, index, key, value));
                }
                refresh = false;
            }
        }
        if (!check_point.isNull("delta")){
            JSONObject delta = check_point.getJSONObject("delta");
            result.add(checkFileCountDiff(delta));


        }
        // ocr 识别当前界面文本
        if (!check_point.isNull("img")) {
            String language = "chi_sim";
            JSONObject img = check_point.getJSONObject("img");
            Iterator<String> itr = img.keys();
            ArrayList<String> keys = new ArrayList<>();
            while (itr.hasNext()) {
                keys.add(itr.next());
            }
            if (keys.contains("language")){
                language = img.getString("language");
            }
            if (keys.contains("text")) {
                try {
                    JSONArray text = img.getJSONArray("text");
                    result.add(checkIfExistByImage(text.getString(0), language, true));
                    if (text.length() > 0){
                        for (int i = 1; i < text.length(); i++) {
                            result.add(checkIfExistByImage(text.getString(i), language, false));
                        }

                    }
                } catch (Exception e) {
                    String text = img.getString("text");
                    result.add(checkIfExistByImage(text, language, true));
                }
            }
        }
        // 判断logcat内的打点
        if (!check_point.isNull("logcat")){
            String logcat = check_point.getString("logcat");
            String res = AdbUtils.runShellCommand("logcat -d | grep '" + logcat + "'", 0 );
            result.add(res.contains(logcat));
        }
//        for (int i=0; i < result.size(); i++){
//            if (!result.get(i)){
//                return false;
//            }
//
//        }
//        return true;
        return result;
    }

    public static void clickPopWindow() {
        String[] popList = new String[]{"同意并继续", "允许", "确定", "同意", "继续", "好", "暂不升级", "跳过", "立即体验", "知道了", "我知道了", "更新", "立即开通", "我同意",
                "继续安装", "接受", "以后再说", "同意并使用", "您已阅读并同意", "同意并加入"};
        Boolean refresh = true;
        for (String pop: popList) {
            if (Common.get_elements(refresh, "text", pop, 0) != null) {
                Common.click_element(false, "text", pop, 0, 0);
                refresh = true;
//                logUtil.i("", "点击弹框文本：" + pop);
            } else {
                refresh = false;
            }
        }
    }

    public static boolean checkPackageExist(String app) {
        String pkg = "";
        try {
            pkg = CONFIG().getJSONObject("APP").getString(app);
        } catch (JSONException e) {
//            e.printStackTrace();
            return true;
        }
        if (!pkg.equals("")) {
            String res = AdbUtils.runShellCommand("pm list packages | grep " + pkg + "\n", 0);
            if (res.contains("package:")) {
                System.out.println(res.replace("package:", "").trim().equals(pkg));
                return res.replace("package:", "").trim().equals(pkg);
            } else {
                return false;
            }
        }
        return true;
    }

    public static void sendAlarm(final String tag){
        //todo 定义报警逻辑

    }

    public static boolean checkFileCountDiff(JSONObject delta) throws JSONException {
        int cbt = delta.getInt("cbt");
        String regex = delta.getString("file_re");
//        System.out.println(regex);
        String res = AdbUtils.runShellCommand("ls "+delta.getString("path")+ "\n", 0);
//        System.out.println(res);
        Pattern p = Pattern.compile(regex);
        Matcher matcher =p.matcher(res);
        int cat = 0;
        while (matcher.find()) {
            cat++;
        }
//        System.out.println(cat);
        int diff = delta.getInt("diff");
        int _diff = cat - cbt;
        if (_diff == diff){
            logUtil.i("", "Pass| diff=" + _diff + "符合预期");
            return true;
        } else{
            logUtil.i("", "Fail| diff=" + _diff + "不符合预期");
            return false;
        }

    }


}
