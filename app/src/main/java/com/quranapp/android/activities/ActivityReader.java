package com.quranapp.android.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static com.quranapp.android.components.quran.QuranMeta.canShowBismillah;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_VERSES;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_PAGE;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_TRANSLATION;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.BISMILLAH;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_TITLE;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.IS_VOTD;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_PAGE;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.VERSE;
import static com.quranapp.android.utils.IntentUtils.INTENT_ACTION_OPEN_READER;
import static com.quranapp.android.utils.quran.QuranUtils.doesVerseRangeEqualWhole;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_CHAPTER_NO;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_JUZ_NO;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_PENDING_SCROLL;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_READER_STYLE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_READ_TYPE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_VERSES;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

import com.quranapp.android.R;
import com.quranapp.android.adapters.ADPQuranPages;
import com.quranapp.android.adapters.ADPReader;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.components.reader.ChapterVersePair;
import com.quranapp.android.components.reader.QuranPageModel;
import com.quranapp.android.components.reader.QuranPageSectionModel;
import com.quranapp.android.components.reader.ReaderRecyclerItemModel;
import com.quranapp.android.databinding.ActivityReaderBinding;
import com.quranapp.android.db.readHistory.ReadHistoryDBHelper;
import com.quranapp.android.reader_managers.Navigator;
import com.quranapp.android.reader_managers.ReaderParams;
import com.quranapp.android.suppliments.ReaderLayoutManager;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.reader.recitation.player.RecitationPlayerParams;
import com.quranapp.android.utils.services.RecitationService;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.Codes;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.utils.verse.VerseUtils;
import com.quranapp.android.views.reader.VerseView;
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem;
import com.quranapp.android.views.readerSpinner2.adapters.VerseSelectorAdapter2;
import com.quranapp.android.views.recitation.RecitationPlayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import kotlin.Pair;

public class ActivityReader extends ReaderPossessingActivity {
    public static final String KEY_RECITER_CHANGED = "reciter.changed";
    public static final String KEY_TRANSLATION_RECITER_CHANGED = "translation_reciter.changed";
    public static final String KEY_SCRIPT_CHANGED = "script.changed";
    public static final String KEY_TAFSIR_CHANGED = "tafsir.changed";

