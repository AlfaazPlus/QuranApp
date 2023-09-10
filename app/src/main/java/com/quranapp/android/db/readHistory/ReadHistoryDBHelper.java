package com.quranapp.android.db.readHistory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_CHAPTER_NO;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_DATETIME;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_FROM_VERSE_NO;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_JUZ_NO;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_READER_STYLE;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_READ_TYPE;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.COL_TO_VERSE_NO;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry.TABLE_NAME;
import static com.quranapp.android.db.readHistory.ReadHistoryContract.ReadHistoryEntry._ID;
import static java.lang.String.valueOf;

import com.quranapp.android.components.readHistory.ReadHistoryModel;
import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.univ.DBUtils;
import com.quranapp.android.utils.univ.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ReadHistoryDBHelper extends SQLiteOpenHelper {
    private static final int HISTORY_LIMIT = 40;
    private static final String DB_NAME = "ReadHistory.db";
    public static final int DB_VERSION = 1;

    public ReadHistoryDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COL_READ_TYPE + " INTEGER," +
            COL_READER_STYLE + " INTEGER," +
            COL_JUZ_NO + " INTEGER," +
            COL_CHAPTER_NO + " INTEGER," +
            COL_FROM_VERSE_NO + " INTEGER," +
            COL_TO_VERSE_NO + " INTEGER," +
            COL_DATETIME + " TEXT)";
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

    private void removeOldHistories() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + "" +
            " WHERE " + _ID + " IN " +
            "(SELECT " + _ID + " FROM " + TABLE_NAME + " ORDER BY " + _ID + " DESC LIMIT -1 OFFSET " + HISTORY_LIMIT + ");");
    }

    private boolean isAlreadyAdded(int readType, int readStyle, int juzNo, int chapterNo, int fromVerse, int toVerse) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = DBUtils.createDBSelection(COL_READ_TYPE, COL_READER_STYLE, COL_JUZ_NO,
            COL_CHAPTER_NO, COL_FROM_VERSE_NO, COL_TO_VERSE_NO);
        String[] selectionArgs = {
            valueOf(readType),
            valueOf(readStyle),
            valueOf(juzNo),
            valueOf(chapterNo),
            valueOf(fromVerse),
            valueOf(toVerse)
        };
        return DatabaseUtils.queryNumEntries(db, TABLE_NAME, selection, selectionArgs) > 0;
    }

    private void deleteHistory(
        SQLiteDatabase db, int readType, int readStyle, int juzNo,
        int chapterNo, int fromVerse, int toVerse
    ) {
        if (db == null) {
            db = getWritableDatabase();
        }

        String where = DBUtils.createDBSelection(COL_READ_TYPE, COL_READER_STYLE, COL_JUZ_NO,
            COL_CHAPTER_NO, COL_FROM_VERSE_NO, COL_TO_VERSE_NO);
        String[] whereArgs = {
            valueOf(readType),
            valueOf(readStyle),
            valueOf(juzNo),
            valueOf(chapterNo),
            valueOf(fromVerse),
            valueOf(toVerse)
        };

        db.delete(TABLE_NAME, where, whereArgs);
    }


    public void deleteHistory(@NonNull ReadHistoryModel model) {
        deleteHistory(
            null,
            model.getReadType(),
            model.getReaderStyle(),
            model.getJuzNo(),
            model.getChapterNo(),
            model.getFromVerseNo(),
            model.getToVerseNo()
        );
    }

    public void addToHistory(
        int readType, int readStyle, int juzNo, int chapterNo, int fromVerse, int toVerse,
        OnResultReadyCallback<ReadHistoryModel> callback
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Bring the last history to front, if it exists.
        deleteHistory(db, readType, readStyle, juzNo, chapterNo, fromVerse, toVerse);

        String dateTime = DateUtils.getDateTimeNow();
        ContentValues values = new ContentValues();
        values.put(COL_READ_TYPE, readType);
        values.put(COL_READER_STYLE, readStyle);
        values.put(COL_JUZ_NO, juzNo);

        values.put(COL_CHAPTER_NO, chapterNo);
        values.put(COL_FROM_VERSE_NO, fromVerse);
        values.put(COL_TO_VERSE_NO, toVerse);
        values.put(COL_DATETIME, dateTime);

        long rowId = db.insert(TABLE_NAME, null, values);
        boolean inserted = rowId != -1;

        removeOldHistories();

        if (inserted && callback != null) {
            callback.onReady(
                new ReadHistoryModel(rowId, readType, readStyle, juzNo, chapterNo, fromVerse, toVerse, dateTime));
        }
    }

    public List<ReadHistoryModel> getAllHistories(int limit) {
        SQLiteDatabase db = getReadableDatabase();

        String sortOrder = _ID + " DESC";

        List<ReadHistoryModel> verses = new ArrayList<>();

        String limitStr = limit > 0 ? valueOf(limit) : null;

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, sortOrder, limitStr);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            int readType = cursor.getInt(cursor.getColumnIndexOrThrow(COL_READ_TYPE));
            int readerStyle = cursor.getInt(cursor.getColumnIndexOrThrow(COL_READER_STYLE));
            int juzNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_JUZ_NO));
            int chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHAPTER_NO));
            int fromVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FROM_VERSE_NO));
            int toVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TO_VERSE_NO));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATETIME));

            ReadHistoryModel model = new ReadHistoryModel(id, readType, readerStyle, juzNo, chapterNo, fromVerseNo,
                toVerseNo, date);
            verses.add(model);
        }
        cursor.close();

        return verses;
    }

    public void deleteAllHistories() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}
