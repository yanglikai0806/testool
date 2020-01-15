package com.kevin.testool;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.kevin.testool.utils.FileUtils;
import com.kevin.testool.utils.ToastUtils;

import java.io.File;

public class UICrawlerActivity extends AppCompatActivity {

    private Button startBtn;
    private EditText deepth;
    private EditText pakage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uicrawler);
        startBtn = findViewById(R.id.startButton);
        deepth = findViewById(R.id.c_deepth);
        pakage = findViewById(R.id.c_package);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String packageName = pakage.getText().toString();
                if (packageName.length() == 0){
                    ToastUtils.showLong(UICrawlerActivity.this,"package 不能为空");
                    return;
                }
                Intent crawlerIntent = new Intent(UICrawlerActivity.this, MonkeyService.class);
                crawlerIntent.setAction("com.kevin.testool.monkey.uicrawler");
                crawlerIntent.putExtra("PKG",  packageName);
                crawlerIntent.putExtra("CON", "1");
                crawlerIntent.putExtra("THRO", "1");
                crawlerIntent.putExtra("SEED", "1");
                crawlerIntent.putExtra("DEEPTH", Integer.valueOf(deepth.getText().toString()));
                startService(crawlerIntent);

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear_log) {
            new android.app.AlertDialog.Builder(UICrawlerActivity.this).setTitle("警告！")
                    .setMessage("历史log将被清除")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            FileUtils.RecursionDeleteFile(new File(CONST.LOGPATH + "UICrawler"));
                            ToastUtils.showShort(UICrawlerActivity.this, "log清除完成");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
