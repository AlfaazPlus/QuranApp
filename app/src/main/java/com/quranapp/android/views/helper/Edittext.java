package com.quranapp.android.views.helper;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

public class Edittext extends AppCompatEditText {
    public Edittext(@NonNull Context context) {
        super(context);
    }

    public Edittext(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Edittext(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*@Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {
            clearFocus();
            return false;
        }
        return super.dispatchKeyEvent(event);
    }*/
}
