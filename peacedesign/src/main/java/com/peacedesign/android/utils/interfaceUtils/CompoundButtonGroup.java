/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 2/4/2022.
 * All rights reserved.
 */

package com.peacedesign.android.utils.interfaceUtils;

import androidx.annotation.IdRes;

import com.peacedesign.android.widget.radio.PeaceRadioButton;


public interface CompoundButtonGroup {
    void clearCheck();

    /**
     * Check a button without invoking the Listeners.
     *
     * @param id Id of the {@link PeaceRadioButton} to be checked.
     */
    void checkSansInvocation(@IdRes int id);

    void checkAtPosition(int position);

    void check(@IdRes int id);
}
