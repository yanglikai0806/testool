package com.kevin.testool;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.kevin.testool.utils.FileUtils;
import com.kevin.testool.utils.ToastUtils;

import java.io.File;

public class MonkeyTestActivity extends AppCompatActivity {

    private EditText c_package;
    private EditText c_count;
    private EditText c_throttle;
    private EditText c_seed;


    private Button m_start;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monkey_test);
        c_package = findViewById(R.id.c_package);
        c_count = findViewById(R.id.c_count);
        c_throttle = findViewById(R.id.c_throttle);
        c_seed = findViewById(R.id.c_seed);
        m_start = findViewById(R.id.m_start);

        m_start.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           String pkg = c_package.getText().toString();
                                           String con = c_count.getText().toString();
                                           String thro = c_throttle.getText().toString();
                                           String seed = c_seed.getText().toString();
                                           if (pkg.contains("/")) {
                                               Intent intent = new Intent(MonkeyTestActivity.this, MonkeyService.class);
                                               intent.setAction("com.kevin.testool.monkey");
                                               intent.putExtra("PKG", pkg);
                                               intent.putExtra("CON", con);
                                               intent.putExtra("THRO", thro);
                                               intent.putExtra("SEED", seed);
                                               startService(intent);
                                               moveTaskToBack(true);
                                           } else {
                                               Intent intent_monkey = new Intent(MonkeyTestActivity.this, MonkeyService.class);
//                                           intent_monkey.setAction("com.kevin.testool.monkey");
                                               intent_monkey.setAction("com.kevin.testool.monkey.voiceassist");
                                               intent_monkey.putExtra("PKG", pkg);
                                               intent_monkey.putExtra("CON", con);
                                               intent_monkey.putExtra("THRO", thro);
                                               intent_monkey.putExtra("SEED", seed);
                                               startService(intent_monkey);
                                               moveTaskToBack(true);
                                           }
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
            new android.app.AlertDialog.Builder(MonkeyTestActivity.this).setTitle("警告！")
                    .setMessage("历史Monkey log将全部被清除")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            FileUtils.RecursionDeleteFile(new File(CONST.MONKEY_PATH));
                            ToastUtils.showShort(MonkeyTestActivity.this, "log清除完成");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
