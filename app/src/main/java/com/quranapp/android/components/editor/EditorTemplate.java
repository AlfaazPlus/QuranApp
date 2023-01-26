/*
 * (c) Faisal Khan. Created on 4/2/2022.
 */

package com.quranapp.android.components.editor;

import android.graphics.Color;

import androidx.annotation.DrawableRes;

import com.quranapp.android.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class EditorTemplate {
    private int bgType;
    @DrawableRes
    private int bgImageRes;
    private int[] bgColors;
    private int arabicColor;
    private int translColor;
    private int refColor;
    private int logoColor;

    private EditorTemplate(JSONObject templateJson) throws Exception {
        fillValues(templateJson);
    }

    public static EditorTemplate createWith(JSONObject templateJson) throws Exception {
        return new EditorTemplate(templateJson);
    }

    private void fillValues(JSONObject templateJson) throws Exception {
        JSONArray bg = templateJson.getJSONArray("bg");
        JSONObject fg = templateJson.getJSONObject("fg");
        String fgAr = fg.getString("ar");
        String fgTransl = fg.getString("transl");
        String fgRef = fg.getString("ref");
        String fgLogo = fg.getString("logo");

        arabicColor = Color.parseColor(fgAr);
        translColor = Color.parseColor(fgTransl);
        refColor = Color.parseColor(fgRef);
        logoColor = Color.parseColor(fgLogo);

        bgColors = new int[bg.length()];
        if (bg.length() == 1) {
            String bgClr = bg.getString(0);
            if ("img:default".equalsIgnoreCase(bgClr)) {
                bgType = VerseEditor.BG_TYPE_IMAGE;
                bgImageRes = R.drawable.dr_quran_wallpaper;
            } else {
                bgType = VerseEditor.BG_TYPE_COLORS;
                bgColors[0] = Color.parseColor(bgClr);
            }
        } else {
            bgType = VerseEditor.BG_TYPE_COLORS;
            for (int i = 0, l = bg.length(); i < l; i++) {
                bgColors[i] = Color.parseColor(bg.getString(i));
            }
        }
    }

    public int getBgType() {
        return bgType;
    }

    public int getBgImageRes() {
        return bgImageRes;
    }

    public int getArabicColor() {
        return arabicColor;
    }

    public int[] getBgColors() {
        return bgColors;
    }

    public int getTranslColor() {
        return translColor;
    }

    public int getRefColor() {
        return refColor;
    }

    public int getLogoColor() {
        return logoColor;
    }
}
