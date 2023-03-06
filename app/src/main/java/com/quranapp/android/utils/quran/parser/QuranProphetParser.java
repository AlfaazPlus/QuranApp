package com.quranapp.android.utils.quran.parser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.Looper;

import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.QuranProphet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class QuranProphetParser {
    private static final String PROPHETS_TAG_ROOT = "prophets";
    private static final String PROPHETS_TAG_PROPHET = "prophet";
    private static final String PROPHETS_ATTR_PROPHET_ORDER = "order";
    private static final String PROPHETS_ATTR_PROPHET_NAME_AR = "name-ar";
    private static final String PROPHETS_ATTR_PROPHET_NAME_EN = "name-en";
    private static final String PROPHETS_ATTR_PROPHET_NAME_TRANS = "name-trans";
    private static final String PROPHETS_ATTR_PROPHET_HONORIFIC = "honorific";
    private static final String PROPHETS_ATTR_PROPHET_ICON_RES = "drawable";


    public static void parseProphet(Context context, QuranMeta quranMeta, AtomicReference<QuranProphet> quranProphetRef, Runnable postRunnable) {
        new Thread(() -> {
            try {
                XmlResourceParser parser = context.getResources().getXml(R.xml.quran_prophets);
                QuranProphet parsedQuranTopics = parseProphetInternal(context, parser, quranMeta);
                quranProphetRef.set(parsedQuranTopics);
            } catch (Resources.NotFoundException | XmlPullParserException | IOException e) {
                e.printStackTrace();
            }

            new Handler(Looper.getMainLooper()).post(postRunnable);
        }).start();
    }

    private static QuranProphet parseProphetInternal(Context context, XmlResourceParser parser, QuranMeta quranMeta) throws XmlPullParserException, IOException {
        List<QuranProphet.Prophet> prophetList = new ArrayList<>();

        int eventType = parser.getEventType();

        QuranProphet.Prophet lastProphet = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (PROPHETS_TAG_PROPHET.equalsIgnoreCase(tagName)) {
                    lastProphet = new QuranProphet.Prophet();
                    lastProphet.order = parser.getAttributeIntValue(null, PROPHETS_ATTR_PROPHET_ORDER, -1);
                    lastProphet.nameAr = parser.getAttributeValue(null, PROPHETS_ATTR_PROPHET_NAME_AR);
                    lastProphet.nameEn = parser.getAttributeValue(null, PROPHETS_ATTR_PROPHET_NAME_EN);
                    lastProphet.nameTrans = parser.getAttributeValue(null, PROPHETS_ATTR_PROPHET_NAME_TRANS);
                    lastProphet.honorific = parser.getAttributeValue(null, PROPHETS_ATTR_PROPHET_HONORIFIC);
                    lastProphet.iconRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android",
                        PROPHETS_ATTR_PROPHET_ICON_RES, -1);

                    prophetList.add(lastProphet);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (lastProphet != null) {
                    lastProphet.verses = ParserUtils.prepareVersesList(parser.getText(), true);
                    lastProphet.chapters = ParserUtils.prepareChaptersList(lastProphet.verses);
                    lastProphet.inChapters = ParserUtils.prepareChapterText(context, quranMeta, lastProphet.chapters,
                        2);
                }
            }
            eventType = parser.next();
        }

        Collections.sort(prophetList, Comparator.comparing(o -> o.nameTrans));
        return new QuranProphet(prophetList);
    }
}
