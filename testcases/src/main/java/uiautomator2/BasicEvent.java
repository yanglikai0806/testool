package uiautomator2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BasicEvent {
    private UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    //Get Apps' package name in Launcher
    private String getLauncherPackageName() {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Use PackageManager to get the launcher package name
        PackageManager pm = InstrumentationRegistry.getContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }
    //Get current app's name
    private String getApplicationName() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = InstrumentationRegistry.getContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(mDevice.getCurrentPackageName(), 0);
        }
        catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    //Get current time
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
    private Date curDate = new Date(System.currentTimeMillis());
    private String currentDatetime = formatter.format(curDate);

    //Open an app by its package name
    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public void launch(String packageName, int timeout) {

        // Wait for launcher
        final String launcherPackage = getLauncherPackageName();

        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), timeout);

        // Launch the blueprint app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        intent.addFlags(807403520);

        // Clear out any previous instances
        context.startActivity(intent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(packageName).depth(0)), timeout);
    }

    //Time between every action
    public void wait(int sleep) {
        SystemClock.sleep(sleep);
    }

    //Open Notification bar
    public boolean openNotificationBar() {
        return mDevice.openNotification();
    }

    //Open Notification bar
    public boolean openQuickSettings() {
        return mDevice.openQuickSettings();
    }


    //Screen on(Doing nothing if screen is already on)
    public void wakeUp() throws RemoteException {
        mDevice.wakeUp();
    }

    //Screen off(Doing nothing if screen is already on)
    public void screenOff() throws RemoteException {
        mDevice.sleep();

    }
    public void activeScreen() throws RemoteException {
        if(!mDevice.isScreenOn()){
            mDevice.pressKeyCode(android.view.KeyEvent.KEYCODE_POWER);
//            sleep(1500);
        }
        if(mDevice.getCurrentPackageName().equals("com.android.systemui")){
            mDevice.swipe(400,1000,400,100,20);
//            sleep(2000);
        }
    }

    //Take a screenshot
    public void takeScreenshot(String name) {
        try {
            File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + "/" + name + "/");
            fileDir.mkdir();
            mDevice.takeScreenshot(new File(Environment.getExternalStorageDirectory().getPath()
                    + "/" + name + "/" + currentDatetime + ".png"));
//                +"/"+name+"/"+getApplicationName()+" "+mDevice.getCurrentPackageName()+"_"+currentDatetime+".png"));
        }catch (Exception e){
//            e.printStackTrace();
        }
    }
    public void clearRecentApp() throws RemoteException {
        mDevice.pressRecentApps();
        mDevice.wait(Until.findObject(By.res("com.android.systemui:id/progress_circle_view")), 1000).click();
        SystemClock.sleep(5000);
    }
    public void goBackHome(){
        mDevice.pressBack();
        mDevice.pressBack();
        mDevice.pressHome();
    }
    public void goHome(){
        mDevice.pressHome();
    }
//    public void takeScreenshot(String name) throws IOException {
//        //Get current time
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
//        Date curDate = new Date(System.currentTimeMillis());
//        String currentDatetime = formatter.format(curDate);
//        String savePath="/storage/sdcard0/"+name+"/";
//        File fileDir = new File(savePath);
//
//        if (!fileDir.exists()) {
//            fileDir.mkdir();
//        }
//        Runtime.getRuntime().exec("screencap -p "+savePath+ currentDatetime + ".png");
//
////        device.takeScreenshot(new File(savePath+ currentDatetime + ".png"));
//    }

}

