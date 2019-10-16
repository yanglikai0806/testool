package com.kevin.testool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.kevin.testool.common.WifiHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WirelessAdb extends Activity {


    private long mExitTime = 0;
    static final double NUM = 0.6;

    private String TAG = "WirelessAdb";
    static final String msgStart = "正在启动adb...";
    static final String msgStop = "正在停止adb...";

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private TextView tvState;
    private TextView tvSwitch;
    private ImageView ivWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        setContentView(R.layout.activity_wireless_adb);

        tvState = findViewById(R.id.tv_state);
        tvSwitch = findViewById(R.id.tv_switch);
        ivWifi = findViewById(R.id.iv_wifi);

        if (!new WifiHelper(getApplicationContext()).isWifi()) {
            tvState.setText(getString(R.string.net_error));
            tvState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent("android.settings.WIFI_SETTINGS"));
                }
            });
            tvSwitch.setEnabled(false);
            ivWifi.setImageResource(R.drawable.wifi_off);
        } else {
            tvState.setOnClickListener(null);
            tvSwitch.setEnabled(true);
            ivWifi.setImageResource(R.drawable.wifi_on);
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        ivWifi.getLayoutParams().width = (int) (dm.widthPixels * NUM);
        ivWifi.getLayoutParams().height = (int) (dm.widthPixels * NUM);

        if (AdbUtils.adbIfRunning()) {
            tvSwitch.setText(getString(R.string.adb_stop));
            String str = getString(R.string.adb_started) + "\n" +
//                    System.getProperty("line.separator", "\\n") +
                    "adb connect " + new WifiHelper(getApplicationContext()).getIPAddress() + ":" + AdbUtils.PORT;
            tvState.setText(str);
            ivWifi.setImageResource(R.drawable.wifi_on);
        } else {
            tvSwitch.setText(getString(R.string.adb_start));
            tvState.setText(getString(R.string.adb_stopped));
            ivWifi.setImageResource(R.drawable.wifi_off);
        }

        tvSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!new WifiHelper(getApplicationContext()).isWifi()) {
                    ToastUtils.showShort(WirelessAdb.this, getString(R.string.wifi_off));

                }
                if (getString(R.string.adb_start).equals(tvSwitch.getText().toString())) {
                    logUtil.d(TAG, "start it");
                    new startAdbTask().executeOnExecutor(executorService, "null_param");
                } else if (getString(R.string.adb_stop).equals(tvSwitch.getText().toString())) {
                    logUtil.d(TAG, "stop it");
                    new stopAdbTask().executeOnExecutor(executorService, "null_param");
                }
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class startAdbTask extends AsyncTask<String, Integer, String> {

        AlertDialog alertDialog = null;
        AlertDialog.Builder builder = null;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(WirelessAdb.this);
            builder.setCancelable(false)
                    .setMessage(msgStart);
            alertDialog = builder.create();
            alertDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return startAdb();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            if (null != alertDialog) {
                alertDialog.dismiss();
            }
            if ("ok".equals(s)) {
                //MToast.Show(MainActivity.this, getString(R.string.adb_started));
                String str = getString(R.string.adb_started) + "\n" +
//                    System.getProperty("line.separator", "\\n") +
                        "adb connect " + new WifiHelper(getApplicationContext()).getIPAddress() + ":" + AdbUtils.PORT;
                tvState.setText("" + str);
                tvState.refreshDrawableState();
                tvSwitch.setText(getString(R.string.adb_stop));
                ivWifi.setImageResource(R.drawable.wifi_on);
            } else {
                ToastUtils.showShort(WirelessAdb.this, getString(R.string.adb_start_fail));
            }
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            logUtil.d(TAG, values[0].toString());
            super.onProgressUpdate(values);
        }
    }

    private class stopAdbTask extends AsyncTask<String, Integer, String> {//params,progress,result

        AlertDialog alertDialog = null;
        AlertDialog.Builder builder = null;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(WirelessAdb.this);
            builder.setCancelable(false)
                    .setMessage(msgStop);
            alertDialog = builder.create();
            alertDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return stopAdb();
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != alertDialog) {
                alertDialog.dismiss();
            }
            if ("ok".equals(s)) {
                //MToast.Show(MainActivity.this, getString(R.string.adb_stopped));
                tvState.setText(getString(R.string.adb_stopped));
                tvSwitch.setText(getString(R.string.adb_start));
                ivWifi.setImageResource(R.drawable.wifi_off);
            } else {
                ToastUtils.showShort(WirelessAdb.this, getString(R.string.adb_stop_fail));
            }
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            logUtil.d(TAG, values[0].toString());
            super.onProgressUpdate(values);
        }
    }


    private String startAdb() {
        if (AdbUtils.adbStart()) {
            return "ok";
        } else {
            return "no";
        }
    }

    private String stopAdb() {
        if (AdbUtils.adbStop()) {
            return "ok";
        } else {
            return "no";
        }
    }
}


