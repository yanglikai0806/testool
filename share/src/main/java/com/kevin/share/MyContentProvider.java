package com.kevin.share;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kevin.share.utils.DbHelper;
import com.kevin.share.utils.logUtil;

public class MyContentProvider extends ContentProvider {

    public static final String TAG = "MyContentProvider";
    public static final String AUTHORITY = "com.kevin.testool";
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/tool");
    public static final int URI_CODE = 0;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private SQLiteDatabase mDb;
    private Context mContext;
    static{
        sUriMatcher.addURI(AUTHORITY,"tool",URI_CODE);
    }

    public MyContentProvider() {
    }

    @Override
    public boolean onCreate() {
        logUtil.d(TAG,"onCerate,current thread:"+Thread.currentThread().getName());
        mContext = getContext();
        initProviderData();
        return true;
    }
    private void initProviderData(){
        mDb = new DbHelper(mContext).getWritableDatabase();
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        logUtil.d(TAG,"query,current thread:"+Thread.currentThread().getName());
        String table = getTableName(uri);
        if (table==null)
        {
            throw new IllegalArgumentException("Unsupported URI:"+uri);
        }
        return mDb.query(table,projection,selection,selectionArgs,null,null,sortOrder,null);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        logUtil.d(TAG,"getType,current thread:"+Thread.currentThread().getName());
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = getTableName(uri);
        logUtil.d(TAG,uri);
        if (TextUtils.isEmpty(table)){
            throw new IllegalArgumentException("Unsupport URI");
        }
        mDb.insert(table,null,values);
        mContext.getContentResolver().notifyChange(uri,null);
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        logUtil.d(TAG,"delete,current thread:"+Thread.currentThread().getName());
        String table = getTableName(uri);
        if (table==null){
            throw new IllegalArgumentException("Unsupport URI");
        }
        int count = mDb.delete(table,selection,selectionArgs);
        if (count>0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        logUtil.d(TAG,"update,current thread:"+Thread.currentThread().getName());
        String table  = getTableName(uri);
        if (table==null){
            throw new IllegalArgumentException("Unsupport URI");
        }
        int row = mDb.update(table,values,selection,selectionArgs);
        if (row>0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return row;
    }

    private String getTableName(Uri uri){
        String tableName=null;
        switch (sUriMatcher.match(uri)){
            case URI_CODE:
                tableName = DbHelper.TOOL_TABLE_NAME;
                break;
        }
        return tableName;
    }
}
