package com.kevin.share.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.kevin.share.CONST;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.logUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ocr 识别截图文本
 *
 */
public class Imagett {
    private static String TAG = "IMAGETT";
    private static final String DEFAULT_LANGUAGE = "chi_sim";
    private static String text;


    /**
     * 初始化必须的文件夹及训练数据
     */
    public static void init(String language){
            FileUtils.creatDir(CONST.TESSDATA);
            final File languageFile = new File(CONST.TESSDATA + File.separator + language + ".traineddata");
            if (!languageFile.exists()){
                logUtil.d("imagett", "下载训练数据包");
                Request request = new Request.Builder()
                        .url("https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/"+language+".traineddata")
                        .build();
                try {
                    Response response = new OkHttpClient().newCall(request).execute();
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    try {
//                            long total = response.body().contentLength();
//                            Log.e(TAG, "total------>" + total);
                        long current = 0;
                        is = response.body().byteStream();
                        fos = new FileOutputStream(languageFile);
                        while ((len = is.read(buf)) != -1) {
                            current += len;
                            fos.write(buf, 0, len);
//                                Log.e(TAG, "current------>" + current);
                        }
                        fos.flush();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                logUtil.d("imagett", "文件已存在：" + languageFile);
            }
    }

    /**
     *
     * @param imageFile 识别的图片文件
     * @param language 识别的语言 chi_sim : 中文， eng：英文
     * @param refresh 是否重新获取图片
     * @return
     */

    public static String imageToText(final String imageFile, final String language, boolean refresh){ //language :简体中文 chi_sim, 英文 eng
        init(language);
        if (!refresh){
            try {
                return FileUtils.readFile(CONST.TESSDATA + File.separator + "text.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile);
                TessBaseAPI tessBaseAPI = new TessBaseAPI();
                tessBaseAPI.init(CONST.LOGPATH, language);
                tessBaseAPI.setImage(bitmap);
                text = tessBaseAPI.getUTF8Text();
//                logUtil.i(TAG, "run: text " + System.currentTimeMillis() + text);
                //识别的文本内容写入的文件中
                try {
                    FileUtils.writeFile(CONST.TESSDATA + File.separator + "text.txt", text, false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                tessBaseAPI.end();
            }
        });
        t.start();
        //等待识别完成
        while (t.isAlive()){
            SystemClock.sleep(100);
        }
        return text;
    }
    }
