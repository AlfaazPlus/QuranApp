package com.quranapp.android.activities

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.peacedesign.android.utils.AppBridge
import com.peacedesign.android.utils.DrawableUtils
import com.quranapp.android.BuildConfig
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.databinding.ActivityAboutBinding
import com.quranapp.android.databinding.LytReaderSettingsItemBinding
import com.quranapp.android.utils.app.InfoUtils.openAbout
import com.quranapp.android.utils.app.InfoUtils.openDiscord
import com.quranapp.android.utils.extensions.isRTL
import com.quranapp.android.utils.extensions.updateMarginHorizontal
import com.quranapp.android.utils.extensions.updateMarginVertical
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.widgets.IconedTextView

class ActivityAbout : BaseActivity() {
    override fun shouldInflateAsynchronously() = true

    override fun getLayoutResource() = R.layout.activity_about

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityAboutBinding.bind(activityView)
        initHeader(binding.header)
        init(binding)
    }

    private fun initHeader(header: BoldHeader) {
        header.let {
            it.setCallback { onBackPressedDispatcher.onBackPressed() }
            it.setTitleText(R.string.strTitleAboutUs)
            it.setShowRightIcon(false)
            it.setShowSearchIcon(false)
            it.setBGColor(R.color.colorBGPage)
        }
    }

    private fun init(binding: ActivityAboutBinding) {
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater),
            R.drawable.dr_logo,
            R.string.strTitleAppVersion,
            BuildConfig.VERSION_NAME,
            false
        )
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater).apply {
                root.setOnClickListener { openAbout(this@ActivityAbout) }
            },
            R.drawable.dr_icon_info,
            R.string.strTitleAboutUs
        )
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater).apply {
                root.setOnClickListener {
                    AppBridge.newOpener(it.context).browseLink(ApiConfig.GITHUB_REPOSITORY_URL)
                }
            },
            R.drawable.icon_github,
            R.string.github
        )
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater).apply {
                root.setOnClickListener { openDiscord(this@ActivityAbout) }
            },
            R.drawable.icon_discord,
            R.string.discord
        )
    }

    private fun setup(
        parent: ActivityAboutBinding,
        binding: LytReaderSettingsItemBinding,
        startIcon: Int,
        titleRes: Int,
        subtitle: String? = null,
        showArrow: Boolean = true
    ) {
        setupLauncherParams(binding.root)
        prepareTitle(binding, titleRes, subtitle)
        setupIcons(startIcon, binding.launcher, showArrow)
        parent.container.addView(binding.root)
    }

    private fun prepareTitle(
        binding: LytReaderSettingsItemBinding,
        titleRes: Int,
        subtitle: String?
    ) {
        val ssb = SpannableStringBuilder()

        ssb.append(
            SpannableString(str(titleRes)).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )

        if (!subtitle.isNullOrEmpty()) {
            ssb.append("\n").append(
                SpannableString(subtitle).apply {
                    setSpan(
                        AbsoluteSizeSpan(dimen(R.dimen.dmnCommonSize2)),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        ForegroundColorSpan(color(R.color.colorText3)),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )
        }

        binding.launcher.text = ssb
    }

    private fun setupIcons(startIconRes: Int, textView: IconedTextView, showArrow: Boolean) {
        var chevronRight = if (showArrow) drawable(R.drawable.dr_icon_chevron_right) else null
        if (chevronRight != null && isRTL()) {
            chevronRight = DrawableUtils.rotate(this, chevronRight, 180f)
        }
        textView.setDrawables(drawable(startIconRes), null, chevronRight, null)
    }

    private fun setupLauncherParams(launcherView: View) {
        launcherView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            updateMarginVertical(dp2px(5f))
            updateMarginHorizontal(dp2px(10f))
        }
    }
}
