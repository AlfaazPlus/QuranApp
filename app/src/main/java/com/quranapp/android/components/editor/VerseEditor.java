/*
 * (c) Faisal Khan. Created on 4/2/2022.
 */

package com.quranapp.android.components.editor;

import android.content.Context;

import com.quranapp.android.activities.ActivityEditShare;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.interfaceUtils.editor.OnEditorChangeListener;
import com.quranapp.android.utils.univ.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class VerseEditor {
    public static final int BG_TYPE_IMAGE = 0;
    public static final int BG_TYPE_COLORS = 1;
    public static final String BRIGHTNESS_DARK = "dark";
    public static final String BRIGHTNESS_LIGHT = "light";

    private final OnEditorChangeListener mEditorChangeListener;
    private final int mChapNo;
    private final int mVerseNo;
    private ArrayList<EditorBG> mBGs;
    private ArrayList<EditorFG> mFGs;

    public static final float BG_ALPHA_DEFAULT = .7f;
    private float mBGAlpha = BG_ALPHA_DEFAULT;
    private int mBGAlphaColor;
    private EditorBG mLastBG;
    private Verse verse;

    private VerseEditor(ActivityEditShare activity, int chapNo, int verseNo) throws Exception {
        mEditorChangeListener = activity;

        mChapNo = chapNo;
        mVerseNo = verseNo;

        initializeTemplates(activity);
    }

    public static VerseEditor initialize(ActivityEditShare activity, int chapNo, int verseNo) throws Exception {
        return new VerseEditor(activity, chapNo, verseNo);
    }

    private void initializeTemplates(Context ctx) throws Exception {
        mBGs = new ArrayList<>();
        mFGs = new ArrayList<>();

        InputStream is = ctx.getAssets().open("editor_templates.json");
        JSONObject json = new JSONObject(StringUtils.readInputStream(is));

        JSONArray bgs = json.getJSONArray("bgs");
        for (int i = 0, l = bgs.length(); i < l; i++) {
            mBGs.add(EditorBG.createWith(ctx, bgs.getJSONObject(i)));
        }

        JSONArray fgs = json.getJSONArray("fgs");
        for (int i = 0, l = fgs.length(); i < l; i++) {
            mFGs.add(EditorFG.createWith(fgs.getJSONObject(i)));
        }
    }

    public ArrayList<EditorBG> getBGs() {
        return mBGs;
    }

    public ArrayList<EditorFG> getFGs() {
        return mFGs;
    }

    public int getChapNo() {
        return mChapNo;
    }

    public int getVerseNo() {
        return mVerseNo;
    }

    public OnEditorChangeListener getListener() {
        return mEditorChangeListener;
    }

    public void setBGAlpha(float alpha) {
        mBGAlpha = alpha;
    }

    public float getBGAlpha() {
        return mBGAlpha;
    }

    public void setLastBG(EditorBG lastBG) {
        mLastBG = lastBG;
    }

    public EditorBG getLastBG() {
        return mLastBG;
    }

    public boolean isBGBrightnessNotSameAsBefore(EditorBG nBg) {
        return getLastBG() == null || !Objects.equals(getLastBG().getBrightness(), nBg.getBrightness());
    }

    public int getBGAlphaColor() {
        return mBGAlphaColor;
    }

    public void setBGAlphaColor(int bgAlphaColor) {
        mBGAlphaColor = bgAlphaColor;
    }

    public ArrayList<Integer> getFGIndicesAgainstBrightness(String bgBrightness) {
        String fgBrightness = BRIGHTNESS_DARK.equals(bgBrightness) ? BRIGHTNESS_LIGHT : BRIGHTNESS_DARK;
        ArrayList<Integer> compatibleFGPos = new ArrayList<>();
        for (int i = 0, l = mFGs.size(); i < l; i++) {
            if (fgBrightness.equalsIgnoreCase(mFGs.get(i).getBrightness())) {
                compatibleFGPos.add(i);
            }
        }
        return compatibleFGPos;
    }

    public void setVerse(Verse verse) {
        this.verse = verse.copy();
    }

    public Verse getVerse() {
        return verse;
    }
}
