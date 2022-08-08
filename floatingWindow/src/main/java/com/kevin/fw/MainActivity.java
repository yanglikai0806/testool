package com.kevin.fw;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.kevin.share.utils.logUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, FloatingWindowService.class));
            }
        } else if (requestCode == 1) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startService(new Intent(MainActivity.this, RecordCaseService.class));
            }
        } else if (requestCode == 2) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startFloatingButtonService(View view) {
        if (FloatingWindowService.isStarted) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
            } catch (Exception e){
                logUtil.e("", e.getMessage());
            }
        } else {
            startService(new Intent(MainActivity.this, FloatingWindowService.class));
        }
    }

    public void startRecordCaseService(View view) {
        if (FloatingWindowService.isStarted) {
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 1);
            } catch (Exception e){
                logUtil.e("", e.getMessage());
            }
        } else {
            startService(new Intent(MainActivity.this, RecordCaseService.class));
        }
    }


}
