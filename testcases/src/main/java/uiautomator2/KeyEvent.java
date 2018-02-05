package uiautomator2;

import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.view.InputDevice;
import android.view.KeyCharacterMap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class KeyEvent {
    // Initialize UiDevice instance
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    //Long press key by keycode
    private boolean longPressKeyCode(int keyCode, long pressTime) {
        try {
            Field mUiAutomationBridge = Class.forName("android.support.test.uiautomator.UiDevice").getDeclaredField("mUiAutomationBridge");
            mUiAutomationBridge.setAccessible(true);

            Object bridgeObj = mUiAutomationBridge.get(mDevice);
            Method injectInputEvent = Class.forName("android.support.test.uiautomator.UiAutomatorBridge")
                    .getDeclaredMethod("injectInputEvent", android.view.InputEvent.class, boolean.class);

            final long eventTime = SystemClock.uptimeMillis();
            android.view.KeyEvent downEvent = new android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN,
                    keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                    InputDevice.SOURCE_KEYBOARD);

            if ((Boolean) injectInputEvent.invoke(bridgeObj, downEvent, true)) {
                SystemClock.sleep(pressTime);
                android.view.KeyEvent upEvent = new android.view.KeyEvent(eventTime, eventTime,
                        android.view.KeyEvent.ACTION_UP, keyCode, 0, 0,
                        KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                        InputDevice.SOURCE_KEYBOARD);
                if ((Boolean) injectInputEvent.invoke(bridgeObj, upEvent, true)) {
                    return true;
                }
            }
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Press home key
    public boolean pressHome() {
        return mDevice.pressHome();
    }

    //Press menu key
    public boolean pressMenu() {
        return mDevice.pressMenu();
    }

    //Press back key
    public boolean pressBack() {
        return mDevice.pressBack();
    }

    //Press recentApps key
    public boolean pressRecentApps() throws RemoteException {
        return mDevice.pressRecentApps();
    }

    //Press volume up key
    public boolean pressVolumeUp() {
        return mDevice.pressKeyCode(android.view.KeyEvent.KEYCODE_VOLUME_UP);
    }

    //Press volume down key
    public boolean pressVolumeDown() {
        return mDevice.pressKeyCode(android.view.KeyEvent.KEYCODE_VOLUME_DOWN);
    }

    //Press volume mute key
    public boolean pressVolumeMute() {
        return mDevice.pressKeyCode(android.view.KeyEvent.KEYCODE_VOLUME_MUTE);
    }

    //Press power key
    public boolean pressPower() {
        return mDevice.pressKeyCode(android.view.KeyEvent.KEYCODE_POWER);
    }

    //Long press power key
    public boolean longPressPower(long pressTime) {
        return longPressKeyCode(android.view.KeyEvent.KEYCODE_POWER, pressTime);
    }

    //Long press home key
    public boolean longPressHome(long pressTime) {
        return longPressKeyCode(android.view.KeyEvent.KEYCODE_HOME, pressTime);
    }
}
