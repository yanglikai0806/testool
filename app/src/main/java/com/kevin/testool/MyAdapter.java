package com.kevin.testool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import static com.kevin.testool.CONST.LOGPATH;
import static com.kevin.testool.CONST.TESTCASES_PATH;

public class MyAdapter extends BaseAdapter{
    // 填充数据的list
    private ArrayList<String> list;
    // 用来控制CheckBox的选中状况
    private static HashMap<Integer,Boolean> isSelected;
    // 上下文
    private Context context;
    // 用来导入布局
    private LayoutInflater inflater = null;

    // 构造器
    public MyAdapter(ArrayList<String> list, Context context) {
        this.context = context;
        this.list = list;
        inflater = LayoutInflater.from(context);
        isSelected = new HashMap<Integer, Boolean>();
        // 初始化数据
        initDate();
    }

    // 初始化isSelected的数据
    void initDate(){
        for(int i=0; i<list.size();i++) {
            getIsSelected().put(i,false);
        }
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
            convertView = inflater.inflate(R.layout.list_item, null);
            holder.tv = (TextView) convertView.findViewById(R.id.item_tv);
            holder.cb = (CheckBox) convertView.findViewById(R.id.item_cb);
            // 为view设置标签
            convertView.setTag(holder);
        } else {
            // 取出holder
            holder = (ViewHolder) convertView.getTag();
        }

        // 设置list中TextView的显示
        holder.tv.setText(list.get(position));
        holder.tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ToastUtils.showShort(context, list.get(position));
                android.support.v7.app.AlertDialog.Builder popWindow = new android.support.v7.app.AlertDialog.Builder(context);
                //设置对话框标题
                popWindow.setTitle("用例详情：");
//                设置对话框消息
                String res = logUtil.readToString(TESTCASES_PATH + list.get(position).split("\\.")[1].trim() + ".json");

                TextView showText = new TextView(context);
                showText.setTextSize(18);
                assert res != null;
                showText.setText(res.replace("}},","}},\n").replace(",", ",\n"));

                showText.setTextIsSelectable(true);
                popWindow.setView(showText);

                // 添加选择按钮并注册监听
//                popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
////                        finish();
//                    }
//                });
                popWindow.setNegativeButton("关闭", null);
                //对话框显示
                popWindow.show();
                return true;

            }
        });
        // 根据isSelected来设置checkbox的选中状况
        holder.cb.setChecked(getIsSelected().get(position));
        holder.cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getIsSelected().get(position)){
                    getIsSelected().put(position, false);
                } else {
                    getIsSelected().put(position, true);
                }

            }
        });
        return convertView;
    }

    public static HashMap<Integer,Boolean> getIsSelected() {
        return isSelected;
    }

    public static void setIsSelected(HashMap<Integer,Boolean> isSelected) {
        MyAdapter.isSelected = isSelected;
    }

    public final class ViewHolder {
        private TextView tv;
        private CheckBox cb;
    }

}
