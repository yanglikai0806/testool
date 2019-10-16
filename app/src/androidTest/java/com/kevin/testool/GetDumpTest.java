package com.kevin.testool;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.kevin.testool.common.Common;
import com.kevin.testool.stub.AutomatorServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class GetDumpTest {
    @Test
    public void getDump() {
        String os = AutomatorServiceImpl.dumpWindowHierarchys(true);
//        System.out.println(os);
    }
}
