package com.quranapp.android.frags.settings.recitations.manage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.models.recitation.RecitationManageAudioInfoModel
import com.quranapp.android.frags.settings.FragSettingsBase
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.views.BoldHeader

class FragSettingsManageAudioReciter : FragSettingsBase() {
    companion object {
        const val KEY_RECITER_MODEL = "key.reciter_slug"
    }

    private var model: RecitationManageAudioInfoModel? = null

    override fun getFragTitle(ctx: Context): String? {
        return model?.reciter
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)

        header.apply {
            setCallback(object : BoldHeader.BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                }
            })

            disableRightBtn(false)
            setSearchHint(R.string.strHintSearchReciter)
            setRightIconRes(
                R.drawable.dr_icon_refresh,
                activity.getString(R.string.strLabelRefresh)
            )
        }
    }

    override val layoutResource get() = R.layout.frag_settings_transl

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        model = getArgs().serializableExtra(KEY_RECITER_MODEL)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
    }
}