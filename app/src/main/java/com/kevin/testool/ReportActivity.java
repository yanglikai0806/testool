package com.kevin.testool;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.kevin.testool.R.layout.list_report;
import static java.util.Collections.reverse;

public class ReportActivity extends AppCompatActivity {

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
                holder.fn = (TextView) convertView.findViewById(R.id.folder_name);
                holder.sb = (Button) convertView.findViewById(R.id.showBtn);
                // 为view设置标签
                convertView.setTag(holder);
            } else {
                // 取出holder
                holder = (ViewHolder) convertView.getTag();
            }
            holder.fn.setText(list.get(position));
            holder.sb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent_web = new Intent(ReportActivity.this, WebViewActivity.class);
                    intent_web.putExtra("FOLDER_NAME", list.get(position));
                    startActivity(intent_web);
                }
            });
            return convertView;
        }
    }

    public final class ViewHolder {
        private TextView fn;
        private Button sb;
    }
}
