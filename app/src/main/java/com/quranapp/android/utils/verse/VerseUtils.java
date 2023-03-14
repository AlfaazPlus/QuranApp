package com.quranapp.android.utils.verse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import androidx.annotation.NonNull;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.text.Spanned.SPAN_POINT_MARK;

import com.peacedesign.android.utils.span.TypefaceSpan2;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.db.readHistory.ReadHistoryDBHelper;
import com.quranapp.android.interfaceUtils.VOTDCallback;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.others.ShortcutUtils;
import com.quranapp.android.utils.reader.ArabicUtils;
import com.quranapp.android.utils.reader.quranPage.VerseArabicHighlightSpan;
import com.quranapp.android.utils.sharedPrefs.SPVerses;
import com.quranapp.android.utils.thread.runner.RunnableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseRunnableTask;

import java.time.Duration;
import java.util.Calendar;
import java.util.Random;

public abstract class VerseUtils {
    private static final int VOTD_RESET_MINUTES = 24 * 60; // one day
    private static int VOTD_chap_no = -1;
    private static int VOTD_verse_no = -1;

    /**
     * verseSerial and verseSerialFont will be null if isKFQPCFont() is true
     */
    public static CharSequence decorateVerse(
        String arabicText,
        Integer verseSerial,
        Typeface verseFont,
        Typeface verseSerialFont,
        int verseTextSize,
        int serialTextSize
    ) {
        if (TextUtils.isEmpty(arabicText)) {
            return "";
        }

        SpannableString arabicSS = new SpannableString(arabicText);
        // Set the typeface to span over arabic text
        arabicSS.setSpan(new TypefaceSpan2(verseFont), 0, arabicSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        if (verseTextSize > 0) {
            arabicSS.setSpan(new AbsoluteSizeSpan(verseTextSize), 0, arabicSS.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (verseSerial == null) {
            return arabicSS;
        }

        SpannableString verseSerialSS = prepareVerseSerial(verseSerial, verseSerialFont);
        if (serialTextSize > 0) {
            verseSerialSS.setSpan(new AbsoluteSizeSpan(serialTextSize), 0, verseSerialSS.length(),
                SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return TextUtils.concat(arabicSS, " ", verseSerialSS);
    }

    /**
     * verseSerialFont will be null if isKFQPCFont() is true
     */
    public static CharSequence decorateQuranPageVerse(String arabicText, int verseNo, Typeface verseFont, Typeface verseSerialFont) {
        if (TextUtils.isEmpty(arabicText)) {
            return "";
        }

        SpannableString arabicSS = new SpannableString(arabicText);
        // Set the typeface to span over arabic text
        arabicSS.setSpan(new TypefaceSpan2(verseFont), 0, arabicSS.length(), SPAN_POINT_MARK);

        final CharSequence concat;

        if (verseSerialFont != null) {
            concat = TextUtils.concat(arabicSS, " ", prepareVerseSerial(verseNo, verseSerialFont));
        } else {
            concat = arabicSS;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(concat);
        builder.setSpan(new VerseArabicHighlightSpan(verseNo), 0, builder.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private static SpannableString prepareVerseSerial(int verseNo, Typeface verseSerialFont) {
        StringBuilder verseNumberSB = new StringBuilder(ArabicUtils.convertToArabicDecimal(verseNo));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            verseNumberSB = verseNumberSB.reverse();
        } else {
            verseNumberSB.insert(0, "﴿");
            verseNumberSB.append("﴾");
        }
        String verseNumber = verseNumberSB.toString();

        SpannableString verseNoSpannable = new SpannableString(verseNumber);
        // Set the typeface to span over verse number text
        verseNoSpannable.setSpan(new TypefaceSpan2(verseSerialFont), 0, verseNumber.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        return verseNoSpannable;
    }

    /**
     * Decorate translation text without reference or footnote
     */
    public static SpannableString decorateSingleTranslSimple(String translText, int translClr, int txtSize, Typeface translFont) {
        if (TextUtils.isEmpty(translText)) {
            return null;
        }

        int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        SpannableString transl = new SpannableString(translText);
        if (translClr != -1) {
            transl.setSpan(new ForegroundColorSpan(translClr), 0, transl.length(), flag);
        }
        if (txtSize > 0) {
            transl.setSpan(new AbsoluteSizeSpan(txtSize), 0, transl.length(), flag);
        }

        transl.setSpan(new TypefaceSpan2(translFont), 0, transl.length(), flag);
        return transl;
    }

    public static SpannableString prepareTranslAuthorText(
        String authorText, int authorClr, int txtSize,
        Typeface authorFont, boolean isTransliteration
    ) {
        int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        SpannableString author = new SpannableString(authorText);
        if (!isTransliteration) {
            author.setSpan(new ForegroundColorSpan(authorClr), 0, author.length(), flag);
        }
        author.setSpan(new AbsoluteSizeSpan(txtSize), 0, author.length(), flag);
        author.setSpan(new TypefaceSpan2(authorFont), 0, author.length(), flag);

        return author;
    }


    public static synchronized void getVOTD(Context ctx, QuranMeta quranMeta, Quran quran, VOTDCallback callback) {
        RunnableTaskRunner taskRunner = new RunnableTaskRunner();
        taskRunner.runAsync(new BaseRunnableTask() {
            @Override
            public void runTask() {
                boolean forceNew = false;
                while (true) {
                    int[] randomVerse = getVOTD(ctx, quranMeta, forceNew);
                    VOTD_chap_no = randomVerse[0];
                    VOTD_verse_no = randomVerse[1];

                    if (QuranMeta.isChapterValid(VOTD_chap_no) && quranMeta.isVerseValid4Chapter(VOTD_chap_no,
                        VOTD_verse_no)) {
                        boolean breakable = quran == null || quran.getVerse(VOTD_chap_no,
                            VOTD_verse_no).isIdealForVOTD();
                        if (breakable) {
                            break;
                        }
                    }

                    forceNew = true;
                }
            }

            @Override
            public void onComplete() {
                if (callback != null) {
                    callback.onObtainVOTD(VOTD_chap_no, VOTD_verse_no);
                }
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                e.printStackTrace();
                Logger.reportError(e);
            }
        });
    }

    private static int[] getVOTD(Context ctx, QuranMeta quranMeta, boolean forceNew) {
        SharedPreferences sp = ctx.getSharedPreferences(SPVerses.SP_VOTD, Context.MODE_PRIVATE);
        int chapterNo = sp.getInt(SPVerses.KEY_VOTD_CHAPTER_NO, -1);
        int verseNo = sp.getInt(SPVerses.KEY_VOTD_VERSE_NO, -1);

        boolean isChapterValid = QuranMeta.isChapterValid(chapterNo);
        boolean isVerseValid = quranMeta.isVerseValid4Chapter(chapterNo, verseNo);

        if (forceNew || !isChapterValid || !isVerseValid || doesVOTDNeedReset(ctx, sp)) {
            Random random = new Random();

            chapterNo = random.nextInt(QuranMeta.totalChapters());
            if (chapterNo < 1) {
                return new int[]{-1, -1};
            }
            verseNo = random.nextInt(quranMeta.getChapterVerseCount(chapterNo));

            Calendar cal = Calendar.getInstance();
            if (cal.get(Calendar.HOUR_OF_DAY) < 4) {
                cal.add(Calendar.DAY_OF_MONTH, -1);
            }

            cal.set(Calendar.HOUR_OF_DAY, 4);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            SPVerses.saveVOTD(ctx, cal.getTimeInMillis(), chapterNo, verseNo);

            ShortcutUtils.pushVOTDShortcut(ctx, chapterNo, verseNo);
        }

        return new int[]{chapterNo, verseNo};
    }

    private static boolean doesVOTDNeedReset(Context ctx, SharedPreferences sp) {
        try {
            long lastDateTimeMillis = sp.getLong(SPVerses.KEY_VOTD_DATE, -1);
            if (lastDateTimeMillis == -1) {
                return true;
            }

            Calendar calThen = Calendar.getInstance();
            calThen.setTimeInMillis(lastDateTimeMillis);

            Calendar calNow = Calendar.getInstance();

            Duration duration = Duration.between(calThen.toInstant(), calNow.toInstant());
            long minutesBetween = duration.toMinutes();
            return minutesBetween < 0 || minutesBetween >= VOTD_RESET_MINUTES;
        } catch (ClassCastException e) {
            Logger.reportError(e);
            SPVerses.removeVOTD(ctx);
            return true;
        }
    }

    public static boolean isVOTD(Context ctx, int chapterNo, int verseNo) {
        if (VOTD_chap_no <= 0 || VOTD_verse_no <= 0) {
            int[] votd = SPVerses.getVOTD(ctx);
            VOTD_chap_no = votd[0];
            VOTD_verse_no = votd[1];
        }

        return chapterNo == VOTD_chap_no && verseNo == VOTD_verse_no;
    }

    public static void saveLastVerses(
        Context ctx, ReadHistoryDBHelper dbHelper, QuranMeta quranMeta,
        int readType, int readerStyle, int juzNo, int chapterNo, int fromVerse, int toVerse
    ) {
        dbHelper.addToHistory(readType, readerStyle, juzNo, chapterNo, fromVerse, toVerse, null);

        try {
            ShortcutUtils.pushLastVersesShortcut(ctx, quranMeta, readType, readerStyle, juzNo, chapterNo, fromVerse,
                toVerse);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }
    }
}
