package com.kevin.share.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TableLayout;

import com.kevin.share.AppContext;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


// Note:
// Here is a copy of android.support.test.uiautomator.AccessibilitiNodeInfoDumper source code
// in order to fix dump hierarchy error
//
// Sync to new code: https://android.googlesource.com/platform/frameworks/testing/+/master/uiautomator/library/core-src/com/android/uiautomator/core/AccessibilityNodeInfoDumper.java
public class AccessibilityNodeInfoDumper {
    private static final String LOGTAG = AccessibilityNodeInfoDumper.class.getSimpleName();
    private static final String[] NAF_EXCLUDED_CLASSES = new String[]{
            GridView.class.getName(), GridLayout.class.getName(),
            ListView.class.getName(), TableLayout.class.getName()
    };
    private static String Bounds = "";
    private static String ClickType = "";
    private static AccessibilityNodeInfo TargetNode = null;
    private static boolean ClickFlag = false;
    private static boolean ReloadFlag = false;

    AccessibilityNodeInfoDumper() {
    }

    public static void dumpWindowHierarchy(AccessibilityService service, OutputStream out) throws IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.setOutput(out, "UTF-8");
        serializer.startDocument("UTF-8", true);
        serializer.startTag("", "hierarchy");
        AccessibilityNodeInfo[] arr$ = getWindowRoots(service);
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            AccessibilityNodeInfo root = arr$[i$];

            WindowManager wm = (WindowManager) AppContext.getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;         // 屏幕宽度（像素）
            int height = dm.heightPixels;       // 屏幕高度（像素）
            float density = dm.density;         // 屏幕密度（0.75 / 1.0 / 1.5）
            int densityDpi = dm.densityDpi;     // 屏幕密度dpi（120 / 160 / 240）
            // 屏幕宽度算法:屏幕宽度（像素）/屏幕密度
            int screenWidth = (int) (width / density);  // 屏幕宽度(dp)
            int screenHeight = (int) (height / density);// 屏幕高度(dp)
            dumpNodeRec(root, serializer, 0, width, height);
        }

        serializer.endTag("", "hierarchy");
        serializer.endDocument();
    }

    public static AccessibilityNodeInfo[] getWindowRoots(AccessibilityService service) {
//        device.waitForIdle();
        Set<AccessibilityNodeInfo> roots = new HashSet();
        AccessibilityNodeInfo activeRoot = service.getRootInActiveWindow();
        if (activeRoot != null) {
            roots.add(activeRoot);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            Iterator i$ = service.getWindows().iterator();

            while (i$.hasNext()) {
                AccessibilityWindowInfo window = (AccessibilityWindowInfo) i$.next();
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) {
//                    Log.w(LOGTAG, String.format("Skipping null root node for window: %s", window.toString()));
                } else {
                    roots.add(root);
                }
            }
        }

        return (AccessibilityNodeInfo[]) roots.toArray(new AccessibilityNodeInfo[roots.size()]);
    }

    public static void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer, int index, int width, int height) throws IOException {

        if (ClickFlag && getNodeInfoByBounds(Bounds, node) != null){
            switch (ClickType){
                case "click":
                    AccessibilityHelper.performClick(TargetNode);
                    ClickFlag = false;
                    break;
                case "long_click":
                    AccessibilityHelper.performLongClick(TargetNode);
                    ClickFlag = false;
                    break;
            }

        }
        serializer.startTag("", "node");
//        serializer.startTag("", safeCharSeqToString(node.getClassName()));
        if (!nafExcludedClass(node) && !nafCheck(node)) {
            serializer.attribute("", "NAF", Boolean.toString(true));
        }

        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "resource-id", safeCharSeqToString(node.getViewIdResourceName()));
        serializer.attribute("", "class", safeCharSeqToString(node.getClassName()));
        serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
        serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
        serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
        serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
        serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
        serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
        serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
        serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
        serializer.attribute("", "scrollable", Boolean.toString(node.isScrollable()));
        serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
        serializer.attribute("", "password", Boolean.toString(node.isPassword()));
        serializer.attribute("", "selected", Boolean.toString(node.isSelected()));
        serializer.attribute("", "visible-to-user", Boolean.toString(node.isVisibleToUser()));
        serializer.attribute("", "bounds", getVisibleBoundsInScreen(node, width, height).toShortString());
        int count = node.getChildCount();

        for (int i = 0; i < count; ++i) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (child.isVisibleToUser()) {
                    dumpNodeRec(child, serializer, i, width, height);
                    child.recycle();
                } else {
//                    Log.i(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
                }
            } else {
//                Log.i(LOGTAG, String.format("Null child %d/%d, parent: %s", i, count, node.toString()));
            }
        }

        serializer.endTag("", "node");
