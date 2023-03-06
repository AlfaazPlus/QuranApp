package com.quranapp.android.views.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.quranapp.android.R;

public class CheckBox2 extends AppCompatCheckBox {
    public CheckBox2(@NonNull Context context) {
        this(context, null);
    }

    public CheckBox2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckBox2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(new ContextThemeWrapper(context, R.style.PeaceCheckBox), attrs, defStyleAttr);
        setPaddingRelative(0, 0, 0, 0);
    }
}
