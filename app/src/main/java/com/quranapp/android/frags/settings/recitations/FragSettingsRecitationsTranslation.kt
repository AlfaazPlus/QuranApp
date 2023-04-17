package com.quranapp.android.frags.settings.recitations

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.adapters.recitation.ADPRecitationTranslations
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.StringUtils
import java.util.regex.Pattern

class FragSettingsRecitationsTranslation : FragSettingsRecitationsBase() {
    private val adapter = ADPRecitationTranslations(this)
    private var models: List<RecitationTranslationInfoModel>? = null

    private var initialRecitation: String? = null

    override fun getFinishingResult(ctx: Context): Bundle? {
        if (initialRecitation != null && SPReader.getSavedRecitationSlug(ctx) != initialRecitation) {
            return bundleOf(ActivityReader.KEY_TRANSLATION_RECITER_CHANGED to true)
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = view.context

        initialRecitation = SPReader.getSavedRecitationTranslationSlug(ctx)

        init(ctx)
    }

    private fun init(ctx: Context) {
        adapter.isManageAudio = isManageAudio
        refresh(ctx, SPAppActions.getFetchRecitationTranslationsForce(ctx))
    }

    override fun refresh(context: Context, force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(context)) {
            noInternet(context)
            return
        }

        showLoader()

        RecitationManager.prepareTranslations(context, force) {
            val models = RecitationManager.getTranslationModels()

            if (!models.isNullOrEmpty()) {
                populateRecitations(context, models)
            } else {
                noRecitersAvailable(context)
            }

            hideLoader()
        }
    }

    override fun search(query: CharSequence) {
        val models = models ?: return

        if (query.isEmpty()) {
            if (adapter.itemCount != models.size) {
                resetAdapter(models)
            }
            return
        }

        val pattern = Pattern.compile(
            StringUtils.escapeRegex(query.toString()),
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        val found = ArrayList<RecitationTranslationInfoModel>()
        for (model in models) {
            if (pattern.matcher(model.reciter).find() || pattern.matcher(model.getReciterName()).find()) {
                found.add(model)
            }
        }

        resetAdapter(found)
    }

    private fun populateRecitations(ctx: Context, models: List<RecitationTranslationInfoModel>) {
        this.models = models

        binding.list.layoutManager = LinearLayoutManager(ctx)
        (binding.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        resetAdapter(models)
    }

    private fun resetAdapter(models: List<RecitationTranslationInfoModel>) {
        adapter.setModels(models)
        binding.list.adapter = adapter
    }
}
