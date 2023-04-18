package com.quranapp.android.frags.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.adapters.recitation.ADPRecitations
import com.quranapp.android.api.models.recitation.RecitationInfoModel
import com.quranapp.android.databinding.LytOnboardRecitationsBinding
import com.quranapp.android.utils.reader.recitation.RecitationManager.getModels
import com.quranapp.android.utils.reader.recitation.RecitationManager.prepare
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.thread.runner.CallableTaskRunner
import com.quranapp.android.widgets.PageAlert

class FragOnboardRecitation : FragOnboardBase() {
    private val recitationTaskRunner = CallableTaskRunner<String>()
    private lateinit var binding: LytOnboardRecitationsBinding
    private lateinit var pageAlert: PageAlert

    override fun onDestroy() {
        recitationTaskRunner.cancel()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.lyt_onboard_recitations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = LytOnboardRecitationsBinding.bind(view)
        pageAlert = PageAlert(view.context)

        initRecitations(binding.list)
    }

    private fun initRecitations(list: RecyclerView) {
        list.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        list.layoutManager = LinearLayoutManager(list.context)
        (list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        refresh(list, false)
    }

    private fun refresh(list: RecyclerView, force: Boolean) {
        if (force && !NetworkStateReceiver.isNetworkConnected(list.context)) {
            noInternet()
            return
        }

        showLoader()
        prepare(list.context, force) {
            val models = getModels()

            if (!models.isNullOrEmpty()) {
                populateRecitation(list, models)
            } else {
                noRecitersAvailable()
            }

            hideLoader()
        }
    }

    private fun populateRecitation(list: RecyclerView, items: List<RecitationInfoModel>) {
        list.adapter = ADPRecitations(null).apply {
            setModels(items)
        }
    }

    private fun noRecitersAvailable() {
        hideLoader()

        pageAlert.let {
            it.setIcon(null)
            it.setMessage("", binding.root.context.getString(R.string.strMsgRecitationsNoAvailable))
            it.setActionButton(R.string.strLabelRefresh) { refresh(binding.list, true) }
            it.show(binding.root)
        }
    }

    private fun showLoader() {
        hideAlert()
        binding.loader.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        binding.loader.visibility = View.GONE
    }

    private fun showAlert(msgRes: Int, btnRes: Int, action: Runnable) {
        hideLoader()
        pageAlert.let {
            it.setIcon(null)
            it.setMessage("", binding.root.context.getString(msgRes))
            it.setActionButton(btnRes, action)
            it.show(binding.root)
        }
    }

    private fun hideAlert() {
        pageAlert.remove()
    }

    private fun noInternet() {
        pageAlert.setupForNoInternet { refresh(binding.list, true) }
        pageAlert.show(binding.root)
    }
}
