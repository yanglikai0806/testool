package com.kevin.share.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.kevin.share.AppContext;

public class CpUtils {

    public static void getValue(){
        Uri uri = Uri.parse("content://com.kevin.testool/tool");
        ContentValues values = new ContentValues();
        values.put("name","my00000000000");
        AppContext.getContext().getContentResolver().insert(uri,values);
        Cursor mCursor = AppContext.getContext().getContentResolver().query(uri,null,null,null,null);
        while (mCursor.moveToNext()) {
            logUtil.d("", mCursor.getColumnIndex("tool"));
            logUtil.d("", "tool:" + mCursor.getString(mCursor.getColumnIndex("tool")));
        }
        mCursor.close();
    }
}
