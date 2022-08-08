package com.kevin.share.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "testool.db";
    public static final String TOOL_TABLE_NAME = "tool";
    private static final int DB_VWESION = 1;
    private String CREATE_TOOL_TABLE = "CREATE TABLE IF NOT EXISTS "+ TOOL_TABLE_NAME + "(id integer primary key autoincrement,name varchar(30))";
    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VWESION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TOOL_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}