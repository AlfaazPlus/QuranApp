package com.quranapp.android.utils.quran.parser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.os.Looper;

import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.QuranTopic;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public final class QuranTopicParser {
    private static final String TOPICS_TAG_ROOT = "topics";
    private static final String TOPICS_TAG_TOPIC = "topic";
    private static final String TOPICS_ATTR_TOPIC_NAME = "name";
    private static final String TOPICS_ATTR_TOPIC_FEATURED = "featured";
    private static final String TOPICS_ATTR_TOPIC_OTHER_TERMS = "other-terms";


    public static void parseTopics(Context context, QuranMeta quranMeta, AtomicReference<QuranTopic> quranTopicRef, Runnable postRunnable) {
        new Thread(() -> {
            try {
                XmlResourceParser parser = context.getResources().getXml(R.xml.quran_topics);
                QuranTopic parsedQuranTopics = parseTopicInternal(context, parser, quranMeta);
                quranTopicRef.set(parsedQuranTopics);
            } catch (Resources.NotFoundException | XmlPullParserException | IOException e) {
                e.printStackTrace();
            }

            new Handler(Looper.getMainLooper()).post(postRunnable);
        }).start();
    }

    private static QuranTopic parseTopicInternal(Context context, XmlResourceParser parser, QuranMeta quranMeta) throws XmlPullParserException, IOException {
        Map<Integer, QuranTopic.Topic> topicMap = new TreeMap<>();
        LinkedList<Character> availableAlphabets = new LinkedList<>();

        int eventType = parser.getEventType();

        int id = 1;
        QuranTopic.Topic lastTopic = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (TOPICS_TAG_TOPIC.equalsIgnoreCase(tagName)) {
                    lastTopic = new QuranTopic.Topic();
                    lastTopic.id = id++;
                    lastTopic.isFeatured = parser.getAttributeBooleanValue(null, TOPICS_ATTR_TOPIC_FEATURED, false);
                    lastTopic.name = parser.getAttributeValue(null, TOPICS_ATTR_TOPIC_NAME);
                    lastTopic.otherTerms = parser.getAttributeValue(null, TOPICS_ATTR_TOPIC_OTHER_TERMS);

                    char startsWith = lastTopic.name.toLowerCase().charAt(0);
                    if (startsWith == '\'') {
                        startsWith = lastTopic.name.toLowerCase().charAt(1);
                    }

                    if (availableAlphabets.size() == 0 || availableAlphabets.getLast() != startsWith) {
                        availableAlphabets.add(startsWith);
                    }

                    topicMap.put(lastTopic.id, lastTopic);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (lastTopic != null) {
                    lastTopic.verses = ParserUtils.prepareVersesList(parser.getText(), true);
                    lastTopic.chapters = ParserUtils.prepareChaptersList(lastTopic.verses);
                    lastTopic.inChapters = ParserUtils.prepareChapterText(context, quranMeta, lastTopic.chapters, 2);
                }
            }
            eventType = parser.next();
        }

        QuranTopic quranTopic = new QuranTopic(sortTopics(topicMap));
        quranTopic.setAvailableAlphabets(availableAlphabets.toArray(new Character[0]));
        return quranTopic;
    }


    private static Map<Integer, QuranTopic.Topic> sortTopics(Map<Integer, QuranTopic.Topic> map) {
        List<Entry<Integer, QuranTopic.Topic>> entries = new LinkedList<>(map.entrySet());
        Collections.sort(entries, Comparator.comparing(o -> o.getValue().name));

        Map<Integer, QuranTopic.Topic> result = new LinkedHashMap<>();
        for (Entry<Integer, QuranTopic.Topic> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
