package com.kevin.testcases;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import uiautomator2.BasicEvent;
import uiautomator2.CheckEvent;
import uiautomator2.KeyEvent;
import uiautomator2.SettingEvent;
import uiautomator2.TouchEvent;

/**
 * Created by Administrator on 2017/8/2.
 */


@RunWith(AndroidJUnit4.class)
public class wechatTest {
    String sendContent = "Wechat_Power_Autotest";
    String galleryName="Apicture";
//微信版本：6.5.8
//    String id_switchKeyboard = "com.tencent.mm:id/a47";
//    String id_inputEdit = "com.tencent.mm:id/a49";
//    String id_addEmotion = "com.tencent.mm:id/cm4";
//    String id_moreFunction = "com.tencent.mm:id/a4d";
//    String id_sendMessage = "com.tencent.mm:id/a4e";
//    String id_picAndVideo = "com.tencent.mm:id/cfi";
//    String id_takePicture = "com.tencent.mm:id/bx6";
//    String id_selectPhoto = "com.tencent.mm:id/py";
//    String id_cameraIconMoment = "com.tencent.mm:id/fh";


//微信版本：6.5.10
    String id_switchKeyboard = "com.tencent.mm:id/a5c";
    String id_inputEdit = "com.tencent.mm:id/a5e";
    String id_addEmotion = "com.tencent.mm:id/cp0";
    String id_moreFunction = "com.tencent.mm:id/a5j";
    String id_sendMessage = "com.tencent.mm:id/a5k";
    String id_picAndVideo = "com.tencent.mm:id/ci7";
    String id_takePicture = "com.tencent.mm:id/byw";
    String id_selectPhoto = "com.tencent.mm:id/q8";
    String id_cameraIconMoment = "com.tencent.mm:id/fk";

    private UiDevice mDevice;
    private Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context = instrumentation.getContext();
    private Context targetContext = instrumentation.getTargetContext();
    private TouchEvent touch = new TouchEvent();
    private BasicEvent basic = new BasicEvent();
    private KeyEvent key = new KeyEvent();
    private SettingEvent Set = new SettingEvent(targetContext);
    private CheckEvent check = new CheckEvent();


    public void sendMess() throws IOException {
        int i = 0;
        boolean flag = false;
        while (true) {
            if (i == 10) {
                flag = true;
                break;
            }
            if (!check.isObjectExistByText("发送", 2000)) {
                if(check.isObjectExistByRes(id_sendMessage,2000)){
                    touch.clickByObjectResourceID(id_sendMessage,2000);
                    break;
                }
                SystemClock.sleep(500);
            } else {
                SystemClock.sleep(1000);
                touch.clickByObjectText("发送",1);
                SystemClock.sleep(10000);
                break;
            }
            i++;
        }
        if (flag) {
            basic.takeScreenshot("a发送失败");
//			throw new UiObjectNotFoundException(
//					"Not found [发送] button and enabled attribute to true. Please check xml file");
        }
    }
    public void judgeEnterMoments(){
        basic.goBackHome();
        SystemClock.sleep(1500);
        basic.launch("com.tencent.mm",2000);
        SystemClock.sleep(2000);
        touch.clickByObjectText("发现",2000);
        touch.clickByObjectText("朋友圈",2000);
        SystemClock.sleep(1000);
    }

    private void releaseMess() throws UiObjectNotFoundException, IOException {
        int i = 0;
        boolean flag = false;
        while (true) {
            if(i == 10) {
                flag = true;
                break;
            }
            if (!mDevice.findObject(new UiSelector().textStartsWith("完成").enabled(true)).exists()) {
                SystemClock.sleep(500);
            } else if(mDevice.findObject(new UiSelector().textStartsWith("完成").enabled(true)).exists()){
                mDevice.findObject(new UiSelector().textStartsWith("完成")).clickAndWaitForNewWindow(3000);
                mDevice.wait(Until.hasObject(By.clazz(EditText.class).text("这一刻的想法...")), 3000);
                SystemClock.sleep(2000);
                break;
            }
            i++;
        }
        if(flag){
            throw new UiObjectNotFoundException("Not found [完成] button and enabled attribute to true. Please check xml file");
        }
        mDevice.findObject(By.clazz(EditText.class)).setText("Wechat_Power_autoTest");
        sendMess();
    }
    @Before
    public void startBatteryLog() throws IOException, UiObjectNotFoundException {
        mDevice = UiDevice.getInstance(instrumentation);
        mDevice.executeShellCommand("pm clear kr.hwangti.batterylog");
        SystemClock.sleep(1000);
        basic.launch("kr.hwangti.batterylog", 500);
        touch.clickTextWhenExists("继续");
        SystemClock.sleep(2000);
        touch.clickByObjectText("Start Service", 500);
        basic.goHome();
    }

