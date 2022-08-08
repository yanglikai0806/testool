package com.kevin.testool.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kevin.fw.TextDisplayWindowService;
import com.kevin.share.CONST;
import com.kevin.share.Common;
import com.kevin.share.utils.BitmapUtil;
import com.kevin.share.utils.FileUtils;
import com.kevin.share.utils.HttpUtil;
import com.kevin.share.utils.ToastUtils;
import com.kevin.share.utils.logUtil;
import com.kevin.testool.MyApplication;
import com.kevin.testool.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;

import static com.kevin.share.CONST.TESTCASES_PATH;
import static com.kevin.share.CONST.SERVER_BASE_URL;


public class ImageEditActivity extends AppCompatActivity {

    private static String imgBase64Code = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cropper);

    }

    /**
     * Start pick image activity with chooser.
     */
    public void onSelectImageClick(View view) {
        CropImage.activity(null).setGuidelines(CropImageView.Guidelines.ON).start(this);
    }

    public void onUploadImgClick(View view){
        AlertDialog.Builder popWindow = new AlertDialog.Builder(ImageEditActivity.this);
        View dialogView = LayoutInflater.from(MyApplication.getContext()).inflate(R.layout.image_upload,null);
        popWindow.setTitle("");
        popWindow.setView(dialogView);
        popWindow.setPositiveButton("确定", (dialog, which) -> {
            EditText idEt = dialogView.findViewById(R.id.inputId);
            EditText tagEt = dialogView.findViewById(R.id.inputTag);
            String id1 = idEt.getText().toString();
            String tag = tagEt.getText().toString();
            int id = -1;
            if (!TextUtils.isEmpty(id1)){
                id = Integer.parseInt(id1);
            }
            if (TextUtils.isEmpty(tag)){
                ToastUtils.showShort(this, "标签不能为空");
            } else {
                if (TextUtils.isEmpty(imgBase64Code)){
                    try {
                        imgBase64Code = FileUtils.readFile(CONST.LOGPATH + "image_base64.txt");
                    } catch (IOException e) {
                        logUtil.e("", e);
                    }
                }
                String url = SERVER_BASE_URL + "image";
                String data = String.format("{\"data\":{\"id\":%s, \"image\":\"%s\", \"tag\":\"%s\"}}", id, imgBase64Code,tag);
                logUtil.d("", data);
                new Thread(() -> {
                    String res = HttpUtil.postResp(url, data);
                    ToastUtils.showShortByHandler(ImageEditActivity.this, res);
                }).start();
            }
        });
        popWindow.setNegativeButton("取消", null);
        //对话框显示
        popWindow.show();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // handle result of CropImageActivity
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                ImageView iv = ((ImageView) findViewById(R.id.quick_start_cropped_image));
                iv.setImageURI(result.getUri());
                Drawable drawable = iv.getDrawable();
                Bitmap mBitmap = getBitmap(drawable);
                imgBase64Code = BitmapUtil.bitmapToBase64(mBitmap);

                FileUtils.writeFile(CONST.LOGPATH + "image_base64.txt", imgBase64Code,false);
                ToastUtils.showLong(this, "base64数据存储于/sdcard/autotest/image_base64.txt文件中");
//                Intent textDisplayIntent = new Intent(getApplicationContext(), TextDisplayWindowService.class);
//                textDisplayIntent.putExtra("TEXT", BitmapUtil.bitmapToBase64(mBitmap));
//                startService(textDisplayIntent);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }


    private Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;

    }
}
