package com.kevin.testool.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kevin.share.CONST;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.kevin.share.CONST.REPORT_PATH;
import static com.kevin.testool.R.layout.list_report;

/**
 * 测试报告
 */

public class ReportActivity extends BasicActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_report);

        listView = new ListView(this);
        listView.setAdapter(new MyAdapter(getData(), this));
        setContentView(listView);
    }

    private ArrayList<String> getData(){
        ArrayList<String> reportLst = new ArrayList<>();
        File reportFolder = new File(CONST.REPORT_PATH);
        if (reportFolder.exists()) {
            for (File f : reportFolder.listFiles()) {
                if (f.isDirectory()) {
                    reportLst.add(f.getName());
                }
            }
        } else {
            ToastUtils.showShort(this, "未发现测试报告");
        }
        Collections.sort(reportLst);
        Collections.reverse(reportLst);
        return reportLst;
    }

    public class MyAdapter extends BaseAdapter{

        // 填充数据的list
        private ArrayList<String> list;
        // 用来控制CheckBox的选中状况
        private HashMap<Integer,Boolean> isSelected;
        // 上下文
        private Context context;
        // 用来导入布局
        private LayoutInflater inflater = null;

        public MyAdapter(ArrayList<String> list, Context context) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                // 获得ViewHolder对象
                holder = new ViewHolder();
                // 导入布局并赋值给convertview
                convertView = inflater.inflate(list_report, null);
                holder.fn = convertView.findViewById(R.id.folder_name);
                holder.sb = convertView.findViewById(R.id.showBtn);
                holder.lb = convertView.findViewById(R.id.logBtn);
                // 为view设置标签
                convertView.setTag(holder);
            } else {
                // 取出holder
                holder = (ViewHolder) convertView.getTag();
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent_web = new Intent(ReportActivity.this, WebViewActivity.class);
                    intent_web.putExtra("FOLDER_NAME", list.get(position));
                    startActivity(intent_web);
                }
            });
            holder.fn.setText(list.get(position));
            holder.sb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent_web = new Intent(ReportActivity.this, WebViewActivity.class);
                    intent_web.putExtra("FOLDER_NAME", list.get(position));
                    startActivity(intent_web);
                }
            });
            holder.lb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder logWindow = new AlertDialog.Builder(context);
                    View dialogView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_detail,null);
                    //设置对话框标题
                    logWindow.setTitle("log：");
//                设置对话框消息
                    logWindow.setView(dialogView);

                    final TextView case_detail = dialogView.findViewById(R.id.case_detail);
                    String res = "";
                    res = FileUtils.readJsonFile(REPORT_PATH + list.get(position) + File.separator+ "log.txt");
                    case_detail.setText(res);
                    logWindow.setNegativeButton("关闭", null);
                    //对话框显示
                    logWindow.show();
                }
            });
            return convertView;
        }
    }

    public final class ViewHolder {
        private TextView fn;
        private Button sb;
        private Button lb;
    }
}
