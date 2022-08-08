package com.kevin.share.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.SystemClock;
import android.text.TextUtils;

import com.kevin.share.CONST;
import com.kevin.share.Checkpoint;
import com.kevin.share.Common;
import com.kevin.share.ocr.Imagett;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.kevin.share.CONST.DUMP_PNG_PATH;
import static com.kevin.share.CONST.REPORT_PATH;
import static com.kevin.share.Common.isInBounds;
import static com.kevin.share.Common.parseBounds;
import static com.kevin.share.utils.HttpUtil.postResp;
import static com.kevin.share.Common.screenShot;
import static java.lang.Math.abs;
import static org.opencv.core.Core.NORM_MINMAX;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imdecode;

public class CvUtils {

    private static String TAG = "CvUtils";

    /**
     * opencv 模板匹配实现
     * @param inFile
     * @param templateFile
     * @param outFile
     * @param match_method
     * @param param 整数数组,[index, mLimit, similarity] index 表示界面有多个匹配元素时，选择第index+1个，mLimit 限制匹配精度范围0~100
     *              similarity 限制相似度范围0-100
     * @return 返回 bounds
     */
    public static String getMatchImgByTemplate(String inFile, String templateFile, String outFile, int match_method, String targetBounds, double... param) {
//        TM_SQDIFF = 0,
//        TM_SQDIFF_NORMED = 1,
//        TM_CCORR = 2,
//        TM_CCORR_NORMED = 3,
//        TM_CCOEFF = 4,
//        TM_CCOEFF_NORMED = 5;
//        TM_SQDIFF是平方差匹配；TM_SQDIFF_NORMED是标准平方差匹配。利用平方差来进行匹配,最好匹配为0.匹配越差,匹配值越大。
//        TM_CCORR是相关性匹配；TM_CCORR_NORMED是标准相关性匹配。采用模板和图像间的乘法操作,数越大表示匹配程度较高, 0表示最坏的匹配效果。
//        TM_CCOEFF是相关性系数匹配；TM_CCOEFF_NORMED是标准相关性系数匹配。将模版对其均值的相对值与图像对其均值的相关值进行匹配,1表示完美匹配,-1表示糟糕的匹配,0表示没有任何相关性(随机序列)。
//        总结：随着从简单的测量(平方差)到更复杂的测量(相关系数),我们可获得越来越准确的匹配(同时也意味着越来越大的计算代价)。
        int mIndex = -1;
        double mLimit = 0.98;
        double similarity = 0;
        double howMatch = 0;
        String resBounds = "";
        double mResize = 0.5;
        if (param != null){
            if (param.length > 0){
                mIndex = (int) param[0];
            }
            if (param.length > 1){
                mLimit = param[1];
            }
            if (param.length > 2){
                similarity = param[2];
            }
        }

        Mat img = Imgcodecs.imread(inFile);
        Imgproc.resize(img, img, new Size(), mResize, mResize);
        Mat templ = Imgcodecs.imread(templateFile);
        Imgproc.resize(templ, templ, new Size(), mResize, mResize);
//        double[] bgr = templ.get(50, 50);
//        logUtil.i("", "red:"+bgr[0]+"green:"+bgr[1]+"blue:"+bgr[2]);
        logUtil.i("", "匹配模板:");
        logUtil.i("", new File(templateFile).getName());
        double Limit = mLimit;
        logUtil.d("limit系数", Limit + "");

        // / Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        int result_rows = img.rows() - templ.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        // / Do the Matching and Normalize
        Imgproc.matchTemplate(img, templ, result, match_method);
        Core.normalize(result, result, 0, 1, NORM_MINMAX, -1, new Mat()); //归一化
        //匹配多个目标模板
        if (mIndex >= 0 && match_method>2){
            //遍历result结果,找相似结果通过Limit筛选。
            //因为我们用的是TM_CCOEFF_NORMED，所以结果越接近1标识匹配值越大，所以我们将匹配值大于0.9的区域框起来实现多目标匹配
            int tIndex = 0;
            boolean findFlag; //标记是否找到目标
            ok:
            for(int i = 0;i<result.rows();i++) {
                findFlag = false;
                for(int j = 0;j<result.cols();j++){
                    double okValue =result.get(i,j)[0];
                    if(okValue>Limit) {	//绘制匹配到的结果
                        Imgproc.rectangle(img ,new Point(j,i),new Point(j+templ.cols(),i+templ.rows()),new Scalar( 0, 225, 0),1);
                        Imgproc.putText(img,"#"+tIndex,new Point(j-1,i-1),Imgproc.FONT_HERSHEY_SCRIPT_COMPLEX, 1.0, new Scalar(0, 0, 225),1);
                        if (mIndex == tIndex){
                            resBounds = String.format("[%s,%s][%s,%s]", j/mResize, i/mResize, (j+templ.cols())/mResize, (i+templ.rows())/mResize);
                            howMatch = okValue;
                            break ok;
                        }

                        tIndex++;
                        j = j+templ.cols()-1; // 避免相邻区域重复匹配
                        findFlag = true;
                    }
                }
                if (findFlag){
                    i = i+templ.rows()-1;
                }
            }
        } else {
            // / Localizing the best match with minMaxLoc
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            Point matchLoc;
            if (match_method < 2) {
                matchLoc = mmr.minLoc;
                howMatch = mmr.minVal;
            } else {
                matchLoc = mmr.maxLoc;
                howMatch = mmr.maxVal;
            }


            // / Show me what you got
            Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x + templ.cols(),
                    matchLoc.y + templ.rows()), new Scalar(0, 255, 0), 1);
            resBounds = String.format("[%s,%s][%s,%s]", (int) matchLoc.x/mResize, (int) matchLoc.y/mResize, (int) (matchLoc.x + templ.cols())/mResize, (int) (matchLoc.y + templ.rows())/mResize);
        }
