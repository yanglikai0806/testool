package com.kevin.testool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.ToastUtils;
import java.util.ArrayList;
import java.util.HashMap;

import static com.kevin.share.CONST.TESTCASES_PATH;

public class MyAdapter extends BaseAdapter{
    // 填充数据的list
    private ArrayList<String> list;
    // 用来控制CheckBox的选中状况
    private static HashMap<Integer,Boolean> isSelected;
    // 上下文
    private Context context;
    // 用来导入布局
    private LayoutInflater inflater = null;

    int touchX;
    int touchY;

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
        //记录触摸坐标
        convertView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        touchX = (int) event.getRawX();
                        touchY = (int) event.getRawY();
                        break;
                }
                return false;
            }
        });
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder=new AlertDialog.Builder(context);
//                builder.setIcon(R.mipmap.ic_launcher);
//                builder.setTitle(R.string.simple_list_dialog);
                final String[] Items={"删除","同步","上传"};
                builder.setItems(Items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String caseFileName = list.get(position).split("\\.", 2)[1].trim();
                        String fp = TESTCASES_PATH + caseFileName + ".json";
                        switch(Items[i]){
                            case "删除":
                                FileUtils.deleteFile(fp);
                                context.sendBroadcast(new Intent(CONST.ACTION_UPDATECASELIST));
                                break;
                            case "同步":
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Common.syncTestcase(caseFileName);
                                    }
                                }).start();

                                break;
                            case "上传":
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Common.updateTestCases(FileUtils.readJsonFile(fp))){
                                            ToastUtils.showShortByHandler(context, "上传完成");
                                        } else {
                                            ToastUtils.showShortByHandler(context, "上传失败");
                                        }
                                    }
                                }).start();
                                break;

                        }
                    }
                });
                builder.setCancelable(true);
                AlertDialog dialog=builder.create();
                dialog.show();
                Window window = dialog.getWindow();
                //重新设置
                WindowManager.LayoutParams lp = window.getAttributes();
                window .setGravity(Gravity.START | Gravity.TOP);
                lp.x = touchX; // 新位置X坐标
                lp.y = touchY-100; // 新位置Y坐标
                lp.width = 500; // 宽度
                lp.height = 500; // 高度
//                lp.alpha = 0.7f; // 透明度

//                dialog.onWindowAttributesChanged(lp);
//                (当Window的Attributes改变时系统会调用此函数)
                window.setAttributes(lp);
//                dialog.show();
                dialog.setView(v,0,0,0,0);
                return false;
            }
        });
        holder.tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                ToastUtils.showShort(context, list.get(position));
                String caseFileName = list.get(position).split("\\.", 2)[1].trim();
                AlertDialog.Builder popWindow = new AlertDialog.Builder(context);
                View dialogView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_detail,null);
                //设置对话框标题
                popWindow.setTitle(caseFileName);
//                设置对话框消息
                popWindow.setView(dialogView);
                String fp = TESTCASES_PATH + caseFileName + ".json";
                String res = FileUtils.readJsonFile(fp);
                final TextView case_detail = dialogView.findViewById(R.id.case_detail);
                case_detail.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
                case_detail.setTextIsSelectable(true);

//                 添加选择按钮并注册监听
                popWindow.setPositiveButton("编辑", (dialog, which) -> {
                    AlertDialog.Builder editWindow = new AlertDialog.Builder(context);
                    View editWindowView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.case_edit,null);
//                    editWindow.setTitle("编辑用例");
                    editWindow.setView(editWindowView);
                    final EditText et = editWindowView.findViewById(R.id.caseEdit);
                    et.setText(res.replace("}},","}},\n").replace("\",", "\",\n").replace("\n\n", "\n"));
                    editWindow.setPositiveButton("保存", (dialog1, which1) -> {
                        FileUtils.writeFile(fp, et.getText().toString(), false);

                            });
                    editWindow.setNegativeButton("取消", null);
                    editWindow.show();
                });
                popWindow.setNegativeButton("关闭", null);
                //对话框显示
                popWindow.show();
//                return true;

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
