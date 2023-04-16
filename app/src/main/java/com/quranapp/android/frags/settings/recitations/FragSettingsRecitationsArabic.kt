package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.adapters.recitation.ADPRecitations
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.StringUtils
import java.util.regex.Pattern

class FragSettingsRecitationsArabic : FragSettingsRecitationsBase() {
    private val mAdapter = ADPRecitations()
    private var mModels: List<RecitationInfoModel>? = null
    private var mInitialRecitation: String? = null

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (mInitialRecitation != null && SPReader.getSavedRecitationSlug(ctx) != mInitialRecitation) {
            return bundleOf(ActivityReader.KEY_RECITER_CHANGED to true)
        }
        return null
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        super.onViewReady(ctx, view, savedInstanceState)
        mInitialRecitation = SPReader.getSavedRecitationSlug(ctx)

        init(ctx)
    }

    private fun init(ctx: Context) {
        refresh(ctx, SPAppActions.getFetchRecitationsForce(ctx))
    }

    override fun refresh(context: Context, force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(context)) {
            noInternet(context)
        }

        showLoader()

        RecitationManager.prepare(context, force) {
            val models = RecitationManager.getModels()

            if (!models.isNullOrEmpty()) {
                populateRecitations(context, models)
            } else {
                noRecitersAvailable(context)
            }

            hideLoader()
        }
    }

    override fun search(query: CharSequence) {
        val models = mModels ?: return

        if (query.isEmpty()) {
            if (mAdapter.itemCount != models.size) {
                resetAdapter(models)
            }
            return
        }
        val pattern = Pattern.compile(
            StringUtils.escapeRegex(query.toString()),
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        val found = ArrayList<RecitationInfoModel>()
        for (model in models) {
            if (pattern.matcher(model.reciter).find() || pattern.matcher(model.getReciterName()).find()) {
                found.add(model)
            }
        }

        resetAdapter(found)
    }

    private fun populateRecitations(ctx: Context, models: List<RecitationInfoModel>) {
        mModels = models

        binding.list.layoutManager = LinearLayoutManager(ctx)
        (binding.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        resetAdapter(models)

        activity()?.header?.apply {
            setShowSearchIcon(true)
            setShowRightIcon(true)
        }
    }

    private fun resetAdapter(models: List<RecitationInfoModel>) {
        mAdapter.setModels(models)
        binding.list.adapter = mAdapter
    }
}