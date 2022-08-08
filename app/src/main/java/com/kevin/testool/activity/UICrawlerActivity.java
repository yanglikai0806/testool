package com.kevin.testool.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kevin.share.CONST;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.R;
import com.kevin.testool.service.MonkeyService;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;

import org.json.JSONException;

import java.io.File;

import static com.kevin.share.Common.CONFIG;

public class UICrawlerActivity extends BasicActivity {

    private Button startBtn;
    private Button setParamBtn;
    private EditText deepth;
    private EditText pakage;
    private EditText page;
    private TextView paramDis;
    private TextView m_package;
    private TextView m_deepth;
    private TextView m_page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uicrawler);
        startBtn = findViewById(R.id.m_start);
        setParamBtn = findViewById(R.id.m_param);
        deepth = findViewById(R.id.c_deepth);
        pakage = findViewById(R.id.c_package);
        page = findViewById(R.id.c_page);
        paramDis = findViewById(R.id.param_dis);
        m_package = findViewById(R.id.m_package);
        m_deepth = findViewById(R.id.m_deepth);
        m_page = findViewById(R.id.m_page);

        if (!CONFIG().isNull("TARGET_APP")){
            try {
                pakage.setText(CONFIG().getString("TARGET_APP"));
            } catch (JSONException e) {
                logUtil.e("", e);
            }
        }
        startBtn.setOnClickListener(v -> {

            String packageName = pakage.getText().toString();
            if (packageName.length() == 0){
                ToastUtils.showLong(UICrawlerActivity.this, "package 不能为空");
                return;
            }

            Intent crawlerIntent = new Intent(UICrawlerActivity.this, MonkeyService.class);
            crawlerIntent.setAction("com.kevin.testool.monkey.uicrawler");
            crawlerIntent.putExtra("PKG", packageName);
            crawlerIntent.putExtra("DEEPTH", Integer.valueOf(deepth.getText().toString()));
            crawlerIntent.putExtra("PAGE", page.getText().toString());
            startService(crawlerIntent);
            moveTaskToBack(true);

        });

        setParamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        if (new File(CONST.UICRAWLWER_CONFIG_FILE).exists()) {
            paramDis.setText(FileUtils.readJsonFile(CONST.UICRAWLWER_CONFIG_FILE));
            paramDis.setMovementMethod(ScrollingMovementMethod.getInstance());
        }

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
