package com.kevin.testool;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kevin.testool.utils.ToastUtils;
import com.kevin.testool.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class EditCaseActivity extends AppCompatActivity {

    private EditText input_url;
    private EditText input_case_id;
    private EditText input_query;
    private EditText input_wait_time;
    private EditText input_check_point;
    private TextView log_dis;
    private Button btn_run_url;
    private Button btn_debug;
    private Button btn_add;


    private ArrayList<String> list_item;
//    private EditeCaseAdapter mAdapter;
    private JSONArray fileList;
    private int selected;

    private DebugReceiver debugReceiver;

    String case_id = "";
    String query;
    String wait_time;
    String check_point;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_case);

        input_url = findViewById(R.id.input_url);
        input_case_id = findViewById(R.id.input_case_id);
        input_query = findViewById(R.id.input_query);
        input_wait_time = findViewById(R.id.input_wait_time);
        input_check_point = findViewById(R.id.input_check_point);
        log_dis =  findViewById(R.id.log_dis);
        btn_run_url = findViewById(R.id.btn_run_url);
        btn_debug = findViewById(R.id.btn_debug);
        btn_add = findViewById(R.id.btn_add);
        //注册广播
        debugReceiver = new DebugReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.kevin.testool.action.debug.finish");
        registerReceiver(debugReceiver, filter);


        btn_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent_debug =  new Intent(EditCaseActivity.this, MyIntentService.class);
                    intent_debug.setAction("com.kevin.testool.action.debug");
                    intent_debug.putExtra("DEBUG_CASE", generate_case());
                    startService(intent_debug);
                    String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" +  File.separator +"temp.txt";
                    MyFile myFile = new MyFile();
                    myFile.createTempFile(tempFile, myFile.creatLogDir());
                } catch (Exception e) {
                    e.printStackTrace();
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
                                               Toast.makeText(getApplicationContext(), "未发现用例配置文件", Toast.LENGTH_SHORT).show();
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
                                                   e.printStackTrace();
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
                                               try {
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
                                                                           try {
                                                                               logUtil.editCaseJsonFile(input, generate_case()); // 添加测试case到新文件
                                                                               ToastUtils.showShort(EditCaseActivity.this, "添加完成");
                                                                           } catch (IOException e) {
                                                                               e.printStackTrace();
                                                                           }
                                                                       }
                                                                   }
                                                               })
                                                               .setNegativeButton("取消", null)
                                                               .show();
                                                   } else {
                                                       logUtil.editCaseJsonFile(strs[selected].split("\\.")[1].trim(), generate_case()); // 添加测试case到已有用例文件
                                                       ToastUtils.showShort(EditCaseActivity.this, "添加完成");
                                                   }
                                           } catch (IOException e) {
                                               e.printStackTrace();
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

    public String generate_case() {
         case_id = input_case_id.getText().toString().trim();
         if (case_id.trim().length() == 0) {
             case_id = "test";
         }
         query = input_query.getText().toString().replace("，",",").replace(" ",""); //处理中文字符与空格
        if (query.contains("[")){

        } else {
            query = "[" + query + "]";
        }
        if (query.contains("{")){
            query = query.replace("：", ":"); //处理中文字符
//            query = query.replace("\"{", "\"}").replace("}\"", "\"}").replace(":", "\":\"");
        }

        wait_time = input_wait_time.getText().toString().replace("，",",");
        if (wait_time.length() == 0) {
            wait_time = "3";
        }
        wait_time = "[" + wait_time + "]";

        check_point = input_check_point.getText().toString().replace("，",",").replace("：",":");
        if (check_point.length() == 0) {
            check_point = "\"text\":\"\"";
        }
//        check_point = "\""+check_point.replace(":", "\":\"").replace(",", "\",\"")+ "\"";
//        if (check_point.contains("{")){
//            check_point = check_point.replace("\"{", "{\"").replace("}\"", "\"}");
//        }

        // 将参数组装成test case 单元
        String _case = String.format("{\"id\":\"%s\",\n\"case\":{\n\"query\":%s,\n\"wait_time\":%s},\n\"check_point\":{\n%s\n\t}\n}\n", case_id, query,wait_time,check_point);
        System.out.println(_case);
        return _case;

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
                System.out.println(_file.getName());
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.activity_tool) {
            ToastUtils.showShort(EditCaseActivity.this, "5秒后获取activity");
            //启动AlarmService服务实现等待时间
            Intent iw = new Intent(this, MyIntentService.class);
            iw.setAction("com.kevin.testool.action.debug.tool");
            iw.putExtra("TOOL", "activity");
            startService(iw);

            return true;
        }
        if (id == R.id.dump_tool) {
            ToastUtils.showShort(EditCaseActivity.this, "5秒后获取界面元素");
            //启动AlarmService服务实现等待时间
            Intent iw = new Intent(this, MyIntentService.class);
            iw.setAction("com.kevin.testool.action.debug.tool");
            iw.putExtra("TOOL", "dump");
            startService(iw);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class DebugReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent_debug) {
            log_dis.setText(intent_debug.getStringExtra("DEBUG_LOG"));
            ToastUtils.showShort(context, "已获取，将返回测试工具查看");

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
