package com.quranapp.android.frags.readerindex

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReaderIndexPage
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.databinding.FragReaderIndexBinding
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.interfaceUtils.readerIndex.FragReaderIndexCallback
import com.quranapp.android.viewModels.FavChaptersViewModel
import com.quranapp.android.views.helper.RecyclerView2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

abstract class BaseFragReaderIndex : BaseFragment(), FragReaderIndexCallback {
    lateinit var favChaptersModel: FavChaptersViewModel
    private val quranMetaRef = AtomicReference<QuranMeta>()
    protected lateinit var binding: FragReaderIndexBinding
    private var isReversed = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (context as? ActivityReaderIndexPage)?.addToCallbacks(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_reader_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favChaptersModel = ViewModelProvider(requireActivity())[FavChaptersViewModel::class.java]
        favChaptersModel.refreshFavChapters(view.context)
        binding = FragReaderIndexBinding.bind(view)

        // ViewUtils.setBounceOverScrollRV(mBinding.list);
        binding.loader.visibility = View.VISIBLE
        QuranMeta.prepareInstance(view.context, object : OnResultReadyCallback<QuranMeta> {
            override fun onReady(r: QuranMeta) {
                quranMetaRef.set(r)

                binding.loader.visibility = View.VISIBLE

                lifecycleScope.launch(Dispatchers.Main) {
                    initList(binding.list, view.context)

                    binding.loader.visibility = View.GONE
                }
            }
        })
    }

    @CallSuper
    protected open fun initList(list: RecyclerView2, ctx: Context) {
        val animator = list.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    override fun scrollToTop(smooth: Boolean) {
        binding.list.layoutManager?.let {
            if (smooth) binding.list.smoothScrollToPosition(0)
            else binding.list.scrollToPosition(0)
        }
    }

    override fun sort(ctx: Context) {
        binding.loader.visibility = View.VISIBLE
        lifecycleScope.launch {
            resetAdapter(binding.list, ctx, !isReversed)

            withContext(Dispatchers.Main) {
                binding.loader.visibility = View.GONE
            }
        }
    }

    @CallSuper
    protected open fun resetAdapter(list: RecyclerView2, ctx: Context, reverse: Boolean) {
        isReversed = reverse
    }

    val quranMeta: QuranMeta get() = quranMetaRef.get()
}