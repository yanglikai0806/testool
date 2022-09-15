package com.kevin.share.utils;

import android.os.SystemClock;
import android.text.TextUtils;

import com.kevin.share.AppContext;
import com.kevin.share.Common;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {


    /**
     *
     * @param url 接口url
     * @param data json格式的string
     */
    public static void postJson(String url, String data) {
        logUtil.d("postJson", url);
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logUtil.d("POST_RESULT","数据上传失败");
                logUtil.d("POST_RESULT", e.getMessage());
//                Looper.prepare();
//                Looper.loop();
            }

            @Override
            public void onResponse(Call call, Response response) {

//                try {
//                    logUtil.d("POST_RESULT", response.body().string());
//                } catch (IOException e) {
//                    logUtil.d("POST_RESULT", e.toString());
//                }
                response.close();
            }
        });
    }

    public static String postResp(String url, String data){
        logUtil.d("postResp", url);
        // 开启wifi
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            String msg = response.body().string();
            logUtil.d("postResp-------", msg);
            return msg.trim();
        } catch (Exception e) {
            logUtil.e("postResp", e);
            return String.format("{\"error\": \"%s\"}", e.getMessage());
        }
    }

    public static String getResp(String url) {
//        logUtil.d("getResp", url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            Response response = new OkHttpClient().newCall(request).execute();
            String msg = response.body().string();

            return msg.trim();
        } catch (IOException e) {
            logUtil.e("错误", e);
            return "null";
        }
    }

    public static String uploadFile(File mFile, String url, String fileName, String partName){
        String res = "";
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(partName, fileName,
                        RequestBody.create(MediaType.parse("multipart/form-data"), mFile))
                .build();

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url(url)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            res = response.body().string();
            logUtil.d("uploadFile", res);
        } catch (Exception e) {
            logUtil.e("", e);

        }
        return res;
    }


    public static String postFile(String url, File file, String filename, JSONObject param){
        Iterator<String> itr = param.keys();
        ArrayList<String> keys = new ArrayList<>();
        while (itr.hasNext()) {
            keys.add(itr.next());
        }
        RequestBody requestBody;
        try {
            if (keys.size() > 0) {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, RequestBody.create(MediaType.parse("application/octet-stream"), file))
                        .addFormDataPart("data", param.toString())
                        .build();
            } else {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, RequestBody.create(MediaType.parse("application/octet-stream"), file))
                        .build();
            }
            Request request = new Request.Builder()
                    .header("Content-Type", "multipart/form-data")
                    .url(url)
                    .post(requestBody)
                    .build();

            final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
            OkHttpClient okHttpClient = httpBuilder
                    //设置超时
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .readTimeout(100, TimeUnit.SECONDS)
                    .writeTimeout(100, TimeUnit.SECONDS).build();

            Response response = okHttpClient.newCall(request).execute();
            String res = response.body().string();
            logUtil.d("postFile", res);
            return res;
        } catch (Exception e){
            logUtil.e("", e);
            return String.format("{\"error\": \"%s\"}", e.toString());

        }

    }

    public static void uploadMonkeyLog(File mFile, String url, String fileName, String partName, String targetApp){
        int reConnectWifiCount = 3;
        while (!Common.isNetworkConnected(AppContext.getContext()) && reConnectWifiCount > 0){
            logUtil.d("", "网络不可用尝试重连");
            Common.openWifi();
            reConnectWifiCount --;
            SystemClock.sleep(2000);
        }

        logUtil.d("上传monkey log", url);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("target_app", targetApp)
                .addFormDataPart("device_id", Common.getDeviceId())
                .addFormDataPart("device_name", Common.getDeviceAlias())
                .addFormDataPart(partName, fileName,
                        RequestBody.create(MediaType.parse("multipart/form-data"), mFile))
                .build();

        Request request = new Request.Builder()
                .header("Content-Type", "multipart/form-data")
                .url(url)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            String resp = response.body().string();

            logUtil.d("uploadFile", resp + "");
            ToastUtils.showLongByHandler(AppContext.getContext(), resp+"");

        } catch (IOException e) {
            logUtil.e("", e);
            logUtil.d("uploadFile", e.toString());
            ToastUtils.showLongByHandler(AppContext.getContext(), e.toString());

        }
    }
}
