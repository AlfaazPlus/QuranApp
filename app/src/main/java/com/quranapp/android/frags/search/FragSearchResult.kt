package com.quranapp.android.frags.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySearch
import com.quranapp.android.adapters.search.ADPVerseResults
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.components.search.SearchResultModelBase
import com.quranapp.android.components.search.VerseResultCountModel
import com.quranapp.android.components.search.VerseResultModel
import com.quranapp.android.databinding.FragSearchResultsBinding
import com.quranapp.android.db.transl.QuranTranslContract.QuranTranslEntry.*
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.interfaceUtils.Destroyable
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.search.SearchFilters
import com.quranapp.android.utils.thread.runner.CallableTaskRunner
import com.quranapp.android.utils.thread.tasks.BaseCallableTask
import com.quranapp.android.utils.univ.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class FragSearchResult : BaseFragment(), Destroyable {
    private val mTaskRunner = CallableTaskRunner<ArrayList<SearchResultModelBase?>>()
    private lateinit var mBinding: FragSearchResultsBinding

    private var firstTime = true
    private var mVerseResultsAdapter: ADPVerseResults? = null

    @JvmField
    var mIsLoadingInProgress = false
    override fun destroy() {
        if (mVerseResultsAdapter != null) {
            mVerseResultsAdapter!!.destroy()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (firstTime || !::mBinding.isInitialized) {
            mBinding = FragSearchResultsBinding.inflate(inflater, container, false)
        }
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!firstTime) {
            return
        }
        firstTime = false
        init(view.context)
    }

    private fun init(context: Context) {
        initRecyclerView(context)
    }

    private fun initTransls(actvt: ActivitySearch) {
        val selectedTranslSlug = actvt.mSearchFilters.selectedTranslSlug

        for (bookInfo in actvt.availableTranslModels.values) {
            if (selectedTranslSlug == bookInfo.slug) {
                actvt.mBinding.btnSelectTransl.text = bookInfo.bookName
                break
            }
        }

        actvt.mBinding.btnQuickLinks.isChecked = actvt.mSearchFilters.showQuickLinks
    }

    private fun initRecyclerView(context: Context) {
        mVerseResultsAdapter = ADPVerseResults(context, this)

        mBinding.let {
            it.results.adapter = mVerseResultsAdapter
            it.results.itemAnimator = null
            it.results.layoutManager = LinearLayoutManager(context)
            it.results.addItemDecoration(GapedItemDecoration(Dimen.dp2px(context, 5f)))
        }
    }

    fun initSearch(activitySearch: ActivitySearch) {
        initTransls(activitySearch)
        searchAsync(activitySearch)
    }

    private fun searchAsync(activitySearch: ActivitySearch) {
        val quranMeta = activitySearch.mQuranMeta
        activitySearch.mBinding.filter.visibility = View.GONE
        activitySearch.mBinding.voiceSearch.visibility = View.GONE
        mIsLoadingInProgress = true
        searchAsyncAfterTranslReady(activitySearch, quranMeta)
    }

    private fun searchAsyncAfterTranslReady(activity: ActivitySearch, meta: QuranMeta) {
        mTaskRunner.cancel()

        val query = activity.mLocalHistoryManager.lastQuery ?: return

        mTaskRunner.callAsync(object : BaseCallableTask<ArrayList<SearchResultModelBase?>>() {
            override fun preExecute() {
                mVerseResultsAdapter!!.setActivitySearch(activity)

                mBinding.loader.visibility = View.VISIBLE
                mBinding.noResultsFound.visibility = View.GONE
                mBinding.results.scrollToPosition(0)
                mBinding.results.adapter = null
            }

            @Throws(Exception::class)
            override fun call(): ArrayList<SearchResultModelBase?> {
                var jumpers = ArrayList<SearchResultModelBase>()
                val results = ArrayList<SearchResultModelBase?>()

                if (activity.mSearchFilters.showQuickLinks) {
                    jumpers = activity.prepareJumper(activity.mQuranMeta, query)
                    results.addAll(jumpers)
                }

                val pattern = resolveQuerySearchPattern(activity.mSearchFilters, query)
                val resultCount = AtomicInteger(0)

                val selectedTranslSlug = activity.mSearchFilters.selectedTranslSlug
                if (!selectedTranslSlug.isNullOrEmpty()) {
                    searchInDB(activity, meta, results, selectedTranslSlug, query, 20, 0, resultCount, pattern)
                }

                val jumpersCount = jumpers.size
                if (results.size > 0) {
                    val resultCountModel = VerseResultCountModel(
                        if (!selectedTranslSlug.isNullOrEmpty()) activity.availableTranslModels[selectedTranslSlug] else null
                    )
                    resultCountModel.resultCount = resultCount.get()
                    results.add(jumpersCount, resultCountModel)
                }

                return results
            }

            override fun onComplete(results: ArrayList<SearchResultModelBase?>) {
                if (results.size == 0) {
                    mBinding.noResultsFound.visibility = View.VISIBLE
                    val bookInfo = activity.availableTranslModels[activity.mSearchFilters.selectedTranslSlug]

                    mBinding.noResultsFound.text = if (bookInfo != null) {
                        activity.getString(R.string.strMsgSearchNoResultsFoundIn, bookInfo.bookName)
                    } else {
                        activity.getString(R.string.strMsgSearchNoResultsFoundAbsolute)
                    }
                    return
                }
                populateResults(results)
            }

            override fun postExecute() {
                mIsLoadingInProgress = false
                mBinding.loader.visibility = View.GONE
                if (isVisible) {
                    activity.mBinding.filter.visibility = View.VISIBLE
                    activity.mBinding.voiceSearch.visibility =
                        if (activity.mSupportsVoiceInput) View.VISIBLE else View.GONE
                }
            }
        })
    }

    fun searchInDB(
        actvt: ActivitySearch,
        meta: QuranMeta,
        results: ArrayList<SearchResultModelBase?>,
        slug: String,
        query: String,
        limit: Int,
        offset: Int,
        resultCount: AtomicInteger,
        pattern: Pattern
    ) {
        val bookInfo = actvt.mTranslFactory.getTranslationBookInfo(slug)
        val db = actvt.mTranslFactory.dbHelper.readableDatabase
        val rawQuery = actvt.mTranslFactory.prepareQuerySingle(
            actvt.mSearchFilters,
            query,
            slug,
            limit,
            offset
        )
        val cursor = db.rawQuery(rawQuery, null, null)
        Log.d(rawQuery, Arrays.toString(cursor.columnNames), cursor.count)

        while (cursor.moveToNext()) {
            val translations = ArrayList<Translation>()
            val nTranslSlugs = TreeSet<String>()
            val nTranslDisplayNames = ArrayList<String>()
            val wordStartIndices = ArrayList<Int>()
            val wordEndIndices = ArrayList<Int>()

            val translation = Translation()
            translation.chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CHAPTER_NO))
            translation.verseNo = cursor.getInt(cursor.getColumnIndexOrThrow(COL_VERSE_NO))
            translation.text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT))
            translation.bookSlug = slug

            Log.d(translation.text)

            // find query indices in the translation text
            if (translation.text.isNotEmpty()) {
                translation.text = StringUtils.removeHTML(translation.text, false)

                if ("en" == bookInfo.langCode) {
                    translation.text = org.apache.commons.lang3.StringUtils.stripAccents(
                        translation.text
                    )
                }

                val matcher = pattern.matcher(translation.text)
                if (matcher.find()) {
                    translations.add(translation)
                    nTranslSlugs.add(translation.bookSlug)
                    nTranslDisplayNames.add(bookInfo.getDisplayNameWithHyphen())
                    wordStartIndices.add(matcher.start(1))
                    wordEndIndices.add(matcher.end(1))
                    resultCount.getAndIncrement()
                }
            }

            if (translations.isNotEmpty() && wordStartIndices.isNotEmpty()) {
                val verseResultModel = prepareVerseResult(
                    actvt, meta, translation.chapterNo, translation.verseNo, nTranslSlugs,
                    nTranslDisplayNames, translations, wordStartIndices, wordEndIndices
                )
                results.add(verseResultModel)
            }
        }
        cursor.close()
    }

    fun populateResults(results: ArrayList<SearchResultModelBase?>?) {
        mVerseResultsAdapter?.setResults(results)
        mBinding.results.adapter = mVerseResultsAdapter
    }

    private fun resolveQuerySearchPattern(filters: SearchFilters, query: String): Pattern {
        val nQuery = Regex.escape(query)
        val wordPattern = if (filters.searchWordPart) {
            "($nQuery)"
        } else {
            "\\b($nQuery)\\b"
        }

        return Pattern.compile(wordPattern, Pattern.CASE_INSENSITIVE)
    }

    private fun prepareVerseResult(
        activity: ActivitySearch,
        quranMeta: QuranMeta,
        chapterNo: Int,
        verseNo: Int,
        translSlugs: Set<String>,
        translDisplayNames: List<String>,
        translations: List<Translation>,
        startIndices: List<Int>,
        endIndices: List<Int>
    ): VerseResultModel {
        return VerseResultModel().apply {
            this.chapterNo = chapterNo
            this.verseNo = verseNo
            this.chapterName = quranMeta.getChapterName(activity, chapterNo, true)
            this.chapterNameSansPrefix = quranMeta.getChapterName(activity, chapterNo, false)
            this.verseSerial = "$chapterNo:$verseNo"
            this.translSlugs = translSlugs
            this.translDisplayNames = translDisplayNames
            this.translations = translations
            this.startIndices = startIndices
            this.endIndices = endIndices
        }
    }
}
