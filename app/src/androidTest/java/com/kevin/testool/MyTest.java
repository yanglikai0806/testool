package com.kevin.testool;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;

import com.kevin.testool.aw.Common2;
import com.kevin.testool.common.Common;
import com.kevin.testool.stub.Automator;
import com.kevin.testool.utils.ToastUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.kevin.testool.aw.Common2.getRomName;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MyTest {

    @Test
    public void mytest() throws IOException {
        System.out.println("----------------------------------------------------");
        long a =  System.currentTimeMillis();
        System.out.println(getRomName());
//        Automator.findObject(By.text("设置")).click();
//        Common2.click_element(false, "text", "设置", 0, 0);
        ToastUtils.showShortByHandler(MyApplication.getContext(), (System.currentTimeMillis() - a)+ "");
        System.out.println("----------------------------------------------------");

    }
}