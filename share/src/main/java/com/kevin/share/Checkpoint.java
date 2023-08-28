package com.kevin.share;

import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;

import com.kevin.share.ocr.Imagett;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.CvUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.logUtil;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kevin.share.CONST.SERVER_BASE_URL;
import static com.kevin.share.utils.StringUtil.isNumeric;
import static com.kevin.share.utils.CvUtils.getMatchImgByTemplate;
import static com.kevin.share.utils.CvUtils.getMatchImgByFeature;

public class Checkpoint extends Common {

    public static boolean isCheckPageElem = false; // 检测点是否包含界面元素, 根据此参数判断是否需要动态检查
    public static JSONObject failInfoDetail = new JSONObject();
    private static String inFile = "";
    private static String imageText = "";

    private static void putFailInfo(String key, String value){
        String msg = "";
        try {
            if (key.equals("element")){
                if (failInfoDetail.isNull("element")){
                    msg = "界面元素不存在:" + value;
                } else {
                    msg = failInfoDetail.getString("element");
                    if (!msg.contains(value)){
                        msg = msg + "|" + value;
                    }
                }
            } else {
                msg = value;
            }
            failInfoDetail.put(key, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static Boolean checkIfExist(Boolean refresh, String key, String value, int nex) {
        if (value.contains("|")){
            ArrayList<Boolean> res_lst = new ArrayList<>();
            for (String item: value.split("\\|")){
                boolean res = checkIfExist(refresh, key, item, nex);
                res_lst.add(res);
                refresh = false;
                if (res){
                    break;
                }
            }
            return res_lst.contains(true);

        }
        if (key.length() == 0 || key.equals("txt")){ // txt 表示界面显示的文本内容，包括text, content-desc等文字内容
            if (refresh) {
                get_elements(refresh, "", "", 0);
            }
            try {
                String content = FileUtils.readFile(Environment.getExternalStorageDirectory().getPath() + File.separator + "window_dump.xml");
                if (content.contains(value)){
                    logUtil.i(TAG, "true|当前界面存在元素:" + value);
                    return true;
                    // 对于webview页面进行ocr识别
                } else //if (content.contains("android.webkit.WebView")){
                        if (key.equals("txt")){
                            return checkIfExistByImage(value, "chi_sim", inFile, refresh, "");
                        }
                        logUtil.i(TAG, "false|当前界面不存在元素:" + value);

                    putFailInfo("element", value);
                    return false;
//                }
            } catch (IOException e) {
                logUtil.e("", e);
            }
        } else {
//
            if (get_elements(refresh, key, value, nex).size() == 0) {
                logUtil.i(TAG, "false|当前界面不存在元素 " + key + ":" + value);
                try {
                    clickPopWindow(false);
                } catch (JSONException e) {
                    logUtil.e("", e);
                }
                putFailInfo("element", value);
                return false;
            } else {
                logUtil.i(TAG, "true|当前界面存在元素 " + key + ":" + value);
                return true;
            }
        }
        logUtil.i(TAG, "false|当前界面不存在元素 " + key + ":" + value);
        putFailInfo("element", value);
        return false;
    }

    public static Boolean checkShellRes(JSONObject shellJson){
        String shellCmd = "";
        String expectRes = "";
        String mode = "";
        shellCmd = shellJson.optString("cmd");
        expectRes = shellJson.optString("result");
        mode = shellJson.optString("mode","contain");
        String res = ShellUtils.runShellCommand(shellCmd, 0);
        logUtil.i("", "执行命令" + shellCmd + "结果为:" + res);
        boolean resOfMode;
        if (mode.equals("==")) {
            resOfMode = res.equals(expectRes);
        } else if (mode.equals("!=")){
            resOfMode = !res.equals(expectRes);
        } else {
            resOfMode = res.contains(expectRes);
        }
        if (!TextUtils.isEmpty(res)) {
            if (resOfMode){
                logUtil.i("","true|shell执行结果" + res + mode + expectRes);
                return true;
            }else{
                logUtil.i("","false|shell执行结果" + res + mode + expectRes + "不成立");
                putFailInfo("shell", shellCmd + " 执行结果" + res + mode + expectRes + "不成立");
                return false;
            }
        } else {
            logUtil.i("",shellCmd + "执行结果为空");
            putFailInfo("shell", shellCmd + " 执行结果为空");
            return false;
        }

    }

    public static Boolean checkIfExistByImage(String text, String language, String imageFile, boolean refreshImg, String bounds){
        logUtil.d("", imageFile);
        ArrayList<Boolean> result=new ArrayList<>();
        // 输出图片文件
//        String outFileName = dateFormat.format(new Date()) + ".jpg";
//        String outFile = imageFile;
//        try {
//            outFile = REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + outFileName;
//        } catch (IOException e) {
//            logUtil.e("", e);
//        }
        if (!TextUtils.isEmpty(bounds)) {
            Common.cropeImage(imageFile, bounds);
        }
        // 先通过接口识别文本
//        if (refreshImg || TextUtils.isEmpty(imageText)) {
//            imageText = CvUtils.getTextFromImage("", imageFile, imageFile, 0);
//        }
        // 如果接口不能访问通过本地的ocr识别文本
        if (imageText.length() == 0 || imageText.equals("null")){
            try {
                if (new File(imageFile).exists()){
                    imageText = Imagett.imageToText(imageFile, language, true, bounds);
                }
            } catch (Exception e){
                logUtil.e("", e);
                imageText = "";
            }
        }
        if (text.contains("|")) {
            ArrayList<Boolean> res_lst = new ArrayList<>();
            for (String item : text.split("\\|")) {
                res_lst.add(imageText.contains(item));
            }
            result.add(res_lst.contains(true));
        } else {
            result.add(imageText.contains(text));

        }
        if (!result.contains(false)){
            logUtil.i(TAG,"true|OCR识别当前界面存在：" + text);
            return true;
        } else {
            putFailInfo("text", text);
            logUtil.i(TAG,"false|OCR识别当前界面不存在：" + text);
            return false;
        }
    }

    public static Boolean checkIfNotExist(Boolean refresh ,String key, String value, int nex){
        if (!checkIfExist(refresh, key, value, nex)){
            logUtil.i(TAG,"true|nd结果：当前界面不存在元素"+ key + ":" +value);
            return true;
        }else {
            logUtil.i(TAG,"false|nd结果：当前界面存在元素 "+ key + ":" +value);
            putFailInfo("element", value);
            return false;
        }
    }

    public static boolean checkActivity(String targetAct){
        targetAct = targetAct.trim();
        logUtil.i(TAG, "期望activity：" + targetAct);
        String currentAct = Common.getActivity();
        if (currentAct.contains("/.")){
            currentAct = currentAct + "&" + currentAct.replace("/", "");

        }
        logUtil.i(TAG, "当前activity：" + currentAct);
        ArrayList<Boolean> res = new ArrayList<>();
        if (targetAct.contains("|")) {
            String[] targetActs = targetAct.split("\\|");
            for (String item: targetActs) {
                logUtil.i("actitity", item);
                res.add(compareActivity(currentAct, item));
            }
        } else {
            res.add(compareActivity(currentAct, targetAct));
        }
        return res.contains(true);

    }

    public static boolean compareActivity(String currentAct, String targetAct){
        if (currentAct.toLowerCase().contains(targetAct.toLowerCase())){
            logUtil.i(TAG, "true|当前activity符合预期");
            return true;
        } else {
            logUtil.i(TAG, "false|当前activity不符合预期");
            putFailInfo("activity", "activity不符合预期");
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
            putFailInfo("status",String.format("界面元素%s:%s 不存在", key, value) );
            return false;
        }else {
            try {
                status = element.get(index).attribute(exp_attr).getValue();
                if (status.equals(exp_sts)) {
                    logUtil.i(TAG, "true|控件状态：" + exp_sts + "|符合预期");
                    return true;
                } else {
                    logUtil.i(TAG, "false|控件状态：" + exp_sts + "|不符合预期");
                    putFailInfo("status",String.format("界面元素%s:%s的当前属性%s不符合预期%s", key, value, status, exp_sts) );
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return false;
                }
            }
    }

    public static boolean checkAsrResult(String asrResult, String targetTxt) {
        if (targetTxt.length() > 0){
            if (targetTxt.contains("|")) {
                ArrayList<Boolean> res_lst = new ArrayList<>();
                for (String item : targetTxt.split("\\|")) {
                    res_lst.add(checkAsrResult(asrResult, item));
                }
                return res_lst.contains(true);
            } else {
                if (asrResult.contains(targetTxt)) {
                    logUtil.i("", "true| asr识别结果中存在：" + targetTxt);
                    return true;
                } else {
                    logUtil.i("", "false| asr识别结果中不存在：" + targetTxt);
                    putFailInfo("asr", "asr识别结果不存在" + targetTxt);
                    return false;
                }
            }

        }
        return true;
    }

    public static void clickPopWindow(boolean refresh) throws JSONException {
        JSONArray popList = new JSONArray("[\"同意并继续\", \"允许\", \"确定\", \"同意\", \"继续\", \"好\", \"暂不升级\", \"跳过\", \"立即体验\", \"知道了\", \"我知道了\", \"更新\", \"立即开通\", \"我同意\",\"继续安装\", \"接受\", \"以后再说\", \"同意并使用\", \"您已阅读并同意\", \"同意并加入\"]");
        try {
            popList = CONFIG().getJSONArray("POP_WINDOW_LIST");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        click_pop_window(refresh, popList);
    }

    public static boolean checkFileCountDiff(JSONObject delta) throws JSONException {
        int cbt = delta.getInt("cbt");
        String regex = delta.getString("file_re");
//        System.out.println(regex);
        String res = ShellUtils.runShellCommand("ls "+delta.getString("path"), 0);
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
            logUtil.i("", "true| diff=" + _diff + "符合预期");
            return true;
        } else{
            logUtil.i("", "false| diff=" + _diff + "不符合预期");
            putFailInfo("diff", "文件数目diff:" + _diff + "不符合预期");
            return false;
        }

    }

    public static boolean checkToast(String toast) throws IOException {
        String mtoast = getLastToast();
        if (mtoast.length() > 0){
            return mtoast.contains(toast);
        }
        putFailInfo("toast", "toast:" + mtoast + "不符合预期:" + toast);
        return false;
    }

    // 综合检查
    public static ArrayList<Boolean> muitiCheck(JSONObject check_point, boolean refresh) throws IOException, JSONException {
        isCheckPageElem = false; //初始化
        ArrayList<Boolean> result=new ArrayList<>();
//        if (checkNetwork()){
//            return true;
//        }
        String id = "";
        String targetBounds = "";
        if (!check_point.isNull("bounds")) {
            targetBounds = check_point.getString("bounds");
        }

        if (!check_point.isNull("image")){
            SystemClock.sleep(1500);
            JSONObject image;
            double limit = 0.98;
            double similarity = 0.98;
            String method = "";
            try {
                image = check_point.getJSONObject("image");
            } catch (JSONException e){
//                logUtil.e("check_point.muitiCheck.image", e);
                image = new JSONObject().put("src", check_point.getString("image"));
            }
            if (refresh || TextUtils.isEmpty(inFile)) {
                inFile = Common.screenShot2();
            }

            if (image.length() > 0){
                if (TextUtils.isEmpty(targetBounds)) {
                    targetBounds = get_bounds(refresh, image);
                }

                if (!image.isNull("bounds")){
                    targetBounds = image.getString("bounds");
                    targetBounds = parseRect(parseBounds(targetBounds));
//                    keys.remove("bounds");
                }
                if (!image.isNull("limit")){
                    limit = image.getDouble("limit");
                }

                if (!image.isNull("method")){
                    method = image.getString("method");
                }

                if (!image.isNull("similarity")){
                    similarity = image.getDouble("similarity");
                }

                if (!image.isNull("src")){
                    String image_src_base64 = image.getString("src");
                    if (image_src_base64.length() < 20 ){
                        if (isNumeric(image_src_base64)) {
                            image_src_base64 = HttpUtil.getResp(SERVER_BASE_URL + "image?id=" + image_src_base64);
                        } else {
                            image_src_base64 = HttpUtil.getResp(SERVER_BASE_URL + "image?tag=" + image_src_base64);// 标签匹配
                        }
                    }
                    try {
//                        logUtil.d("", image_src_base64);
                        String templateFile = FileUtils.base64ToFile(image_src_base64, inFile.replace(".", "_m."));
                        String resBounds = null;
                        String outFile = inFile.replace(".", "_out.");

                        // 通过图像识别服务进行图像匹配
                        if (Common.isNetworkConnected(AppContext.getContext())) {
                            JSONObject param = new JSONObject();
                            param.put("src_base64", FileUtils.xToBase64(inFile));
                            param.put("sch_base64", image_src_base64);
                            param.put("target_bounds", parseRect(parseBounds(targetBounds)));
                            param.put("resize", 1);
                            param.put("method", method);
                            param.put("threshold", limit);
                            param.put("similarity", similarity);
                            param.put("index", -1);
                            JSONObject res = CvUtils.getMatchImgByApi(param);
                            if (res != null) {
                                if (res.getInt("code") == 200) {
                                    if (res.getBoolean("success")) {
                                        resBounds = res.getString("res_bounds");
                                        logUtil.d("", "使用图像识别服务");
                                    }
                                    FileUtils.base64ToFile(res.getString("image"), outFile);
                                }
                            }
                        }
                        // 本地图像识别方法
                        if (resBounds == null) {
                            logUtil.i("", "使用本地图像识别");
                            if (method.equals("color") || method.equals("colour")){
                                result.add(CvUtils.isSimilarColor(Imgcodecs.imread(inFile), Imgcodecs.imread(templateFile), targetBounds, similarity));
                            }

                            if (method.equals("contour")) {
                                result.add(CvUtils.isSimilarContour(Imgcodecs.imread(inFile), Imgcodecs.imread(templateFile), targetBounds, similarity));
                            }

                            if (method.length() == 0 || method.equals("template")) {
                                resBounds = getMatchImgByTemplate(inFile, templateFile, outFile, Imgproc.TM_CCOEFF_NORMED, targetBounds, -1, limit, similarity);
                                result.add(!TextUtils.isEmpty(resBounds));
                            }
                            if (method.equals("sift")) {
                                resBounds = getMatchImgByFeature(inFile, templateFile, outFile, method, targetBounds, similarity);
                                result.add(!TextUtils.isEmpty(resBounds));
                            }
                        }


                    } catch (Exception e) {
                        logUtil.e("", e);
                    }
                }

                if (!image.isNull("text")){
                    Object txt = image.get("text");
                    String language = "chi_sim";
                    isCheckPageElem = true;
                    if (!image.isNull("language")){
                        language = image.getString("language");
                    }
                    if (targetBounds.length() > 0 && refresh) {
                        cropeImage(inFile, targetBounds);
                    }
                    if (txt instanceof JSONArray){
                        for (int i = 0; i < ((JSONArray) txt).length(); i++) {
                            if (i==0) {
                                result.add(checkIfExistByImage(((JSONArray) txt).getString(i), language, inFile, refresh, ""));
                                continue;
                            }
                            result.add(checkIfExistByImage(((JSONArray) txt).getString(i), language, inFile, false, ""));
                        }
                    } else {
                        result.add(checkIfExistByImage(txt.toString(), language, inFile,refresh, ""));
                    }

                }
            }
        }

        if (!check_point.isNull("text")) {
            String _key = (refresh ? "": "txt") ; // 此转化逻辑为动态检测中在最后一次检查中通过ocr进行结果兜底
            try {
                JSONArray text = check_point.getJSONArray("text");
                if (text.length() > 0){
                    isCheckPageElem = true;
                    result.add(checkIfExist(refresh, _key, text.getString(0), 0));
                    refresh = false;
                    for (int i = 1; i < text.length(); i++) {
                        result.add(checkIfExist(refresh, _key, text.getString(i), 0));
                    }

                }

            } catch (Exception e) {
                String text = check_point.getString("text");
                if (text.length() > 0) {
                    isCheckPageElem = true;
                    result.add(checkIfExist(refresh, _key, text, 0));
                    refresh = false;
                }

            }

        }

        if (!check_point.isNull("txt")) {
            try {
                JSONArray text = check_point.getJSONArray("txt");
                if (text.length() > 0) {
                    isCheckPageElem = true;
                    result.add(checkIfExist(refresh, "txt", text.getString(0), 0));
                    refresh = false;
                    for (int i = 1; i < text.length(); i++) {
                        result.add(checkIfExist(refresh, "txt", text.getString(i), 0));
                    }
                }

            } catch (Exception e) {
                String text = check_point.getString("txt");
                if (text.length() > 0) {
                    isCheckPageElem = true;
                    result.add(checkIfExist(refresh, "txt", text, 0));
                    refresh = false;
                }

            }
        }

        if (!check_point.isNull("resource-id")){
            id = check_point.getString("resource-id");
            if (id.length()>0){
                isCheckPageElem = true;
                result.add(checkIfExist(refresh,"", id, 0));
                refresh = false;
            }

        }

        if (!check_point.isNull("id")){
            id = check_point.getString("id");
            if (id.length()>0){
                isCheckPageElem = true;
                result.add(checkIfExist(refresh,"", id, 0));
                refresh = false;
            }

        }

        if (!check_point.isNull("activity")){
            String activity = check_point.getString("activity");
            if (activity.length() > 0){
                isCheckPageElem = true;
                result.add(checkActivity(activity));
            }
        }

        if (!check_point.isNull("nd")){
            try {
                JSONArray nd = check_point.getJSONArray("nd");
                if (nd.length() > 0){
                    isCheckPageElem = true;
                    result.add(checkIfNotExist(refresh,"", nd.getString(0), 0));
                    for (int i = 1; i < nd.length(); i++) {
                        result.add(checkIfExist(false,"", nd.getString(i), 0));
                    }
                    refresh = false;
                }

            } catch (JSONException e){
                String nd = check_point.getString("nd");
                if (nd.length() > 0){
                    isCheckPageElem = true;
                    result.add(checkIfNotExist(refresh,"", nd,  0));
                    refresh = false;
                }
            }

        }
        if (!check_point.isNull("toast")){
            String toast = check_point.getString("toast");
            if (toast.length()>0) {
                isCheckPageElem = true;
                result.add(checkToast(toast));
            }
        }

        if (!check_point.isNull("status")){
            String sText = "";
            String sContent = "";
            JSONObject status = check_point.getJSONObject("status");
            if(status.length() > 0){
                isCheckPageElem = true;
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
                for (String key:keys) {
                    String value = (String) status.get(key);
                    if (id.length() > 0) {
                        result.add(checkElementStatus(refresh, "resource-id", id, nex, index, key, value));
                    }
                    if (sText.length() > 0) {
                        result.add(checkElementStatus(refresh, "text", sText, nex, index, key, value));
                    }
                    if (sContent.length() > 0) {
                        result.add(checkElementStatus(refresh, "content", sContent, nex, index, key, value));
                    }
                    refresh = false;
                }
            }
        }

        if (!check_point.isNull("delta")){
            JSONObject delta = check_point.getJSONObject("delta");
            result.add(checkFileCountDiff(delta));


        }
        // ocr 识别当前界面文本
        if (!check_point.isNull("ocr")) {
            String language = "chi_sim";
            JSONObject ocr = new JSONObject();
            try {
                ocr = check_point.getJSONObject("ocr");
            } catch (JSONException e){
                ocr.put("text", new JSONArray(check_point.getString("ocr")));
            }
            if(ocr.length() > 0){
                isCheckPageElem = true;
                if (!ocr.isNull("language")){
                    language = ocr.getString("language");
                }
                if (!ocr.isNull("text")) {
                    Object txt = ocr.get("text");
                    isCheckPageElem = true;
                    if (targetBounds.length() > 0 && refresh) {
                        cropeImage(inFile, targetBounds);
                    }
                    if (txt instanceof JSONArray){
                        for (int i = 0; i < ((JSONArray) txt).length(); i++) {
                            if (i==0) {
                                result.add(checkIfExistByImage(((JSONArray) txt).getString(i), language, inFile, refresh, ""));
                                continue;
                            }
                            result.add(checkIfExistByImage(((JSONArray) txt).getString(i), language, inFile, false, ""));
                        }
                    } else {
                        result.add(checkIfExistByImage(txt.toString(), language, inFile,refresh, ""));
                    }
                }
            }
        }
        // 判断logcat内的打点
        if (!check_point.isNull("logcat")){
            String logcat = check_point.getString("logcat");
            String res = ShellUtils.runShellCommand("logcat -d | grep '" + logcat + "'", 0 );
            result.add(!TextUtils.isEmpty(res));
        }


        // 判断shell指令结果
        if (!check_point.isNull("shell")){
            try {
                JSONObject shellJson = check_point.optJSONObject("shell");
                result.add(checkShellRes(shellJson));
            }catch (Exception e){
                logUtil.e("",e);
            }
        }

        return result;
    }


}
