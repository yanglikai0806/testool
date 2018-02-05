package uiautomator2;

import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

public class TextEvent {
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    //Click once element by Object Resource ID
    public void setTextByObjectResourceID(String resourceID, String content, long timeout) {
        mDevice.wait(Until.findObject(By.res(resourceID)), timeout).setText(content);
    }

    //Click once element by Object Text
    public void setTextByObjectText(String text, String content, long timeout) {
        mDevice.wait(Until.findObject(By.text(text)), timeout).setText(content);
    }

    //Click once element by Object Text
    public void setTextByObjectDesc(String desc, String content, long timeout) {
        mDevice.wait(Until.findObject(By.desc(desc)), timeout).setText(content);
    }
}
