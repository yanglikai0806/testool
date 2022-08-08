package com.kevin.share.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.kevin.share.Common;
import com.kevin.share.utils.SPUtils;
import com.kevin.share.utils.logUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class RecordService extends Service {
    private static MediaProjection mediaProjection;
    private static MediaRecorder mediaRecorder;
    private static VirtualDisplay virtualDisplay;

    public static boolean running = false;
    public static boolean isShotFinished = false;



    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int zoom = intent.getIntExtra("ZOOM", 1);
        int quality = intent.getIntExtra("QUALITY", 100);
        int sWidth = Common.getScreenWidth();
        int sHeight = Common.getScreenHeight();
        String filePath = intent.getStringExtra("FILE_PATH");
        running = false;
        try {
            if (filePath.toLowerCase().endsWith(".mp4")){
                int seconds = intent.getIntExtra("SECONDS", 15);
                recordScreen(filePath, sWidth / zoom, sHeight / zoom, seconds);
            } else {
                screenShots(filePath, sWidth / zoom, sHeight / zoom, quality);
            }
        } catch (Exception e){
            logUtil.e("", e);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        HandlerThread serviceThread = new HandlerThread("service_thread",
//                android.os.Process.THREAD_PRIORITY_BACKGROUND);
//        serviceThread.start();
        mediaRecorder = new MediaRecorder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }


    public static boolean startRecord(String mp4File, int sWidth, int sHeight) {
        if (mediaProjection == null || running) {
            return false;
        }
        logUtil.d("", "开始录屏");
        initRecorder(mp4File, sWidth, sHeight);
        createVirtualDisplay(sWidth, sHeight);
        mediaRecorder.start();
        running = true;
        return true;
    }

    public static boolean stopRecord() {
        if (!running) {
            return false;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            virtualDisplay.release();
            mediaProjection.stop();
            running = false;
            logUtil.d("", "结束录屏");
            return true;
        } catch (Exception e){
            logUtil.e("", e);
            return false;
        }
    }

    private static void createVirtualDisplay(int width, int height) {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, Resources.getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private static void initRecorder(String mp4File, int sWidth, int sHeight) {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(mp4File);
        mediaRecorder.setVideoSize(sWidth, sHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean screenShots(String imagePath, int sWidth, int sHeight, int quality) {
        if (mediaProjection == null){
            logUtil.d("", "mediaProjection is null");
            return false;
        }
        ImageReader mImageReader = ImageReader.newInstance(
                sWidth,
                sHeight,
                PixelFormat.RGBA_8888, 1);

        VirtualDisplay mVirtualDisplay = mediaProjection.createVirtualDisplay("screen-mirror",
                sWidth,
                sHeight,
                Resources.getSystem().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);

        Image image = null;
        long st = System.currentTimeMillis();
        while (image == null && System.currentTimeMillis() - st < 700){
            SystemClock.sleep(50);
            image = mImageReader.acquireLatestImage();
        }
        if (image == null){
            logUtil.d("", "image is null");
            return false;
        }
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        //每个像素的间距
        int pixelStride = planes[0].getPixelStride();
        //总的间距
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * sWidth;
        Bitmap bitmap = Bitmap.createBitmap(sWidth + rowPadding / pixelStride, sHeight,
                Bitmap.Config.ARGB_8888);//虽然这个色彩比较费内存但是 兼容性更好
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, sWidth, sHeight);
        image.close();
        File fileImage = new File(imagePath);
        if (bitmap != null) {
            try {
                if (!fileImage.exists()) {
                    fileImage.createNewFile();
                }
                FileOutputStream out = new FileOutputStream(fileImage);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                logUtil.e("", e);
                return false;
            }
        }
        mVirtualDisplay.release();
        logUtil.d("", "MediaProjection截图-耗时：" + (System.currentTimeMillis()-st));
        return true;





//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Image image = mImageReader.acquireLatestImage();
//
//                    int width = image.getWidth();
//                    int height = image.getHeight();
//                    final Image.Plane[] planes = image.getPlanes();
//                    final ByteBuffer buffer = planes[0].getBuffer();
//                    //每个像素的间距
//                    int pixelStride = planes[0].getPixelStride();
//                    //总的间距
//                    int rowStride = planes[0].getRowStride();
//                    int rowPadding = rowStride - pixelStride * width;
//                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
//                            Bitmap.Config.ARGB_8888);//虽然这个色彩比较费内存但是 兼容性更好
//                    bitmap.copyPixelsFromBuffer(buffer);
//                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
//                    image.close();
//                    File fileImage = new File(imagePath + File.separator + finalIndex + ".jpg");
//                    if (bitmap != null) {
//                        try {
//
//                            if (!fileImage.exists()) {
//                                fileImage.createNewFile();
//                            }
//                            FileOutputStream out = new FileOutputStream(fileImage);
//                            if (out != null) {
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
//                                out.flush();
//                                out.close();
//                            }
//
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                            fileImage = null;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            fileImage = null;
//                        }
//                    }
//                }
//            }, 300);
    }

    public static boolean recordScreen(String mp4File, int sWidth, int sHeight, int seconds){
        if (startRecord(mp4File, sWidth, sHeight)) {
            SystemClock.sleep(seconds * 1000);
            return stopRecord();
        }
        return false;



    }

    /**
     * 屏幕发生变化时截取
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    // Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);


                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

}