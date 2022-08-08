package com.kevin.share.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.text.TextUtils;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.kevin.share.AppContext;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.logUtil;

import java.io.File;

/**
 * ocr 识别截图文本
 *
 */
public class Imagett {
    private static String TAG = "IMAGETT";
    private static final String DEFAULT_LANGUAGE = "chi_sim";
    private static String text = "";


    /**
     * 初始化必须的文件夹及训练数据
     */
    public static void init(){
            FileUtils.creatDir(CONST.TESSDATA);
            File chTrainDataFile = new File(CONST.TESSDATA + File.separator + "chi_sim.traineddata");
            if (!chTrainDataFile.exists()){
                String url = "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/chi_sim.traineddata";
                String saveSubPath = "tessdata/"+url.substring(url.lastIndexOf("/") + 1);
                String url2 = "https://raw.githubusercontent.com/yanglikai0806/testool/master/resource/eng.traineddata";
                String saveSubPath2 = "tessdata/"+url.substring(url.lastIndexOf("/") + 1);
                Common.downloadResource(AppContext.getContext(), url, saveSubPath);
                Common.downloadResource(AppContext.getContext(), url2, saveSubPath2);
                logUtil.d("", "下载训练数据");
        }
    }

    /**
     *
     * @param imageFile 识别的图片文件
     * @param language 识别的语言 chi_sim : 中文， eng：英文
     * @param refresh 是否重新获取图片
     * @return
     */

    public static String imageToText(final String imageFile, final String language, boolean refresh, String bounds){ //language :简体中文 chi_sim, 英文 eng

            init();
            try {
                if (!refresh) {

                    return FileUtils.readFile(CONST.TESSDATA + File.separator + "text.txt");

                }
                if (!TextUtils.isEmpty(bounds)) {
                    Common.cropeImage(imageFile, bounds);
                }

                File trainDataFile = new File(CONST.TESSDATA + File.separator + language + ".traineddata");
                if (!trainDataFile.exists()) {
                    return "";
                }

                if (TextUtils.isEmpty(imageFile)){
                    return "";
                }
                logUtil.d("image file", imageFile);
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile);
                        TessBaseAPI tessBaseAPI = new TessBaseAPI();
                        tessBaseAPI.init(CONST.LOGPATH, language);
                        tessBaseAPI.setImage(bitmap);
                        text = tessBaseAPI.getUTF8Text();
                        //识别的文本内容写入的文件中

                        FileUtils.writeFile(CONST.TESSDATA + File.separator + "text.txt", text, false);


                        tessBaseAPI.end();
                    }
                });
                t.start();
                //等待识别完成
                while (t.isAlive()) {
                    SystemClock.sleep(100);
                }
                logUtil.d("本地OCR识别结果", text);
                return text != null ? text : "";
            } catch (Exception e){
                logUtil.e("", e);
                return "";
            }
    }
    }
