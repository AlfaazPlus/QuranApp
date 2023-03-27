package com.quranapp.android.utils.tafsir;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;

import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityTafsir2;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.univ.ResUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;

public class TafsirWebViewClient2 extends WebViewClient {
    private final ActivityTafsir2 mActivityTafsir;
    private final boolean isDarkTheme;

    public TafsirWebViewClient2(ActivityTafsir2 activityTafsir) {
        mActivityTafsir = activityTafsir;
        isDarkTheme = WindowUtils.isNightMode(activityTafsir);
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
            return null;
        }
        Context ctx = view.getContext();

        InputStream data = null;
        String uriStr = uri.toString().toLowerCase();
        switch (host) {
            case "assets-file":
                data = ctx.getAssets().open(uri.toString().substring("https://assets-file/".length()));
                break;
            case "assets-font": {
                if (uriStr.contains("me_quran")) {
                    data = ctx.getResources().openRawResource(+R.font.uthmanic_hafs);
                } else if (uriStr.contains("verse-preview")) {
                    data = ctx.getResources().openRawResource(+R.font.uthmanic_hafs);
                } /*else if (TafsirUtils.TAFSIR_SLUG_TAFSIR_IBN_KATHIR_UR.equals(
                    mActivityTafsir.mTafsirSlug) && uriStr.contains(
                    "content")) {
                    data = view.getContext().getResources().openRawResource(+R.font.font_urdu);
                }*/
                break;
            }
            case "assets-image": {
                if (uriStr.contains("left-arrow")) {
                    InputStream inputStream = createArrowDrawableStream(mActivityTafsir, 0);
                    if (inputStream != null) {
                        data = inputStream;
                    }
                } else if (uriStr.contains("right-arrow")) {
                    InputStream inputStream = createArrowDrawableStream(mActivityTafsir, 180);
                    if (inputStream != null) {
                        data = inputStream;
                    }
                } else if (uriStr.contains("top-arrow")) {
                    InputStream inputStream = createArrowDrawableStream(mActivityTafsir, 90);
                    if (inputStream != null) {
                        data = inputStream;
                    }
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

    private InputStream createArrowDrawableStream(Context context, float rotate) {
        Drawable drawable = ContextKt.drawable(context, R.drawable.dr_icon_arrow_left);
        if (drawable == null) {
            return null;
        }

        drawable = (DrawableCompat.wrap(drawable)).mutate();

        drawable.setTint(isDarkTheme ? Color.parseColor("#BBBBBB") : Color.BLACK);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (rotate != 0) {
            canvas.rotate(rotate, drawable.getIntrinsicWidth() >> 1, drawable.getIntrinsicHeight() >> 1);
        }
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return ResUtils.getBitmapInputStream(bitmap);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        Log.d(error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        try {
            installContents(view);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void installContents(WebView webView) throws JSONException {
        String tafsirSlug = mActivityTafsir.mTafsirSlug;
        int chapterNo = mActivityTafsir.mChapterNo;
        int verseNo = mActivityTafsir.mVerseNo;
        Chapter chapter = mActivityTafsir.mQuran.getChapter(chapterNo);

        JSONObject contentJson = new JSONObject();
        contentJson.put("tafsir-title", TafsirUtils.getTafsirName(tafsirSlug));
        contentJson.put("verse-info-title",
            mActivityTafsir.str(R.string.strLabelVerseWithChapNameWithBar, chapter.getName(), verseNo));
        contentJson.put("verse-preview", chapter.getVerse(verseNo).arabicText);
        contentJson.put("previous-tafsir-title", preparePrevVerseTitle(verseNo));
        contentJson.put("next-tafsir-title", prepareNextVerseTitle(verseNo, chapter.getVerseCount()));

        String js = "javascript:installContents('" + contentJson + "')";
        webView.loadUrl(js);
    }

    private String preparePrevVerseTitle(int verseNo) {
        return verseNo == 1 ? "" : mActivityTafsir.str(R.string.strLabelVerseNo, verseNo - 1);
    }

    private String prepareNextVerseTitle(int verseNo, int totalVerseCount) {
        return verseNo == totalVerseCount ? "" : mActivityTafsir.str(R.string.strLabelVerseNo, verseNo + 1);
    }


    private String makeFunction(String functionName, Object param) {
        return "javascript:" + functionName + "('" + param + "')";
    }
}