//        FileUtils.deleteFile(inFile); //删除截图源文件
        logUtil.i("", "匹配方法："+match_method + ",匹配结果:" + howMatch);
        if (TextUtils.isEmpty(targetBounds)){
            Imgcodecs.imwrite(outFile, img);
            logUtil.i("", new File(outFile).getName());
            logUtil.d("resbounds", resBounds);

            if (howMatch > Limit){
                if (similarity > 0) {
                    if (isSameImage(inFile, templateFile, resBounds, mResize,similarity)){
                        logUtil.i("", "true|匹配到目标image");
                    } else {
                        logUtil.i("", "false|没有匹配到目标image");
                        return null;
                    }
                } else {
                    logUtil.i("", "true|匹配到目标image");
                }
                return resBounds;
            } else {
                logUtil.i("", "false|没有匹配到目标image");
                return null;
            }
        } else {
            //画出targetbounds范围
            Rect targetRect = parseBounds(targetBounds);
            Imgproc.rectangle(img ,new Point(targetRect.left*mResize,targetRect.top*mResize),new Point(targetRect.right*mResize,targetRect.bottom*mResize),new Scalar( 225, 0, 0),2);
        }
        Imgcodecs.imwrite(outFile, img);
        logUtil.i("", new File(outFile).getName());

        logUtil.d("resbounds", resBounds);
        logUtil.d("targetBounds", targetBounds);
        if (Common.isInBounds(resBounds, targetBounds, 50)){
            //相似度判断
            if (similarity > 0) {
                if (isSameImage(inFile, templateFile, resBounds, mResize, similarity)){
                    logUtil.i("", "true|匹配到目标image");
                } else {
                    logUtil.i("", "false|没有匹配到目标image");
                    return null;
                }
            } else {
                logUtil.i("", "true|匹配到目标image");
            }
            return resBounds;
        } else {
            logUtil.i("", "false|没有匹配到目标image");
            return null;
        }
    }

    /**
     * 对比图片相似度
     * @param inFile
     * @param templateFile
     * @param bounds
     * @return
     */

    public static boolean isSameImage(String inFile, String templateFile, String bounds, double mResize, double target){
        Mat img = Imgcodecs.imread(inFile, IMREAD_COLOR );
        Mat templ = Imgcodecs.imread(templateFile, IMREAD_COLOR);
        return isSameImage(img, templ, bounds, mResize, target);
    }

    public static boolean isSameImage(Mat srcMat, Mat desMat, String bounds, double mResize, double target){
        if (!TextUtils.isEmpty(bounds)) {
            Rect rt = parseBounds(bounds);
            srcMat = srcMat.submat(rt.top, rt.bottom, rt.left, rt.right);
        }
        if (mResize != 1) {
            Imgproc.resize(desMat, desMat, new Size(), mResize, mResize);
            Imgproc.resize(srcMat, srcMat, new Size(), mResize, mResize);
        }
        boolean a = isSimilarColor(srcMat.clone(), desMat.clone(), null, target);
        boolean b = isSimilarContour(srcMat, desMat, null, target);
        return a && b ;
//        double[] bgr = templ.get(50, 50);
//        logUtil.i("", "red:"+bgr[2]+"green:"+bgr[1]+"blue:"+bgr[1]);
    }

    /**
     * 颜色相似度判断 根据直方图统计
     * @param srcMat
     * @param desMat
     * @param target
     * @return
     */
    public static boolean isSimilarColor(Mat srcMat, Mat desMat, String bounds, double target){
        if (!TextUtils.isEmpty(bounds)) {
            Rect rt = parseBounds(bounds);
            srcMat = srcMat.submat(rt.top, rt.bottom, rt.left, rt.right);
        }
        return compareHist(srcMat, desMat, target);
    }


    /**
     * 轮廓相似度判断
     * @param srcMat
     * @param desMat
     * @param target
     * @return
     */
    public static boolean isSimilarContour(Mat srcMat, Mat desMat, String bounds, double target){
        if (!TextUtils.isEmpty(bounds)) {
            Rect rt = parseBounds(bounds);
            srcMat = srcMat.submat(rt.top, rt.bottom, rt.left, rt.right);
        }
        try { //轮廓匹配
//            List<MatOfPoint> srcCounts = FindContours(mImg);
//            List<MatOfPoint> desCounts = FindContours(templ);
            Mat srcCounts = FindContours(srcMat);
            Mat desCounts = FindContours(desMat);
            if (!srcCounts.empty() && !desCounts.empty()) {
//                double msRes = Imgproc.matchShapes(srcCounts.get(0), desCounts.get(0), 1, 0.0);
                double msRes = Imgproc.matchShapes(srcCounts, desCounts, 1, 0.0) * 10;
                if (msRes < 1 && msRes >=0){
                    msRes = 1- msRes;
                } else{
                    msRes = 0.1;
                }
                logUtil.i(TAG, "轮廓匹配相似度：" + msRes);
                return msRes > target;
            }
        } catch (Exception e){
            logUtil.e("", e);
        }
        return false;
    }

    /**
     * 直方图相似度比较
     * @param srcMat
     * @param desMat
     */
    public static boolean compareHist(Mat srcMat,Mat desMat, double target){
//        Mat srcHsv = new Mat();
//        Mat desHsv = new Mat();
//        Imgproc.cvtColor(srcMat, srcHsv, Imgproc.COLOR_BGR2HSV);
//        Imgproc.cvtColor(desMat, desHsv, Imgproc.COLOR_BGR2HSV);
//        srcHsv.convertTo(srcHsv, CvType.CV_32F);
//        desHsv.convertTo(desHsv, CvType.CV_32F);
        Mat histSrc = new Mat();
        Mat histDes = new Mat();

        srcMat.convertTo(srcMat, CvType.CV_32F);
        desMat.convertTo(desMat, CvType.CV_32F);
        double value = 0;
        for (int i=0; i<1; i++) {
            histSrc = mHist(srcMat, "src").get(i);
            histDes = mHist(desMat, "des").get(i);
            value = Imgproc.compareHist(histSrc, histDes, Imgproc.CV_COMP_CORREL);
            logUtil.i(TAG, i +"直方图相似度：" + value);
//            double value1 = Imgproc.compareHist(histSrc, histDes, Imgproc.CV_COMP_BHATTACHARYYA);
//            logUtil.d(TAG, i+"BHATTACHARYYA相似度-：" + (1-value1));
        }

        return value > target;
    }

    /**
     * 生成直方图
     * @param srcMat
     * @param flag
     * @return
     */

    public static ArrayList<Mat> mHist(Mat srcMat, String flag) {
//        Mat mMat = new Mat();
//        Imgproc.cvtColor(srcMat, mMat, type);//转换为灰度图
        // Imgproc.HoughCircles(rgbMat, gray,Imgproc.CV_HOUGH_GRADIENT, 1, 18);
        // //霍夫变换找园
        ArrayList<Mat> hists = new ArrayList<>();
        int mHistSizeNum = 5;
        MatOfInt[] mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
//        float[] mBuff = new float[mHistSizeNum];
        MatOfInt mHistSize = new MatOfInt(mHistSizeNum);
        MatOfFloat mRanges = new MatOfFloat(80f, 220f);
        Mat mMat0  = new Mat();
//        Scalar[] mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
//        Point mP1 = new Point();
//        Point mP2 = new Point();

        ArrayList<Mat> gbrChanel = new ArrayList<>();
        Core.split(srcMat, gbrChanel);
        Mat gChanel = gbrChanel.get(0);
//        Mat bChanel = gbrChanel.get(1);
//        Mat rChanel = gbrChanel.get(2);
//        Imgcodecs.imwrite("/sdcard/"+flag+"0.png", srcMat);
//        logUtil.d("**********", gbrChanel.toString()+ "");
//        Imgcodecs.imwrite("/sdcard/"+flag+"1.png", gChanel);
//        Imgcodecs.imwrite("/sdcard/"+flag+"2.png", bChanel);
//        Imgcodecs.imwrite("/sdcard/"+flag+"3.png", rChanel);
//        Size sizeRgba = srcMat.size();
        Mat hist = new Mat(); //转换直方图进行绘制
        for(int c=0; c<1; c++) {
            Imgproc.calcHist(Arrays.asList(srcMat), mChannels[c], mMat0, hist, mHistSize, mRanges);
//            Core.normalize(hist, hist, sizeRgba.height, 0, Core.NORM_INF);
            Core.normalize(hist, hist, 0, 1, NORM_MINMAX, -1, new Mat());
//            logUtil.d("hist", hist.dump());
//            hist.get(0, 0, mBuff);
//            for(int h=0; h<mHistSizeNum; h++) {
//                mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
//                mP1.y = sizeRgba.height-1;
//                mP2.y = mP1.y - 2 - (int)mBuff[h];
////                Imgproc.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
//                Imgcodecs.imwrite("/sdcard/"+flag+c+".png", gbrChanel.get(c));
//            }
            hists.add(hist);
        }

        return hists;
    }

    /**
     * 通过特征匹配识别图像
     * @param inFile
     * @param templateFile
     * @param outFile
     * @param match_method
     * @param targetBounds
     * @param param
     * @return bounds
     */
    public static String getMatchImgByFeature(String inFile, String templateFile, String outFile, String match_method, String targetBounds, double... param){
        double similarity = 0;
        double mResize = 0.5;
        if (param != null){
            if (param.length > 0){
                similarity = param[0];
            }
        }
        Mat img1 = Imgcodecs.imread(templateFile, Imgcodecs.IMREAD_GRAYSCALE);
        Mat img2 = Imgcodecs.imread(inFile, Imgcodecs.IMREAD_GRAYSCALE);
        if (img1.empty() || img2.empty()) {
            logUtil.d("","Cannot read images!");
        }

        //-- Step 1: Detect the keypoints using SIFT Detector, compute the descriptors
        int nfeatures = 0;
        int nOctaveLayers = 5;
        double contrastThreshold = 0.02;
        double edgeThreshold = 2000;
        double sigma = 1.6;
//
//        nfeatures：特征点数目（算法对检测出的特征点排名，返回最好的nfeatures个特征点）。
//        nOctaveLayers：金字塔中每组的层数。
//        contrastThreshold：过滤掉较差的特征点的对阈值。contrastThreshold越大，返回的特征点越少。
//        edgeThreshold：过滤掉边缘效应的阈值。edgeThreshold越大，特征点越多（被滤掉的越少）。
//        sigma：金字塔第0层图像高斯滤波系数，也就是σ。
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint(), keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
        if (match_method.equals("sift")) {
            SIFT detector = SIFT.create(nfeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
            detector.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
            detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);
        }
        if (match_method.equals("orb")){
            ORB detector = ORB.create();
            detector.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
            detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);
        }

        //-- Step 2: Matching descriptor vectors with a FLANN based matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);