    @Test
    public void test_wechat() throws RemoteException, UiObjectNotFoundException, IOException {
        mDevice = UiDevice.getInstance(instrumentation);
        Random random = new Random();
        Rect rect = null;

        while(true) {
            try {
//                common.unlockScreen();
                basic.goBackHome();
                step0();
                step1(1);
                step2();
                step3(rect);
                step4(random);
                step5(random, galleryName);
                step6();
                step7(rect);
                step8();
                step9(galleryName, random);
                step10(sendContent);
                step11(rect);
                step12();
//				device.sleep();
//				common.lockScreen();
            } catch (Exception e) {
                String clazz = this.getClass().getName();
                basic.takeScreenshot(clazz);

            }
        }
    }
    private void step0() throws RemoteException, UiObjectNotFoundException, IOException {

        basic.activeScreen();
        SystemClock.sleep(1000);
        basic.launch("com.tencent.mm", 2000);
        SystemClock.sleep(5000);
    }
    public void step1( int num) throws UiObjectNotFoundException {
        num=1;
        touch.clickByObjectText("通讯录",2000);
        SystemClock.sleep(3000);
        touch.clickTextBySwipe("test"+num);
        SystemClock.sleep(3000);
    }
    public void step2() throws UiObjectNotFoundException, IOException {
        touch.clickByObjectText("发消息",2000);
        SystemClock.sleep(3000);
        UiObject2 switchKeyboard = mDevice.findObject(By.res(id_switchKeyboard));

//        UiObject switchKeyboard = new UiObject(new UiSelector().resourceId("com.tencent.mm:id/a47"));
        if (switchKeyboard.getContentDescription().equals("切换到键盘")) {
            switchKeyboard.click();
            SystemClock.sleep(500);
        }
        UiObject2 inputEdit = mDevice.findObject(By.res(id_inputEdit));
        if (!inputEdit.isFocused()) {
            inputEdit.click();
            SystemClock.sleep(500);
        }
        int sum = 10;
        for (int i = 0; i < sum; i++) {
//            inputEdit.clear();
            inputEdit.setText("I am just a test message");
            sendMess();
        }
        if (switchKeyboard.getContentDescription().equals("切换到按住说话")) {
            switchKeyboard.click();
            SystemClock.sleep(3000);
        }
    }
    public void step3(Rect rect) throws UiObjectNotFoundException {
        UiObject pressSay = mDevice.findObject(new UiSelector().className(Button.class).text("按住 说话"));
        rect = pressSay.getBounds();
        int sum = 10;
//        mDevice.registerWatcher("accept", new UiWatcher() {
//            @Override
//            public boolean checkForCondition() {
//                //监听权限框
//                UiObject accept = new UiObject(new UiSelector().resourceId("amigo:id/amigo_button1").text("同意"));
//                System.out.println("出现同意弹框1");
//                if (accept.exists()){
//                    System.out.println("出现同意弹框");
//                    try{
//                        accept.click();
//                    } catch (UiObjectNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                }
//                return false;
//            }
//        });
//        mDevice.runWatchers();
        for (int i = 0; i < sum; i++) {
            mDevice.swipe(rect.centerX(), rect.centerY(), rect.centerX(), rect.centerY(), 590);
            SystemClock.sleep(10000);
        }
    }
    public void step4( Random random) throws UiObjectNotFoundException, IOException {
        touch.clickByObjectDesc("表情",2000);
        if (!check.isObjectExistByRes(id_addEmotion,2000) ) {
            touch.clickByObjectDesc("表情",2000);
            SystemClock.sleep(3000);
        }
        UiObject expressionLib = mDevice.findObject(new UiSelector().className(GridView.class));
        int count = expressionLib.getChildCount();
        int sum = 5;
        for (int i = 0; i < sum; i++) {
            for (int j = 0; j < 5; j++) {
                expressionLib.getChild(new UiSelector().index(j)).click();
//					expressionLib.getChild(new UiSelector().index(random.nextInt(count-1))).click();
                SystemClock.sleep(200);
            }
            SystemClock.sleep(2000);
//				common.clickByText("发送");
            sendMess();
        }
    }

