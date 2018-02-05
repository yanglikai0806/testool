package uiautomator2;

import android.app.Instrumentation;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import java.io.IOException;


public class SettingEvent {
    private UiDevice mDevice;
    private Instrumentation instrumentation;
    private TouchEvent touch = new TouchEvent();
    private CheckEvent check = new CheckEvent();
    private BasicEvent basic = new BasicEvent();
    private WifiManager mWifiManager;
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    private LocationManager mLocationManager;

    public SettingEvent(Context context) {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        mDevice = UiDevice.getInstance(instrumentation);

        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLocationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

    }
    public void launchSettings() throws IOException {
        mDevice.executeShellCommand("am start -n com.android.settings/.GnSettingsTabActivity");
    }
    public void openWifi() {

        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            if (check.isObjectExistByText("确定",2000)) {
                touch.clickByObjectText("确定",1000);
            }
        }
    }
    public void closeWifi() {

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            if (check.isObjectExistByText("确定",2000)) {
                touch.clickByObjectText("确定",1000);
            }
        }
    }
    public void openBluetooth() {
        mBtAdapter.enable();

    }
    public void closeBluetooth() {
        mBtAdapter.disable();

    }
    public void switchButton(String id, int index, boolean expectation) throws UiObjectNotFoundException {
        UiObject btn = mDevice.findObject(new UiSelector().resourceId(id).index(index));
        if (btn.isChecked() !=expectation){
            btn.click();
            SystemClock.sleep(1000);
        }

    }


}

