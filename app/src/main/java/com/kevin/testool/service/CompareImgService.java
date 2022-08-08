package com.kevin.testool.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.kevin.share.utils.logUtil;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class CompareImgService extends Service {
    public CompareImgService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String inFile = intent.getStringExtra("IN_FILE");
        String templateFile = intent.getStringExtra("TEMPLATE_FILE");
        new MatchingDemo().run(inFile, templateFile, inFile.replace(".png", "_out.png"), Imgproc.TM_CCOEFF);

        return super.onStartCommand(intent, flags, startId);
    }

    static class MatchingDemo {
        public void run(String inFile, String templateFile, String outFile,int match_method) {
            Mat img = Imgcodecs.imread(inFile);
            Mat templ = Imgcodecs.imread(templateFile);

            // / Create the result matrix
            int result_cols = img.cols() - templ.cols() + 1;
            int result_rows = img.rows() - templ.rows() + 1;
            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

            // / Do the Matching and Normalize
            Imgproc.matchTemplate(img, templ, result, match_method);
            Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

            // / Localizing the best match with minMaxLoc
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            Point matchLoc;
            if (match_method == Imgproc.TM_SQDIFF
                    || match_method == Imgproc.TM_SQDIFF_NORMED) {
                matchLoc = mmr.minLoc;
            } else {
                matchLoc = mmr.maxLoc;
            }

            // / Show me what you got
            Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x + templ.cols(),
                    matchLoc.y + templ.rows()), new Scalar(0, 255, 0));

            logUtil.d("",matchLoc.x + "");
            logUtil.d("",matchLoc.x + templ.cols() + "");
            logUtil.d("",matchLoc.y + "");
            logUtil.d("",matchLoc.y + templ.rows() +"");

            // Save the visualized detection.
            Imgcodecs.imwrite(outFile, img);
            logUtil.i("", outFile);

        }
    }
}
