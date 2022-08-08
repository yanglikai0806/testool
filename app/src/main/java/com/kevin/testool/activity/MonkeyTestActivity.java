package com.kevin.testool.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.StringUtil;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.R;
import com.kevin.testool.service.MonkeyService;
import com.kevin.share.utils.ToastUtils;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.kevin.share.Common.CONFIG;

public class MonkeyTestActivity extends FirstActivity {

    private EditText c_package;
    private EditText c_count;
    private EditText c_throttle;
    private EditText c_seed;
    private EditText c_time;
    private CheckBox checkBox;


    private Button m_start;
    private Button m_upload;
    private int selected = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        setContentView(R.layout.activity_monkey_test);
        c_package = findViewById(R.id.c_package);
        c_count = findViewById(R.id.c_count);
        c_throttle = findViewById(R.id.c_throttle);
        c_seed = findViewById(R.id.c_seed);
        c_time = findViewById(R.id.c_time);
        m_start = findViewById(R.id.m_start);
        m_upload = findViewById(R.id.m_upload);
//        checkBox = findViewById(R.id.checkBox);

        c_package.setText(CONFIG().optString("TARGET_APP"));

        setDisable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (StringUtil.isNumeric(Common.getProcessId("uicrawler").trim())|| StringUtil.isNumeric(Common.getMonkeyProcess().trim())){
                    setDisable(true);
                    ToastUtils.showLongByHandler(MonkeyTestActivity.this, "monkey 执行中");
                }
            }
        }).start();

//        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (checkBox.isChecked()){
//                    SPUtils.putBoolean(SPUtils.MY_DATA, "MONKEY_LOG_UPLOAD", true);
//                } else {
//                    SPUtils.putBoolean(SPUtils.MY_DATA, "MONKEY_LOG_UPLOAD", false);
//                }
//
//            }
//        });

        m_start.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           String pkg = c_package.getText().toString();
                                           String con = c_count.getText().toString();
                                           String thro = c_throttle.getText().toString();
                                           String seed = c_seed.getText().toString();
                                           String timeout = c_time.getText().toString();
                                           startMonkeyService("com.kevin.testool.monkey", pkg,con,thro,seed,timeout);
                                           setDisable(true);
                                       }


        });

        m_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder popWindow = new AlertDialog.Builder(MonkeyTestActivity.this);
                popWindow.setTitle("选择文件：");
                File monkeyLogFolder = new File(CONST.MONKEY_PATH);
                if (! monkeyLogFolder.exists()){
                    monkeyLogFolder.mkdirs();
                }
                String[] _logs = new File(CONST.MONKEY_PATH).list();
                ArrayList<String> logs = new ArrayList<>();
                for (String log : _logs) {
                    if (new File(CONST.MONKEY_PATH +log).isDirectory()) {
                        logs.add(log);

                    }
                }
                Collections.reverse(logs);
                String[] choices = new String[logs.size()];
                for (int i=0; i< logs.size(); i++){
                    choices[i] = logs.get(i);
                }
                popWindow.setSingleChoiceItems(choices, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selected = which;
                    }
                });
                popWindow.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uploadLogFolder = CONST.MONKEY_PATH + choices[selected];
                        String zipFilePath = uploadLogFolder + ".zip";
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ZipLogFolder(uploadLogFolder, zipFilePath);
                                    File zipFile = new File(zipFilePath);
                                    if (zipFile.exists()){
                                        HttpUtil.uploadMonkeyLog(zipFile, Common.SERVER_BASE_URL() + "upload", zipFile.getName(), "zipfile", CONST.TARGET_APP);
//                                        FileUtils.deleteFile(zipFile); //删除生成的压缩包
                                    } else {
                                        ToastUtils.showShortByHandler(getApplicationContext(), "未生成log压缩文件");
                                    }
                                } catch (Exception e) {
                                    logUtil.e("", e);
                                }
                            }
                        }).start();

                    }
                });

                popWindow.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                popWindow.show();
            }
        });

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true); // monkey中阻止页面跳转
    }


    private void startMonkeyService(String action,String pkg, String con, String thro, String seed, String timeout){
        Intent intent = new Intent(MonkeyTestActivity.this, MonkeyService.class);
        intent.setAction(action);
        intent.putExtra("PKG", pkg);
        intent.putExtra("CON", con);
        intent.putExtra("THRO", thro);
        intent.putExtra("SEED", seed);
        intent.putExtra("TIMEOUT", timeout);
        startService(intent);
        moveTaskToBack(true);
    }

    /**
     * 压缩 monkey log 文件
     *
     * @param srcFileString 要压缩的文件或文件夹
     * @param zipFileString 压缩完成的Zip路径
     */
    public static void ZipLogFolder(String srcFileString, String zipFileString) throws Exception {
        //创建ZIP
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));
        //创建文件
        File file = new File(srcFileString);
        //压缩
        logUtil.d("","---->"+file.getParent()+"==="+file.getAbsolutePath());
        ZipMonkeyLogs(file.getParent()+ File.separator, file.getName(), outZip);
        //完成和关闭
        outZip.finish();
        outZip.close();
    }

    /**
     * 压缩monkey log文件
     *
     * @param folderString
     * @param fileString
     * @param zipOutputSteam
     */
    private static void ZipMonkeyLogs(String folderString, String fileString, ZipOutputStream zipOutputSteam) throws Exception {
        logUtil.d("", "folderString:" + folderString + "\n" +
                "fileString:" + fileString + "\n==========================");
        if (zipOutputSteam == null)
            return;
        File file = new File(folderString + fileString);
            if (file.isFile()) {
                if (fileString.contains("_error.txt") || fileString.startsWith("bugreport")) {
                    String[] lastFolders = folderString.split("/");
                    String lastFolder = lastFolders[lastFolders.length-1];
                    ZipEntry zipEntry = new ZipEntry(lastFolder+"/"+fileString);
                    FileInputStream inputStream = new FileInputStream(file);
                    zipOutputSteam.putNextEntry(zipEntry);
                    int len;
                    byte[] buffer = new byte[4096];
                    while ((len = inputStream.read(buffer)) != -1) {
                        zipOutputSteam.write(buffer, 0, len);
                    }
                    zipOutputSteam.closeEntry();
                }
            } else {
                //文件夹
                String[] fileList = file.list();
                //没有子文件和压缩
                if (fileList == null){
                    return;
                }
                if (fileList.length == 0) {
                    ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                    zipOutputSteam.putNextEntry(zipEntry);
                    zipOutputSteam.closeEntry();
                }
                //子文件和递归
                for (int i = 0; i < fileList.length; i++) {
                    ZipMonkeyLogs(folderString + fileString + "/", fileList[i], zipOutputSteam);
                }
            }
    }


    private void setDisable(boolean yes){
        try {
            c_package.setEnabled(!yes);
            c_count.setEnabled(!yes);
            c_throttle.setEnabled(!yes);
            c_seed.setEnabled(!yes);
            c_time.setEnabled(!yes);
            m_start.setEnabled(!yes);
            m_upload.setEnabled(!yes);
            checkBox.setEnabled(!yes);
        } catch (Exception ignore){

        }
    }
}
