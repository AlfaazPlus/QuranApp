/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 5/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.interfaceUtils;

import android.content.Intent;

import androidx.core.app.ActivityOptionsCompat;

public interface ActivityResultStarter {
    void startActivity4Result(Intent intent, ActivityOptionsCompat options);
}
