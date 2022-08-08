package com.kevin.share.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

/**
 * Base64和Bitmap相互转换类
 */
public class BitmapUtil {

    /**
     * bitmap转为base64
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 解析base64资源
     * @param base64Data
     * @return
     */
    public static byte[] decodeBase64(String base64Data) {
        if (base64Data == null) {
            return null;
        }
        return Base64.decode(base64Data, Base64.DEFAULT);
    }

    /**
     * Bitmap转byte数组
     * @param bitmap
     * @return
     */
    public static byte[] bitmapToByte(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                return baos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * base64转为bitmap
     *
     * @param base64
     * @return
     */
    public static Bitmap base64ToBitmap(byte[] base64) {
        if (base64 == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(base64, 0, base64.length);
    }

    /**
     * base64转为bitmap
     *
     * @param base64Data
     * @return
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 压缩图片到限定大小以内
     *
     * @param bitmap    被压缩的图片
     * @param sizeLimit 大小限制  单位 k
     * @return 压缩后的图片
     */
    public static Bitmap compressBitmap(Bitmap bitmap, long sizeLimit) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 90;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        // 循环判断压缩后图片是否超过限制大小
        while (baos.toByteArray().length / 1024 > sizeLimit) {
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            quality -= 10;
        }
        return BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()), null, null);
    }

    /**
     * 二进制数组 转 Bitmap
     *
     * @param data
     * @return
     */
    public static Bitmap decodeImg(byte[] data) {
        Bitmap bitmap = null;

        byte[] imgByte = null;
        InputStream input = null;
        try {
            if (data == null)
                return null;
            imgByte = data;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            input = new ByteArrayInputStream(imgByte);
            SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(input, null, options));
            bitmap = (Bitmap) softRef.get();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (imgByte != null) {
                imgByte = null;
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logUtil.e("", e);
                }
            }
        }
        return bitmap;
    }

    /**
     * Bitmap 转二进制
     * @param bitmap
     * @return
     */
    public static byte[] getByteBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] datas = baos.toByteArray();
        return datas;
    }

}
