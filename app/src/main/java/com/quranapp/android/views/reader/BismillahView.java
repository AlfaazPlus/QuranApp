package com.quranapp.android.views.reader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.LayoutParamsKt;

public class BismillahView extends AppCompatTextView {
    public BismillahView(Context context) {
        this(context, null);
    }

    public BismillahView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BismillahView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        setText(context.getString(R.string.strBismillahEntity));
        TextViewCompat.setTextAppearance(this, R.style.TextAppearanceBismillah);
        setGravity(Gravity.CENTER);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof MarginLayoutParams) {
            params.width = MATCH_PARENT;
            LayoutParamsKt.updateMarginVertical((MarginLayoutParams) params, Dimen.dp2px(getContext(), 10));
        }
        super.setLayoutParams(params);
    }
}
