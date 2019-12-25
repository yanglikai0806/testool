package com.kevin.testool;

import android.annotation.SuppressLint;

import com.kevin.testool.utils.logUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by Administrator on 2018/2/2.
 */

public class MyFile {
    private static BufferedWriter bw;
    private static SimpleDateFormat sdf;

    @SuppressLint("SimpleDateFormat")
    public static String creatLogDir() {
        sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String time_tag = sdf.format(new Date(System.currentTimeMillis()));
        File ssDir = new File(CONST.REPORT_PATH + time_tag + File.separator + "screenshot");
        ssDir.mkdirs();
        return time_tag;
    }

    public static void writeFile(String file_name, String content, Boolean append) throws FileNotFoundException {
//        String tempFile = Environment.getExternalStorageDirectory() + File.separator + "AutoTest" + File.separator + "temp.txt";
        File file = new File(file_name);
        FileOutputStream fos = new FileOutputStream(file,append);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        bw = new BufferedWriter(osw);
        try {
            bw.write(content);
//            logUtil.d("writeFile",content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void deleteFile(String file_name) {
        File myDelFile = new File(file_name);
        if(myDelFile.exists()){
            myDelFile.delete();
            logUtil.d("deleteFile", "删除文件:"+file_name);

        } else {
            logUtil.d("deleteFile", "文件不存在:"+file_name);
        }
    }

    public static void RecursionDeleteFile(File file) {
        System.out.println("删除文件夹：" + file.toString());
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                RecursionDeleteFile(f);
            }
            file.delete();
        }
    }


    public static void createTempFile(String file_name, String content){

//        deleteFile(file_name);
        try {
            writeFile(file_name,content, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    public static void unzip(String zipFilePath, String unzipFilePath) throws Exception{
        //验证是否为空
//        if (!isEmpty(zipFilePath)) {
//            isEmpty(unzipFilePath);
//        }
        File zipFile = new File(zipFilePath);
        //创建解压缩文件保存的路径
        File unzipFileDir = new File(unzipFilePath);
        if (!unzipFileDir.exists()){
            unzipFileDir.mkdirs();
        }
        //开始解压
        ZipEntry entry = null;
        String entryFilePath = null;
        int count = 0, bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>)zip.entries();
        //循环对压缩包里的每一个文件进行解压
        while(entries.hasMoreElements()){
            entry = entries.nextElement();
//            System.out.println("log ing5:"+entry.getName());
            //这里提示如果当前元素是文件夹时，在目录中创建对应文件，如果是文件，得出路径交给下一步处理
            entryFilePath = unzipFilePath + File.separator + entry.getName();
            File file = new File(entryFilePath);
            System.out.println("~~是否是文件夹:"+file.isDirectory());
            if(entryFilePath.endsWith("/")){
                if(!file.exists()){
                    file.mkdir();
                }
                continue;
            }
            //这里即是上一步所说的下一步，负责文件的写入
            bos = new BufferedOutputStream(new FileOutputStream(entryFilePath+"/"));
            bis = new BufferedInputStream(zip.getInputStream(entry));
            while ((count = bis.read(buffer, 0, bufferSize)) != -1){
                bos.write(buffer, 0, count);
            }
            bos.flush();
            bos.close();
        }
    }

    public static void creatDir(String path) {
        File ssDir = new File(path);
        if (!ssDir.exists()) {
            ssDir.mkdirs();
            logUtil.d("MyFile", "创建文件夹" + path);
        } else {
            logUtil.d("MyFile", "文件夹已存在" + path);
        }

    }


    public static void writeCaseJsonFile(String Name, JSONArray content) {
//        logPath = Environment.getExternalStorageDirectory() + File.separator + "AutoTest";//获得文件储存路径
        String fileName = CONST.TESTCASES_PATH  + Name +".json";

        if (Name.contains("/")) {
            fileName = CONST.TESTCASES_PATH + Name.split("/")[0] + File.separator + Name.split("/")[1] + ".json";
            File Folder = new File(CONST.TESTCASES_PATH + Name.split("/")[0]);
            if (!Folder.exists()) {
                Folder.mkdirs();//创建父路径
                File file = new File(fileName);
                if (! file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        File Folder = new File(CONST.TESTCASES_PATH);
        if (!Folder.exists()) {
            Folder.mkdirs();//创建父路径
            File file = new File(fileName);
            if (! file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {

            fos = new FileOutputStream(fileName, false);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            assert content != null;
            bw.write(String.valueOf(content));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String readFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        if (!file.exists()) {
            logUtil.d("MyFile", "文件不存在");
            return "";
        }
        String line;
        BufferedReader br = new BufferedReader(new FileReader(file));
        while((line = br.readLine()) != null){
            sb.append(line);
        }

        br.close();
        return sb.toString();
    }


    public static boolean copyFile(File src, String destPath) {
        boolean result = false;
        if ((src == null) || (destPath== null)) {
            return result;
        }
        File dest= new File(destPath);
        if (dest!= null && dest.exists()) {
            dest.delete(); // delete file
        }
        try {
            dest.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            srcChannel = new FileInputStream(src).getChannel();
            dstChannel = new FileOutputStream(dest).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
        try {
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String readJsonFile(String file){
        File tcJson = new File(file);

        Long fileLengthLong = tcJson.length();
        byte[] fileContent = new byte[fileLengthLong.intValue()];
        try {
            FileInputStream inputStream = new FileInputStream(tcJson);
            inputStream.read(fileContent);
            inputStream.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return new String(fileContent);
    }

    /**
     * 压缩文件和文件夹
     *
     * @param srcFileString 要压缩的文件或文件夹
     * @param zipFileString 压缩完成的Zip路径
     * @throws Exception
     */
    public static void ZipFolder(String srcFileString, String zipFileString) throws Exception {
        //创建ZIP
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));
        //创建文件
        File file = new File(srcFileString);
        //压缩
        logUtil.d("","---->"+file.getParent()+"==="+file.getAbsolutePath());
        ZipFiles(file.getParent()+ File.separator, file.getName(), outZip);
        //完成和关闭
        outZip.finish();
        outZip.close();
    }

    /**
     * 压缩文件
     *
     * @param folderString
     * @param fileString
     * @param zipOutputSteam
     * @throws Exception
     */
    private static void ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) throws Exception {
        logUtil.d("","folderString:" + folderString + "\n" +
                "fileString:" + fileString + "\n==========================");
        if (zipOutputSteam == null)
            return;
        File file = new File(folderString + fileString);
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(fileString);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[4096];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }
            zipOutputSteam.closeEntry();
        } else {
            //文件夹
            String fileList[] = file.list();
            //没有子文件和压缩
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }
            //子文件和递归
            for (int i = 0; i < fileList.length; i++) {
                ZipFiles(folderString+fileString+"/",  fileList[i], zipOutputSteam);
            }
        }
    }

    public static void editJsonFile(String filePath, JSONObject JO) {
        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        JSONObject json_content = null;
        try {
            String content = readJsonFile(filePath);
//            System.out.println(content);

            fos = new FileOutputStream(filePath, false);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            if (content.isEmpty()) {
                bw.write(JO.toString());
            } else {
                try {
                    json_content = new JSONObject(content);
                    json_content.put(JO.keys().next(), JO.getString(JO.keys().next()));
//                    Iterator<?> iterator = json_content.keys();// 应用迭代器Iterator 获取所有的key值
//                    while (iterator.hasNext()) { // 遍历每个key
//                        String key = (String) iterator.next();
//                        if (!JO.isNull(key)){
//                            json_content.put(key, JO.getString(JO.keys().next()));
//                            break;
//                        }
//
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                bw.write(String.valueOf(json_content).replace(",", ",\n"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



}
