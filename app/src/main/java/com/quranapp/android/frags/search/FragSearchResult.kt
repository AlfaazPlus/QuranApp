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
import com.quranapp.android.components.quran.Quran
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.components.search.SearchResultModelBase
import com.quranapp.android.components.search.VerseResultCountModel
import com.quranapp.android.components.search.VerseResultModel
import com.quranapp.android.databinding.FragSearchResultsBinding
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.*
import com.quranapp.android.frags.BaseFragment
import com.quranapp.android.interfaceUtils.Destroyable
import com.quranapp.android.interfaceUtils.OnResultReadyCallback
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.extended.GapedItemDecoration
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.search.ArabicSearchManager
import com.quranapp.android.utils.search.SearchFilters
import com.quranapp.android.utils.thread.runner.CallableTaskRunner
import com.quranapp.android.utils.thread.tasks.BaseCallableTask
import com.quranapp.android.utils.univ.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.math.ceil

class FragSearchResult : BaseFragment(), Destroyable {
    private val mTaskRunner = CallableTaskRunner<ArrayList<SearchResultModelBase?>>()
    private lateinit var mBinding: FragSearchResultsBinding

    private var firstTime = true
    private var mVerseResultsAdapter: ADPVerseResults? = null
    
    private var mFullResults = ArrayList<SearchResultModelBase?>()
    private var mCurrentLimit = 10

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
        mCurrentLimit = 10

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
                val results = ArrayList<SearchResultModelBase?>()
                var jumpers = ArrayList<SearchResultModelBase>()

                if (activity.mSearchFilters.showQuickLinks) {
                    jumpers = activity.prepareJumper(activity.mQuranMeta, query)
                    results.addAll(jumpers)
                }

                val resultCount = AtomicInteger(0)

                val isArabicQuery = ArabicSearchManager.isArabic(query)
                if (isArabicQuery) {
                    searchInArabic(activity, meta, results, query, resultCount)
                } else {
                    val pattern = resolveQuerySearchPattern(activity.mSearchFilters, query)
                    val selectedTranslSlug = activity.mSearchFilters.selectedTranslSlug
                    if (!selectedTranslSlug.isNullOrEmpty()) {
                        searchInDB(activity, meta, results, selectedTranslSlug, query, 500, 0, resultCount, pattern)
                    }
                }

                val jumpersCount = jumpers.size
                if (results.size > 0) {
                    val resultCountModel = VerseResultCountModel(
                        if (!isArabicQuery && !activity.mSearchFilters.selectedTranslSlug.isNullOrEmpty()) {
                            activity.availableTranslModels[activity.mSearchFilters.selectedTranslSlug]
                        } else null
                    )
                    resultCountModel.resultCount = resultCount.get()
                    results.add(jumpersCount, resultCountModel)
                }

