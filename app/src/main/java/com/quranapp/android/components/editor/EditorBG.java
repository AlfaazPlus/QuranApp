/*
 * (c) Faisal Khan. Created on 4/2/2022.
 */

package com.quranapp.android.components.editor;

import static com.quranapp.android.components.editor.VerseEditor.BRIGHTNESS_DARK;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;

import com.quranapp.android.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class EditorBG {
    private int bgType;
    private Drawable bgImage;
    private boolean isCustomImage;
    private int[] bgColors;
    private String brightness;

    private EditorBG(Context ctx, JSONObject bgJson) throws Exception {
        fillValues(ctx, bgJson);
    }

    private EditorBG(Drawable drawable) {
        bgType = VerseEditor.BG_TYPE_IMAGE;
        bgImage = drawable;
        isCustomImage = true;
        brightness = BRIGHTNESS_DARK;
    }

    public static EditorBG createWith(Context ctx, JSONObject bgJson) throws Exception {
        return new EditorBG(ctx, bgJson);
    }

    public static EditorBG createWith(Drawable drawable) throws Exception {
        return new EditorBG(drawable);
    }

    private void fillValues(Context ctx, JSONObject bgJson) throws Exception {
        JSONArray bg = bgJson.getJSONArray("bg");

        bgColors = new int[bg.length()];
        if (bg.length() == 1) {
            String bgClr = bg.getString(0);
            if ("img:default".equalsIgnoreCase(bgClr)) {
                bgType = VerseEditor.BG_TYPE_IMAGE;
                bgImage = AppCompatResources.getDrawable(ctx, R.drawable.dr_quran_wallpaper);
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

        brightness = bgJson.getString("brightness");
    }

    public int getBgType() {
        return bgType;
    }

    public void setBgImage(Drawable bgImage) {
        this.bgImage = bgImage;
    }

    public Drawable getBgImage() {
        return bgImage;
    }

    public boolean isCustomImage() {
        return isCustomImage;
    }

    public int[] getBgColors() {
        return bgColors;
    }

    public String getBrightness() {
        return brightness;
    }

    public boolean isDark() {
        return BRIGHTNESS_DARK.equals(getBrightness());
    }
}
