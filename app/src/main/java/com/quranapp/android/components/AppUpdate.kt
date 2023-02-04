/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/2/2023.
 * All rights reserved.
 */

package com.quranapp.android.components

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdate(
    val version: Long,
    val priority: Int
) {
    val priorityName = priorityIntToName(priority)

    private fun priorityIntToName(priorityInt: Int): String {
        return when (priorityInt) {
            AppUpdateInfo.CRITICAL -> "CRITICAL"
            AppUpdateInfo.MAJOR -> "MAJOR"
            AppUpdateInfo.MODERATE -> "MODERATE"
            AppUpdateInfo.MINOR -> "MINOR"
            AppUpdateInfo.COSMETIC -> "COSMETIC"
            else -> "NONE"
        }
    }

    override fun toString(): String {
        return "AppUpdate(version=$version, priority=$priority, priorityName=$priorityName)"
    }
}