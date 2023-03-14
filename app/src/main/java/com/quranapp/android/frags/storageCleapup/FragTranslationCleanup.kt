package com.quranapp.android.frags.storageCleapup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.quranapp.android.R
import com.quranapp.android.adapters.storageCleanup.ADPTranslationCleanup
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.components.storageCleanup.TranslationCleanupItemModel
import com.quranapp.android.components.transls.TranslBaseModel
import com.quranapp.android.components.transls.TranslTitleModel
import com.quranapp.android.databinding.FragStorageCleanupBinding
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragTranslationCleanup : FragStorageCleanupBase() {
    override fun getFragTitle(ctx: Context) = ctx.getString(R.string.strTitleTranslations)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_storage_cleanup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragStorageCleanupBinding.bind(view)

        init(binding)
    }

    private fun init(binding: FragStorageCleanupBinding) {
        binding.loader.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val translFactory = QuranTranslationFactory(binding.root.context)
            val translItems = ArrayList<TranslBaseModel>()

            val languageAndInfo = HashMap<String, MutableList<QuranTranslBookInfo>>()
            for (bookInfo in translFactory.getDownloadedTranslationBooksInfo().values) {
                var listOfLang = languageAndInfo[bookInfo.langCode]

                if (listOfLang == null) {
                    listOfLang = ArrayList()
                    languageAndInfo[bookInfo.langCode] = listOfLang
                }

                listOfLang.add(bookInfo)
            }

            languageAndInfo.forEach { (langCode, listOfBooks) ->
                val translTitleModel = TranslTitleModel(langCode, null)
                translItems.add(translTitleModel)

                for (book in listOfBooks) {
                    val model = TranslationCleanupItemModel(book)
                    translTitleModel.langName = book.langName
                    translItems.add(model)
                }
            }

            withContext(Dispatchers.Main) {
                populateTranslations(binding, translItems)
            }
        }
    }

    private fun populateTranslations(
        binding: FragStorageCleanupBinding,
        items: List<TranslBaseModel>
    ) {
        val mAdapter = ADPTranslationCleanup(binding.root.context, items)
        binding.list.adapter = mAdapter
        binding.list.layoutManager = LinearLayoutManager(binding.root.context)

        binding.loader.visibility = View.GONE
    }
}
