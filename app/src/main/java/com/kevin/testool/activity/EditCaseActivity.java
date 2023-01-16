package com.kevin.testool.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevin.fw.FloatingButtonService;
import com.kevin.fw.TextDisplayWindowService;
import com.kevin.share.CONST;
import com.kevin.testool.MyIntentService;
import com.kevin.testool.R;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import static com.kevin.share.CONST.ACTION_DEBUG;
import static com.kevin.share.CONST.EDIT_TESTCASE;
import static com.kevin.share.CONST.TESTOOL_SETTING;


public class EditCaseActivity extends BasicActivity {

    private TextView log_dis;
    private Button btn_debug;
    private Button btn_add;
    private EditText case_info;
    private ScrollView case_view;
    private CheckBox isHome;


    private ArrayList<String> list_item;
    //    private EditeCaseAdapter mAdapter;
    private JSONArray fileList;
    private int selected;

    private DebugReceiver debugReceiver;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_case);

        log_dis = findViewById(R.id.log_dis);
        btn_debug = findViewById(R.id.btn_debug);
        btn_add = findViewById(R.id.btn_add);
        isHome = findViewById(R.id.checkBox);
        //注册广播
        debugReceiver = new DebugReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.kevin.testool.action.debug.finish");
        registerReceiver(debugReceiver, filter);
        //
        case_view = findViewById(R.id.caseView);
        case_info = findViewById(R.id.case_info);

        SharedPreferences tc = getSharedPreferences(TESTOOL_SETTING, 0);
        String defaultCase = "{\"id\": 0,\n" +
                "  \"case\": {\n" +
                "    \"cid\": \"\",\n" +
                "    \"step\": [{\"text\":\"设置\"}],\n" +
                "    \"wait_time\": [3]},\n" +
                "  \"check_point\": {\n" +
                "    \"text\": [\"我的设备\"],\n" +
                "    \"activity\": \"\"},\n" +
                "  \"skip_condition\": {}\n" +
                "}";
        String debugCase = tc.getString(EDIT_TESTCASE, defaultCase);

        case_info.setText(debugCase);


        btn_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (isHome.isChecked()){
                        Common.press("home");
                    }
                    Intent intent_debug =  new Intent(EditCaseActivity.this, MyIntentService.class);
                    intent_debug.setAction(ACTION_DEBUG);
                    intent_debug.putExtra("DEBUG_CASE", case_info.getText().toString());
                    tc.edit().putString(EDIT_TESTCASE, case_info.getText().toString()).apply();
                    startService(intent_debug);
                    String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" +  File.separator +"temp.txt";
                    FileUtils.createTempFile(tempFile, FileUtils.creatLogDir());
                } catch (Exception e) {
                    logUtil.e("", e);
                }
            }
        });
        btn_add.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               final AlertDialog.Builder popWindow = new AlertDialog.Builder(EditCaseActivity.this);
               popWindow.setTitle("选择文件：");

               JSONArray fileList;
               ArrayList<String> cases_list = new ArrayList<String>();
               cases_list.add("0. 新建文件");
               File testcasesFolder = new File(CONST.TESTCASES_PATH);
               if (! testcasesFolder.exists()){
                   testcasesFolder.mkdirs();
               }
               fileList = getAllTestCases(CONST.TESTCASES_PATH, "json", "reload", "");
               logUtil.d("EditCase", fileList.toString());
               for (int i = 0; i < fileList.length(); i++) {
                   JSONObject caseItem;
                   try {
                       caseItem = fileList.getJSONObject(i);

                       try {
                           cases_list.add((i+1) + ". "+ caseItem.get("name"));
                       } catch (Exception ignored) {

                       }

                   } catch (JSONException e) {
                       logUtil.e("", e);
                   }
               }
               final String[] strs = cases_list.toArray(new String[]{}); // arryList 转 数组
               popWindow.setSingleChoiceItems(strs, 0, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {

                       ToastUtils.showShort(EditCaseActivity.this,strs[which]);
                       selected = which;
                   }
               });
               popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) {
                       if (selected == 0){
                           final EditText et = new EditText(EditCaseActivity.this);
                           // 新建文件名称输入弹框
                           new AlertDialog.Builder(EditCaseActivity.this).setTitle("请输入文件名")
                                   .setIcon(android.R.drawable.ic_dialog_info)
                                   .setView(et)
                                   .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                       public void onClick(DialogInterface dialog, int which) {
                                           String input = et.getText().toString();
                                           if (input.equals("")) {
                                               Toast.makeText(getApplicationContext(), "搜索内容不能为空！" + input, Toast.LENGTH_LONG).show();
                                           } else{
                                               FileUtils.editCaseJsonFile(input, case_info.getText().toString()); // 添加测试case到新文件
                                               ToastUtils.showShort(EditCaseActivity.this, "添加完成");
                                           }
                                       }
                                   })
                                   .setNegativeButton("取消", null)
                                   .show();
                       } else {
                           FileUtils.editCaseJsonFile(strs[selected].split("\\.")[1].trim(), case_info.getText().toString()); // 添加测试case到已有用例文件
                           ToastUtils.showShort(EditCaseActivity.this, "添加完成");
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
        );
    }

    private JSONArray getAllTestCases(String dir, String _type, String flag, String tag) {
        if (flag.equals("reload")) {
            fileList = new JSONArray();
        }
        File casesDir = new File(dir);
        if (!casesDir.exists()) {
            Toast.makeText(getApplicationContext(), "导入用例失败", Toast.LENGTH_SHORT).show();
        }
        File[] files = casesDir.listFiles();

//        JSONArray fileList = new JSONArray();
//        JSONObject _fInfo = new JSONObject();
        for (File _file : files) {//遍历目录
            if (_file.isFile() && _file.getName().endsWith(_type)) {
                String _name = _file.getName();
                String filePath = _file.getAbsolutePath();//获取文件路径
                String fileName = _file.getName().substring(0, _name.length() - 5);//获取文件名

                try {
                    JSONObject _fInfo = new JSONObject();
                    _fInfo.put(fileName, filePath);
                    if (tag.length() > 0) {
                        _fInfo.put("name", tag + "/" + fileName);
                    } else {
                        _fInfo.put("name", fileName);
                    }
                    _fInfo.put("path", filePath);
                    fileList.put(_fInfo);
                } catch (Exception ignored) {

                }
            } else if (_file.isDirectory()) {//查询子目录
                getAllTestCases(_file.getAbsolutePath(), _type, "append", _file.getName());
            }
        }
//        System.out.println(fileList);
        return fileList;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main2, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent fbsIntent = new Intent(this, FloatingButtonService.class);
        if (id == R.id.activity_tool) {
//            ToastUtils.showShort(EditCaseActivity.this, "5秒后获取activity");
//            Intent iw = new Intent(this, MyIntentService.class);
//            iw.setAction("com.kevin.testool.action.debug.tool");
//            iw.putExtra("TOOL", "activity");
//            startService(iw);
            fbsIntent.putExtra("BUTTON_TEXT", "activity");
            startService(fbsIntent);

            return true;
        }
        if (id == R.id.dump_tool) {
            fbsIntent.putExtra("BUTTON_TEXT", "dump");
            startService(fbsIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class DebugReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent_debug) {
            log_dis.setText(intent_debug.getStringExtra("DEBUG_LOG"));

            Intent textDisplayIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);
            textDisplayIntent.putExtra("TEXT", intent_debug.getStringExtra("DEBUG_LOG"));
            startService(textDisplayIntent);

            if (intent_debug.hasExtra("RESULT")) {
                if (intent_debug.getStringExtra("RESULT").equals("true")) {
                    case_view.setBackgroundColor(Color.parseColor("#FF4CAF50"));

                } else {
                    case_view.setBackgroundColor(Color.parseColor("#FFF44336"));

                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debugReceiver != null){
            unregisterReceiver(debugReceiver);
        }
    }

}
