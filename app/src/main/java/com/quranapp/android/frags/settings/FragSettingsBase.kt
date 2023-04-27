/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */
package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.widget.NestedScrollView
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.utils.extensions.color
import com.quranapp.android.views.BoldHeader

abstract class FragSettingsBase : BaseFragment() {
    protected fun activity(): ActivitySettings? = activity as? ActivitySettings

    protected open val shouldCreateScrollerView = false

    protected open fun getFragView(ctx: Context): View? = null

    abstract fun getFragTitle(ctx: Context): String?

    @get:LayoutRes
    abstract val layoutResource: Int

    @ColorInt
    open fun getPageBackgroundColor(ctx: Context): Int = ctx.color(R.color.colorBGPage)

    open fun getFinishingResult(ctx: Context): Bundle? = null

    @CallSuper
    open fun onNewArguments(args: Bundle) {
        arguments = args
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (shouldCreateScrollerView) {
            NestedScrollView(inflater.context).apply {
                id = R.id.scrollView
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                addView(
                    getFragView(inflater.context) ?: inflater.inflate(layoutResource, this, false)
                )
            }
        } else {
            getFragView(inflater.context) ?: inflater.inflate(layoutResource, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(getPageBackgroundColor(view.context))

        if (shouldCreateScrollerView) {
            onViewReady(view.context, (view as ViewGroup).getChildAt(0), savedInstanceState)
        } else {
            onViewReady(view.context, view, savedInstanceState)
        }
    }

    abstract fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?)

    @CallSuper
    open fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        header.setTitleText(getFragTitle(activity))
    }

    fun launchFrag(cls: Class<out FragSettingsBase?>, args: Bundle?) {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.frags_container, cls, args, cls.simpleName)
            setReorderingAllowed(true)
            addToBackStack(cls.simpleName)
            commit()
        }
    }
}
