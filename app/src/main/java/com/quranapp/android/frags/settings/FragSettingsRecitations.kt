package com.quranapp.android.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.adapters.recitation.ADPRecitations
import com.quranapp.android.components.recitation.Recitation
import com.quranapp.android.components.recitation.RecitationModel
import com.quranapp.android.databinding.FragSettingsTranslBinding
import com.quranapp.android.interfaceUtils.RecitationExplorerImpl
import com.quranapp.android.utils.sp.SPAppActions
import com.quranapp.android.utils.sp.SPReader
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.views.BoldHeader
import com.quranapp.android.views.BoldHeader.BoldHeaderCallback
import com.quranapp.android.widgets.PageAlert
import java.util.regex.Pattern

class FragSettingsRecitations : FragSettingsBase(), RecitationExplorerImpl {

    private lateinit var mBinding: FragSettingsTranslBinding
    private lateinit var mFileUtils: FileUtils

    private var mSavedRecitation: String? = null
    private var mAdapter: ADPRecitations? = null
    private var mModels: List<RecitationModel>? = null
    private var mPageAlert: PageAlert? = null

    override var savedReciter: String?
        get() = mSavedRecitation
        set(value) {
            mSavedRecitation = value
        }

    override fun getFragTitle(ctx: Context): String {
        return ctx.getString(R.string.strTitleRecitations)
    }

    override fun getLayoutResource(): Int {
        return R.layout.frag_settings_transl
    }

    override fun getFinishingResult(ctx: Context?): Bundle? {
        if (SPReader.getSavedRecitationSlug(ctx) != mSavedRecitation) {
            return bundleOf(ActivityReader.KEY_RECITER_CHANGED to true)
        }
        return null
    }

    override fun setupHeader(activity: ActivitySettings, header: BoldHeader) {
        super.setupHeader(activity, header)
        header.apply {
            setCallback(object : BoldHeaderCallback {
                override fun onBackIconClick() {
                    activity.onBackPressed()
                }

                override fun onRightIconClick() {
                    refresh(activity, true)
                }

                override fun onSearchRequest(searchBox: EditText, newText: CharSequence) {
                    search(newText)
                }
            })

            setShowSearchIcon(false)
            setShowRightIcon(false)
            disableRightBtn(false)
            setSearchHint(R.string.strHintSearchReciter)
            setRightIconRes(R.drawable.dr_icon_refresh, activity.getString(R.string.strLabelRefresh))
        }
    }

    override fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?) {
        mFileUtils = FileUtils.newInstance(ctx)
        mSavedRecitation = SPReader.getSavedRecitationSlug(ctx)
        mBinding = FragSettingsTranslBinding.bind(view)

        init(ctx)
    }

    private fun init(ctx: Context) {
        refresh(ctx, SPAppActions.getFetchRecitationsForce(ctx))
    }

    private fun initPageAlert(ctx: Context) {
        mPageAlert = PageAlert(ctx)
    }


    private fun refresh(ctx: Context, force: Boolean) {
        showLoader()
        Recitation.prepare(ctx, force) { availableRecitationsModel ->
            val models = availableRecitationsModel.reciters

            if (models.isNotEmpty()) {
                populateTranslations(ctx, models)
            } else {
                noRecitersAvailable(ctx)
            }

            hideLoader()
        }
    }

    private fun search(query: CharSequence) {
        val adapter = mAdapter ?: return
        val models = mModels ?: return

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

        val found = ArrayList<RecitationModel>()
        for (model in models) {
            val reciter = model.reciter

            if (reciter.isEmpty()) continue

            if (pattern.matcher(reciter).find()) {
                found.add(model)
            }
        }

        resetAdapter(found)
    }


    private fun populateTranslations(ctx: Context, models: List<RecitationModel>) {
        mModels = models

        mBinding.list.layoutManager = LinearLayoutManager(ctx)
        val itemAnimator = mBinding.list.itemAnimator
        if (itemAnimator is SimpleItemAnimator) {
            itemAnimator.supportsChangeAnimations = false
        }

        mAdapter = ADPRecitations()

        resetAdapter(models)

        (activity as? ActivitySettings)?.header?.apply {
            setShowSearchIcon(true)
            setShowRightIcon(true)
        }
    }

    private fun resetAdapter(models: List<RecitationModel>) {
        mAdapter?.setModels(models)
        mBinding.list.adapter = mAdapter
    }

    private fun noRecitersAvailable(ctx: Context) {
        showAlert(ctx, 0, R.string.strMsgRecitationsNoAvailable, R.string.strLabelRefresh) { refresh(ctx, true) }
    }

    private fun showLoader() {
        hideAlert()
        mBinding.loader.visibility = View.VISIBLE

        (activity as? ActivitySettings)?.header?.apply {
            setShowRightIcon(false)
            setShowSearchIcon(false)
        }
    }

    private fun hideLoader() {
        mBinding.loader.visibility = View.GONE

        (activity as? ActivitySettings)?.header?.apply {
            setShowRightIcon(true)
            setShowSearchIcon(true)
        }
    }

    private fun showAlert(ctx: Context, titleRes: Int, msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()

        if (mPageAlert == null) {
            initPageAlert(ctx)
        }

        mPageAlert!!.apply {
            setIcon(null)
            setMessage(if (titleRes > 0) ctx.getString(titleRes) else "", ctx.getString(msgRes))
            setActionButton(btnRes, action)
            show(mBinding.container)
        }

        (activity as? ActivitySettings)?.header?.apply {
            setShowSearchIcon(false)
            setShowRightIcon(true)
        }
    }

    private fun hideAlert() {
        mPageAlert?.remove()
    }


    private fun noInternet(ctx: Context) {
        if (mPageAlert == null) {
            initPageAlert(ctx)
        }

        mPageAlert!!.apply {
            setupForNoInternet { refresh(ctx, true) }
            show(mBinding.container)
        }

        (activity as? ActivitySettings)?.header?.apply {
            setShowSearchIcon(false)
            setShowRightIcon(true)
        }
    }
}