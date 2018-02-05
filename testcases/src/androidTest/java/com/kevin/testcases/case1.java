package com.kevin.testcases;

import android.app.Instrumentation;
import android.content.Context;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import uiautomator2.BasicEvent;
import uiautomator2.KeyEvent;
import uiautomator2.SettingEvent;
import uiautomator2.TouchEvent;
import uiautomator2.logUtil;

/**
 * Created by Administrator on 2018/1/30.
 */
@RunWith(AndroidJUnit4.class)
public class case1 {
    private UiDevice mDevice;;

    private Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private Context context = instrumentation.getContext();
    private Context targetContext = instrumentation.getTargetContext();
    private TouchEvent touch = new TouchEvent();
    private BasicEvent basic = new BasicEvent();
    private KeyEvent key = new KeyEvent();
    private SettingEvent Set = new SettingEvent(targetContext);

    public case1() {
        mDevice = UiDevice.getInstance(instrumentation);
    }
    @Before
    public void setUp(){
        basic.goHome();
    }
    @Test
    public void test() throws RemoteException, IOException {
        logUtil.i("hello word","hello word");
        logUtil.i("hello word","你好");

//        basic.clearRecentApp();
//        basic.launch("com.android.calculator2",2000);
    }
}
