package com.kevin.share;

import android.os.SystemClock;

import com.kevin.share.utils.ShellUtils;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.logUtil;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class UICrawler extends Common {
    private String UICRAWLER_PATH = CONST.LOGPATH + "uicrawler";
    private String PKG;
    private static int pageIndex = 0;
    public static JSONObject UICrawlerConfig;
    public static int clickCount = 0;
    private JSONArray exSteps = new JSONArray();
    private JSONObject exSwipe = new JSONObject();
    public String version;
    private String swipeEnable;
    private String swipeType;
    private static JSONArray swipeParam = null;
    private boolean CHECK_UI_EXCEPTION = false;
    private int maxDeep;
    private JSONArray xys = new JSONArray();
    private long sdTime = System.currentTimeMillis();//记录开始时间

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式;


    public UICrawler(String app_package, String versionName, int mDeep){

            //初始化log.txt
        FileUtils.writeFile(UICRAWLER_PATH + File.separator + "log.txt", "", false);

        PKG = app_package;
        version = versionName;
        maxDeep = mDeep;
        UICrawlerConfig = UICrawlerConfig();
        try {
            CHECK_UI_EXCEPTION = UICrawlerConfig.getJSONObject("CHECK_ERROR").length() > 0;
            swipeEnable = UICrawlerConfig.getJSONObject("SWIPE_TYPE").getString("enable").toLowerCase();
            swipeType = UICrawlerConfig.getJSONObject("SWIPE_TYPE").getString("type").toLowerCase();
            swipeParam = UICrawlerConfig.getJSONObject("SWIPE_TYPE").getJSONArray("params");
        } catch (JSONException e) {
            swipeEnable = "false";
        }
    }

    public ArrayList<Element> getPageSrc(int index){
        ArrayList<Element> pageElements = new ArrayList<>();
        ArrayList<Element> elements = get_elements(true, "", "", 0);
        if (elements != null) {
            FileUtils.copyFile(new File(CONST.DUMP_PATH), CONST.LOGPATH + "UICrawler" + File.separator + "page"+index+".xml");
            ShellUtils.runShellCommand("screencap -p " + CONST.LOGPATH + "UICrawler" + File.separator + "page"+index+".png", 0);
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
//        AttrVals.add(element.attributeValue("checked"));
        AttrVals.add(element.attributeValue("clickable"));
//        AttrVals.add(element.attributeValue("enabled"));
//        AttrVals.add(element.attributeValue("focusable"));
//        AttrVals.add(element.attributeValue("focused"));
//        AttrVals.add(element.attributeValue("scrollable"));
        return AttrVals;
    }

    public boolean isSamePage(ArrayList<Element> page1, ArrayList<Element> page2){
        ArrayList<Boolean> isSame = new ArrayList<>();
        if (!Common.getActivity().contains(PKG)){
            logUtil.u("isSamePage: activity:", "false");
            return false;
        }

        if (Math.abs(page1.size() - page2.size()) > 2){
            logUtil.u("isSamePage: pageSize:", "false");
            return false;
        }

        ArrayList<ArrayList> page1Elems = new ArrayList<>();
        ArrayList<ArrayList> page2Elems = new ArrayList<>();
        for (Element e: page1){
            page1Elems.add(elemtAttrVals(e));
        }
        for (Element e: page2){
            page2Elems.add(elemtAttrVals(e));
        }

        for (ArrayList l: page1Elems){
            if (page2Elems.contains(l)){
                isSame.add(true);
            }
        }

        if (Math.abs(page1Elems.size() - isSame.size()) > 2){
            logUtil.u("isSamePage:pageElements", "false");
            return false;
        }
        logUtil.u("isSamePage:", "true");
        return true;

    }

    public static JSONObject UICrawlerConfig(){
        try {
            return new JSONObject(FileUtils.readJsonFile(CONST.UICRAWLWER_CONFIG_FILE));
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONObject crawlerStatus(){
        try {
            return new JSONObject(FileUtils.readJsonFile(CONST.UICRAWLWER_CONFIG_FILE)).getJSONObject("CRAWLER_STATUS");
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray elemBlackList(){
        try {
            return UICrawlerConfig.getJSONArray("ELEM_BLACK");
        } catch (JSONException e) {
            return new JSONArray();
        }

    }

    private JSONArray elemRcpageBlackList(){
        try {
            return UICrawlerConfig.getJSONArray("ELEM_BLACK_RECOMMEND");
        } catch (JSONException e) {
            return new JSONArray();
        }

    }

    private JSONArray elemWhiteList(){
        try {
            return UICrawlerConfig.getJSONArray("ELEM_WHITE");
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private boolean isBlack(Element elem, String page) throws JSONException {
        ArrayList<Boolean> result = new ArrayList<>();
        JSONArray elemBlacks = elemBlackList();
        JSONArray elemRcpageBlacks = elemRcpageBlackList();

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

        if (page.contains("recommend")) {
            for (int i=0; i<elemRcpageBlacks.length(); i++){
                JSONObject attr1 = elemRcpageBlacks.getJSONObject(i);
                Iterator<String> itr1 = attr1.keys();
                while (itr1.hasNext()){
                    String key = itr1.next();
                    if (key.equals("bounds")){
                        result.add(isInBounds(attr1.getString("bounds"), elem.attribute("bounds").getValue()));
                    } else {
                        result.add(attr1.getString(key).equals(elem.attribute(key).getValue()));
                    }
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

//    public static boolean isInBounds(String s1, String s2){
//        String[] b1 = s1.replace("\n","").replace("[", "").replace("]", ",").split(",");
//        String[] b2 = s2.replace("\n","").replace("[", "").replace("]", ",").split(",");
//        int x = (Integer.parseInt(b2[0]) + Integer.parseInt(b2[2])) / 2;
//        int y = (Integer.parseInt(b2[1]) + Integer.parseInt(b2[3])) / 2;
//
//        return (Integer.parseInt(b1[0]) < x) & (x< Integer.parseInt(b1[2])) & (Integer.parseInt(b1[1]) < y) & (y < Integer.parseInt(b1[3]));
//    }

    private boolean isClickable(Element elem) throws JSONException {
        if (UICrawlerConfig.getString("ELEM_TYPE").equals("clickable")) {
            return elem.attribute("clickable").getValue().equals("true");
        }
        return true;
    }

    private JSONObject clickRecord(Element elem){
        JSONObject ce = new JSONObject();
        String te = elem.attributeValue("text");
        String ti = elem.attributeValue("index");
        String re = elem.attributeValue("resource-id");
        String co = elem.attributeValue("content-desc");
        String cl = elem.attributeValue("class");
        String bd = elem.attributeValue("bounds");
        try {
            ce.put("text", te);
            ce.put("index", ti);
            ce.put("resource-id", re);
            ce.put("content-desc", co);
            ce.put("class", cl);
            ce.put("bounds", bd);
            if (te.equals("") & re.equals("") & co.equals("")){
                ce.put("bounds", bd);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ce;
    }

    private void exStepsRecord(int index, JSONObject step){

        try {
            JSONArray mExStep = UICrawlerConfig().getJSONArray("EX_STEPS");
            FileUtils.editJsonFile(CONST.UICRAWLWER_CONFIG_FILE, new JSONObject().put("EX_STEPS", mExStep.put(index, step)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void exSwipeRecord(String index, JSONArray step){

        try {
            JSONObject mExSwipe = UICrawlerConfig().getJSONObject("EX_SWIPE");
            FileUtils.editJsonFile(CONST.UICRAWLWER_CONFIG_FILE, new JSONObject().put("EX_SWIPE", mExSwipe.put(index, step)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void guideStep(String pageName){
        try {
            logUtil.u("", "guide step ");
            Common.openWifi();
            execute_step(UICrawlerConfig.getJSONArray("GUIDE_STEP"), new JSONArray().put(2));
            if (!pageName.contains("default")) {
                execute_step(UICrawlerConfig.getJSONArray("GUIDE_STEP_" + pageName.toUpperCase()), new JSONArray().put(2));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void lastStep(){
        try {
            execute_step(UICrawlerConfig.getJSONArray("LAST_STEP"), new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void continueCrawler(){
        try {
            execute_step(UICrawlerConfig.getJSONArray("GUIDE_STEP"), new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray mExSteps = new JSONArray();
        JSONObject mSwipStep = new JSONObject();
        try {
            mExSteps = crawlerStatus().getJSONArray("exSteps");
            mSwipStep = crawlerStatus().getJSONObject("exSwipe");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i=0; i < mExSteps.length();i++ ) {
            try {
                JSONObject exStep = mExSteps.getJSONObject(i);
                ArrayList<String > keys = new ArrayList<>();
                ArrayList<String > values = new ArrayList<>();
                Iterator<String> itr = exStep.keys();

                while (itr.hasNext()){
                    String nex = itr.next();
                    if (!nex.equals("bounds")) {
                        keys.add(nex);
                        values.add(exStep.getString(nex));
                    }
                }
                String prePageXml = CONST.LOGPATH + "UICrawler" + File.separator + "page" + i +".xml";

                Element te = get_element_xml(prePageXml, keys, values, 0);
//                System.out.println("回溯页面点击的元素");
//                System.out.println(te);
                String points;
                if (te != null) {
                    points = te.attribute("bounds").getValue();

                } else {
                    points = exStep.getString("bounds");
                }

                String[] x_y = points.replace("[", "").replace("]", ",").split(",");
                int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
                int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
                // 滑动页面
                int s = 0;
//                logUtil.u("滑动数据", mSwipStep.toString());
                while (!mSwipStep.isNull(i + "." + s)) {
                    JSONArray xys = mSwipStep.getJSONArray(i + "." + s);
                    if (xys != null) {
                        swipe(xys.getDouble(0), xys.getDouble(1), xys.getDouble(2), xys.getDouble(3), xys.getInt(4));
                    }
                    s ++;
                }
                SystemClock.sleep(300);
                //点击坐标
                click(x, y);
                SystemClock.sleep(300);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean goBackPrePage(int preIndex, String page){

        logUtil.d("goBackPrePage", "回到 index: "+preIndex+"页面");
        ArrayList<Element> prePage = getPageSrcByFile(preIndex);
        if (!Checkpoint.checkActivity(PKG)){
            Common.launchApp(AppContext.getContext(), PKG);
        } else {
            try {
                execute_step(UICrawlerConfig.getJSONArray("BACK_TYPE"), new JSONArray().put(3));
            } catch (JSONException e) {
                press("back");
                logUtil.d("goBackPrePage", "按back键");
            }
        }
        SystemClock.sleep(300);
        if (isSamePage(prePage, getPageSrc())){
            logUtil.u("goBackPrePage", "press back key 回到 index: "+preIndex+"页面成功");
//            pageIndex = preIndex;
            return true;
        }
        // index == 0
        if (preIndex == 0){
            guideStep(page);
            pageIndex = 0;

            int s = 1;
            while (!exSwipe.isNull("0" + "." + s)) {
                try {
                    JSONArray xys = exSwipe.getJSONArray("0" + "." + s);
                    if (xys != null) {
                        swipe(xys.getDouble(0), xys.getDouble(1), xys.getDouble(2), xys.getDouble(3), xys.getInt(4));
                    }
                    s ++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        //回溯操作
        try {
            execute_step(UICrawlerConfig.getJSONArray("GUIDE_STEP"), new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        exSteps.remove(preIndex + 1);
        exSteps.remove(preIndex);

        logUtil.d("goBackPrePage",exSteps.toString());
        logUtil.d("goBackPrePage",exSwipe.toString());
        for (int i=0; i < exSteps.length();i++ ) {
            try {
                JSONObject exStep = exSteps.getJSONObject(i);
                ArrayList<String > keys = new ArrayList<>();
                ArrayList<String > values = new ArrayList<>();
                Iterator<String> itr = exStep.keys();

                while (itr.hasNext()){
                    String nex = itr.next();
                    if (!nex.equals("bounds")) {
                        keys.add(nex);
                        values.add(exStep.getString(nex));
                    }
                }
                String prePageXml = CONST.LOGPATH + "UICrawler" + File.separator + "page" + i +".xml";

                Element te = get_element_xml(prePageXml, keys, values, 0);
//                System.out.println("回溯页面点击的元素");
//                System.out.println(te);
                String points;
                if (te != null) {
                    points = te.attribute("bounds").getValue();

                } else {
                    points = exStep.getString("bounds");
                }

                String[] x_y = points.replace("[", "").replace("]", ",").split(",");
                int x = (Integer.parseInt(x_y[0]) + Integer.parseInt(x_y[2])) / 2;
                int y = (Integer.parseInt(x_y[1]) + Integer.parseInt(x_y[3])) / 2;
                // 滑动页面
                int s = 0;
//                logUtil.u("滑动数据", exSwipe.toString());
                while (!exSwipe.isNull(i + "." + s)) {
                    JSONArray xys = exSwipe.getJSONArray(i + "." + s);
                    if (xys != null) {
                        swipe(xys.getDouble(0), xys.getDouble(1), xys.getDouble(2), xys.getDouble(3), xys.getInt(4));
                    }
                    s ++;
                }

                SystemClock.sleep(300);
                //点击坐标
                click(x, y);
                SystemClock.sleep(300);

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
        if (isSamePage(prePage, getPageSrc())){
            logUtil.u("goBackPrePage", "回溯点击到 index:"+preIndex+"页面成功");
//            pageIndex = preIndex;
            return true;
        }
        logUtil.u("goBackPrePage", "回到 index"+preIndex+"页面失败");
        return true;
    }

    public boolean swipePage() throws JSONException {
        if (swipeEnable.equals("true")){
            if (swipeParam != null && swipeParam.length() == 5){
                xys = swipe(swipeParam.getDouble(0), swipeParam.getDouble(1), swipeParam.getDouble(2), swipeParam.getDouble(3), swipeParam.getInt(4));
            } else {

                switch (swipeType) {
                    case "down2up":
                        xys = swipe(0.5, 0.9, 0.5, 0.08, 1500);
                        break;
                    case "up2down":
                        xys = swipe(0.5, 0.2, 0.5, 0.9, 1500);
                        break;
                    case "right2left":
                        xys = swipe(0.9, 0.5, 0.1, 0.5, 1500);
                        break;
                    case "left2right":
                        xys = swipe(0.1, 0.5, 0.9, 0.5, 1500);
                        break;
                }
            }
//            exSwipe.put(pageIndex + "." + swipeIndex, xys);
//            logUtil.u("swipe page", "滑动页面:"+ exSwipe.toString());
            return true;

        } else {
            return false;
        }

    }

    public boolean stopSwipe(String swipeDeepth) throws JSONException, IOException {
        JSONObject stopCondition = UICrawlerConfig.getJSONObject("SWIPE_TYPE").getJSONObject("stop");
        logUtil.u("stopSwipe", "停止滑动" + stopCondition.toString());
        if (!stopCondition.isNull("num")){
            int mPageIndex = Integer.valueOf(swipeDeepth.split("\\.")[0]);
            int mSwipeIndex = Integer.valueOf(swipeDeepth.split("\\.")[1]);

            if (mSwipeIndex >=  stopCondition.getInt("num")){
                logUtil.u("stopSwipe","停止滑动 " + swipeDeepth);
                return true;
            }
        }
        if (Common.resultCheck(stopCondition, false)){
            logUtil.u("stopSwipe", "停止滑动" + swipeDeepth);
            return true;
        }
        return false;
    }

    public void crawlerPage(int mPageIndex, int swipeIndex, String page) throws JSONException, IOException {
        if (!Common.getActivity().contains(PKG)){ //跳出被测应用停止当前遍历
            return;
        }
        String[] x_y;
        String points;
        ArrayList<Element> page1 = getPageSrc(pageIndex);
        //页面遍历
        for (Element elem: page1) {
            if (isWhite(elem) | (!isBlack(elem,page) && isClickable(elem))) {
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
                try {
                    SystemClock.sleep(UICrawlerConfig.getInt("CLICK_WAIT"));
                } catch (Exception ignore){
                    SystemClock.sleep(300);
                }

                String bugreportFile = "bugreport_" + dateFormat.format(new Date()) + ".txt";
                String logFolder = UICRAWLER_PATH + File.separator + version + File.separator + dateFormat.format(new Date());

                if (ErrorDetect.isDetectCrash(UICRAWLER_PATH)){
                    if (!new File(logFolder).exists()){
                        new File(logFolder).mkdirs();
                    }
                    ShellUtils.runShellCommand("screencap -p " + logFolder + File.separator + "error.png", 0);
                    generateBugreport(logFolder + File.separator + bugreportFile);
                    FileUtils.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");

                    for (int i=0; i<pageIndex; i++){
                        FileUtils.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                    }
                } else if (ErrorDetect.isDetectAnr(UICRAWLER_PATH)){
                    if (!new File(logFolder).exists()){
                        new File(logFolder).mkdirs();
                    }
                    ShellUtils.runShellCommand("screencap -p " + logFolder + File.separator + "error.png", 0);
                    generateBugreport(logFolder + File.separator + bugreportFile);
                    FileUtils.copyFile(new File(UICRAWLER_PATH + File.separator + "error.txt"), logFolder + File.separator + "error.txt");
                    for (int i=0; i<pageIndex; i++){
                        FileUtils.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                    }
                }

                // 记录点击坐标
                logUtil.d("crawler","点击坐标：" + x + "," + y);

                FileUtils.writeFile(CONST.LOGPATH + "record.txt", pageIndex + ":" +x+","+y+"\n", true);

                clickCount++;
                exSteps.put(pageIndex,clickRecord(elem)); //记录执行步骤
                exStepsRecord(pageIndex, clickRecord(elem));


                logFolder = UICRAWLER_PATH + File.separator + version + File.separator + dateFormat.format(new Date());
                if (isSamePage(page1, getPageSrc())) {
                    logUtil.u("crawler","界面未变化，继续遍历当前页面");
                } else {
                    logUtil.u("crawler","界面变化");
                    pageIndex++;
                    // 检查界面问题
                    if (CHECK_UI_EXCEPTION && ErrorDetect.muitiCheck(UICrawlerConfig.getJSONObject("CHECK_ERROR"), false).contains(true)){
                        if (!new File(logFolder).exists()){
                            new File(logFolder).mkdirs();
                        }
                        ShellUtils.runShellCommand("screencap -p " + logFolder + File.separator + "error.png", 0);
                        generateBugreport(logFolder + File.separator + bugreportFile);
                        for (int i=0; i<pageIndex; i++){
                            FileUtils.copyFile(new File(UICRAWLER_PATH + File.separator + "page" + i + ".png"), logFolder + File.separator + "page" + i + ".png");
                        }
                        pageIndex--;
                        goBackPrePage(pageIndex, page);
                        continue;
                    }
                    //
                    logUtil.d("","deepth:"+ pageIndex +" ,maxdeep:" + maxDeep);
                    if ( maxDeep > pageIndex) {
                        crawlerPage(pageIndex, 0, page);
                        pageIndex--;
                        goBackPrePage(pageIndex, page);

                    } else {
                        logUtil.u("crawler","达到最大遍历深度");
                        pageIndex--;
                        goBackPrePage(pageIndex, page);
                    }
                }
            }
        }
        logUtil.u("crawler","遍历页面"+ pageIndex + "结束");
//        if (pageIndex > 0) {
//            pageIndex--;
//            goBackPrePage(pageIndex);
//        }

        // 可滑动页面遍历

        ArrayList<Element> pageBeforSwipe = page1;
        while ( mPageIndex == pageIndex){
            String swipeDeepth = pageIndex + "." + swipeIndex;
            if (stopSwipe(swipeDeepth)){
                break;
            }
            if (!swipePage()){
                break;
            }
            ArrayList<Element> pageAfterSwipe =getPageSrc();
            if (!isSamePage(pageBeforSwipe, pageAfterSwipe)){
                exSwipe.put(swipeDeepth, xys);
                exSwipeRecord(swipeDeepth, xys);
                logUtil.u("swipe page", "滑动页面:"+ exSwipe.toString());
                logUtil.u("","滑动后遍历开始" + swipeDeepth);
                crawlerPage(pageIndex, swipeIndex + 1, page);
                pageBeforSwipe = pageAfterSwipe;
            } else {
                break;
            }
            swipeIndex++;
        }
//        exSwipe = new JSONObject();
        logUtil.u("", String.format("完成点击：%s次", clickCount));
    }

}