//        serializer.endTag("", safeCharSeqToString(node.getClassName()));
    }

    /**
     * 根据bounds 获取 AccessibilityNodeInfo
     *
     */

    public static AccessibilityNodeInfo getNodeInfoByBounds(String bounds, AccessibilityNodeInfo nodeInfo){
        if (TextUtils.isEmpty(bounds)){
            return null;
        }
        String[] x_y = bounds.replace("[", "").replace("]", ",").split(",");
        int left, top, right, bottom;
        left = Integer.valueOf(x_y[0]);
        top = Integer.valueOf(x_y[1]);
        right = Integer.valueOf(x_y[2]);
        bottom = Integer.valueOf(x_y[3]);
        Rect Rt = new Rect(left, top, right, bottom);

        Rect nodeRt = new Rect();
        nodeInfo.getBoundsInScreen(nodeRt);
        if (Rt.equals(nodeRt)){
            TargetNode = nodeInfo;
            return nodeInfo;
        }

//        if (nodeInfo.getText()!= null && nodeInfo.getText().toString().equals("添加")){
//            Log.d("KEVIN_DEBUG", "reDumpNodeRec: " + nodeInfo.getText());
//            Log.d("KEVIN_DEBUG", "reDumpNodeRec: " + nodeRt);
//            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }
        return null;
    }

    /**
     * The list of classes to exclude my not be complete. We're attempting to
     * only reduce noise from standard layout classes that may be falsely
     * configured to accept clicks and are also enabled.
     *
     * @param node
     * @return true if node is excluded.
     */
    public static boolean nafExcludedClass(AccessibilityNodeInfo node) {
        String className = safeCharSeqToString(node.getClassName());
        for (String excludedClassName : NAF_EXCLUDED_CLASSES) {
            if (className.endsWith(excludedClassName))
                return true;
        }
        return false;
    }

    /**
     * We're looking for UI controls that are enabled, clickable but have no
     * text nor content-description. Such controls configuration indicate an
     * interactive control is present in the UI and is most likely not
     * accessibility friendly. We refer to such controls here as NAF controls
     * (Not Accessibility Friendly)
     *
     * @param node
     * @return false if a node fails the check, true if all is OK
     */
    public static boolean nafCheck(AccessibilityNodeInfo node) {
        boolean isNaf = node.isClickable() && node.isEnabled()
                && safeCharSeqToString(node.getContentDescription()).isEmpty()
                && safeCharSeqToString(node.getText()).isEmpty();
        if (!isNaf)
            return true;
        // check children since sometimes the containing element is clickable
        // and NAF but a child's text or description is available. Will assume
        // such layout as fine.
        return childNafCheck(node);
    }


    /**
     * This should be used when it's already determined that the node is NAF and
     * a further check of its children is in order. A node maybe a container
     * such as LinerLayout and may be set to be clickable but have no text or
     * content description but it is counting on one of its children to fulfill
     * the requirement for being accessibility friendly by having one or more of
     * its children fill the text or content-description. Such a combination is
     * considered by this dumper as acceptable for accessibility.
     *
     * @param node
     * @return false if node fails the check.
     */
    private static boolean childNafCheck(AccessibilityNodeInfo node) {
        int childCount = node.getChildCount();
        for (int x = 0; x < childCount; x++) {
            AccessibilityNodeInfo childNode = node.getChild(x);
            if (childNode == null) {
                Log.i(LOGTAG, String.format("Null child %d/%d, parent: %s",
                        x, childCount, node.toString()));
                continue;
            }
            if (!safeCharSeqToString(childNode.getContentDescription()).isEmpty()
                    || !safeCharSeqToString(childNode.getText()).isEmpty())
                return true;
            if (childNafCheck(childNode))
                return true;
        }
        return false;
    }


    public static String safeCharSeqToString(CharSequence cs) {
        return cs == null ? "" : stripInvalidXMLChars(cs);
    }

    private static String stripInvalidXMLChars(CharSequence cs) {
        // ref: https://stackoverflow.com/questions/4237625/removing-invalid-xml-characters-from-a-string-in-java
        String xml10pattern = "[^"
                + "\u0009\r\n"
                + "\u0020-\uD7FF"
                + "\uE000-\uFFFD"
                + "\ud800\udc00-\udbff\udfff"
                + "]";

        return cs.toString().replaceAll(xml10pattern, "?");
    }

    public static Rect getVisibleBoundsInScreen(AccessibilityNodeInfo node, int width, int height) {
        if (node == null) {
            return null;
        } else {
            Rect nodeRect = new Rect();
            node.getBoundsInScreen(nodeRect);
            Rect displayRect = new Rect();
            displayRect.top = 0;
            displayRect.left = 0;
            displayRect.right = width;
            displayRect.bottom = height;
            nodeRect.intersect(displayRect);
            if (Build.VERSION.SDK_INT >= 21) {  //  UiDevice.API_LEVEL_ACTUAL
                Rect window = new Rect();
                if (node.getWindow() != null) {
                    node.getWindow().getBoundsInScreen(window);
                    nodeRect.intersect(window);
                }
            }

            return nodeRect;
        }
    }

    public static void setTargetBounds(String bds){
        Bounds = bds;
    }

    public static void setClickType(String ct){
        ClickFlag = true;
        ClickType = ct;
    }

}
