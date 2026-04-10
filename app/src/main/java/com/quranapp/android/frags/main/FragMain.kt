package com.quranapp.android.frags.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityQuranScience
import com.quranapp.android.compose.components.VerseOfTheDay
import com.quranapp.android.compose.theme.QuranAppTheme
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.FragMainBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.app.UpdateManager

class FragMain : BaseFragment() {
    private lateinit var binding: FragMainBinding
    private lateinit var updateManager: UpdateManager
    private var votdComposeView: ComposeView? = null

    override fun networkReceiverRegistrable(): Boolean {
        return true
    }

    override fun onPause() {
        super.onPause()
        updateManager.onPause()
    }


    override fun onResume() {
        super.onResume()
        updateManager.onResume()
        val context = context ?: return

        QuranMeta.prepareInstance(context, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                binding.readHistory.post { binding.readHistory.refresh(r) }
            }
        })
    }

    override fun onDestroy() {
        votdComposeView?.disposeComposition()
        votdComposeView = null
        binding.readHistory.destroy()
        super.onDestroy()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateManager = UpdateManager(view.context, binding.appUpdateContainer)

        // If update is not critical, proceed to load the rest of the content
        if (!updateManager.check4Update()) {
            QuranMeta.prepareInstance(
                view.context,
                object : OnResultReadyCallback<QuranMeta> {
                    override fun onReady(r: QuranMeta) {
                        initContent(r)
                    }
                }
            )
        }

    }

    private fun initContent(quranMeta: QuranMeta) {
        initVOTD()
        binding.let {
            arrayOf(
                it.readHistory,
                it.featuredReading,
                it.featuredDua,
                it.solutions,
                it.etiquette,
                it.majorSins,
                it.prophets
            ).forEach { layout ->
                layout.initialize()
                layout.refresh(quranMeta)
            }

            it.quranScience.visibility = View.VISIBLE
            it.quranScienceBg.clipToOutline = true
            it.quranScience.setOnClickListener { v ->
                v.context.startActivity(Intent(v.context, ActivityQuranScience::class.java))
            }
        }
    }


    private fun initVOTD() {
        if (votdComposeView != null) {
            return
        }

        votdComposeView = ComposeView(binding.container.context).apply {
            id = R.id.homepageVOTD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                QuranAppTheme {
                    VerseOfTheDay()
                }
            }
        }

        binding.container.addView(votdComposeView, 1)
    }

}
