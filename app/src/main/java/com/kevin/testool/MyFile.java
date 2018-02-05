package com.kevin.testool;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2018/2/2.
 */

public class MyFile {
    private BufferedWriter bw;
    private SimpleDateFormat sdf;

    @SuppressLint("SimpleDateFormat")

    public String creatLogDir() throws FileNotFoundException {
        sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        String time_tag = sdf.format(new Date(System.currentTimeMillis()));
        File ssDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "AutoTest" + File.separator + time_tag + File.separator + "screenshot");
        ssDir.mkdirs();
        return time_tag;
    }

    public void writeFile(String file_name, String content) throws FileNotFoundException {
//        String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
        FileOutputStream fos = new FileOutputStream(new File(file_name),true);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        bw = new BufferedWriter(osw);
        try {
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void deleteFile(String file_name) {
        File myDelFile = new File(file_name);
        try {
            //noinspection ResultOfMethodCallIgnored
            myDelFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void createTempFile(String file_name, String content){

        deleteFile(file_name);
        try {
            writeFile(file_name,content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
