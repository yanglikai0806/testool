package com.kevin.testool.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kevin.fw.RecordCaseService;
import com.kevin.share.CONST;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RecordCaseActivity extends AppCompatActivity {
    private Button startRecord;
    private EditText caseFile;
    private EditText caseName;
    private EditText caseDesc;
    String caseId;
    String caseDes;

    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_case);
        startRecord = findViewById(R.id.start_record);
        caseFile = findViewById(R.id.case_file);
        caseName = findViewById(R.id.case_name);
        caseDesc = findViewById(R.id.case_desc);

        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Settings.canDrawOverlays(RecordCaseActivity.this)) {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
                } else {
                    caseId = caseName.getText().toString();
                    if (TextUtils.isEmpty(caseId)){
                        caseId = "-1";
                    }
                    caseDes = caseDesc.getText().toString();

//                    if (TextUtils.isEmpty(caseId)){
//                        caseName.setBackgroundResource(R.drawable.edite_rk);
//                        caseName.setHint("case id 不能为空");
//                    } else {
                        caseName.setBackgroundResource(R.drawable.edite_bk);
                        moveTaskToBack(true);
                        Intent rcIntent = new Intent(RecordCaseActivity.this, RecordCaseService.class);
                        rcIntent.putExtra("CASE_NAME", caseId);
                        rcIntent.putExtra("CASE_DESC", caseDes);
                        rcIntent.putExtra("CASE_FILE", caseFile.getText().toString());
                        startService(rcIntent);
//                    }
                }

            }
        });

        caseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCaseFile();
            }
        });

    }

    private void selectCaseFile(){
        final AlertDialog.Builder popWindow = new AlertDialog.Builder(RecordCaseActivity.this);
        popWindow.setTitle("选择文件：");

        ArrayList<String> cases_list = new ArrayList<String>();

        File testcasesFolder = new File(CONST.TESTCASES_PATH);
        if (! testcasesFolder.exists()){
            testcasesFolder.mkdirs();
        }
        JSONArray fileList = Common.getAllTestCases(CONST.TESTCASES_PATH, "json", "reload", "");
        for (int i = 0; i < fileList.length(); i++) {
            JSONObject caseItem;
            try {
                caseItem = fileList.getJSONObject(i);

                try {
                    cases_list.add(caseItem.get("name")+"");
                } catch (Exception ignored) {

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(cases_list, String.CASE_INSENSITIVE_ORDER);
        cases_list.add("新建文件");
        final String[] strs = cases_list.toArray(new String[]{}); // arryList 转 数组
        popWindow.setSingleChoiceItems(strs, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                ToastUtils.showShort(RecordCaseActivity.this, strs[which]);
                selected = which;
            }
        });
        popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (selected == cases_list.size()-1){
                    final EditText et = new EditText(RecordCaseActivity.this);
                    // 新建文件名称输入弹框
                    new AlertDialog.Builder(RecordCaseActivity.this).setTitle("请输入文件名")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setView(et)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String input = et.getText().toString();
                                    if (input.equals("")) {
                                        Toast.makeText(getApplicationContext(), "搜索内容不能为空！" + input, Toast.LENGTH_LONG).show();
                                    } else{
                                        caseFile.setText(input);
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    caseFile.setText(strs[selected]);
                }
            }
        });

        popWindow.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        popWindow.show();
    }
}
