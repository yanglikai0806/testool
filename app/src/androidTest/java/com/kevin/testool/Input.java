package com.kevin.testool;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;

import com.kevin.testool.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class Input {

    private UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    @Test
    public void input(){
        final String content;
        JSONObject inputObj = new JSONObject();
        UiObject2 minput = null;
        content = FileUtils.readJsonFile(CONST.INPUT_FILE);
        try {
            inputObj = new JSONObject(content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            String key = inputObj.getString("key");
            String mode = inputObj.getString("mode");
            switch (key){
                case "id":
                    minput = device.findObject(By.res(inputObj.getString("value")));
                    break;
                case "resource-id":
                    minput = device.findObject(By.res(inputObj.getString("value")));
                    break;
                case "resourceId":
                    minput = device.findObject(By.res(inputObj.getString("value")));
                    break;
                case "text":
                    minput = device.findObject(By.text(inputObj.getString("value")));
                    break;
                case "content":
                    minput = device.findObject(By.desc(inputObj.getString("value")));
                    break;
                case "clazz":
                    minput = device.findObject(By.clazz(inputObj.getString("value")));
                    break;
            }
            if (minput != null) {
                if (mode.equals("a")){
                    inputObj.put("msg", minput.getText() + inputObj.getString("msg"));
                }
                minput.setText(inputObj.getString("msg"));
            } else {
                System.out.println("控件没有找到");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}