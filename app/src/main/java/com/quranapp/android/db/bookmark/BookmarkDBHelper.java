package com.quranapp.android.db.bookmark;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_CHAPTER_NO;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_DATETIME;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_NOTE;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.TABLE_NAME;
import static com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry._ID;

import com.quranapp.android.R;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.univ.DBUtils;
import com.quranapp.android.utils.univ.DateUtils;

import java.util.ArrayList;

public class BookmarkDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Bookmark.db";
    public static final int DB_VERSION = 2;
    private final Context mContext;


    public BookmarkDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COL_CHAPTER_NO + " INTEGER," +
            COL_FROM_VERSE_NO + " INTEGER," +
            COL_TO_VERSE_NO + " INTEGER," +
            COL_DATETIME + " TEXT," +
            COL_NOTE + " TEXT)";
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

    public void addToBookmark(int chapterNo, int fromVerse, int toVerse, String note, OnResultReadyCallback<BookmarkModel> callback) {
        if (isBookmarked(chapterNo, fromVerse, toVerse)) {
            Toast.makeText(mContext, R.string.strMsgBookmarkAddedAlready, Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = getWritableDatabase();

        String dateTime = DateUtils.getDateTimeNow();
        ContentValues values = new ContentValues();
        values.put(COL_CHAPTER_NO, chapterNo);
        values.put(COL_FROM_VERSE_NO, fromVerse);
        values.put(COL_TO_VERSE_NO, toVerse);
        values.put(COL_DATETIME, dateTime);
        values.put(COL_NOTE, note);

        long rowId = db.insert(TABLE_NAME, null, values);
        boolean inserted = rowId != -1;

        final int msg;
        if (inserted) {
            if (callback != null) {
                BookmarkModel model = new BookmarkModel(rowId, chapterNo, fromVerse, toVerse, dateTime);
                model.setNote(note);
                callback.onReady(model);
            }
            msg = R.string.strMsgBookmarkAdded;
        } else {
            msg = R.string.strMsgBookmarkAddFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public void updateBookmark(int chapterNo, int fromVerse, int toVerse, String note, OnResultReadyCallback<BookmarkModel> callback) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = COL_CHAPTER_NO + "=? AND " + COL_FROM_VERSE_NO + "=? AND " + COL_TO_VERSE_NO + "=?";
        String[] whereArgs = {String.valueOf(chapterNo), String.valueOf(fromVerse), String.valueOf(toVerse)};

        String dateTime = DateUtils.getDateTimeNow();

        ContentValues values = new ContentValues();
        values.put(COL_CHAPTER_NO, chapterNo);
        values.put(COL_FROM_VERSE_NO, fromVerse);
        values.put(COL_TO_VERSE_NO, toVerse);
        values.put(COL_DATETIME, dateTime);
        values.put(COL_NOTE, note);

        long rowId = db.update(TABLE_NAME, values, whereClause, whereArgs);
        boolean updated = rowId != -1;

        if (updated) {
            if (callback != null) {
                BookmarkModel model = new BookmarkModel(rowId, chapterNo, fromVerse, toVerse, dateTime);
                model.setNote(note);
                callback.onReady(model);
            }
        }
    }

    public void removeFromBookmark(int chapterNo, int fromVerse, int toVerse, Runnable runOnSucceed) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = DBUtils.createDBSelection(COL_CHAPTER_NO, COL_FROM_VERSE_NO, COL_TO_VERSE_NO);
        String[] whereArgs = {String.valueOf(chapterNo), String.valueOf(fromVerse), String.valueOf(toVerse)};

        int rowsAffected = db.delete(TABLE_NAME, whereClause, whereArgs);

        boolean deleted = rowsAffected >= 1;

        final int msg;
        if (deleted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
            msg = R.string.strMsgBookmarkRemoved;
        } else {
            msg = R.string.strMsgBookmarkRemoveFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public void removeBookmarksBulk(long[] ids, Runnable runOnSucceed) {
        SQLiteDatabase db = getWritableDatabase();

        int rowsAffected = 0;
        for (long id : ids) {
            String whereClause = _ID + "=?";
            String[] whereArgs = {String.valueOf(id)};

            rowsAffected += db.delete(TABLE_NAME, whereClause, whereArgs);
        }

        boolean deleted = rowsAffected >= 1;
        final int msg;
        if (deleted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
            msg = R.string.strMsgBookmarkRemoved;
        } else {
            msg = R.string.strMsgBookmarkRemoveFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public boolean isBookmarked(int chapterNo, int fromVerse, int toVerse) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = DBUtils.createDBSelection(COL_CHAPTER_NO, COL_FROM_VERSE_NO, COL_TO_VERSE_NO);
        String[] selectionArgs = {
            String.valueOf(chapterNo),
            String.valueOf(fromVerse),
            String.valueOf(toVerse)
        };

        long count = DatabaseUtils.queryNumEntries(db, TABLE_NAME, selection, selectionArgs);

        return count > 0;
    }

    public void removeAllBookmarks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public BookmarkModel getBookmark(int chapNo, int fromVerse, int toVerse) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = DBUtils.createDBSelection(COL_CHAPTER_NO, COL_FROM_VERSE_NO, COL_TO_VERSE_NO);
        String[] selectionArgs = {String.valueOf(chapNo), String.valueOf(fromVerse), String.valueOf(toVerse)};
        String sortOrder = _ID + " DESC";

        BookmarkModel model = null;

        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, sortOrder);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            int chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHAPTER_NO));
            int fromVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FROM_VERSE_NO));
            int toVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TO_VERSE_NO));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE));

            model = new BookmarkModel(id, chapterNo, fromVerseNo, toVerseNo, date);
            model.setNote(note);
        }
        cursor.close();

        return model;
    }

    public ArrayList<BookmarkModel> getBookmarks() {
        SQLiteDatabase db = getReadableDatabase();

        String sortOrder = _ID + " DESC";

        ArrayList<BookmarkModel> verses = new ArrayList<>();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, sortOrder);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            int chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHAPTER_NO));
            int fromVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FROM_VERSE_NO));
            int toVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TO_VERSE_NO));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE));

            BookmarkModel model = new BookmarkModel(id, chapterNo, fromVerseNo, toVerseNo, date);
            model.setNote(note);
            verses.add(model);
        }
        cursor.close();

        return verses;
    }
}
