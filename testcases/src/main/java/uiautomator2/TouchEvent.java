package uiautomator2;

import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

public class TouchEvent {
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    //Click once action in pixels
    public boolean click(int x, int y) {
        return mDevice.click(x, y);
    }

    //Long Click action in pixels
    public boolean longClick(int x, int y, int pressTime) {
        int steps = pressTime / 5;
        return mDevice.swipe(x, y, x, y, steps);
    }

    //Multi Click action in pixels
    public void multiClick(int x, int y, long interval, int times) {
        long timeout = Configurator.getInstance().getActionAcknowledgmentTimeout();
        Configurator.getInstance().setActionAcknowledgmentTimeout(interval);
        for (int i = 0; i < times; i++) {
            mDevice.click(x, y);
        }
        Configurator.getInstance().setActionAcknowledgmentTimeout(timeout);
    }

    //Swipe actions in pixels(5ms per step)
    public boolean swipe(int startX, int startY, int endX, int endY, int steps) {
        return mDevice.swipe(startX, startY, endX, endY, steps);
    }

    //Drag actions in pixels
    public boolean drag(int startX, int startY, int endX, int endY, int steps) {
        return mDevice.drag(startX, startY, endX, endY, steps);
    }

    //Scroll up screen
    public boolean scrollUp(int steps) {
        return mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() * 3 / 4,
                mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() / 4, steps);
    }

    //Scroll down screen
    public boolean scrollDown(int steps) {
        return mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() / 4,
                mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight() * 3 / 4, steps);
    }

    //Scroll left screen
    public boolean scrollLeft() {
        return mDevice.swipe(mDevice.getDisplayWidth() * 3 / 4, mDevice.getDisplayHeight() / 2,
                mDevice.getDisplayWidth() / 4, mDevice.getDisplayHeight() / 2, 100);
    }

    //Scroll right screen
    public boolean scrollRight() {
        return mDevice.swipe(mDevice.getDisplayWidth() / 4, mDevice.getDisplayHeight() / 2,
                mDevice.getDisplayWidth() * 3 / 4, mDevice.getDisplayHeight() / 2, 100);
    }

    //Click once element by Object Resource ID
    public void clickByObjectResourceID(String resourceID, long timeout) {
        mDevice.wait(Until.findObject(By.res(resourceID)), timeout).click();
    }

    //Click once element by Object Text
    public void clickByObjectText(String text, long timeout) {
        mDevice.wait(Until.findObject(By.text(text)), timeout).click();
    }

    //Click once element by Object Text
    public void clickByObjectDesc(String desc, long timeout) {
        mDevice.wait(Until.findObject(By.desc(desc)), timeout).click();
    }

    //Click once element by Object Resource ID
    public void longClickByObjectResourceID(String resourceID, long timeout) {
        mDevice.wait(Until.findObject(By.res(resourceID)), timeout).longClick();
    }

    //Click once element by Object Text
    public void longClickByObjectText(String text, long timeout) {
        mDevice.wait(Until.findObject(By.text(text)), timeout).longClick();
    }

    //Click once element by Object Text
    public void longClickByObjectDesc(String desc, long timeout) {
        mDevice.wait(Until.findObject(By.desc(desc)), timeout).longClick();
    }
    public void clickTextWhenExists(String text) throws UiObjectNotFoundException {
        UiObject txt = mDevice.findObject(new UiSelector().text(text));
        if (txt.exists() ==true) {
            txt.click();
            SystemClock.sleep(1000);
        }
    }
    public void clickTextBySwipe(String text){
        for(int i=0;i<10;i++){
            if(!mDevice.findObject(new UiSelector().text(text)).exists()){
                mDevice.swipe(mDevice.getDisplayWidth()/2, mDevice.getDisplayHeight()*3/4, mDevice.getDisplayWidth()/2, mDevice.getDisplayHeight()/4,30);
            }
        }
        clickByObjectText(text,2000);
    }


}
