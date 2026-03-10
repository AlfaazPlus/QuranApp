package com.quranapp.android.frags.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.adapters.transl.ADPTransls
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslBaseModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslTitleModel
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.thread.runner.CallableTaskRunner
import com.quranapp.android.utils.thread.tasks.BaseCallableTask
import com.quranapp.android.utils.univ.FileUtils

class FragOnboardTranslations : FragOnboardBase() {
    private val translTaskRunner = CallableTaskRunner<List<TranslBaseModel>>()
    private var translSlugs: Set<String> = HashSet()

    override fun onDestroy() {
        translTaskRunner.cancel()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return RecyclerView(inflater.context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        translSlugs = SPReader.getSavedTranslations(view.context)
        initTranslations(view as RecyclerView)
    }

    private fun initTranslations(list: RecyclerView) {
        list.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        list.layoutManager = LinearLayoutManager(list.context)
        showTranslations(list)
    }

    private fun showTranslations(list: RecyclerView) {
        translTaskRunner.callAsync(
            object : LoadTranslsTask(FileUtils.newInstance(list.context), translSlugs) {
                override fun onComplete(translItems: List<TranslBaseModel>) {
                    if (translItems.isNotEmpty()) {
                        populateTranslations(list, translItems)
                    }
                }
            }
        )
    }

    private fun populateTranslations(list: RecyclerView, translItems: List<TranslBaseModel>) {
        list.adapter = ADPTransls(list.context, translItems) { ctx, model, isSelected ->
            TranslUtils.resolveSelectionChange(ctx, translSlugs, model, isSelected, true)
        }
    }

    abstract class LoadTranslsTask(
        private val fileUtils: FileUtils,
        private val translSlugs: Set<String>
    ) : BaseCallableTask<List<TranslBaseModel>>() {

        private var translFactory: QuranTranslationFactory? = null

        @CallSuper
        override fun preExecute() {
            translFactory = QuranTranslationFactory(fileUtils.context)
        }

        @CallSuper
        override fun postExecute() {
            translFactory?.close()
        }

        override fun call(): List<TranslBaseModel> {
            return getTranslationsFromDatabase()
        }

        private fun getTranslationsFromDatabase(): List<TranslBaseModel> {
            val translItems = mutableListOf<TranslBaseModel>()

            val languageAndInfo = mutableMapOf<String, MutableList<TranslationBookInfoModel>>()

            translFactory!!
                .getAvailableTranslationBooksInfo()
                .values
                .forEach { bookInfo ->

                    val listOfLang = languageAndInfo.getOrPut(bookInfo.langCode) {
                        mutableListOf()
                    }

                    listOfLang.add(bookInfo)
                }

            languageAndInfo.forEach { (langCode, listOfBooks) ->

                val translTitleModel = TranslTitleModel(langCode, null)
                translItems.add(translTitleModel)

                listOfBooks.forEach { book ->
                    val model = TranslModel(book)
                    model.isChecked = translSlugs.contains(book.slug)
                    translTitleModel.langName = book.langName
                    translItems.add(model)
                }
            }

            return translItems
        }

    }
}
