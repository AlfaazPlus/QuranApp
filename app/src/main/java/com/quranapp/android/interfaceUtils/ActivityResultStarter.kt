/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 5/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.interfaceUtils

import android.content.Intent
import androidx.core.app.ActivityOptionsCompat

interface ActivityResultStarter {
    fun startActivity4Result(intent: Intent, options: ActivityOptionsCompat?)
}