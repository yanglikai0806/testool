package com.kevin.testool.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import com.kevin.testool.MyApplication;
import com.kevin.share.utils.logUtil;

import java.util.List;

public class BaseAccessibility extends AccessibilityService {

    private static AccessibilityManager mAccessibilityManager;
    private static Context mContext;
    private static BaseAccessibility mInstance;

    public void init(Context context) {
        mContext = context.getApplicationContext();
        mAccessibilityManager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    public static BaseAccessibility getInstance() {
        if (mInstance == null) {
            mInstance = new BaseAccessibility();
        }
        return mInstance;
    }



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