                return results
            }

            override fun onComplete(results: ArrayList<SearchResultModelBase?>) {
                mFullResults = results
                if (results.size == 0) {
                    mBinding.noResultsFound.visibility = View.VISIBLE
                    val isAr = ArabicSearchManager.isArabic(activity.mLocalHistoryManager.lastQuery ?: "")
                    val bookInfo = if (!isAr) {
                        activity.availableTranslModels[activity.mSearchFilters.selectedTranslSlug]
                    } else null

                    mBinding.noResultsFound.text = if (bookInfo != null) {
                        activity.getString(R.string.strMsgSearchNoResultsFoundIn, bookInfo.bookName)
                    } else if (isAr) {
                        activity.getString(R.string.strMsgSearchNoResultsFoundIn, activity.getString(R.string.labelArabic))
                    } else {
                        activity.getString(R.string.strMsgSearchNoResultsFoundAbsolute)
                    }
                    return
                }
                showPaginatedResults()
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

    private fun showPaginatedResults() {
        val displayResults = ArrayList<SearchResultModelBase?>()
        
        // Find position of VerseResultCountModel
        var resultCountIdx = -1
        for (i in mFullResults.indices) {
            if (mFullResults[i] is VerseResultCountModel) {
                resultCountIdx = i
                break
            }
        }

        if (resultCountIdx != -1) {
            // Add everything up to and including VerseResultCountModel
            for (i in 0..resultCountIdx) {
                displayResults.add(mFullResults[i])
            }
            
            // Add limited results
            val remainingResults = mFullResults.size - (resultCountIdx + 1)
            val limit = minOf(mCurrentLimit, remainingResults)
            
            for (i in (resultCountIdx + 1) until (resultCountIdx + 1 + limit)) {
                displayResults.add(mFullResults[i])
            }
            
            // Add "Load More" button if there are more results
            if (remainingResults > mCurrentLimit) {
                displayResults.add(LoadMoreModel())
            }
        } else {
            displayResults.addAll(mFullResults)
        }

        populateResults(displayResults)
    }

    fun loadMore() {
        mCurrentLimit += 20
        showPaginatedResults()
    }

    private fun searchInArabic(
        activity: ActivitySearch,
        meta: QuranMeta,
        results: ArrayList<SearchResultModelBase?>,
        query: String,
        resultCount: AtomicInteger
    ) {
        val arabicResults = ArabicSearchManager.search(activity, query, activity.mSearchFilters.searchWordPart)
        if (arabicResults.isEmpty()) return

        val quranRef = java.util.concurrent.atomic.AtomicReference<Quran>()
        val latch = java.util.concurrent.CountDownLatch(1)
        Quran.prepareInstance(activity, QuranScriptUtils.SCRIPT_UTHMANI, meta, object : OnResultReadyCallback<Quran> {
            override fun onReady(r: Quran) {
                quranRef.set(r)
                latch.countDown()
            }
        })
        latch.await()

        val quran = quranRef.get() ?: return
        val normalizedQuery = ArabicSearchManager.cleanAndNormalize(query)
        val queryWordsCount = normalizedQuery.split(" ").filter { it.isNotEmpty() }.size

        for (res in arabicResults) {
            val verse = quran.getVerse(res.chapterNo, res.verseNo) ?: continue
            val allRanges = ArabicSearchManager.findHighlightRanges(verse.arabicText, query, activity.mSearchFilters.searchWordPart)
            
            if (allRanges.isEmpty()) continue

            var filteredRanges = allRanges
            if (queryWordsCount >= 3) {
                filteredRanges = allRanges.filter { r ->
                    val matchText = verse.arabicText.substring(r.first, r.second)
                    val cleanedMatch = ArabicSearchManager.cleanAndNormalize(matchText)
                    val wordsInMatchCount = cleanedMatch.split(" ").filter { it.isNotEmpty() }.size
                    wordsInMatchCount >= 2 || cleanedMatch.length >= 4
                }
                if (filteredRanges.isEmpty()) continue
                if (queryWordsCount >= 4) {
                    val totalWordsInSignificantMatches = filteredRanges.sumOf { r ->
                        val cleanedMatch = ArabicSearchManager.cleanAndNormalize(verse.arabicText.substring(r.first, r.second))
                        cleanedMatch.split(" ").filter { it.isNotEmpty() }.size
                    }
                    if (totalWordsInSignificantMatches < ceil(queryWordsCount / 2.0)) continue
                }
            } else if (queryWordsCount == 2) {
                if (allRanges.size == 1) {
                    val cleanedMatch = ArabicSearchManager.cleanAndNormalize(verse.arabicText.substring(allRanges[0].first, allRanges[0].second))
                    if (cleanedMatch.length <= 3) continue
                }
            }

            val chapterName = meta.getChapterName(activity, res.chapterNo, false)
            val verseResultModel = VerseResultModel().apply {
                this.chapterNo = res.chapterNo
                this.verseNo = res.verseNo
                this.chapterName = meta.getChapterName(activity, res.chapterNo, true)
                this.chapterNameSansPrefix = chapterName
                this.verseSerial = "$chapterName ${res.chapterNo}:${res.verseNo}"
                this.arabicText = verse.arabicText
                this.arabicStartIndices = filteredRanges.map { it.first }
                this.arabicEndIndices = filteredRanges.map { it.second }
            }
            results.add(verseResultModel)
            resultCount.incrementAndGet()
        }
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

            if (translation.text.isNotEmpty()) {
                translation.text = StringUtils.removeHTML(translation.text, false)
                if ("en" == bookInfo.langCode) {
                    translation.text = org.apache.commons.lang3.StringUtils.stripAccents(translation.text)
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
        mVerseResultsAdapter?.setResults(results as ArrayList<SearchResultModelBase>)
        mBinding.results.adapter = mVerseResultsAdapter
    }

    private fun resolveQuerySearchPattern(filters: SearchFilters, query: String): Pattern {
        val nQuery = Regex.escape(query)
        val wordPattern = if (filters.searchWordPart) "($nQuery)" else "\\b($nQuery)\\b"
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
        val chapterName = quranMeta.getChapterName(activity, chapterNo, false)
        return VerseResultModel().apply {
            this.chapterNo = chapterNo
            this.verseNo = verseNo
            this.chapterName = quranMeta.getChapterName(activity, chapterNo, true)
            this.chapterNameSansPrefix = chapterName
            this.verseSerial = "$chapterName $chapterNo:$verseNo"
            this.translSlugs = translSlugs
            this.translDisplayNames = translDisplayNames
            this.translations = translations
            this.startIndices = startIndices
            this.endIndices = endIndices
        }
    }

    class LoadMoreModel : SearchResultModelBase()
}
