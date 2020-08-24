package com.kevin.testool.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import com.kevin.share.utils.FileUtils;
import com.kevin.testool.MyApplication;
import com.yorhp.tyhjffmpeg.Mv2Gif;
import com.yorhp.tyhjffmpeg.Setting;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AVUtils {

    public static MediaExtractor mediaExtractor;


    public static void mp4ToGif(String mp4File, int mp4Time, boolean toDeleteMp4){
        if(!new File(mp4File).exists()){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Setting setting = new Setting(true,
                        720,
                        1080,
                        15,
                        0,
                        mp4Time
                );
                String gifFile = mp4File.replace(".mp4", ".gif");
                boolean success = false;
                try {
                    success = Mv2Gif.convert(mp4File, mp4File.replace(".mp4", ".gif"), setting);
                } catch (Exception e){
                    e.printStackTrace();
                }
                if (toDeleteMp4 && success){
                    FileUtils.deleteFile(mp4File);
                }
            }
        }).start();


    }


    public static boolean getAudioFromMp4(String mp4File, String audioFile){
        boolean isFinish;
        MediaMuxer mMediaMuxer;
        int mAudioTrackIndex = 0;
        long frameRate;

        try {
            mediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mediaExtractor.setDataSource(mp4File);//媒体文件的位置
//            System.out.println("==========getTrackCount()===================" + mediaExtractor.getTrackCount());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
                    ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 2);
                    {   mediaExtractor.selectTrack(i);//选择此音频轨道
                        mediaExtractor.readSampleData(buffer, 0);
                        long first_sampletime = mediaExtractor.getSampleTime();
                        mediaExtractor.advance();
                        long second_sampletime = mediaExtractor.getSampleTime();
                        frameRate = Math.abs(second_sampletime - first_sampletime);//时间戳
                        mediaExtractor.unselectTrack(i);
                    }
                    mediaExtractor.selectTrack(i);
//                    System.out.println("==============frameRate111=============="+frameRate+"");
                    mMediaMuxer = new MediaMuxer(audioFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mAudioTrackIndex = mMediaMuxer.addTrack(format);
                    mMediaMuxer.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.presentationTimeUs = 0;

                    int sampleSize = 0;
                    while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = mediaExtractor.getSampleFlags();
                        info.presentationTimeUs += frameRate;
                        mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, info);
                        mediaExtractor.advance();
                    }

                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                }
            }
            isFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
            isFinish = false;
        }finally {
            mediaExtractor.release();
            mediaExtractor = null;
        }
        return isFinish;
    }

    public static boolean isSameAudio(String audio1, String audio2){
        String res1 = FileUtils.xToBase64(audio1);
        String res2 = FileUtils.xToBase64(audio2);

        if (res1.contains(res2) | res2.contains(res1)){
            System.out.println("-----------------音频相同-------------");
        }
        return true;


    }

    public static void muxerdata() {
        String srcPath = Environment.getExternalStorageDirectory()
                .getPath() + "/DCIM/Camera/123.mp4";

        String dirP = Environment.getExternalStorageDirectory()
                .getPath() + "/demo2";
        String fPath1 = Environment.getExternalStorageDirectory()
                .getPath() + "/demo2/demo1.mp4";
        String fPath2 = Environment.getExternalStorageDirectory()
                .getPath() + "/demo2/demo2.mp4";
        File file = new File(dirP);
        if (!file.exists()){
            file.mkdir();
        }

        File file1 = new File(fPath1);
        File file2 = new File(fPath2);
        try {
            if (file1.exists()){
                file1.delete();

            }
            file1.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (file2.exists()){
                file2.delete();
            }
            file2.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaMuxer mMediaMuxer;
        int mVideoTrackIndex = 0;
        int mAudioTrackIndex = 0;
        long frameRate;

        try {
            mediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mediaExtractor.setDataSource(srcPath);//媒体文件的位置
            System.out.println("==========getTrackCount()===================" + mediaExtractor.getTrackCount());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
                    ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 2);
                    {   mediaExtractor.selectTrack(i);//选择此音频轨道
                        mediaExtractor.readSampleData(buffer, 0);
                        long first_sampletime = mediaExtractor.getSampleTime();
                        mediaExtractor.advance();
                        long second_sampletime = mediaExtractor.getSampleTime();
                        frameRate = Math.abs(second_sampletime - first_sampletime);//时间戳
                        mediaExtractor.unselectTrack(i);
                    }
                    mediaExtractor.selectTrack(i);
                    System.out.println("==============frameRate111=============="+frameRate+"");
                    mMediaMuxer = new MediaMuxer(fPath2, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mAudioTrackIndex = mMediaMuxer.addTrack(format);
                    mMediaMuxer.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.presentationTimeUs = 0;

                    int sampleSize = 0;
                    while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) > 0) {
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = mediaExtractor.getSampleFlags();
                        info.presentationTimeUs += frameRate;
                        mMediaMuxer.writeSampleData(mAudioTrackIndex, buffer, info);
                        mediaExtractor.advance();
                    }

                    mMediaMuxer.stop();
                    mMediaMuxer.release();

                }

                if (mime.startsWith("video")){
                    mediaExtractor.selectTrack(i);//选择此视频轨道
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    System.out.println("==============frameRate222=============="+ 1000 * 1000 / frameRate+"");
                    mMediaMuxer = new MediaMuxer(fPath1, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mVideoTrackIndex = mMediaMuxer.addTrack(format);
                    mMediaMuxer.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    info.presentationTimeUs = 0;
                    ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 2);
                    int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                    System.out.println(mediaExtractor.readSampleData(buffer, 0));
                    while (sampleSize > 0) {
                        info.offset = 0;
                        info.size = sampleSize;
                        info.flags = mediaExtractor.getSampleFlags();
                        info.presentationTimeUs += 1000 * 1000 / frameRate;
                        mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
                        mediaExtractor.advance();
                        sampleSize = mediaExtractor.readSampleData(buffer, 0);
                    }

                    mMediaMuxer.stop();
                    mMediaMuxer.release();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            mediaExtractor.release();
            mediaExtractor = null;
        }

    }
}
