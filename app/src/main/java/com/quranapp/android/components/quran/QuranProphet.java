package com.quranapp.android.components.quran;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.quranapp.android.interfaceUtils.OnResultReadyCallback;
import com.quranapp.android.utils.quran.parser.QuranProphetParser;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class QuranProphet {
    private static final AtomicReference<QuranProphet> sQuranProphetRef = new AtomicReference<>();
    private final List<Prophet> prophets;

    public QuranProphet(List<Prophet> prophetList) {
        prophets = prophetList;
    }

    public static void prepareInstance(Context context, QuranMeta quranMeta, OnResultReadyCallback<QuranProphet> resultReadyCallback) {
        if (sQuranProphetRef.get() == null) {
            synchronized (QuranProphet.class) {
                prepare(context, quranMeta, resultReadyCallback);
            }
        } else {
            resultReadyCallback.onReady(sQuranProphetRef.get());
        }
    }

    private static void prepare(Context context, QuranMeta quranMeta, OnResultReadyCallback<QuranProphet> resultReadyCallback) {
        QuranProphetParser.parseProphet(context, quranMeta, sQuranProphetRef,
            () -> resultReadyCallback.onReady(sQuranProphetRef.get()));
    }


    public List<Prophet> getProphets() {
        return prophets;
    }

    public static class Prophet implements Serializable {
        public int order;
        public String nameAr;
        public String nameEn;
        public String nameTrans;
        public String honorific;
        @DrawableRes
        public int iconRes;
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
            return MessageFormat.format("Prophet: {0} ({1}) {2} : [order={3}, iconRes={4}]", nameTrans, nameEn,
                honorific, order,
                iconRes);
        }
    }
}
