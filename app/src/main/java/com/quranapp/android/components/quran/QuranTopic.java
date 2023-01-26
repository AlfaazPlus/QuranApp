package com.quranapp.android.components.quran;

import android.content.Context;

import androidx.annotation.NonNull;

import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.quran.parser.QuranTopicParser;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class QuranTopic {
    private static final AtomicReference<QuranTopic> sQuranTopicRef = new AtomicReference<>();
    private final Map<Integer, Topic> topics;
    private Character[] mAvailableAlphabets;

    public QuranTopic(Map<Integer, Topic> topicMap) {
        topics = topicMap;
    }

    public static void prepareInstance(Context context, QuranMeta quranMeta, OnResultReadyCallback<QuranTopic> resultReadyCallback) {
        if (sQuranTopicRef.get() == null) {
            synchronized (QuranTopic.class) {
                prepare(context, quranMeta, resultReadyCallback);
            }
        } else {
            resultReadyCallback.onReady(sQuranTopicRef.get());
        }
    }

    private static void prepare(Context context, QuranMeta quranMeta, OnResultReadyCallback<QuranTopic> resultReadyCallback) {
        QuranTopicParser.parseTopics(context, quranMeta, sQuranTopicRef, () -> resultReadyCallback.onReady(sQuranTopicRef.get()));
    }

    public void setAvailableAlphabets(Character[] availableAlphabets) {
        mAvailableAlphabets = availableAlphabets;
    }

    public Character[] getAvailableAlphabets() {
        return mAvailableAlphabets;
    }


    public List<Topic> getTopicsOfAlphabet(int alphabet) {
        LinkedList<Topic> topicList = new LinkedList<>();

        boolean previouslyFound = false;
        for (Integer id : topics.keySet()) {
            Topic topic = topics.get(id);
            if (topic != null) {
                int pos = 0;
                if (topic.name.toLowerCase().charAt(pos) == '\'') {
                    pos = 1;
                }
                boolean found = topic.name.toLowerCase().charAt(pos) == alphabet;

                if (found) {
                    topicList.add(topic);
                } else if (previouslyFound) {
                    break;
                }

                previouslyFound = found;
            }
        }

        return topicList;
    }

    public Map<Integer, Topic> getTopics() {
        return topics;
    }

    public static class Topic implements Serializable {
        public int id;
        public boolean isFeatured = false;
        public String name;
        /**
         * Maybe empty or null, Separated by comma if multiple
         */
        public String otherTerms;
        /**
         * To display in recycler view
         */
        public String inChapters;
        public List<Integer> chapters;
        /**
         * Item format -> chapNo:VERSE or chapNo:fromVERSE-toVERSE
         */
        public List<String> verses;

        @NonNull
        @Override
        public String toString() {
            return "Topic{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", verses=" + verses +
                    '}';
        }
    }
}
