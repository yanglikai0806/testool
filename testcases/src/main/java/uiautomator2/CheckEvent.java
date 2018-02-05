package uiautomator2;

import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

public class CheckEvent {
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    //Check screen on(return true) or off(return false)
    public boolean isScreenOn() throws RemoteException {
        return mDevice.isScreenOn();
    }

    //Check the object is exist or not by resource ID
    public boolean isObjectExistByRes(String resourceID, long timeout) {
        //UiObject testObject = new UiObject(new UiSelector().resourceId(resourceID));
        //return testObject.exists();
        return mDevice.wait(Until.hasObject((By.res(resourceID))), timeout);
    }

    //Check the object is exist or not by text
    public boolean isObjectExistByText(String text, long timeout) {
        //UiObject testObject = new UiObject(new UiSelector().text(text));
        //return testObject.exists();
        return mDevice.wait(Until.hasObject((By.text(text))), timeout);
    }

    //Check the object is exist or not by description
    public boolean isObjectExistByDesc(String desc, long timeout) {
        //UiObject testObject = new UiObject(new UiSelector().description(desc));
        //return testObject.exists();
        return mDevice.wait(Until.hasObject((By.desc(desc))), timeout);
    }
}
