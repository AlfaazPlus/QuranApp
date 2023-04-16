package com.quranapp.android.widgets.radio

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.RadioButton
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.colorStateList
import com.quranapp.android.widgets.compound.PeaceCompoundButton

/**
 * <p>
 * Radio buttons are normally used together in a
 * [PeaceRadioGroup]. When several radio buttons live inside
 * a radio group, checking one radio button unchecks all the others.</p>
 * </p>
 */
class PeaceRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PeaceCompoundButton(context, attrs, defStyleAttr) {
    private lateinit var radio: RadioHelper

    override fun makeComponents() {
        makeRadio()
        super.makeComponents()
    }

    private fun makeRadio() {
        radio = RadioHelper(context).apply {
            isChecked = initialChecked
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun getCompoundButton() = radio

    @SuppressLint("AppCompatCustomView")
    inner class RadioHelper(context: Context) : RadioButton(context) {
        init {
            setButtonDrawable(R.drawable.dr_radio)
            buttonTintList = context.colorStateList(R.color.radio_button_tint)
            compoundDrawablePadding = 0
            setOnCheckedChangeListener(this@PeaceRadioButton)
        }
    }
}
