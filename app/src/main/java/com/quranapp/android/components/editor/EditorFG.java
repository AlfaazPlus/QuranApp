/*
 * (c) Faisal Khan. Created on 4/2/2022.
 */

package com.quranapp.android.components.editor;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

public class EditorFG {
    private int[] colors;
    private String brightness;

    private EditorFG(JSONObject fgJson) throws Exception {
        fillValues(fgJson);
    }

    public static EditorFG createWith(JSONObject fgJson) throws Exception {
        return new EditorFG(fgJson);
    }

    private void fillValues(JSONObject fgJson) throws Exception {
        JSONArray fg = fgJson.getJSONArray("fg");
        colors = new int[fg.length()];
        for (int i = 0, l = fg.length(); i < l; i++) {
            colors[i] = Color.parseColor(fg.getString(i));
        }

        brightness = fgJson.getString("brightness");
    }

    public String getBrightness() {
        return brightness;
    }

    public int[] getColors() {
        return colors;
    }

    public void shuffle() {
        Random random = new Random();
        int count = colors.length;
        for (int i = count; i > 1; i--) {
            swap(colors, i - 1, random.nextInt(i));
        }
    }

    private static void swap(int[] array, int i, int j) {
        if (i == j) {
            i = i - 1;
        }

        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
