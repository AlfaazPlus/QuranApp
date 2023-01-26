package com.peacedesign.android.widget.radio;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.peacedesign.R;
import com.peacedesign.android.widget.compound.PeaceCompoundButton;

/**
 * <p>
 * Radio buttons are normally used together in a
 * {@link PeaceRadioGroup}. When several radio buttons live inside
 * a radio group, checking one radio button unchecks all the others.</p>
 * </p>
 */
public class PeaceRadioButton extends PeaceCompoundButton {
    private RadioHelper mRadio;


    public PeaceRadioButton(@NonNull Context context) {
        this(context, null);
    }

    public PeaceRadioButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeaceRadioButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PeaceRadioButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    protected void makeComponents() {
        makeRadio();
        super.makeComponents();
    }

    private void makeRadio() {
        mRadio = new RadioHelper(getContext());
        mRadio.setLayoutParams(new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
    }

    @NonNull
    @Override
    protected CompoundButton getCompoundButton() {
        return mRadio;
    }

    @SuppressLint("AppCompatCustomView")
    class RadioHelper extends RadioButton {
        public RadioHelper(@NonNull Context context) {
            super(context);
            setButtonDrawable(R.drawable.dr_radio);
            setButtonTintList(ContextCompat.getColorStateList(context, R.color.radio_button_tint));
            setCompoundDrawablePadding(0);
            setChecked(mInitialChecked);
            setOnCheckedChangeListener(PeaceRadioButton.this);
        }
    }
}
