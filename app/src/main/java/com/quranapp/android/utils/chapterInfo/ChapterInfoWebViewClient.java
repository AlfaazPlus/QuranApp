package com.quranapp.android.utils.chapterInfo;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityChapInfo;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.quran.QuranUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import kotlin.Pair;

public class ChapterInfoWebViewClient extends WebViewClient {
    private final QuranMeta.ChapterMeta mChapterInfoMeta;
    private final ActivityChapInfo mActivityChapInfo;

    public ChapterInfoWebViewClient(ActivityChapInfo activityChapInfo) {
        mChapterInfoMeta = activityChapInfo.mChapterInfoMeta;
        mActivityChapInfo = activityChapInfo;
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        try {
            WebResourceResponse webResourceResponse = handleRequest(view, request);
            if (webResourceResponse != null) {
                return webResourceResponse;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.shouldInterceptRequest(view, request);
    }

    private WebResourceResponse handleRequest(WebView view, WebResourceRequest request) throws IOException {
        Uri uri = request.getUrl();
        String host = uri.getHost();
        if (host == null) {
            return super.shouldInterceptRequest(view, request);
        }

        String uriStr = uri.toString().toLowerCase();

        InputStream data = null;
        switch (host) {
            case "assets-file":
                data = view.getContext().getAssets().open(uri.toString().substring("https://assets-file/".length()));
                break;
            case "assets-font": {
                if (uriStr.contains("surah-icon")) {
                    data = view.getContext().getResources().openRawResource(+R.font.suracon);
                } else if ("ur".equals(mActivityChapInfo.mLanguage) && uriStr.contains("content")) {
                    data = view.getContext().getResources().openRawResource(+R.font.font_urdu);
                }
                break;
            }
            case "assets-image": {
                if (uriStr.contains("revelation")) {
                    boolean isMeccan = Objects.equals(mChapterInfoMeta.revelationType, "meccan");
                    int resId = isMeccan ? R.drawable.dr_makkah_old : R.drawable.dr_madina_old;
                    data = view.getContext().getResources().openRawResource(+resId);
                }
                break;
            }
        }

        if (data == null) {
            return null;
        }

        Map<String, String> headers = request.getRequestHeaders();
        headers.put("Access-Control-Allow-Origin", "*");

        return new WebResourceResponse(URLConnection.guessContentTypeFromName(uri.getPath()), "utf-8", 200, "OK",
            headers, data);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Log.d(request);
        return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Log.d(errorCode, description, failingUrl);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
        Log.d(url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        try {
            installContents(view);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String text(@StringRes int strResId, Object obj) {
        return mActivityChapInfo.getString(strResId) + ":" + obj;
    }

    private void installContents(WebView webView) throws JSONException {
        JSONObject contentJson = new JSONObject();
        contentJson.put("text-direction", prepareTextDirection());
        contentJson.put("chapter-icon-unicode", prepareChapterIconUnicode());
        contentJson.put("chapter-title", prepareChapterTitle());
        contentJson.put("chapter-no", text(R.string.strTitleChapInfoChapterNo, mChapterInfoMeta.chapterNo));
        contentJson.put("juz-no", text(R.string.strTitleChapInfoJuzNo, prepareJuzs()));
        contentJson.put("verse-count", text(R.string.strTitleChapInfoVerses, mChapterInfoMeta.verseCount));
        contentJson.put("ruku-count", text(R.string.strTitleChapInfoRukus, mChapterInfoMeta.rukuCount));
        contentJson.put("pages", text(R.string.strTitleChapInfoPages, preparePages(mChapterInfoMeta.pageRange)));
        contentJson.put("revelation-order", text(R.string.strTitleChapInfoRevOrder, mChapterInfoMeta.revelationOrder));

        boolean isMeccan = Objects.equals(mChapterInfoMeta.revelationType, "meccan");
        contentJson.put("revelation-type", text(
            R.string.strTitleChapInfoRevType,
            mActivityChapInfo.getString(isMeccan ? R.string.strTitleMakki : R.string.strTitleMadani)
        ));

        String contentJsonStr = contentJson.toString().replaceAll("'", "\\\\'");
        Log.d(contentJsonStr);
        String js = "javascript:installContents('" + contentJsonStr + "')";
        webView.loadUrl(js);
    }

    private String prepareChapterTitle() {
        return mActivityChapInfo.mQuranMeta.getChapterName(mActivityChapInfo, mChapterInfoMeta.chapterNo, true);
    }

    private String prepareTextDirection() {
        return "ur".equals(mActivityChapInfo.mLanguage) || "ar".equals(
            mActivityChapInfo.mLanguage) ? "rtl" : "ltr";
    }

    private String prepareChapterIconUnicode() {
        return QuranUtils.getChapterIconUnicode(mChapterInfoMeta.chapterNo) + QuranUtils.getChapterIconUnicode(0);
    }

    private String prepareJuzs() {
        ArrayList<Integer> chapterJuzs = mActivityChapInfo.mQuranMeta.getChapterJuzs(mChapterInfoMeta.chapterNo);
        final String juzs;

        if (chapterJuzs.size() < 2) {
            juzs = String.valueOf(chapterJuzs.get(0));
        } else {
            juzs = chapterJuzs.get(0) + "-" + chapterJuzs.get(chapterJuzs.size() - 1);
        }

        return juzs;
    }

    private String preparePages(Pair<Integer, Integer> pages) {
        final String page;
        if (Objects.equals(pages.getFirst(), pages.getSecond())) {
            page = String.valueOf(pages.getFirst());
        } else {
            StringJoiner joiner = new StringJoiner("-");
            joiner.add(pages.getFirst().toString()).add(pages.getSecond().toString());
            page = joiner.toString();
        }
        return page;
    }

    private String makeFunction(String functionName, Object param) {
        return "javascript:" + functionName + "('" + param + "')";
    }
}
