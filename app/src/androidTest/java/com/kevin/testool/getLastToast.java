package com.kevin.testool;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.kevin.testool.stub.AccessibilityEventListener;
import com.kevin.testool.stub.AutomatorServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static android.content.Context.CONTEXT_INCLUDE_CODE;
import static android.os.ParcelFileDescriptor.MODE_WORLD_READABLE;
import static android.os.ParcelFileDescriptor.MODE_WORLD_WRITEABLE;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class getLastToast {

    @Test
    public void getToast() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        String func = "lastToast";
        AutomatorServiceImpl asi = new AutomatorServiceImpl();
        try {

                Method method = asi.getClass().getDeclaredMethod(func);
                method.setAccessible(true);
                method.invoke(asi);


            } catch (Exception NoSuchMethodException){
                Method method = asi.getClass().getDeclaredMethod("makeToast", String.class, int.class);
                method.setAccessible(true);
                method.invoke(asi, func + "方法不存在", 1);
            }
        }


}