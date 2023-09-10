package com.quranapp.android.db.search;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_DATETIME;
import static com.quranapp.android.db.search.SearchHistoryContract.SearchEntry.COL_TEXT;
import static com.quranapp.android.db.search.SearchHistoryContract.SearchEntry.TABLE_NAME;
import static com.quranapp.android.db.search.SearchHistoryContract.SearchEntry._ID;

import com.quranapp.android.components.search.SearchHistoryModel;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.utils.univ.DateUtils;

import java.util.ArrayList;

public class SearchHistoryDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "SearchHistory.db";
    public static final int DB_VERSION = 1;


    public SearchHistoryDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COL_TEXT + " TEXT," +
            SearchHistoryContract.SearchEntry.COL_DATE + " TEXT)";
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


    public void addToHistory(String text, Runnable runOnSucceed) {
        if (updateHistory(text)) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
            return;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_TEXT, text);
        values.put(COL_DATETIME, DateUtils.getDateTimeNow());

        long rowId = db.insert(TABLE_NAME, null, values);
        boolean inserted = rowId != -1;

        if (inserted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
        }
    }

    public void removeFromHistory(int historyId, Runnable runOnSucceed) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = _ID + "=?";
        String[] whereArgs = {String.valueOf(historyId)};

        int rowsAffected = db.delete(TABLE_NAME, whereClause, whereArgs);

        boolean deleted = rowsAffected == 1;

        if (deleted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
        }
    }

    public boolean isInHistory(String text) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = COL_TEXT + "=?";
        String[] selectionArgs = {text.toLowerCase()};

        long count = DatabaseUtils.queryNumEntries(db, TABLE_NAME, selection, selectionArgs);

        return count > 0;
    }

    public boolean updateHistory(String text) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = COL_TEXT + "=?";
        String[] whereArgs = {text.toLowerCase()};

        ContentValues values = new ContentValues();
        values.put(COL_DATETIME, DateUtils.getDateTimeNow());

        long rowsAffected = db.update(TABLE_NAME, values, whereClause, whereArgs);
        return rowsAffected > 0;
    }

    public ArrayList<SearchResultModelBase> getHistories(String query) {
        SQLiteDatabase db = getReadableDatabase();

        String sortOrder = COL_DATETIME + " DESC";

        final String selection;
        final String[] selectionArgs;

        if (TextUtils.isEmpty(query)) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = COL_TEXT + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        ArrayList<SearchResultModelBase> histories = new ArrayList<>();

        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, sortOrder);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            String text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME));

            SearchHistoryModel historyModel = new SearchHistoryModel(id, text, date);
            histories.add(historyModel);
        }
        cursor.close();
        return histories;
    }

    public long getHistoriesCount() {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), TABLE_NAME);
    }

    public void clearHistories() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}
