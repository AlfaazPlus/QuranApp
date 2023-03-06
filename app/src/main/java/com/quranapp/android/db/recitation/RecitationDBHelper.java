package com.quranapp.android.db.recitation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_IS_PREMIUM;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_RECITER;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_SLUG;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_STYLE;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_URL_HOST;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.COL_URL_PATH;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry.TABLE_NAME;
import static com.quranapp.android.db.recitation.RecitationContract.RecitationEntry._ID;

public class RecitationDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Recitation.db";
    public static final int DB_VERSION = 1;


    public RecitationDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COL_SLUG + " TEXT," +
            COL_RECITER + " TEXT," +
            COL_STYLE + " TEXT," +
            COL_URL_HOST + " TEXT," +
            COL_URL_PATH + " TEXT," +
            COL_IS_PREMIUM + " BOOLEAN)";
        DB.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase DB, int oldVersion, int newVersion) {
        final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        DB.execSQL(DELETE_TABLE);
        onCreate(DB);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
