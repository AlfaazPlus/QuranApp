package com.quranapp.android.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.ActivityChapterInfoBinding;
import com.quranapp.android.exc.NoInternetException;
import com.peacedesign.android.utils.Log;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.tafsir.TafsirJSInterface;
import com.quranapp.android.utils.tafsir.TafsirUtils;
import com.quranapp.android.utils.tafsir.TafsirWebViewClient;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.widgets.PageAlert;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ActivityTafsir extends ReaderPossessingActivity {
    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>();
    private ActivityChapterInfoBinding mBinding;
    private FileUtils fileUtils;
    public QuranMeta mQuranMeta;
    public Quran mQuran;
    public String mTafsirSlug;
    public int mChapterNo;
    public int mVerseNo;
    private boolean mLoadFullChapter;
    private PageAlert mPageAlert;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_chapter_info;
    }

    @Override
    protected void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        fileUtils = FileUtils.newInstance(this);
        mBinding = ActivityChapterInfoBinding.bind(activityView);
        initThis();
    }

    @Override
    protected void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState) {
        mQuranMeta = mQuranMetaRef.get();
        mQuran = mQuranRef.get();

        initWebView();
        initContent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initContent(intent);
    }

    public void initContent(Intent intent) {
        String slug = intent.getStringExtra(TafsirUtils.KEY_TAFSIR_SLUG);

        int chapterNo = -1;
        int verseNo = -2;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            try {
                Uri data = intent.getData();
                List<String> pathSegments = data.getPathSegments();
                String[] seg1 = pathSegments.get(0).split(":");
                chapterNo = Integer.parseInt(seg1[0]);
                verseNo = Integer.parseInt(seg1[1]);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Invalid params", false);
            }
        } else {
            chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, 1);
            verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, 2);
        }

        if (chapterNo == -1 || verseNo == -1) {
            fail("Invalid params", false);
            return;
        }

        if (slug == null) {
            slug = TafsirUtils.TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN;
        }

        mTafsirSlug = slug;
        mChapterNo = chapterNo;
        mVerseNo = verseNo;
        mLoadFullChapter = mVerseNo == 0;

        loadContent();
    }

    private void initThis() {
        mBinding.title.setText(R.string.strTitleTafsir);
        mBinding.back.setOnClickListener(v -> finish());

        mBinding.loader.setVisibility(View.VISIBLE);
    }

    private void initPageAlert() {
        mPageAlert = new PageAlert(this);
    }

    private void initWebView() {
        setupWebView(mBinding.webView);
    }

    private void loadContent() {
        mTaskRunner.callAsync(new LoadTafsirTask());
        //        renderDataTest();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        webView.addJavascriptInterface(new TafsirJSInterface(this), "TafsirJSInterface");
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("[" + consoleMessage.lineNumber() + "]" + consoleMessage.message());
                return true;
            }
        });
        webView.setWebViewClient(new TafsirWebViewClient(this) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mBinding.loader.setVisibility(View.GONE);
            }
        });
    }

    private String getBoilerPlateHTML() {
        return ResUtils.readAssetsTextFile(this, "tafsir/tafsir_page.html");
    }

    private void renderDataTest() {
        String boilerPlateHTML = getBoilerPlateHTML();
        mBinding.webView.loadData(boilerPlateHTML, "text/html", "UTF-8");
    }

    private void renderData(String rawData) throws JSONException {
        Log.d(rawData);
        JSONObject jsonObject = new JSONObject(rawData);
        Log.d(jsonObject);
        JSONArray tafsirs = jsonObject.getJSONArray("tafsirs");
        if (tafsirs.length() < 1) {
            fail("Could not find tafsir.", false);
            return;
        }

        String data = tafsirs.getJSONObject(0).getString("text");
        String boilerPlateHTML = getBoilerPlateHTML();
        data = String.format(boilerPlateHTML, resolveDarkMode(), data);
        mBinding.webView.loadDataWithBaseURL(null, data, "text/html; charset=UTF-8", "utf-8", null);
    }

    private String resolveDarkMode() {
        return WindowUtils.isNightMode(this) ? "dark" : "light";
    }

    private void loadFailed(String addMsg) {
        String msg = "Failed to load tafsir.";
        if (!TextUtils.isEmpty(addMsg)) {
            msg += " " + addMsg;
        }
        fail(msg, true);
    }

    private void fail(String msg, boolean showRetry) {
        mBinding.loader.setVisibility(View.GONE);

        if (mPageAlert == null) {
            initPageAlert();
        }

        mPageAlert.setMessage(msg, null);

        if (showRetry) {
            mPageAlert.setActionButton(R.string.strLabelRetry, this::loadContent);
        } else {
            mPageAlert.setActionButton(null, null);
        }
        mPageAlert.show(mBinding.container);

        deleteSavedFileIfExists();
    }

    private void noInternet() {
        if (mPageAlert == null) {
            initPageAlert();
        }
        mPageAlert.setupForNoInternet(this::loadContent);
        mPageAlert.show(mBinding.container);
    }

    private void deleteSavedFileIfExists() {
        if (mTafsirSlug == null) {
            return;
        }

        final File tafsirFile;
        if (mLoadFullChapter) {
            tafsirFile = fileUtils.getTafsirFileFullChapter(mTafsirSlug, mChapterNo);
        } else {
            tafsirFile = fileUtils.getTafsirFileSingleVerse(mTafsirSlug, mChapterNo, mVerseNo);
        }
        tafsirFile.delete();
    }


    private class LoadTafsirTask extends BaseCallableTask<String> {
        private final File tafsirFile;
        private final String urlStr;

        public LoadTafsirTask() {
            if (mLoadFullChapter) {
                tafsirFile = fileUtils.getTafsirFileFullChapter(mTafsirSlug, mChapterNo);
                urlStr = TafsirUtils.prepareTafsirUrlFullChapter(mTafsirSlug, mChapterNo);
            } else {
                tafsirFile = fileUtils.getTafsirFileSingleVerse(mTafsirSlug, mChapterNo, mVerseNo);
                urlStr = TafsirUtils.prepareTafsirUrlSingleVerse(mTafsirSlug, mChapterNo, mVerseNo);
            }
        }

        @Override
        public void preExecute() {
            if (mPageAlert != null) {
                mPageAlert.remove();
            }
            mBinding.loader.setVisibility(View.VISIBLE);
        }

        @Override
        public String call() throws Exception {
            if (tafsirFile.exists()) {
                String read = FileUtils.readTextFromFile(tafsirFile);
                if (read.length() > 0) {
                    return read;
                }
            } else {
                fileUtils.createFile(tafsirFile);
            }

            if (!NetworkStateReceiver.isNetworkConnected(ActivityTafsir.this)) {
                throw new NoInternetException();
            }

            if (urlStr == null) {
                throw new IllegalStateException("Invalid tafsir url, probably need to check tafsir id.");
            }

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Length", "0");
            conn.setRequestProperty("Connection", "close");
            conn.setConnectTimeout(180000);
            conn.setReadTimeout(180000);
            conn.setAllowUserInteraction(false);
            conn.connect();

            InputStreamReader isr = new InputStreamReader(conn.getInputStream());

            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            br.close();
            conn.disconnect();

            String data = sb.toString();
            FileUtils.writeTextIntoFile(tafsirFile, data);
            return data;
        }

        @Override
        public void onComplete(String result) {
            Log.d(result);
            if (result == null) {
                tafsirFile.delete();
                loadFailed(null);
                return;
            }
            runOnUiThread(() -> {
                try {
                    renderData(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onFailed(@NonNull @NotNull Exception e) {
            e.printStackTrace();
            if (e instanceof NoInternetException || e.getCause() instanceof NoInternetException) {
                tafsirFile.delete();
                noInternet();
            } else {
                loadFailed(null);
            }
        }
    }
}