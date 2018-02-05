package com.kevin.testcases;

/**
 * Created by Administrator on 2017/5/18.
 */

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import uiautomator2.BasicEvent;
import uiautomator2.KeyEvent;
import uiautomator2.SettingEvent;
import uiautomator2.TouchEvent;

@RunWith(AndroidJUnit4.class)
public class videoPlayback {
    private UiDevice mDevice;;

    private Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context = instrumentation.getContext();
    private Context targetContext = instrumentation.getTargetContext();
    private TouchEvent touch = new TouchEvent();
    private BasicEvent basic = new BasicEvent();
    private KeyEvent key = new KeyEvent();
    private SettingEvent Set = new SettingEvent(targetContext);

    public videoPlayback() {
        mDevice = UiDevice.getInstance(instrumentation);
    }

    @Before
    public void setUp() throws IOException, UiObjectNotFoundException, RemoteException {
        Set.closeWifi();
        Set.closeBluetooth();
        mDevice.executeShellCommand("settings put system screen_off_timeout 1800000");

//close screen power saving mode
//        Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
//        intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivities(new Intent[]{intent});
//        SystemClock.sleep(2000);
//        Set.switchButton("amigo:id/amigo_switchWidget", 1, false);
//        SystemClock.sleep(2000);
//        Set.switchButton("amigo:id/amigo_switchWidget", 2, false);
//        SystemClock.sleep(1000);
//close GPS
        Intent intent_loc = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent_loc.addFlags(intent_loc.FLAG_ACTIVITY_NEW_TASK);
        context.startActivities(new Intent[]{intent_loc});
        SystemClock.sleep(2000);
        Set.switchButton("com.android.settings:id/switch_widget", 1, false);
        SystemClock.sleep(2000);
//设置电量显示
        Intent intent_setting = new Intent(Settings.ACTION_SETTINGS);
        intent_setting.addFlags(intent_setting.FLAG_ACTIVITY_NEW_TASK);
        context.startActivities(new Intent[]{intent_setting});
        SystemClock.sleep(1000);
        touch.scrollUp(5);
        SystemClock.sleep(2000);
        touch.clickTextWhenExists("更多设置");
        touch.clickTextWhenExists("高级设置");
        SystemClock.sleep(1000);
        touch.scrollUp(5);
        SystemClock.sleep(1000);
        touch.clickByObjectText("电量管理",2000);
        SystemClock.sleep(1000);
        touch.clickByObjectText("状态栏电量显示",2000);
        SystemClock.sleep(1000);
        touch.clickByObjectText("数字样式（外部）",2000);
        SystemClock.sleep(1000);
        basic.clearRecentApp();

        mDevice.executeShellCommand("pm clear kr.hwangti.batterylog");
        SystemClock.sleep(1000);
        basic.launch("kr.hwangti.batterylog", 500);
        SystemClock.sleep(2000);
        touch.clickByObjectText("继续", 500);
        touch.clickByObjectText("Start Service", 500);
        SystemClock.sleep(1000);

    }

    @Test
    public void test() throws UiObjectNotFoundException {
        mDevice.pressHome();
        basic.launch("com.gionee.video", 2000);
        touch.clickTextWhenExists("确定");
        touch.clickByObjectText("我的", 1000);
        touch.clickByObjectText("设置", 1000);
        Set.switchButton("amigo:id/amigo_switchWidget", 0, true);
        mDevice.pressBack();
//        touch.clickByObjectText("扫描", 1000);
//        SystemClock.sleep(3000);
        touch.clickByObjectText("本地视频", 1000);
        touch.clickByObjectText("test", 1000);
        touch.clickByObjectResourceID("com.gionee.video:id/image_file",3000);
        SystemClock.sleep(2000);
        touch.clickTextWhenExists("我知道了");
        SystemClock.sleep(7000);
        touch.click(700, 700);
        touch.clickByObjectResourceID("com.gionee.video:id/screen_mode_switcher", 1000);
        for(int i =0; i<15;i++) {
            key.pressVolumeUp();
            SystemClock.sleep(200);
            touch.clickTextWhenExists("确定");
        }

    }
}