//        matcher.radiusMatch(descriptors1, descriptors2, knnMatches, 100);
        logUtil.d("knnMatches's size", knnMatches.size() +"");
        //-- Filter matches
        float ratioThresh = 0.7f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    logUtil.d("0", matches[0].distance + "");
                    logUtil.d("1", matches[1].distance + "");
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }

        //计算匹配范围
        logUtil.d("goodMatches's size", listOfGoodMatches.size() + "");
        logUtil.d("keypoints1's size", keypoints1.toArray().length + "");
        if (listOfGoodMatches.size() < keypoints1.toArray().length/4) {
            logUtil.i("", "false|未匹配到目标image，特征匹配度不足25%");
            return null;
        }
        // 获取关键坐标点
        int trainId = 0;
        Point refrence_point = new Point();
        ArrayList<Point> goodPoint = new ArrayList<>();
        for (int i=0; i< listOfGoodMatches.size(); i++) {
//            logUtil.d("", listOfGoodMatches.get(i).queryIdx + "");
//            queryId = listOfGoodMatches.get(i).queryIdx;
            trainId = listOfGoodMatches.get(i).trainIdx;
            if (i == 0){
                refrence_point = keypoints2.toArray()[trainId].pt;
            }
//            logUtil.d("trainId", trainId + "");
            Point match_point = keypoints2.toArray()[trainId].pt;
        //过滤掉偏离较大的点
            if (Math.abs(match_point.x - refrence_point.x) > 3 * img1.cols() || Math.abs(match_point.y - refrence_point.y) > 3 * img1.rows()) {
                continue;
            }
            goodPoint.add(match_point);
        }
        String matchBounds = matchBoundsFromPoints(goodPoint);
        logUtil.d("", matchBounds);
        Rect mathRect = parseBounds(matchBounds);
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        Imgproc.rectangle(imgMatches ,new Point(mathRect.left + img1.cols(),mathRect.top),new Point(mathRect.right + img1.cols(),mathRect.bottom),new Scalar( 0, 255, 0),2);
        logUtil.i("", new File(outFile).getName());
        if (!TextUtils.isEmpty(targetBounds)){
            Rect targetRect = parseBounds(targetBounds);
            Imgproc.rectangle(imgMatches ,new Point(targetRect.left + img1.cols(),targetRect.top),new Point(targetRect.right + img1.cols(),targetRect.bottom),new Scalar( 255, 0, 0),2);
            Imgcodecs.imwrite(outFile, imgMatches);
            if (isInBounds(matchBounds, targetBounds, 50)) {
                //相似度判断
                if (similarity > 0) {
                    if (isSameImage(inFile, templateFile, matchBounds, mResize, similarity)){
                        logUtil.i("", "true|匹配到目标image");
                    } else {
                        logUtil.i("", "false|没有匹配到目标image");
                        return null;
                    }
                } else {
                    logUtil.i("", "true|匹配到目标image");
                }
                return matchBounds;
            } else {
                logUtil.i("", "false|未匹配到目标image");
                return null;
            }
        }
        logUtil.i("", "true|匹配到目标image");
        Imgcodecs.imwrite(outFile, imgMatches);
        return matchBounds;
    }


    public static boolean getMatchImgFromVideo(String videoPath, String templateFile, int match_method, String targetBounds, int gap,double... param){
        File file = new File(videoPath);
        if(file.exists()){
            //提供统一的接口用于从一个输入媒体中取得帧和元数据
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            logUtil.d("", "视频时长:"+ time + " ms");

            double seconds = Double.valueOf(time)/ 1000;
            int i = 1;
            while (seconds > 0){
                Bitmap bitmap = retriever.getFrameAtTime(i*gap*1000, MediaMetadataRetriever.OPTION_CLOSEST);
                String framePicture = file.getPath().replace(".mp4", "_" +i+ ".jpg");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(framePicture);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.close();
//                    图片对比
                    if (getMatchImgByTemplate(framePicture, templateFile, framePicture, match_method, targetBounds, param) != null){
                        return true;
                    } else {
//                        FileUtils.deleteFile(framePicture);
                        seconds -= gap/1000.00;
                        i++;
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else {
            logUtil.d("", "视频文件不存在");
        }
        return false;
    }


    public static boolean getMatchTextFromVideo(String videoPath, String text, String language, int gap , String bounds, boolean refresh){
        File file = new File(videoPath);
        if(file.exists()){
            //提供统一的接口用于从一个输入媒体中取得帧和元数据
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            logUtil.d("", "视频时长:"+ time + " ms");

            int mseconds = Integer.valueOf(time);

            int i = 1;
            while (mseconds > 0){
                Bitmap bitmap = retriever.getFrameAtTime((mseconds - i*gap)*1000, MediaMetadataRetriever.OPTION_CLOSEST);
                if (i*gap > mseconds){
                    logUtil.d("", "帧获取时间够了");
                    break;
                }
                if (bitmap == null){
                    i = i+1;
                    logUtil.d("", "bitmap is null");
                    continue;
                }
                String framePicture = file.getPath().replace(".mp4", "_" +i+ ".jpg");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(framePicture);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.close();
                    // ocr识别

                    boolean result = Checkpoint.checkIfExistByImage(text, language, framePicture, true, bounds);
//                    ArrayList<Boolean> result=new ArrayList<>();
//                    if (text.contains("|")) {
//                        ArrayList<Boolean> res_lst = new ArrayList<>();
//                        for (String item : text.split("\\|")) {
////                            res_lst.add(Imagett.imageToText(framePicture, language, refreshImg, bounds).contains(item));
//                            res_lst.add(Checkpoint.checkIfExistByImage(item, language, framePicture, refreshImg, bounds));
//                            refreshImg = false;
//                        }
//                        result.add(res_lst.contains(true));
//                    } else {
//                        result.add(Checkpoint.checkIfExistByImage(text, language, framePicture, refreshImg, bounds));
//
////                        result.add(Imagett.imageToText(framePicture, language, refreshImg, bounds).contains(text));
//
//                    }
                    if (result){
                        logUtil.i("","true|视频画面中存在：" + text);
                        return true;
                    }
                    mseconds = mseconds - gap;
                    i++;

                }catch (Exception e) {
                    logUtil.e("", e);
                    break;
                }
            }
        }else {
            logUtil.d("", "视频文件不存在");
        }
        logUtil.i("","false|视频画面中不存在：" + text);
        return false;
    }


    /**
     * 图像轮廓提取
     * @param src Mat
     * @return
     */
//    public static List<MatOfPoint> FindContours(Mat src){
    public static Mat FindContours(Mat src){
        int threshold = 100;
//        Random rng = new Random(12345);
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        Mat cannyOutput = new Mat();
        Imgproc.Canny(srcGray, cannyOutput, threshold, threshold * 2);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
//        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        Mat drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
//        for (int i = 0; i < contours.size(); i++) {
////            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
//            Scalar color = new Scalar(200, 200, 200);
//            Imgproc.drawContours(drawing, contours, i, color, 1, Imgproc.LINE_8, hierarchy, 0, new Point());
//        }
//        Imgcodecs.imwrite("/sdcard/"+flag+".png", drawing);
        return cannyOutput;
    }

    /**
     * 根据point集合计算bounds范围
     * @param points
     * @return
     */

    public static String matchBoundsFromPoints(ArrayList<Point> points){
        ArrayList<Integer> xVal = new ArrayList<>();
        ArrayList<Integer> yVal = new ArrayList<>();
        for (int i=0; i < points.size(); i++){
            xVal.add((int) points.get(i).x);
            yVal.add((int) points.get(i).y);
        }
        int xMax = Collections.max(xVal);
        int xMin = Collections.min(xVal);
        int yMax = Collections.max(yVal);
        int yMin = Collections.min(yVal);
        return String.format("[%s,%s][%s,%s]", xMin, yMin, xMax, yMax);

    }

    public static Mat imageResize(String filePath, double resize){
        Mat img = Imgcodecs.imread(filePath);
        Imgproc.resize(img, img, new Size(), resize, resize);
        Imgcodecs.imwrite(filePath, img);
        return img;
    }

    public static Mat imageResize(String filePath, double resize, int q){
        Mat img = Imgcodecs.imread(filePath);
        Imgproc.resize(img, img, new Size(), resize, resize);
        MatOfInt MOI = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, q);
        Imgcodecs.imwrite(filePath, img, MOI);
        return img;
    }

    /**
     * 通过调用接口实现ocr文本识别及定位
     * @param text 要获取的预期文字，为“”时返回当前图片内所有文字
     * @param src_base64 图片源文件的base64编码
     * @param resize 图像缩小的比例，会根据此参数将位置坐标还原
     * @return
     */

    public static JSONObject getTextByOcrApi(String text, String src_base64, double resize, int index){

        String url = CONST.SERVER_BASE_URL + "api/cv";
        JSONObject dataJson = new JSONObject();
        try {
            dataJson.put("src_base64", src_base64);
            dataJson.put("sch_text", text);
            dataJson.put("resize", resize);
            dataJson.put("method", "ocr");
            dataJson.put("index", index);
//            dataJson.put("debug", true);
            String res = postResp(url, dataJson.toString());
            return new JSONObject(res);
        } catch (JSONException e) {
            logUtil.e("", e);
        }
        return new JSONObject();
    }

    /**
     * 从截图中获取文本及位置
     * @param text
     * @param index
     * @param refresh
     * @return
     */

    public static String getTextFromScreenshot(String text, int index, boolean refresh) {

        String res = "";
        double resize = 0.5;
        String jpgImage = DUMP_PNG_PATH;
        if (refresh || !new File(DUMP_PNG_PATH).exists()) {
            jpgImage = screenShot(CONST.DUMP_JPG_PATH);
            SystemClock.sleep(100);
        }
        try {
            CvUtils.imageResize(jpgImage, resize);
        } catch (Exception e){
            resize = 1;
        }
        JSONObject resJson = CvUtils.getTextByOcrApi(text, FileUtils.xToBase64(jpgImage), resize, index);
        try {
            if (text.length() == 0){
                res = resJson.get("text").toString();
            } else {
                res = resJson.getString("res_bounds");
                String png = Common.dateFormat.format(new Date()) + ".png";
                String outFile = REPORT_PATH + logUtil.readTempFile() + File.separator + CONST.SCREENSHOT + File.separator + png;
                FileUtils.base64ToFile(resJson.getString("image"), outFile);
                logUtil.i("", png);
            }
            logUtil.d("", "通过ocr api 解析界面text：" + text + res);
        } catch (Exception e) {
            logUtil.e("", e);
        }
        return res;
    }

    /**
     * 从图片中获取文本及位置
     * @param text
     * @param imageFile
     * @param outFile
     * @param index
     * @return
     */

    public static String getTextFromImage(String text, String imageFile, String outFile,int index) {
        String res = "";
        double resize = 0.5;
        if (!new File(imageFile).exists()) {
            imageFile = Common.screenShot(CONST.DUMP_JPG_PATH);
        }
        try {
            CvUtils.imageResize(imageFile, resize);
        } catch (Exception e) {
            resize = 1;
        }
        logUtil.d("image file for ocr", imageFile);
        JSONObject resJson = CvUtils.getTextByOcrApi(text, FileUtils.xToBase64(imageFile), resize, index);
        logUtil.i("OCR服务识别结果", resJson + "");
        try {
            if (text.length() == 0){
                res = resJson.get("text").toString();
            } else {
                res = resJson.getString("res_bounds");
                FileUtils.base64ToFile(resJson.getString("image"), outFile);
            }
            logUtil.d("", "通过ocr api 解析图片text：" + text + res);
        } catch (Exception e) {
            logUtil.e("", e);
        }
        return res;
    }

    /**
     * 通过AI服务接口进行图像识别
     * @param parm 接口请求参数
     * @return
     */
    public static JSONObject getMatchImgByApi(JSONObject parm){
        String url = CONST.SERVER_BASE_URL + "api/cv";
        String res = postResp(url, parm.toString());
        try {
            return new JSONObject(res);
        } catch (JSONException e) {
            logUtil.e("", e);
            return null;
        }
    }


}