    public final CallableTaskRunner<ArrayList<QuranPageModel>> mPagesTaskRunner = new CallableTaskRunner<>();
    public ReaderParams mReaderParams;
    public Navigator mNavigator;
    public RecitationPlayer mPlayer;
    public boolean persistProgressDialog4PendingTask;
    public ActivityReaderBinding mBinding;
    public ReaderLayoutManager mLayoutManager;
    private boolean mProtectFromPlayerReset;
    public RecitationService mPlayerService;
    private final ServiceConnection mPlayerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof RecitationService.LocalBinder) {
                mPlayerService = ((RecitationService.LocalBinder) service).getService();

                if (mPlayer != null) {
                    mPlayer.setService(mPlayerService);
                }

                mPlayerService.setRecitationPlayer(mPlayer, ActivityReader.this);

                if (!mPlayerService.isPlaying()) {
                    Chapter currChapter = mReaderParams.currChapter;
                    int currJuzNo = mReaderParams.currJuzNo;
                    QuranMeta quranMeta = mQuranMetaRef.get();

                    if (mReaderParams.readType == READER_READ_TYPE_JUZ && currJuzNo > 0 && quranMeta != null) {
                        mPlayerService.onJuzChanged(currJuzNo, quranMeta);
                    } else if (currChapter != null) {
                        final int fromVerse;
                        final int toVerse;
                        Pair<Integer, Integer> verseRange = mReaderParams.verseRange;

                        if (QuranUtils.doesRangeDenoteSingle(verseRange)) {
                            fromVerse = 1;
                            toVerse = currChapter.getVerseCount();
                        } else {
                            fromVerse = verseRange.getFirst();
                            toVerse = verseRange.getSecond();
                        }

                        mPlayerService.onChapterChanged(
                            currChapter.getChapterNumber(),
                            fromVerse,
                            toVerse,
                            mPlayerService.getP().getCurrentVerseNo()
                        );
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerService.setRecitationPlayer(null, null);
            mPlayerService = null;
        }
    };
    private ReadHistoryDBHelper mReadHistoryDBHelper;


    @Override
    protected int getStatusBarBG() {
        return color(R.color.colorBGReaderHeader);
    }

    @Override
    protected int getThemeId() {
        return R.style.Theme_QuranApp_Reader;
    }

    @Override
    protected void onPause() {
        saveReaderState();
        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(null, this);
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        bindPlayerService();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPlayer != null) {
            new Handler().postDelayed(() -> mPlayer.reveal(), 500);
        }

        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(mPlayer, this);
        }
    }

    @Override
    protected void onDestroy() {
        unbindPlayerService();
        mBinding.readerHeader.destroy();
        if (mPlayerService != null) {
            mPlayerService.destroy();
        }

        if (mReadHistoryDBHelper != null) {
            mReadHistoryDBHelper.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("preventRecitationPlayerReset", mPlayerService.isPlaying());

        if (mLayoutManager != null) {
            outState.putParcelable("recyclerView", mLayoutManager.onSaveInstanceState());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (mLayoutManager != null) {
            mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable("recyclerView"));
        }
    }

    public void bindPlayerService() {
        bindService(new Intent(this, RecitationService.class), mPlayerServiceConnection,
            Context.BIND_AUTO_CREATE);
    }

    public void unbindPlayerService() {
        if (mPlayerService == null) {
            return;
        }

        try {
            unbindService(mPlayerServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!intent.getBooleanExtra(Keys.KEY_ACTIVITY_RESUMED_FROM_NOTIFICATION, false)) {
            initQuran(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (isTaskRoot()) {
            launchMainActivity();
            finish();

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_reader;
    }

    @Override
    public void adjustStatusAndNavigationBar() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        int uiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiVisibility);

        window.getAttributes().flags &= ~(FLAG_TRANSLUCENT_STATUS | FLAG_TRANSLUCENT_NAVIGATION);

        int clr = Color.TRANSPARENT;
        window.setStatusBarColor(clr);
        window.setNavigationBarColor(clr);

        boolean isLight = isStatusBarLight();
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
        wic.setAppearanceLightNavigationBars(isLight);
        wic.setAppearanceLightStatusBars(isLight);
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        super.preActivityInflate(savedInstanceState);
        if (savedInstanceState != null) {
            mProtectFromPlayerReset = savedInstanceState.getBoolean("preventRecitationPlayerReset", false);
        }

        mReaderParams = new ReaderParams(this);
    }

    @Override
    protected void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityReaderBinding.bind(activityView);
        mNavigator = new Navigator(this);
        initDummyBars();
    }


    @Override
    protected void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        // TEST
        //        intent.putExtras(ReaderFactory.prepareChapterIntent(105));
        //        intent.putExtras(ReaderFactory.prepareSingleVerseIntent(105, 2));
        //        intent.putExtras(ReaderFactory.prepareVerseRangeIntent(2, 3, 21));
        //        intent.putExtras(ReaderFactory.prepareJuzIntent(30));
        // TEST END

        mBinding.getRoot().post(this::init);
    }

    private void init() {
        mBinding.readerHeader.setActivity(this);
        initReadHistory();
        initFloatingFooter();

        final Intent intent = getIntent();
        final String[] requestedTranslSlugs = intent.getStringArrayExtra(READER_KEY_TRANSL_SLUGS);
        if (requestedTranslSlugs == null) {
            mReaderParams.setVisibleTranslSlugs(SPReader.getSavedTranslations(this));
        } else {
            mReaderParams.setVisibleTranslSlugs(new TreeSet<>(Arrays.asList(requestedTranslSlugs)));
        }

        if (!mReaderParams.isPageReaderStyle() && (mReaderParams.getVisibleTranslSlugs() == null || mReaderParams.getVisibleTranslSlugs().isEmpty())) {
            Toast.makeText(this, R.string.strMsgTranslNoneSelected, Toast.LENGTH_SHORT).show();
        }

        mReaderParams.saveTranslChanges = intent.getBooleanExtra(READER_KEY_SAVE_TRANSL_CHANGES, true);
        mReaderParams.setReaderStyle(this,
            intent.getIntExtra(READER_KEY_READER_STYLE, mReaderParams.defaultStyle(this)));

        prepareReader(getIntent());
    }

    private void prepareReader(Intent intent) {
        initReader();
        initQuran(intent);
    }

    private void validateIntent(Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri url = intent.getData();
            if (url == null) return;

            if (url.getHost().equalsIgnoreCase("quran.com")) {
                validateQuranComIntent(intent, url);
            }
        } else if (INTENT_ACTION_OPEN_READER.equalsIgnoreCase(intent.getAction())) {
            validateQuranAppIntent(intent);
        }

        intent.setAction(null);
    }

    private void validateQuranComIntent(Intent intent, Uri url) {
        List<String> pathSegments = url.getPathSegments();
        if (pathSegments.size() >= 2) {
            String firstSeg = pathSegments.get(0);
            String secondSeg = pathSegments.get(1);

            if (firstSeg.equalsIgnoreCase("juz")) {
                int juzNo = Integer.parseInt(secondSeg);
                intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo));
            } else {
                int chapterNo = Integer.parseInt(firstSeg);

                final Pair<Integer, Integer> verseRange;
                final String[] splits = secondSeg.split("-");
                if (splits.length >= 2) {
                    verseRange = new Pair<>(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]));
                } else {
                    int verseNo = Integer.parseInt(splits[0]);
                    verseRange = new Pair<>(verseNo, verseNo);
                }

                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange));
            }
        } else if (pathSegments.size() >= 1) {
            String[] splits = pathSegments.get(0).split(":");
            int chapterNo = Integer.parseInt(splits[0]);
            if (splits.length >= 2) {
                splits = splits[1].split("-");
                final Pair<Integer, Integer> verseRange;
                if (splits.length >= 2) {
                    verseRange = new Pair<>(Integer.parseInt(splits[0]), Integer.parseInt(splits[1]));
                } else {
                    int verseNo = Integer.parseInt(splits[0]);
                    verseRange = new Pair<>(verseNo, verseNo);
                }
                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verseRange));
            } else {
                intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo));
            }
        }

        Set<String> parameters = url.getQueryParameterNames();
        if (parameters.contains("reading")) {
            boolean reading = url.getBooleanQueryParameter("reading", false);
            mReaderParams.setReaderStyle(this, reading ? READER_STYLE_PAGE : READER_STYLE_TRANSLATION);
        }
    }

    private void validateQuranAppIntent(Intent intent) {
        final String[] requestedTranslSlugs = intent.getStringArrayExtra("translations");
        if (requestedTranslSlugs != null) {
            mReaderParams.setVisibleTranslSlugs(new TreeSet<>(Arrays.asList(requestedTranslSlugs)));
        }

        if (intent.getBooleanExtra("isJuz", false)) {
            final int juzNo = intent.getIntExtra("juzNo", -1);
            intent.putExtras(ReaderFactory.prepareJuzIntent(juzNo));
        } else {
            final int chapterNo = intent.getIntExtra("chapterNo", -1);
            int[] verses = intent.getIntArrayExtra("verses");
            int verseNo = intent.getIntExtra("verseNo", -1);
            if (verses != null) {
                intent.putExtras(ReaderFactory.prepareVerseRangeIntent(chapterNo, verses[0], verses[1]));
            } else if (verseNo != -1) {
                intent.putExtras(ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo));
            } else {
                intent.putExtras(ReaderFactory.prepareChapterIntent(chapterNo));
            }
        }
    }

    private void initReader() {
        mLayoutManager = new ReaderLayoutManager(this, RecyclerView.VERTICAL, false);
        mBinding.readerVerses.setItemAnimator(null);
    }

    private void resetAdapter(RecyclerView.Adapter<?> adapter) {
        mBinding.readerVerses.setAdapter(adapter);
        mBinding.readerVerses.setLayoutManager(mLayoutManager);
        mBinding.readerVerses.post(this::pendingScrollIfAny);

        saveToIntent();
    }

    private void initDummyBars() {
        adjustStatusAndNavigationBar();

        final View navDummy = mBinding.navigationBarDummy;
        final View statusBarDummy = mBinding.readerHeader.getBinding().statusBarDummy;

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            final int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            statusBarDummy.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, statusBarHeight));

            final int navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            navDummy.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, navHeight));

            return WindowInsetsCompat.CONSUMED;
        });

        final int color = color(R.color.colorBGReaderHeader);
        navDummy.setBackgroundColor(color);
        statusBarDummy.setBackgroundColor(color);
    }

    private void initReadHistory() {
        mReadHistoryDBHelper = new ReadHistoryDBHelper(this);
    }

    private void initFloatingFooter() {
        if (!RecitationUtils.isRecitationSupported()) {
            return;
        }

        mPlayer = new RecitationPlayer(this, mPlayerService);

        if (mPlayerService != null) {
            mPlayerService.setRecitationPlayer(mPlayer, this);
        }

        mBinding.floatingFooter.addView(mPlayer, 1);
    }

    private void initQuran(Intent intent) {
        try {
            validateIntent(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        QuranMeta quranMeta = mQuranMetaRef.get();

        Quran quran = mQuranRef.get();
        mReaderParams.readType = intent.getIntExtra(READER_KEY_READ_TYPE, mReaderParams.defaultReadType());
        mReaderParams.readerScript = SPReader.getSavedScript(this);
        mReaderParams.resetTextSizesStates();

        int initJuzNo = intent.getIntExtra(READER_KEY_JUZ_NO, 1);
        int initChapterNo = intent.getIntExtra(READER_KEY_CHAPTER_NO, 1);
        Pair<Integer, Integer> initVerses = resolveIntentVerseRange(intent);

        int[] pendingScroll = intent.getIntArrayExtra(READER_KEY_PENDING_SCROLL);
        if (pendingScroll != null) {
            mNavigator.pendingScrollVerse = pendingScroll;
        }

        if (!QuranMeta.isChapterValid(initChapterNo)) {
            makeMessage(str(R.string.strMsgInvalidChapterNo, initChapterNo));

            mReaderParams.readType = mReaderParams.defaultReadType();
            initChapterNo = 1;
            initVerses = null;
        }

        if (mReaderParams.readType == READER_READ_TYPE_VERSES) {
            boolean anyError = false;
            if (initVerses == null) {
                makeMessage(str(R.string.strMsgInvalidVersesRange));
                anyError = true;
            } else if (QuranUtils.doesRangeDenoteSingle(initVerses) && !quranMeta.isVerseValid4Chapter(initChapterNo,
                initVerses.getFirst())) {
                makeMessage(str(R.string.strMsgInvalidVerseNo, initVerses.getFirst(), initChapterNo));
                anyError = true;
            } else {
                initVerses = QuranUtils.swapVerseRangeIfNeeded(initVerses);

                if (!quranMeta.isVerseRangeValid4Chapter(initChapterNo, initVerses)) {
                    String msg = str(
                        R.string.strMsgInvalidVersesRange2,
                        initVerses.getFirst(),
                        initVerses.getSecond(),
                        initChapterNo
                    );
                    makeMessage(msg);
                    initVerses = QuranUtils.correctVerseInRange(mQuranMetaRef.get(), initChapterNo, initVerses);
                }
            }


            if (anyError) {
                mReaderParams.readType = mReaderParams.defaultReadType();
                initVerses = null;
            }
        } else if (mReaderParams.readType == READER_READ_TYPE_JUZ && !QuranMeta.isJuzValid(initJuzNo)) {
            makeMessage(str(R.string.strMsgInvalidJuzNo, initJuzNo));
            initJuzNo = 1;
        }

        Chapter initialChapter = quran.getChapter(initChapterNo);

        if (initVerses == null) {
            initVerses = new Pair<>(1, initialChapter.getVerseCount());
        }

        switch (mReaderParams.readType) {
            case READER_READ_TYPE_VERSES: initVerseRange(initialChapter, initVerses);
                break;
            case READER_READ_TYPE_JUZ: initJuz(initJuzNo);
                break;
            case READER_READ_TYPE_CHAPTER:
            default: initChapter(initialChapter);
                break;
        }
    }

    private Pair<Integer, Integer> resolveIntentVerseRange(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(READER_KEY_VERSES);

        // The verse range could be passed as a pair or a two items list (as from ShortcutUtils).

        if (serializable instanceof Pair) {
            return (Pair<Integer, Integer>) serializable;
        } else if (serializable instanceof int[]) {
            int[] verses = (int[]) serializable;
            return new Pair<>(verses[0], verses[1]);
        }

        return null;
    }

    private void makeMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void initChapter(Chapter chapter) {
        mReaderParams.readType = READER_READ_TYPE_CHAPTER;
        mReaderParams.setCurrChapter(chapter);
        mReaderParams.currJuzNo = -1;

        mReaderParams.verseRange = new Pair<>(1, chapter.getVerseCount());
        mBinding.readerHeader.initChapterSelector();
        mBinding.readerHeader.selectChapterIntoSpinner(mNavigator.getCurrChapterNo());
        mBinding.readerHeader.initVerseSelector(null, chapter.getChapterNumber());
        mBinding.readerHeader.setupHeaderForReadType();
        updateVerseNumber(chapter.getChapterNumber(), 1);

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset) {
                mPlayer.onChapterChanged(
                    chapter.getChapterNumber(),
                    1,
                    chapter.getVerseCount(),
                    1,
                    false
                );
            } else {
                mPlayer.reveal();
            }
        }

        mProtectFromPlayerReset = false;

        if (mReaderParams.isPageReaderStyle()) {
            initChapterReading(chapter);
        } else {
            initChapterTranslation(chapter);
        }
    }

    private void initChapterReading(Chapter chapter) {
        mReaderParams.setReaderStyle(this, READER_STYLE_PAGE);

        makePages(new Pair<>(chapter.getChapterNumber(), chapter.getChapterNumber()), chapter.getPageRange());
    }

    private void initChapterTranslation(Chapter chapter) {
        mReaderParams.setReaderStyle(this, READER_STYLE_TRANSLATION);

        initTranslationVerses(chapter, 1, chapter.getVerseCount());
    }

    public void initVerseRange(Chapter chapter, Pair<Integer, Integer> verseRange) {
        if (doesVerseRangeEqualWhole(mQuranMetaRef.get(), chapter.getChapterNumber(), verseRange.getFirst(),
            verseRange.getSecond())) {
            initChapter(chapter);
            return;
        }

        mReaderParams.readType = READER_READ_TYPE_VERSES;
        mReaderParams.setReaderStyle(this, READER_STYLE_TRANSLATION);
        mReaderParams.verseRange = verseRange;

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset && (!chapter.equals(mReaderParams.currChapter))) {
                final int fromVerse;
                final int toVerse;

                if (QuranUtils.doesRangeDenoteSingle(verseRange)) {
                    fromVerse = 1;
                    toVerse = chapter.getVerseCount();
                } else {
                    fromVerse = verseRange.getFirst();
                    toVerse = verseRange.getSecond();
                }

                mPlayer.onChapterChanged(
                    chapter.getChapterNumber(),
                    fromVerse,
                    toVerse,
                    verseRange.getFirst(),
                    false
                );
            } else {
                mPlayer.reveal();
            }
        }

        mProtectFromPlayerReset = false;

        if (!chapter.equals(mReaderParams.currChapter)) {
            mReaderParams.setCurrChapter(chapter);
            mBinding.readerHeader.initVerseSelector(null, chapter.getChapterNumber());
        }

        mBinding.readerHeader.initChapterSelector();
        mBinding.readerHeader.selectChapterIntoSpinner(mNavigator.getCurrChapterNo());
        mBinding.readerHeader.setupHeaderForReadType();
        updateVerseNumber(chapter.getChapterNumber(), verseRange.getFirst());

        initTranslationVerses(chapter, verseRange.getFirst(), verseRange.getSecond());
    }

    private void initTranslationVerses(Chapter chapter, int fromVerse, int toVerse) {
        mNavigator.setupNavigator();
        if (!mReaderParams.isSingleVerse()) {
            mActionController.showLoader();
        }

        new Thread(() -> {
            mVerseDecorator.refreshQuranTextFonts(
                mVerseDecorator.isKFQPCScript()
                    ? new Pair<>(chapter.getVerse(fromVerse).pageNo, chapter.getVerse(toVerse).pageNo)
                    : null
            );

            initTranslationVersesFinalAsync(chapter, fromVerse, toVerse);
        }).start();
    }

    private void initTranslationVersesFinalAsync(Chapter chapter, int fromVerse, int toVerse) {
        Set<String> slugs = mReaderParams.getVisibleTranslSlugs();
        Map<String, QuranTranslBookInfo> booksInfo = mTranslFactory.getTranslationBooksInfoValidated(slugs);
        ArrayList<ReaderRecyclerItemModel> models = new ArrayList<>();

        final int chapterNo = chapter.getChapterNumber();

        if (mReaderParams.isSingleVerse() && chapter.getVerse(fromVerse).isVOTD(this)) {
            models.add(0, new ReaderRecyclerItemModel().setViewType(IS_VOTD));
        }

        if (slugs == null || slugs.isEmpty()) {
            models.add(new ReaderRecyclerItemModel().setViewType(NO_TRANSL_SELECTED));
        }

        if (chapter.canShowBismillah() && doesVerseRangeEqualWhole(mQuranMetaRef.get(), chapterNo, fromVerse,
            toVerse)) {
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
            model.setViewType(BISMILLAH);
            models.add(model);
        }

        List<List<Translation>> listOfTranslations = mTranslFactory.getTranslationsVerseRange(slugs, chapterNo,
            fromVerse,
            toVerse);

        for (int verseNo = fromVerse, pos = 0; verseNo <= toVerse; verseNo++, pos++) {
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
            final Verse verse = chapter.getVerse(verseNo);

            List<Translation> translations = listOfTranslations.get(pos);
            verse.setTranslations(translations);

            verse.arabicTextSpannable = prepareVerseText(verse);
            verse.translTextSpannable = prepareTranslSpannable(verse, translations, booksInfo);

            models.add(model.setViewType(VERSE).setVerse(verse));
        }

        runOnUiThread(() -> {
            QuranMeta.ChapterMeta chapterInfoMeta = null;
            if (mReaderParams.readType == READER_READ_TYPE_CHAPTER) {
                chapterInfoMeta = mQuranMetaRef.get().getChapterMeta(chapter.getChapterNumber());
            }
            resetAdapter(new ADPReader(this, chapterInfoMeta, models));
            mActionController.dismissLoader();
        });
    }

    public void initJuz(int juzNo) {
        mReaderParams.setCurrChapter(null);

        if (mPlayer != null) {
            if (!mProtectFromPlayerReset && mReaderParams.currJuzNo != juzNo) {
                mPlayer.onJuzChanged(juzNo, false);
            } else {
                mPlayer.reveal();
                if (mPlayerService != null && mPlayerService.isPlaying()) {
                    mNavigator.pendingScrollVerse = new int[]{
                        mPlayerService.getP().getCurrentChapterNo(),
                        mPlayerService.getP().getCurrentVerseNo()
                    };
                    mNavigator.pendingScrollVerseHighlight = false;
                }
            }
        }

        mProtectFromPlayerReset = false;

        mBinding.readerHeader.initJuzSelector();
        mBinding.readerHeader.selectJuzIntoSpinner(juzNo);
        mBinding.readerHeader.setupHeaderForReadType();
        mNavigator.setupNavigator();

        final QuranMeta quranMeta = mQuranMetaRef.get();
        Pair<Integer, Integer> chaptersInJuz = quranMeta.getChaptersInJuz(juzNo);

        if (mReaderParams.isPageReaderStyle()) {
            initJuzReading(juzNo, quranMeta);
        } else {
            initJuzTranslation(juzNo, chaptersInJuz, quranMeta);
        }

        makeVerseSpinnerJuzItems(juzNo, chaptersInJuz, quranMeta);
    }

    private void initJuzReading(int juzNo, QuranMeta quranMeta) {
        mReaderParams.setReaderStyle(this, READER_STYLE_PAGE);

        makePages(null, quranMeta.getJuzPageRange(juzNo));
    }

    private void initJuzTranslation(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        mActionController.showLoader();
        new Thread(() -> {
            mVerseDecorator.refreshQuranTextFonts(
                mVerseDecorator.isKFQPCScript() ? mQuranMetaRef.get().getJuzPageRange(juzNo) : null
            );
            initJuzTranslationAsync(juzNo, chaptersInJuz, quranMeta);
        }).start();
    }

    private void initJuzTranslationAsync(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        ArrayList<ReaderRecyclerItemModel> models = new ArrayList<>();

        if (mReaderParams.getVisibleTranslSlugs() == null || mReaderParams.getVisibleTranslSlugs().isEmpty()) {
            models.add(new ReaderRecyclerItemModel().setViewType(NO_TRANSL_SELECTED));
        }

        IntStream.rangeClosed(chaptersInJuz.getFirst(), chaptersInJuz.getSecond())
            .forEach(chapterNo -> {
                Pair<Integer, Integer> verses = quranMeta.getVerseRangeOfChapterInJuz(juzNo, chapterNo);
                int fromVerse = verses.getFirst();
                int toVerse = verses.getSecond();

                final boolean startOfChapter = mReaderParams.readType == READER_READ_TYPE_JUZ && mReaderParams.currJuzNo == juzNo && fromVerse == 1;

                if (startOfChapter) {
                    ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();
                    model.setViewType(CHAPTER_TITLE);
                    model.setChapterNo(chapterNo);
                    models.add(model);

                    if (canShowBismillah(chapterNo)) {
                        ReaderRecyclerItemModel bismillahModel = new ReaderRecyclerItemModel();
                        bismillahModel.setViewType(BISMILLAH);
                        models.add(bismillahModel);
                    }
                }

                makeJuzTranslationVerses(models, mQuranRef.get().getChapter(chapterNo), fromVerse, toVerse);
            });

        runOnUiThread(() -> {
            resetAdapter(new ADPReader(this, null, models));
            mActionController.dismissLoader();
        });
    }

    private void makeJuzTranslationVerses(
        ArrayList<ReaderRecyclerItemModel> models,
        Chapter chapter,
        int fromVerse,
        int toVerse
    ) {
        Set<String> slugs = mReaderParams.getVisibleTranslSlugs();
        Map<String, QuranTranslBookInfo> booksInfo = mTranslFactory.getTranslationBooksInfoValidated(slugs);

        List<List<Translation>> listOfTranslations = mTranslFactory.getTranslationsVerseRange(
            slugs,
            chapter.getChapterNumber(),
            fromVerse,
            toVerse
        );

        for (int verseNo = fromVerse, pos = 0; verseNo <= toVerse; verseNo++, pos++) {
            Verse verse = chapter.getVerse(verseNo);
            ReaderRecyclerItemModel model = new ReaderRecyclerItemModel();

            List<Translation> translations = listOfTranslations.get(pos);
            verse.setTranslations(translations);
            verse.arabicTextSpannable = prepareVerseText(verse);
            verse.translTextSpannable = prepareTranslSpannable(verse, translations, booksInfo);

            models.add(model.setViewType(VERSE).setVerse(verse));
        }
    }

    private void makeVerseSpinnerJuzItems(int juzNo, Pair<Integer, Integer> chaptersInJuz, QuranMeta quranMeta) {
        new Thread(() -> {
            List<VerseSpinnerItem> mVerseSpinnerItems = new ArrayList<>();
            String verseNoText = str(R.string.strLabelVerseWithChapNo);

            int firstChapterInJuz = chaptersInJuz.getFirst();
            final AtomicInteger firstVerseInJuz = new AtomicInteger(-1);

            IntStream.rangeClosed(firstChapterInJuz, chaptersInJuz.getSecond())
                .forEach(chapterNo -> {
                    Pair<Integer, Integer> verses = quranMeta.getVerseRangeOfChapterInJuz(juzNo, chapterNo);

                    if (firstVerseInJuz.get() == -1) {
                        firstVerseInJuz.set(verses.getFirst());
                    }

                    IntStream.rangeClosed(verses.getFirst(), verses.getSecond())
                        .forEach(verseNo -> makeVerseSpinnerItemJuz(
                            mVerseSpinnerItems,
                            chapterNo,
                            verseNo,
                            verseNoText
                        ));
                });

            runOnUiThread(() -> {
                VerseSelectorAdapter2 adapter = new VerseSelectorAdapter2(mVerseSpinnerItems);
                mBinding.readerHeader.initVerseSelector(adapter, -1);
                updateVerseNumber(firstChapterInJuz, firstVerseInJuz.get());
            });
        }).start();
    }

    private void makeVerseSpinnerItemJuz(List<VerseSpinnerItem> list, int chapterNo, int verseNo, String verseNoText) {
        VerseSpinnerItem item = new VerseSpinnerItem(chapterNo, verseNo);
        item.setLabel(String.format(verseNoText, chapterNo, verseNo));
        list.add(item);
    }

    private void makePages(
        @Nullable Pair<Integer, Integer> chapters,
        Pair<Integer, Integer> pages
    ) {
        final QuranMeta quranMeta = mQuranMetaRef.get();

        mPagesTaskRunner.cancel();

        mPagesTaskRunner.callAsync(new BaseCallableTask<ArrayList<QuranPageModel>>() {
            @Override
            public void preExecute() {
                mActionController.showLoader();
            }

            @Override
            public ArrayList<QuranPageModel> call() {
                mVerseDecorator.refreshQuranTextFonts(
                    mVerseDecorator.isKFQPCScript() ? pages : null
                );

                return makePagesAsync(chapters, pages, quranMeta);
            }

            @Override
            public void postExecute() {
                mActionController.dismissLoader();
            }

            @Override
            public void onComplete(ArrayList<QuranPageModel> models) {
                mBinding.readerVerses.setLayoutManager(
                    new LinearLayoutManager(ActivityReader.this, RecyclerView.VERTICAL, false));

                QuranMeta.ChapterMeta chapterInfoMeta = null;
                if (chapters != null) {
                    chapterInfoMeta = quranMeta.getChapterMeta(chapters.getFirst());
                }

                resetAdapter(new ADPQuranPages(ActivityReader.this, chapterInfoMeta, models));

                mNavigator.setupNavigator();
            }
        });
    }

    private ArrayList<QuranPageModel> makePagesAsync(
        @Nullable Pair<Integer, Integer> chapterRange,
        Pair<Integer, Integer> pageRange,
        QuranMeta quranMeta
    ) {
        final boolean isJuz = chapterRange == null;
        final Quran quran = mQuranRef.get();

        ArrayList<QuranPageModel> models = new ArrayList<>();

        for (int pageNo = pageRange.getFirst(), l = pageRange.getSecond(); pageNo <= l; pageNo++) {
            QuranPageModel pageModel = createPage(
                isJuz ? quranMeta.getChaptersOnPage(pageNo) : chapterRange,
                pageNo,
                quranMeta,
                quran
            );
            pageModel.setViewType(READER_PAGE);
            models.add(pageModel);
        }

        return models;
    }

    private QuranPageModel createPage(
        Pair<Integer, Integer> chapterRange,
        int pageNo,
        QuranMeta quranMeta,
        Quran quran
    ) {
        ArrayList<QuranPageSectionModel> sections = new ArrayList<>();

        StringBuilder chaptersName = new StringBuilder();
        int firstChapterOnPage = chapterRange.getFirst();

        for (int chapterNo = firstChapterOnPage, toChapterNo = chapterRange.getSecond(); chapterNo <= toChapterNo; chapterNo++) {
            QuranPageSectionModel section = new QuranPageSectionModel();
            ArrayList<Verse> verses = new ArrayList<>();

            final Pair<Integer, Integer> verseRange = quranMeta.getVerseRangeOfChapterOnPage(pageNo, chapterNo);
            final int firstVerse = verseRange.getFirst();

            if (firstVerse == 1) {
                section.setShowTitle(true);
                section.setShowBismillah(canShowBismillah(chapterNo));
            }

            int txtColor = color(R.color.colorText);
            SpannableStringBuilder verseContentSB = new SpannableStringBuilder();

            final int finalChapterNo = chapterNo;
            IntStream.rangeClosed(firstVerse, verseRange.getSecond())
                .forEach(verseNo -> {
                    Verse verse = quran.getVerse(finalChapterNo, verseNo);
                    verses.add(verse);

                    verseContentSB.append(" ").append(
                        mVerseDecorator.setupArabicTextQuranPage(
                            txtColor,
                            verse,
                            () -> mBinding.verseQuickActions.show(section, verse)
                        )
                    );
                });

            section.setContentSpannable(verseContentSB);
            section.setChapterNo(chapterNo);
            section.setVerses(verses);

            sections.add(section);

            chaptersName.append(chapterNo).append(". ").append(quranMeta.getChapterName(this, chapterNo));
            if (chapterNo < toChapterNo) {
                chaptersName.append(", ");
            }
        }

        return new QuranPageModel(pageNo, quranMeta.getJuzForPage(pageNo), chapterRange, chaptersName.toString(),
            sections);
    }

    public void handleVerseSpinnerSelectedVerseNo(int chapterNo, int verseNo) {
        mNavigator.jumpToVerse(chapterNo, verseNo, true);
    }

    private void pendingScrollIfAny() {

        int pendingChapterNo = mNavigator.pendingScrollVerse[0];
        int pendingVerseNo = mNavigator.pendingScrollVerse[1];

        boolean proceed = pendingChapterNo > 0 && pendingVerseNo > 0;

        QuranMeta quranMeta = mQuranMetaRef.get();

        if (mReaderParams.readType == READER_READ_TYPE_JUZ) {
            proceed &= quranMeta.isVerseValid4Juz(mReaderParams.currJuzNo, pendingChapterNo, pendingVerseNo);
        } else if (mReaderParams.readType == READER_READ_TYPE_CHAPTER) {
            proceed &= pendingChapterNo == mReaderParams.currChapter.getChapterNumber();
            proceed &= quranMeta.isVerseValid4Chapter(pendingChapterNo, pendingVerseNo);
        } else if (mReaderParams.readType == READER_READ_TYPE_VERSES) {
            proceed &= pendingChapterNo == mReaderParams.currChapter.getChapterNumber();
            proceed &= QuranUtils.isVerseInRange(pendingVerseNo, mReaderParams.verseRange);
        } else {
            proceed = false;
        }

        if (proceed) {
            mNavigator.scrollToVerse(pendingChapterNo, pendingVerseNo, mNavigator.pendingScrollVerseHighlight);
            updateVerseNumber(pendingChapterNo, pendingVerseNo);

            mNavigator.pendingScrollVerse = new int[]{-1, -1};
            mNavigator.pendingScrollVerseHighlight = true;

            persistProgressDialog4PendingTask = false;
            mActionController.dismissLoader();
        } else {
            mNavigator.pendingScrollVerse = new int[]{-1, -1};
        }
    }

    public void updateVerseNumber(int chapterNo, int verseNo) {
        mBinding.readerHeader.selectVerseIntoSpinner(chapterNo, verseNo);
    }

    public void onVerseRecite(int chapterNo, int verseNo, boolean reciting) {
        mActionController.onVerseRecite(chapterNo, verseNo, reciting);
        updateVerseNumber(chapterNo, verseNo);

        if (mReaderParams.isSingleVerse()) {
            mNavigator.jumpToVerse(chapterNo, verseNo, false);
        }

        if (mPlayerService == null) {
            return;
        }

        final RecyclerView.Adapter<?> adp = mBinding.readerVerses.getAdapter();
        if (adp instanceof ADPReader) {
            onVerseReciteNonPage((ADPReader) adp, chapterNo, verseNo, reciting);
        } else if (adp instanceof ADPQuranPages) {
            onVerseRecitePage((ADPQuranPages) adp, chapterNo, verseNo, reciting);
        }
    }

    private void onVerseReciteNonPage(ADPReader adapter, int chapterNo, int verseNo, boolean reciting) {
        for (int i = 0, l = adapter.getItemCount(); i < l; i++) {
            final ReaderRecyclerItemModel item = adapter.getItem(i);

            if (item == null || item.getViewType() != VERSE) {
                continue;
            }

            adapter.notifyItemChanged(i);

            Verse verse = item.getVerse();
            final boolean isCurrVerse = verse.chapterNo == chapterNo && verse.verseNo == verseNo;
            final boolean bool = reciting && isCurrVerse;
            if (bool && mPlayerService.getP().getSyncWithVerse()) {
                mLayoutManager.scrollToPositionWithOffset(i, 0);
            }
        }
    }

    private void onVerseRecitePage(ADPQuranPages adapter, int chapterNo, int verseNo, boolean reciting) {
        if (mPlayerService == null) {
            return;
        }
        outer:
        for (int pos = 0, l = adapter.getItemCount(); pos < l; pos++) {
            QuranPageModel pageModel = adapter.getPageModel(pos);

            if (pageModel == null || pageModel.getViewType() != READER_PAGE) {
                continue;
            }

            if (!pageModel.hasChapter(chapterNo)) {
                continue;
            }

            adapter.notifyItemChanged(pos);

            for (QuranPageSectionModel section : pageModel.getSections()) {
                if (section.getChapterNo() != chapterNo) {
                    continue;
                }

                final boolean isCurrVerse = section.getChapterNo() == chapterNo && section.hasVerse(verseNo);
                final boolean bool = reciting && isCurrVerse;

                if (bool && mPlayerService.getP().getSyncWithVerse()) {
                    mNavigator.scrollToVerseOnPageValidate(pos, verseNo, mLayoutManager.findViewByPosition(pos),
                        section, false);
                    break outer;
                }
            }
        }
    }

    public void onVerseJump(int chapterNo, int verseNo) {
        if (mPlayerService == null || !mReaderParams.isSingleVerse()) {
            return;
        }

        RecitationPlayerParams recParams = mPlayerService.getP();
        if (recParams.getPreviouslyPlaying()) {
            mPlayerService.reciteVerse(new ChapterVersePair(chapterNo, verseNo));
        }
    }

    @Override
    protected void onQuranReParsed(Quran quran) {
        mActionController.showLoader();
        initQuran(getIntent());
        mActionController.dismissLoader();
    }

    private void setupOnSettingsChanged(Intent data) {
        mProtectFromPlayerReset = true;

        boolean arTextSizeChanged = SPReader.getSavedTextSizeMultArabic(this) != mReaderParams.arTextSizeMult;
        boolean translTextSizeChanged = SPReader.getSavedTextSizeMultTransl(this) != mReaderParams.translTextSizeMult;
        boolean readerStyleChanged = mReaderParams.getReaderStyle() != SPReader.getSavedReaderStyle(this);
        boolean scriptChanged = !Objects.equals(SPReader.getSavedScript(this), mReaderParams.readerScript);

        tryReciterChange();

        final Set<String> translSlugsSet;
        if (data.hasExtra(READER_KEY_TRANSL_SLUGS)) {
            String[] translSlugs = data.getStringArrayExtra(READER_KEY_TRANSL_SLUGS);
            if (translSlugs == null) {
                translSlugsSet = new TreeSet<>();
            } else {
                translSlugsSet = new TreeSet<>(Arrays.asList(translSlugs));
            }
        } else {
            translSlugsSet = mReaderParams.getVisibleTranslSlugs();
        }

        boolean translChanged = !Objects.equals(translSlugsSet, mReaderParams.getVisibleTranslSlugs());
        // Reassign translSlugs regardless of translation change.
        mReaderParams.setVisibleTranslSlugs(translSlugsSet);
        // Reassign readerStyle regardless of style change.
        mReaderParams.setReaderStyle(this, SPReader.getSavedReaderStyle(this));

        // Refresh decorator regardless of any change in it.
        mVerseDecorator.refresh();

        if (scriptChanged) {
            reparseQuran();
            return;
        }

        if (readerStyleChanged) {
            onReaderStyleChanged(arTextSizeChanged, translTextSizeChanged);
        } else {
            if (translChanged) {
                onTranslChanged(arTextSizeChanged, translTextSizeChanged);
            } else {
                applySettingsChanges(arTextSizeChanged, translTextSizeChanged, false);
            }
        }
    }

    private void tryReciterChange() {
        if (mPlayerService == null) return;

        RecitationPlayerParams params = mPlayerService.getP();

        final boolean reciterChanged = !Objects.equals(
            SPReader.getSavedRecitationSlug(this),
            params.getCurrentReciter()
        );
        final boolean translationReciterChanged = !Objects.equals(
            SPReader.getSavedRecitationTranslationSlug(this),
            params.getCurrentTranslationReciter()
        );

        final int audioOption = SPReader.getRecitationAudioOption(this);
        final boolean changed;

        if (audioOption == RecitationUtils.AUDIO_OPTION_BOTH) {
            changed = reciterChanged || translationReciterChanged;
        } else if (audioOption == RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION) {
            changed = translationReciterChanged;
        } else {
            changed = reciterChanged;
        }

        if (changed) {
            mPlayerService.onReciterChanged();
            mPlayerService.onTranslationReciterChanged();
            mPlayerService.restartVerseOnConfigChange();
        }
    }

    private void onReaderStyleChanged(boolean arTextSizeChanged, boolean translTextSizeChanged) {
        mActionController.showLoader();
        initQuran(getIntent());
        applySettingsChanges(arTextSizeChanged, translTextSizeChanged, false);
    }

    private void onTranslChanged(boolean arTextSizeChanged, boolean translTextSizeChanged) {
        mActionController.showLoader();
        applySettingsChanges(arTextSizeChanged, translTextSizeChanged, true);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applySettingsChanges(boolean arTextSizeChanged, boolean translTextSizeChanged, boolean translChanged) {
        final RecyclerView recyclerView = mBinding.readerVerses;

        if (translChanged) {
            mActionController.showLoader();
            if (mPlayerService != null && mPlayerService.isPlaying() && mPlayerService.getP().getSyncWithVerse()) {
                RecitationPlayerParams P = mPlayerService.getP();
                mNavigator.pendingScrollVerse = new int[]{P.getCurrentChapterNo(), P.getCurrentVerseNo()};
            } else {
                final int firstPos = mLayoutManager.findFirstVisibleItemPosition();
                final int lastPos = mLayoutManager.findLastVisibleItemPosition();
                for (int pos = firstPos; pos <= lastPos; pos++) {
                    RecyclerView.ViewHolder vh = mBinding.readerVerses.findViewHolderForAdapterPosition(pos);
                    if (vh != null && vh.itemView instanceof VerseView) {
                        Verse verse = ((VerseView) vh.itemView).getVerse();
                        if (verse != null) {
                            mNavigator.pendingScrollVerse = new int[]{verse.chapterNo, verse.verseNo};
                            break;
                        }
                    }
                }
            }

            mNavigator.pendingScrollVerseHighlight = false;
            initQuran(getIntent());
            recyclerView.post(() -> mActionController.dismissLoader());
        } else if (arTextSizeChanged || translTextSizeChanged) {
            final RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
            if (adapter == null) {
                return;
            }
            adapter.notifyDataSetChanged();
            mReaderParams.resetTextSizesStates();
        }
    }

    private void saveToIntent() {
        final Intent intent = getIntent();
        intent.putExtra(READER_KEY_READ_TYPE, mReaderParams.readType);
        intent.putExtra(READER_KEY_JUZ_NO, mReaderParams.currJuzNo);

        if (mReaderParams.currChapter != null) {
            intent.putExtra(READER_KEY_CHAPTER_NO, mReaderParams.currChapter.getChapterNumber());
        }

        intent.putExtra(READER_KEY_VERSES, mReaderParams.verseRange);
        setIntent(intent);
    }

    private void saveReaderState() {// Get first & last visible item positions (both could be same)
        int firstPos = mLayoutManager.findFirstVisibleItemPosition();
        int lastPos = mLayoutManager.findLastVisibleItemPosition();

        if (firstPos < 0) {
            return;
        }

        RecyclerView.Adapter<?> adapter = mBinding.readerVerses.getAdapter();
        if (adapter instanceof ADPReader) {
            saveTranslationViewState((ADPReader) adapter, firstPos, lastPos);
        } else if (adapter instanceof ADPQuranPages) {
            savePageViewState((ADPQuranPages) adapter, firstPos, lastPos);
        }
    }

    private void saveTranslationViewState(ADPReader adapter, int firstPos, int lastPos) {
        // If the first item is not a verse item (could be chapterTitle, Bismillah etc), then loop until we get the verse item.
        ReaderRecyclerItemModel firstItem = adapter.getItem(firstPos);
        while (firstItem.getViewType() != VERSE && firstPos <= lastPos && firstPos >= 0) {
            firstItem = adapter.getItem(++firstPos);
        }

        ReaderRecyclerItemModel lastItem = null;
        if (lastPos >= 0) {
            // If the last item is not a verse item (could be chapterTitle, Bismillah, footer etc), then loop until we get the verse item.
            lastItem = adapter.getItem(lastPos);
            while (lastItem.getViewType() != VERSE && lastPos >= firstPos && lastPos >= 0) {
                lastItem = adapter.getItem(--lastPos);
            }
        }

        Verse firstVerse = firstItem.getVerse();
        // If we could not find the first verse item then Verse will be null, so exit.
        if (firstVerse == null) {
            return;
        }

        Verse lastVerse = lastItem == null ? null : lastItem.getVerse();
        // If we could not find the last verse item then Verse will be null OR both verses are not of the same chapter,
        // then use the first verse no as the last.
        final int lastVerseNo;
        if (lastVerse == null || lastVerse.chapterNo != firstVerse.chapterNo) {
            lastVerseNo = firstVerse.verseNo;
        } else {
            lastVerseNo = lastVerse.verseNo;
        }

        if (mReadHistoryDBHelper == null) {
            return;
        }

        // Finally save it.
        VerseUtils.saveLastVerses(
            this,
            mReadHistoryDBHelper,
            mQuranMetaRef.get(),
            mReaderParams.readType,
            READER_STYLE_TRANSLATION,
            mReaderParams.currJuzNo,
            firstVerse.chapterNo,
            firstVerse.verseNo,
            lastVerseNo
        );
    }

    private void savePageViewState(ADPQuranPages adapter, int firstPos, int lastPos) {
        // If the first item is not a verse item (could be chapterTitle, Bismillah etc), then loop until we get the verse item.
        QuranPageModel item = adapter.getPageModel(firstPos);
        while (item.getViewType() != READER_PAGE && firstPos <= lastPos && firstPos >= 0) {
            item = adapter.getPageModel(++firstPos);
        }

        // Each page have many verses, so we don't need to find the last visible item.

        List<QuranPageSectionModel> sections = item.getSections();
        QuranPageSectionModel firstSection = sections.get(0);

        int[] verses = firstSection.getFromToVerses();


        // Finally save it.
        VerseUtils.saveLastVerses(
            this,
            mReadHistoryDBHelper,
            mQuranMetaRef.get(),
            mReaderParams.readType,
            READER_STYLE_PAGE,
            mReaderParams.currJuzNo,
            firstSection.getChapterNo(),
            verses[0],
            verses[1]
        );
    }

    @Override
    protected void onActivityResult2(ActivityResult result) {
        super.onActivityResult2(result);

        int resultCode = result.getResultCode();
        Intent data = result.getData();
        if (data == null) {
            return;
        }

        runOnUiThread(() -> {
            if (resultCode == Codes.SETTINGS_LAUNCHER_RESULT_CODE) {
                setupOnSettingsChanged(data);
            } else if (resultCode == Codes.OPEN_REFERENCE_RESULT_CODE) {
                int chapterNo = data.getIntExtra(READER_KEY_CHAPTER_NO, -1);
                if (!QuranMeta.isChapterValid(chapterNo)) {
                    return;
                }
                Pair<Integer, Integer> verses = (Pair<Integer, Integer>) data.getSerializableExtra(READER_KEY_VERSES);
                mActionController.openVerseReference(chapterNo, verses);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        // Check if the user is touching outside the verseQuickActions view, if so, close it.
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            Rect rect = new Rect();
            mBinding.verseQuickActions.getGlobalVisibleRect(rect);
            if (!rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                // Delay some time so that if VerseQuickActionsView is immediately opened again, it doesn't show weird animation.
                mBinding.verseQuickActions.scheduleClose();
            }
        }


        return super.dispatchTouchEvent(ev);
    }
}