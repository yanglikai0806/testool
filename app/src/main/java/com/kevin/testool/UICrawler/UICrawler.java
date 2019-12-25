package com.kevin.testool.UICrawler;

import android.os.SystemClock;

import com.kevin.testool.utils.AdbUtils;
import com.kevin.testool.CONST;
import com.kevin.testool.MyFile;
import com.kevin.testool.common.Common;
import com.kevin.testool.utils.logUtil;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class UICrawler extends Common {
    public String UICRAWLER_PATH = CONST.LOGPATH + "UICrawler";
    public String PKG;
    public int pageIndex = 0;
    public JSONObject pageInfo;
    public int clickCount = 0;
    public JSONArray exSteps = new JSONArray();
    public String version;
    private long sdTime = System.currentTimeMillis();//记录开始时间

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;


    public UICrawler(String app_package, String versionName){
        PKG = app_package;
        version = versionName;


    }

    public ArrayList<Element> getPageSrc(int index){
        ArrayList<Element> pageElements = new ArrayList<>();
        ArrayList<Element> elements = get_elements(true, "", "", 0);
        if (elements != null) {
            MyFile.copyFile(new File(CONST.DUMP_PATH), CONST.LOGPATH + "UICrawler" + File.separator + "page"+index+".xml");
            AdbUtils.runShellCommand("screencap -p " + CONST.LOGPATH + "UICrawler" + File.separator + "page"+index+".png", 0);
            for (Element e : elements) {
                if (e.attribute("package").getValue().equals(PKG)){
                    pageElements.add(e);
                }

            }
        }
        return pageElements;
    }
    public ArrayList<Element> getPageSrc(){
        ArrayList<Element> pageElements = new ArrayList<>();
        ArrayList<Element> elements = get_elements(true, "", "", 0);
        if (elements != null) {
            for (Element e : elements) {
                if (e.attribute("package").getValue().equals(PKG)){
                    pageElements.add(e);
                }

            }
        }
        return pageElements;
    }

    private ArrayList<Element> getPageSrcByFile(int index){
        ArrayList<Element> pageElements = new ArrayList<>();
        File pageXml = new File(CONST.LOGPATH + "UICrawler" + File.separator + "page"+index+".xml");
        if (pageXml.exists()){
            try {
                ArrayList<Element> elements =  parserXml(pageXml.toString(), "", "", 0);
                for (Element e : elements) {
                    if (e.attribute("package").getValue().equals(PKG)){
                        pageElements.add(e);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return pageElements;
    }


    private ArrayList<String> elemtAttrVals(Element element){
        ArrayList<String> AttrVals = new ArrayList<>();
        AttrVals.add(element.attributeValue("text"));
        AttrVals.add(element.attributeValue("resource-id"));
        AttrVals.add(element.attributeValue("class"));
        AttrVals.add(element.attributeValue("package"));
        AttrVals.add(element.attributeValue("content-desc"));
        AttrVals.add(element.attributeValue("checkable"));
        AttrVals.add(element.attributeValue("checked"));
        AttrVals.add(element.attributeValue("clickable"));
        AttrVals.add(element.attributeValue("enabled"));
        AttrVals.add(element.attributeValue("focusable"));
        AttrVals.add(element.attributeValue("focused"));
        AttrVals.add(element.attributeValue("scrollable"));
        return AttrVals;
    }

    public boolean isSamePage(ArrayList<Element> page1, ArrayList<Element> page2){
        // 判断当前页面的包名，activity，以及头部，底部，和中间三个元素的情况，综合判定界面是否变化
        ArrayList<Boolean> isSame = new ArrayList<>();
        for (int i=0; i < Math.min(page1.size(),page2.size()); i++){
            isSame.add(elemtAttrVals(page1.get(i)).equals(elemtAttrVals(page2.get(i))));
            if (Collections.frequency(isSame, false)>isSame.size()/2){
                break;
            }
        }
        return Collections.frequency(isSame, false)<isSame.size()/2;

    }

    public JSONObject UICrawlerConfig() throws JSONException {
        return new JSONObject(MyFile.readJsonFile(CONST.UICRAWLWER_CONFIG_FILE));
    }

    private JSONArray elemBlackList(){
        try {
            return UICrawlerConfig().getJSONArray("ELEM_BLACK");
        } catch (JSONException e) {
            return new JSONArray();
        }

    }

    private JSONArray elemWhiteList(){
        try {
            return UICrawlerConfig().getJSONArray("ELEM_WHITE");
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private boolean isBlack(Element elem) throws JSONException {
        ArrayList<Boolean> result = new ArrayList<>();
        JSONArray elemBlacks = elemBlackList();
        for (int i=0; i<elemBlacks.length(); i++){
            JSONObject attr = elemBlacks.getJSONObject(i);
            Iterator<String> itr = attr.keys();
            while (itr.hasNext()){
                String key = itr.next();
                if (key.equals("bounds")){
                    result.add(isInBounds(attr.getString("bounds"), elem.attribute("bounds").getValue()));
                } else {
                    result.add(attr.getString(key).equals(elem.attribute(key).getValue()));
                }

            }
        }
        return result.contains(true);
    }

    private boolean isWhite(Element elem) throws JSONException {
        ArrayList<Boolean> result = new ArrayList<>();
        JSONArray elemWhites = elemWhiteList();
        for (int i=0; i<elemWhites.length(); i++){
            JSONObject attr = elemWhites.getJSONObject(i);
            Iterator<String> itr = attr.keys();
            while (itr.hasNext()){
                String key = itr.next();
                if (key.equals("bounds")){
                    result.add(isInBounds(attr.getString("bounds"), elem.attribute("bounds").getValue()));
                } else {
                    result.add(attr.getString(key).equals(elem.attribute(key).getValue()));
                }

            }
        }
        return result.contains(true);
    }

    private boolean isInBounds(String s1, String s2){
        String[] b1 = s1.replace("[", "").replace("]", ",").split(",");
        String[] b2 = s2.replace("[", "").replace("]", ",").split(",");
        int x = (Integer.parseInt(b2[0]) + Integer.parseInt(b2[2])) / 2;
        int y = (Integer.parseInt(b2[1]) + Integer.parseInt(b2[3])) / 2;

        return (Integer.parseInt(b1[0]) < x) & (x< Integer.parseInt(b1[2])) & (Integer.parseInt(b1[1]) < y) & (y < Integer.parseInt(b1[3]));
    }

    private boolean isClickable(Element elem) throws JSONException {
        if (UICrawlerConfig().getString("ELEM_TYPE").equals("clickable")) {
            return elem.attribute("clickable").getValue().equals("true");
        }
        return true;
    }

    private JSONObject clickRecord(Element elem){
        JSONObject ce = new JSONObject();
        String te = elem.attributeValue("text");
        String re = elem.attributeValue("resource-id");
        String co = elem.attributeValue("content-desc");
        String cl = elem.attributeValue("class");
        try {
            ce.put("text", te);
            ce.put("resource-id", re);
            ce.put("content-desc", co);
            ce.put("class", cl);
            if (te.equals("") & re.equals("") & co.equals("")){
                ce.put("bounds", elem.attributeValue("bounds"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("clickRecord:" + ce.toString());
        return ce;
    }

    public void guideStep(){
        try {
            logUtil.d("", "guide step ");
            execute_step(UICrawlerConfig().getJSONArray("GUIDE_STEP"), new JSONArray().put(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void lastStep(){
        try {
            execute_step(UICrawlerConfig().getJSONArray("LAST_STEP"), new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public void crawlerPage(int maxDeep) throws JSONException, IOException {
        String[] x_y;
        String points;
        ArrayList<Element> page1 = getPageSrc(pageIndex);
        for (Element elem: page1) {
            if (isWhite(elem) | (!isBlack(elem) && isClickable(elem))) {
                points = elem.attribute("bounds").getValue();
                x_y = points.replace("[", "").replace("]", ",").split(",");
                int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
                int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
                // 开启检查crash /anr
                if (System.currentTimeMillis() - sdTime > 10000){
                    ErrorDetect.startDetectCrashAnr(UICRAWLER_PATH);
                    sdTime = System.currentTimeMillis();
                }
                click(x, y);
                SystemClock.sleep(1000);

                String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                String logFolder = UICRAWLER_PATH + File.separator + version + File.separator + dateFormat.format(new Date());

                if (ErrorDetect.isDetectCrash(UICRAWLER_PATH)){
                    if (!new File(logFolder).exists()){
                        new File(logFolder).mkdirs();
                    }
                    MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");

                    generateBugreport(logFolder + File.separator + bugreportFile);
                    for (int i=0; i<pageIndex+1; i++){
                        MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                    }
                } else if (ErrorDetect.isDetectAnr(UICRAWLER_PATH)){
                    if (!new File(logFolder).exists()){
                        new File(logFolder).mkdirs();
                    }
                    MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");
                    generateBugreport(logFolder + File.separator + bugreportFile);
                    for (int i=0; i<pageIndex+1; i++){
                        MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                    }
                }

                // 记录点击坐标
                try {
                    MyFile.writeFile(CONST.LOGPATH + "record.txt", pageIndex + ":" +x+","+y+"\n", true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                clickCount++;
                exSteps.put(pageIndex,clickRecord(elem)); //记录执行步骤

                logFolder = UICRAWLER_PATH + File.separator + version + File.separator + dateFormat.format(new Date());
                if (isSamePage(page1, getPageSrc())) {
                    // 检查界面问题
                    if (ErrorDetect.muitiCheck(UICrawlerConfig().getJSONObject("CHECK_POINT"), false).contains(true)){
                        if (!new File(logFolder).exists()){
                            new File(logFolder).mkdirs();
                        }
                        MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");
                        generateBugreport(logFolder + File.separator + bugreportFile);
                        for (int i=0; i<pageIndex+1; i++){
                            MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                        }
                    }
                    //
                    logUtil.d("crawler","界面未变化，继续遍历当前页面");
                } else {
                    logUtil.d("crawler","界面变化，操作新页面");
                    pageIndex++;
                    // 检查界面问题
                    if (ErrorDetect.muitiCheck(UICrawlerConfig().getJSONObject("CHECK_POINT"), false).contains(true)){
                        if (!new File(logFolder).exists()){
                            new File(logFolder).mkdirs();
                        }
                        MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");
                        generateBugreport(logFolder + File.separator + bugreportFile);
                        for (int i=0; i<pageIndex+1; i++){
                            MyFile.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                        }
                        goBackPrePage(pageIndex--);
                        continue;
                    }
                    //

                    System.out.println("deepth:"+ pageIndex +" ,maxdeep:" + maxDeep);
                    if (pageIndex < maxDeep) {
                        crawlerPage(maxDeep-1);
                    } else {
                        logUtil.d("crawler","达到最大遍历深度，返回上一界面");
                        goBackPrePage(pageIndex--);
                    }
                }
            }
        }
        logUtil.d("crawler","遍历页面"+ pageIndex + "结束");
        if (pageIndex > 0) {
            goBackPrePage(pageIndex - 1);
        }

    }


    private boolean goBackPrePage(int preIndex){
        if (preIndex < 1){
            guideStep();
            pageIndex = 0;
            return true;
        }
        ArrayList<Element> prePage = getPageSrcByFile(preIndex);
        try {
            execute_step(UICrawlerConfig().getJSONArray("BACK_TYPE"), new JSONArray().put(1));
        } catch (JSONException e) {
            press("back");
            logUtil.d("goBackPrePage", "按back键");
        }
        SystemClock.sleep(300);
        if (isSamePage(prePage, getPageSrc())){
            logUtil.d("goBackPrePage", "回到上一界面成功");
            pageIndex = preIndex;
            return true;
        }
        //回溯操作
        try {
            execute_step(UICrawlerConfig().getJSONArray("GUIDE_STEP"), new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        System.out.println("exSteps----------------------");
//        System.out.println(exSteps);
        exSteps.remove(preIndex+1);
        exSteps.remove(preIndex);

        System.out.println(exSteps);
        for (int i=0; i < exSteps.length();i++ ) {
            try {
                JSONObject exStep = exSteps.getJSONObject(i);
                ArrayList<String > keys = new ArrayList<>();
                ArrayList<String > values = new ArrayList<>();
                Iterator<String> itr = exStep.keys();
                while (itr.hasNext()){
                    String nex = itr.next();
                    keys.add(nex);
                    values.add(exStep.getString(nex));
                }
                String prePageXml = CONST.LOGPATH + "UICrawler" + File.separator + "page" + i +".xml";

                Element te = get_element_xml(prePageXml, keys, values, 0);
                System.out.println("回溯页面点击的元素");
                System.out.println(te);
                if (te != null) {
                    String points = te.attribute("bounds").getValue();
                    String[] x_y = points.replace("[", "").replace("]", ",").split(",");
                    int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
                    int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
                    click(x, y);
                    SystemClock.sleep(300);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
        if (isSamePage(prePage, getPageSrc())){
            logUtil.d("goBackPrePage", "回到上一页面成功");
            pageIndex = preIndex;
            return true;
        }
        logUtil.d("goBackPrePage", "回到上一页面失败");
        return false;
    }


}
