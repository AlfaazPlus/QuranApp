package com.quranapp.android.utils.reader;

public class ArabicUtils {
    public static String convertToArabicDecimal(int number) {
        String str = String.valueOf(number);
        char[] arabicChars = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                builder.append(arabicChars[(int) (str.charAt(i)) - 48]);
            } else {
                builder.append(str.charAt(i));
            }
        }

        /*Locale locale = new Locale.Builder().setLanguageTag("ar-SA-u-nu-arab").build();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setGroupingUsed(false);
        return numberFormat.format(number);*/

        return builder.toString();
    }
}
