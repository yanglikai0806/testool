package com.kevin.testcases;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName("com.kevin.testool","com.kevin.testool.MainActivity"));
//        Intent intent = getPackageManager().getLaunchIntentForPackage("com.kevin.testool");
        String[] casesList = new String[]{"case1", "case2","case3"};
        intent.putExtra("test_case", casesList);
        startActivity(intent);
        Toast.makeText(getApplicationContext(), "测试用例已加载", Toast.LENGTH_SHORT).show();
        finish();
    }

}
