/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 5/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.components.utility;

import android.graphics.drawable.Drawable;

public class CardMessageParams {
    public static final int STYLE_NONE = 0;
    public static final int STYLE_WARNING = 1;
    public static final int STYLE_SUCCESS = 2;
    public static final int STYLE_ERROR = 3;
    private Drawable mIcon;
    private CharSequence mMessage;
    private CharSequence mActionText;
    private Runnable mActionListener;

    private int messageStyle = STYLE_NONE;

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
    }

    public CharSequence getMessage() {
        return mMessage;
    }

    public void setMessage(CharSequence message) {
        mMessage = message;
    }

    public CharSequence getActionText() {
        return mActionText;
    }

    public void setActionText(CharSequence actionText) {
        mActionText = actionText;
    }

    public Runnable getActionListener() {
        return mActionListener;
    }

    public void setActionListener(Runnable actionListener) {
        mActionListener = actionListener;
    }

    public int getMessageStyle() {
        return messageStyle;
    }

    public void setMessageStyle(int messageStyle) {
        this.messageStyle = messageStyle;
    }
}
