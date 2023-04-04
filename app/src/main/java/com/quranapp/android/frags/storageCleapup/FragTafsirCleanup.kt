package com.quranapp.android.frags.storageCleapup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.quranapp.android.R
import com.quranapp.android.adapters.storageCleanup.ADPTafsirCleanup
import com.quranapp.android.components.storageCleanup.TafsirCleanupItemModel
import com.quranapp.android.databinding.FragStorageCleanupBinding
import com.quranapp.android.utils.reader.tafsir.TafsirManager
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragTafsirCleanup : FragStorageCleanupBase() {
    private lateinit var fileUtils: FileUtils

    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleTafsir)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_storage_cleanup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileUtils = FileUtils.newInstance(view.context)

        init(FragStorageCleanupBinding.bind(view))
    }

    private fun init(binding: FragStorageCleanupBinding) {
        binding.loader.visibility = View.VISIBLE

        TafsirManager.prepare(binding.root.context, false) {
            val tafsirItems = ArrayList<TafsirCleanupItemModel>()

            CoroutineScope(Dispatchers.IO).launch {
                fileUtils.tafsirDir.listFiles()?.filter { it.isDirectory }
                    ?.forEach { tafsirDir ->
                        val downloadsCount = tafsirDir.listFiles()?.filter { it.isFile }?.size ?: 0
                        if (downloadsCount > 0) {
                            val item = TafsirCleanupItemModel(
                                tafsirModel = TafsirManager.getModel(tafsirDir.name)
                                    ?: TafsirManager.emptyModel(
                                        slug = tafsirDir.name,
                                        name = tafsirDir.name
                                    ),
                                downloadsCount = downloadsCount
                            )

                            tafsirItems.add(item)
                        }
                    }

                withContext(Dispatchers.Main) {
                    populateReciters(binding, tafsirItems)
                }
            }
        }
    }

    private fun populateReciters(
        binding: FragStorageCleanupBinding,
        items: List<TafsirCleanupItemModel>
    ) {
        val mAdapter = ADPTafsirCleanup(items, fileUtils)
        binding.list.adapter = mAdapter
        binding.list.layoutManager = LinearLayoutManager(binding.root.context)

        binding.loader.visibility = View.GONE
    }
}
