package com.quranapp.android.utils.univ;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public abstract class DateUtils {
    public static final String DATETIME_FORMAT_SYSTEM = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_FORMAT_USER = "dd-MM-yyyy hh:mm a";
    public static final String DATETIME_FORMAT_FILENAME = "yyyyMMddhhmmss";

    public static String reformat(String dateStr, String oldPattern, String newPattern) {
        DateTimeFormatter oFormatter = DateTimeFormatter.ofPattern(oldPattern);
        DateTimeFormatter nFormatter = DateTimeFormatter.ofPattern(newPattern);

        try {
            return nFormatter.format(oFormatter.parse(dateStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getDateTimeNow(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
        return formatter.format(Calendar.getInstance().getTime());
    }

    public static String getDateTimeNow() {
        return getDateTimeNow(DATETIME_FORMAT_SYSTEM);
    }

    public static String getDateTimeNow4Filename() {
        return getDateTimeNow(DATETIME_FORMAT_FILENAME);
    }

    public static boolean isNewer(long oldDate, long newDate) {
        return new Date(oldDate).before(new Date(newDate));
    }

    public static long daysSince(String timeStr) {
        long time = Long.parseLong(timeStr);
        Calendar calThen = Calendar.getInstance();
        calThen.setTimeInMillis(time);

        Calendar calNow = Calendar.getInstance();
        return Duration.between(calThen.toInstant(), calNow.toInstant()).toDays();
    }

    public static long hoursSince(String timeMillisStr) {
        long timeMillis = Long.parseLong(timeMillisStr);
        Calendar calThen = Calendar.getInstance();
        calThen.setTimeInMillis(timeMillis);

        Calendar calNow = Calendar.getInstance();
        return Duration.between(calThen.toInstant(), calNow.toInstant()).toHours();
    }

    public static long hoursSince(Long timeMillis) {
        Calendar calThen = Calendar.getInstance();
        calThen.setTimeInMillis(timeMillis);

        Calendar calNow = Calendar.getInstance();
        return Duration.between(calThen.toInstant(), calNow.toInstant()).toHours();
    }

    public static long minutesSince(String timeMillisStr) {
        return minutesSince(Long.parseLong(timeMillisStr));
    }

    public static long minutesSince(Long time) {
        Calendar calThen = Calendar.getInstance();
        calThen.setTimeInMillis(time);

        Calendar calNow = Calendar.getInstance();
        return Duration.between(calThen.toInstant(), calNow.toInstant()).toMinutes();
    }
}