    public void step5(Random random, String galleryName) throws UiObjectNotFoundException {

        int sum = 5;
        for (int i = 0; i < sum; i++) {
            touch.clickByObjectResourceID(id_moreFunction,2000);
            if (!check.isObjectExistByText("相册",2000)) {
                touch.clickByObjectResourceID(id_moreFunction,2000);
            }
            SystemClock.sleep(2000);
            touch.clickByObjectText("相册",2000);
            SystemClock.sleep(2000);
            touch.clickByObjectResourceID(id_picAndVideo,2000);
            SystemClock.sleep(1000);
            touch.clickTextBySwipe(galleryName);
            SystemClock.sleep(2000);
            List<UiObject2> obj_pic = mDevice.findObject(By.clazz(GridView.class)).getChildren();
            if (obj_pic == null) {
                throw new UiObjectNotFoundException("No Found Gallery \"" + galleryName + "\"");
            }
            obj_pic.get(random.nextInt(obj_pic.size())).clickAndWait(Until.newWindow(), 3000);
            SystemClock.sleep(2000);
            touch.clickByObjectText("发送",2000);
//            touch.clickByObjectResourceID("com.tencent.mm:id/gl",2000);
        }
    }
    public void step6() throws UiObjectNotFoundException {

        int sum = 5;
        int limit = 0;
        boolean pre_flag = false;

        for (int i = 0; i < sum; i++) {
            if (! check.isObjectExistByText("拍摄",2000)) {
                SystemClock.sleep(500);
                touch.clickByObjectResourceID(id_moreFunction,2000);
                SystemClock.sleep(3000);
            }
            touch.clickByObjectText("拍摄",2000);
            touch.clickTextWhenExists("同意");
            touch.clickTextWhenExists("始终同意");
            UiObject pre_camera = new UiObject(new UiSelector().resourceId(id_takePicture));
            while (true) {
                if (limit == 10) {
                    limit = 0;
                    pre_flag = true;
                    break;
                }
                if (pre_camera != null) {
                    break;
                } else {
                    SystemClock.sleep(500);
                }
                limit++;
            }
            if (pre_flag) {
                throw new UiObjectNotFoundException("Not found View[com.tencent.mm:id/bx6]. Please check xml file");
            }
            SystemClock.sleep(2000);
            touch.clickByObjectResourceID(id_takePicture,2000);
            SystemClock.sleep(2000);
            touch.clickByObjectResourceID(id_selectPhoto,2000);
            SystemClock.sleep(10000);
        }
    }
    public void step7(Rect rect) throws UiObjectNotFoundException {

        int sum = 2;
        int limit = 0;
        boolean pre_flag = false;

        for (int i = 0; i < sum; i++) {
            if (!check.isObjectExistByText("拍摄",2000)) {
                SystemClock.sleep(500);
                touch.clickByObjectResourceID(id_moreFunction,2000);
                SystemClock.sleep(2000);
            }
            touch.clickByObjectText("拍摄",2000);
            UiObject pre_camera = new UiObject(new UiSelector().resourceId(id_takePicture));
            while (true) {
                if (limit == 10) {
                    limit = 0;
                    pre_flag = true;
                    break;
                }
                if (pre_camera != null) {
                    break;
                } else {
                    SystemClock.sleep(500);
                }
                limit++;
            }
            if (pre_flag) {
                throw new UiObjectNotFoundException("Not found View[com.tencent.mm:id/bx6]. Please check xml file");
            }
            SystemClock.sleep(2000);
            rect = pre_camera.getVisibleBounds();
            mDevice.swipe(rect.centerX(), rect.centerY(), rect.centerX()+1, rect.centerY()+1, 590);
            SystemClock.sleep(3000);
            touch.clickByObjectResourceID(id_selectPhoto,2000);
            SystemClock.sleep(10000);
        }
    }
    public void step8() throws UiObjectNotFoundException {

        judgeEnterMoments();
        SystemClock.sleep(3000);
    }

