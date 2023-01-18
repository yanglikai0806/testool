package com.kevin.testool.activity;

import static com.kevin.share.Common.reloadCONFIG;

import android.annotation.SuppressLint;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.kevin.share.Common;
import com.kevin.share.utils.ToastUtils;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class ConfigActivity extends BasicActivity {


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_config);
        ScrollView rootView = new ScrollView(this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 10, 0, 10);
        LinearLayout mainView = new LinearLayout(this);
        mainView.setLayoutParams(lp);//设置布局参数
        mainView.setOrientation(LinearLayout.VERTICAL);// 设置子View的Linearlayout// 为垂直方向布局
        //定义子View中两个元素的布局
        ViewGroup.LayoutParams vlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        JSONObject configJson = reloadCONFIG();
        for (Iterator<String> it = configJson.keys(); it.hasNext(); ) {
            String key = it.next();
            // item view
            LinearLayout viewItem = new LinearLayout(this);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            vp.setMargins(15, 5, 15, 20);
            viewItem.setLayoutParams(vp);//设置布局参数
            viewItem.setOrientation(LinearLayout.VERTICAL);// 设置子View的Linearlayout// 为垂直方向布局
            viewItem.setBackgroundColor(getResources().getColor(R.color.white));

            TextView tv1 = new TextView(this);
            TextView tv2 = new TextView(this);
            tv1.setLayoutParams(vlp);//设置TextView的布局
            tv2.setLayoutParams(vlp);
            tv1.setText(key);
            tv1.setTextColor(getResources().getColor(R.color.colorAccent));
            tv2.setHint(configJson.opt(key) + "");
            viewItem.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder editWindow = new AlertDialog.Builder(ConfigActivity.this);
                    View editWindowView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_edit,null);
                    editWindow.setView(editWindowView);
                    editWindow.setTitle(key);
                    final EditText et = editWindowView.findViewById(R.id.caseEdit);
                    Object value = reloadCONFIG().opt(key);
                    String typeName = value.getClass().getSimpleName();
                    et.setText(reloadCONFIG().opt(key) + "");
                    editWindow.setPositiveButton("保存", (dialog1, which1) -> {
                        String etv = et.getText().toString();
                        try {
                            Common.updateCONFIG(String.valueOf(reloadCONFIG().put(key, exchangeType(etv, typeName))));
                            tv2.setText(etv);
                        } catch (Exception e) {
                            ToastUtils.showShort(ConfigActivity.this, e.toString());
                        }
                    });
                    editWindow.setNegativeButton("取消", null);
                    editWindow.show();
                }
            });
            viewItem.addView(tv1);
            viewItem.addView(tv2);
            mainView.addView(viewItem);
        }

        rootView.addView(mainView);
        this.addContentView(rootView,lp);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.toConfigDetail) {
            AlertDialog.Builder popWindow = new AlertDialog.Builder(ConfigActivity.this);
            View dialogView = LayoutInflater.from(ConfigActivity.this).inflate(R.layout.case_detail,null);
            //设置对话框标题
            popWindow.setTitle("配置文件");
//                设置对话框消息
            popWindow.setView(dialogView);
//                String res = FileUtils.readJsonFile(CONFIG_FILE);
            String res = Common.reloadCONFIG().toString();
            final TextView case_detail = dialogView.findViewById(R.id.case_detail);
            case_detail.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
            case_detail.setTextIsSelectable(true);

//                 添加选择按钮并注册监听
            popWindow.setPositiveButton("编辑", (dialog, which) -> {
                AlertDialog.Builder editWindow = new AlertDialog.Builder(ConfigActivity.this);
                View editWindowView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_edit,null);
//                    editWindow.setTitle("编辑用例");
                editWindow.setView(editWindowView);
                final EditText et = editWindowView.findViewById(R.id.caseEdit);
                et.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
                editWindow.setPositiveButton("保存", (dialog1, which1) -> {
                    Common.updateCONFIG(et.getText().toString());

                });
                editWindow.setNegativeButton("取消", null);
                editWindow.show();
            });
            popWindow.setNegativeButton("关闭", null);
            //对话框显示
            popWindow.show();


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Object exchangeType(String value, String theType) throws Exception {
        switch (theType) {
            case "Integer":
                return Integer.valueOf(value);
            case "JSONArray":
                return new JSONArray(value);
            case "JSONObject":
                return new JSONObject(value);
            case "Boolean":
                return Boolean.valueOf(value);
        }
        return value;
    }
}