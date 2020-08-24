package com.kevin.testool.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kevin.testool.R;
import com.kevin.testool.common.HtmlReport;
import com.kevin.share.utils.ToastUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import static android.webkit.WebView.setWebContentsDebuggingEnabled;
import static com.kevin.share.CONST.REPORT_PATH;


public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webview);
        setWebContentsDebuggingEnabled(true);
//        允许解析js
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
//        优先使用缓存
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//        设置此属性，可任意比例缩放
        settings.setUseWideViewPort(true);
//        适应webview
        settings.setLoadWithOverviewMode(true);
//        设置可以支持缩放
        settings.setSupportZoom(true);
//      设置出现缩放工具
        settings.setBuiltInZoomControls(true);
        //不显示webview缩放按钮
        settings.setDisplayZoomControls(false);
//        手势焦点
        webView.requestFocusFromTouch();
//      打开内置浏览器
        WebViewClient webViewClient = new WebViewClient();
        webView.setWebViewClient(webViewClient);
        Intent intent = getIntent();
        String folderName = intent.getStringExtra("FOLDER_NAME");
        if (folderName.length() > 0) {
            File reportHtml = new File(REPORT_PATH + folderName + File.separator + "report.html");
            if (!reportHtml.exists()){
                try {
                    HtmlReport.generateReport(REPORT_PATH + folderName + File.separator+ "log.txt");
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            if (reportHtml.exists()) {
                webView.loadUrl("file:///" + REPORT_PATH + folderName + File.separator + "report.html");
            } else {
                ToastUtils.showShort(this, "未生成报告文件");
            }
        }
    }
}