    public void step9(String galleryName, Random random) throws UiObjectNotFoundException, IOException {

        touch.clickByObjectResourceID(id_cameraIconMoment,2000);
        SystemClock.sleep(2000);
        touch.clickByObjectText("从相册选择",2000);
        touch.clickTextWhenExists("我知道了");
        SystemClock.sleep(2000);
        touch.clickByObjectResourceID(id_picAndVideo,2000);
        SystemClock.sleep(2000);
        touch.clickTextBySwipe(galleryName);
        SystemClock.sleep(2000);

        UiObject lib_pic = new UiObject(new UiSelector().className(GridView.class));
        UiObject random_pic = lib_pic.getChild(new UiSelector().index(random.nextInt(lib_pic.getChildCount())));
        random_pic.clickAndWaitForNewWindow(3000);
        SystemClock.sleep(2000);
        releaseMess();
    }

    public void step10(String sendContent) throws UiObjectNotFoundException, IOException {
        touch.clickByObjectResourceID(id_cameraIconMoment,2000);
        SystemClock.sleep(2000);
        touch.clickByObjectText("拍摄",2000);

        int limit = 0;
        boolean pre_flag = false;

        UiObject pre_camera = new UiObject(new UiSelector().resourceId(id_takePicture));

        while (true) {
            if (limit == 10) {
                pre_flag = true;
                break;
            }
            if (pre_camera != null) {
                break;
            } else {
                SystemClock.sleep(500);
            }
            limit++;
        }
        if (pre_flag) {
            throw new UiObjectNotFoundException("Not found View[com.tencent.mm:id/btw]. Please check xml file");
        }

        SystemClock.sleep(2000);
        pre_camera.click();
        SystemClock.sleep(3000);
        touch.clickByObjectResourceID(id_selectPhoto,2000);
        SystemClock.sleep(1000);
        new UiObject(new UiSelector().className(EditText.class)).setText(sendContent);
        sendMess();
    }

    public void step11(Rect rect) throws UiObjectNotFoundException, IOException {
        touch.clickByObjectResourceID(id_cameraIconMoment,2000);
        SystemClock.sleep(2000);
        touch.clickByObjectText("拍摄",2000);

        int limit = 0;
        boolean pre_flag = false;

        UiObject pre_camera = new UiObject(new UiSelector().resourceId(id_takePicture));
        while (true) {
            if (limit == 10) {
                pre_flag = true;
                break;
            }
            if (pre_camera != null) {
                break;
            } else {
                SystemClock.sleep(500);
            }
            limit++;
        }
        if (pre_flag) {
            throw new UiObjectNotFoundException("Not found View[com.tencent.mm:id/bx6]. Please check xml file");
        }

        SystemClock.sleep(2000);
        rect = pre_camera.getVisibleBounds();
        mDevice.swipe(rect.centerX(), rect.centerY(), rect.centerX()+1, rect.centerY()+1, 590);
        SystemClock.sleep(3000);
        touch.clickByObjectResourceID(id_selectPhoto,2000);
        new UiObject(new UiSelector().className(EditText.class)).setText(sendContent);
        sendMess();
    }

    public void step12() throws UiObjectNotFoundException {
        int width = mDevice.getDisplayWidth();
        int height = mDevice.getDisplayHeight();

        judgeEnterMoments();
        int sum = 2;
        for (int i = 0; i < sum; i++) {
            mDevice.swipe(width / 2, height - 200, width / 2, 400, 22);
            SystemClock.sleep(2000);
        }
        SystemClock.sleep(2000);

    }
}
