package com.quranapp.android.activities;

import static com.quranapp.android.components.editor.VerseEditor.BG_ALPHA_DEFAULT;
import static com.quranapp.android.components.editor.VerseEditor.BG_TYPE_COLORS;
import static com.quranapp.android.components.editor.VerseEditor.BG_TYPE_IMAGE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.peacedesign.android.utils.AppBridge;
import com.peacedesign.android.utils.ColorUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.components.editor.EditorBG;
import com.quranapp.android.components.editor.EditorFG;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.TranslationBook;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.ActivityEditShareBinding;
import com.quranapp.android.databinding.LytEditShareHeaderBinding;
import com.quranapp.android.databinding.LytEditorFinishBinding;
import com.quranapp.android.databinding.LytEditorTemplateScreenBinding;
import com.quranapp.android.databinding.LytReaderIndexTabBinding;
import com.quranapp.android.frags.editshare.FragEditorBG;
import com.quranapp.android.frags.editshare.FragEditorBase;
import com.quranapp.android.frags.editshare.FragEditorFG;
import com.quranapp.android.frags.editshare.FragEditorOptions;
import com.quranapp.android.frags.editshare.FragEditorSize;
import com.quranapp.android.frags.editshare.FragEditorTransls;
import com.quranapp.android.interfaceUtils.editor.OnEditorChangeListener;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.editor.EditorUtils;
import com.quranapp.android.utils.univ.DateUtils;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.views.helper.TabLayout2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ActivityEditShare extends BaseActivity implements OnEditorChangeListener {
    private final EditorUtils mEditorUtils = new EditorUtils();
    private final String IMAGE_EXT = ".png";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ActivityEditShareBinding mBinding;
    private FileUtils mFileUtils;
    private ReaderVerseDecorator mDecorator;
    private VerseEditor mVerseEditor;
    private LytEditorTemplateScreenBinding mScreenMainBinding;
    private Typeface mUrduTypeface;
    private boolean mTranslShowingFirstTime = true;
    private ViewPagerAdapter2 mPagerAdapter;
    private ProgressDialog mProgressDialog;
    private File mTmpImageFile;
    private Bitmap mTmpImageToSave;
    private String mTmpImageToSaveTitle;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return false;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_edit_share;
    }

    @Override
    protected void onDestroy() {
        if (mTmpImageFile != null) {
            mTmpImageFile.delete();
            mTmpImageFile = null;
        }

        mTmpImageToSave = null;
        mTmpImageToSaveTitle = null;
        hideLoader();
        mProgressDialog = null;

        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //        super.onSaveInstanceState(outState);
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        mFileUtils = FileUtils.newInstance(this);
        mDecorator = new ReaderVerseDecorator(this);
        showLoader();
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityEditShareBinding.bind(activityView);


        QuranMeta.prepareInstance(this, quranMeta -> {
            Intent intent = getIntent();
            int chapNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, -1);
            int verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, -1);

            if (!QuranMeta.isChapterValid(chapNo) || !quranMeta.isVerseValid4Chapter(chapNo, verseNo)) {
                somethingWrongOnStart(R.string.strMsgVerseCredsInvalid);
                return;
            }

            Quran.prepareInstance(this, quranMeta, quran -> {
                try {
                    init(mBinding, quran, chapNo, verseNo);
                } catch (Exception e) {
                    e.printStackTrace();
                    hideLoader();
                    somethingWrongOnStart(R.string.strMsgSomethingWrongFixing);
                    Logger.reportError(e);
                }
            });
        });
    }

    private void init(ActivityEditShareBinding binding, Quran quran, int chapNo, int verseNo) throws Exception {
        mVerseEditor = VerseEditor.initialize(this, chapNo, verseNo);
        mUrduTypeface = font(R.font.font_urdu);

        initHeader(binding.header);
        initViewPager(binding.viewPager);
        initTabs(binding.tabLayout);
        binding.screen.post(() -> initScreen(binding.screen, quran));
    }

    private void initHeader(LytEditShareHeaderBinding header) {
        ViewUtils.addStrokedBGToHeader(header.getRoot(), R.color.colorBGPage, R.color.colorDivider);

        header.close.setOnClickListener(v -> finish());
        header.done.setOnClickListener(v -> done());
    }

    private void initScreen(RelativeLayout screen, Quran quran) {
        mScreenMainBinding = LytEditorTemplateScreenBinding.inflate(getLayoutInflater(), screen, false);

        int dimen = Math.min(screen.getWidth(), screen.getHeight());
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(dimen, dimen);
        p.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        screen.addView(mScreenMainBinding.getRoot(), p);

        Verse verse = quran.getVerse(mVerseEditor.getChapNo(), mVerseEditor.getVerseNo());
        mVerseEditor.setVerse(verse);
        //        mScreenMainBinding.txtArabic.setText(
        //            mDecorator.setupArabicText(verse.getArabicText(), mVerseEditor.getVerseNo()));

        String textRef = String.format("%s Qur'an %d:%d", StringUtils.DASH, mVerseEditor.getChapNo(),
            mVerseEditor.getVerseNo());
        mScreenMainBinding.txtRef.setText(textRef);

        mDecorator.setTextSizeArabic(mScreenMainBinding.txtArabic, .7f);
        //        mDecorator.setFontArabic(mScreenMainBinding.txtArabic);

        mDecorator.setTextSizeTransl(mScreenMainBinding.txtTransl, 1f);
    }

    private void initViewPager(ViewPager2 viewPager) {
        mPagerAdapter = new ViewPagerAdapter2(this);

        String[] tabLabels = strArray(R.array.arrEditShareTabs);

        FragEditorBG fragEditorBG = FragEditorBG.newInstance();
        FragEditorFG fragEditorFG = FragEditorFG.newInstance();
        FragEditorOptions fragOptions = FragEditorOptions.newInstance();
        FragEditorTransls fragTransls = FragEditorTransls.newInstance();
        FragEditorSize fragSize = FragEditorSize.newInstance();

        FragEditorBase[] fragments = {fragEditorBG, fragEditorFG, fragOptions, fragTransls, fragSize};

        for (int i = 0, l = tabLabels.length; i < l; i++) {
            fragments[i].setEditor(mVerseEditor);
            mEditorUtils.setFragIndex(fragments[i].getClass(), i);
            mPagerAdapter.addFragment(fragments[i], tabLabels[i]);
        }

        viewPager.setOffscreenPageLimit(mPagerAdapter.getItemCount());
        viewPager.setAdapter(mPagerAdapter);
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);


        mBinding.screen.post(() -> {
            fragTransls.selectFirst();
            fragOptions.setOptionsVisibility(true, true, true);
            hideLoader();
        });
    }

    private void initTabs(TabLayout2 tabLayout) {
        tabLayout.setTabSetupCallback((viewPager, tab, position) -> {
            LytReaderIndexTabBinding binding = LytReaderIndexTabBinding.inflate(LayoutInflater.from(this));
            tab.setCustomView(binding.getRoot());
            ViewPagerAdapter2 adapter = (ViewPagerAdapter2) viewPager.getAdapter();
            if (adapter != null) {
                binding.tabTitle.setText((adapter).getPageTitle(position));
            }
        });
        tabLayout.populateFromViewPager(mBinding.viewPager);
    }

    private GradientDrawable createGradient(int[] bgColors) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, bgColors);
        gd.setCornerRadius(0f);
        return gd;
    }

    private void done() {
        getAsyncLayoutInflater().inflate(R.layout.lyt_editor_finish, null, (view, resid, parent) -> {
            try {
                LytEditorFinishBinding finishBinding = LytEditorFinishBinding.bind(view);
                showFinishDialog(finishBinding);
            } catch (Exception e) {
                Logger.reportError(e);
            }
        });
    }

    private void showFinishDialog(LytEditorFinishBinding finishBinding) throws IOException {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setView(finishBinding.getRoot());
        builder.setCancelable(false);
        builder.setOnDismissListener(dialog -> {
            if (mTmpImageFile != null) {
                mTmpImageFile.delete();
            }
        });
        PeaceDialog dialog = builder.create();

        Bitmap bitmap = prepareImageFromView(mScreenMainBinding.getRoot());
        finishBinding.preview.setImageBitmap(bitmap);

        mTmpImageFile = saveToTmpFile(bitmap);

        finishBinding.close.setOnClickListener(v -> dialog.dismiss());
        finishBinding.download.setOnClickListener(v -> download(bitmap));
        finishBinding.share.setOnClickListener(v -> share(mTmpImageFile));

        togglePreview(finishBinding, true);
        dialog.show();
    }

    private void togglePreview(LytEditorFinishBinding finishBinding, boolean show) {
        finishBinding.progressCont.setVisibility(show ? View.GONE : View.VISIBLE);
        finishBinding.preview.setVisibility(show ? View.VISIBLE : View.GONE);
        ViewUtils.disableView(finishBinding.download, !show);
        ViewUtils.disableView(finishBinding.share, !show);
    }

    private Bitmap prepareImageFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return bitmap;
    }

    private File saveToTmpFile(Bitmap bitmap) throws IOException {
        String name = StringUtils.random(10, StringUtils.RandomPattern.ALPHABETIC);
        File tmpFile = File.createTempFile(name, IMAGE_EXT, getCacheDir());

        FileOutputStream out = new FileOutputStream(tmpFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();

        return tmpFile;
    }

    private void download(Bitmap bitmap) {
        mTmpImageToSave = bitmap;
        mTmpImageToSaveTitle = appName() + "_image_" + DateUtils.getDateTimeNow4Filename() + IMAGE_EXT;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_TITLE, mTmpImageToSaveTitle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("/Pictures"));
        }
        startActivity4Result(intent, null);
    }

    private void saveImage(Uri uri, Bitmap bitmap) {
        try (FileOutputStream out = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            if (mTmpImageToSaveTitle != null) {
                Toast.makeText(this, str(R.string.strTitleImageSavedWithName, mTmpImageToSaveTitle),
                    Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Logger.reportError(e);
        } finally {
            mTmpImageToSave = null;
            mTmpImageToSaveTitle = null;
        }
    }

    private void share(File tmpFile) {
        AppBridge.Sharer sharer = AppBridge.newSharer(this);
        sharer.setPlatform(AppBridge.Platform.SYSTEM_SHARE);
        sharer.setData(mFileUtils.getFileURI(tmpFile));
        sharer.share();
    }

    @Override
    public void onBGChange(EditorBG nBg) {
        if (mScreenMainBinding == null) {
            return;
        }

        mScreenMainBinding.getRoot().post(() -> onBGChangeFinal(nBg));
    }

    private void onBGChangeFinal(EditorBG nBg) {
        if (nBg.getBgType() == BG_TYPE_IMAGE) {
            mScreenMainBinding.bgImage.setImageDrawable(nBg.getBgImage());
            if (mVerseEditor.getLastBG() == null || (mVerseEditor.getLastBG().getBgType() != BG_TYPE_IMAGE)) {
                mVerseEditor.setBGAlpha(BG_ALPHA_DEFAULT);
                mVerseEditor.setBGAlphaColor(Color.BLACK);
            }
            mScreenMainBinding.bgInner.setBackgroundColor(
                ColorUtils.createAlphaColor(mVerseEditor.getBGAlphaColor(), mVerseEditor.getBGAlpha()));
        } else if (nBg.getBgType() == BG_TYPE_COLORS) {
            mVerseEditor.setBGAlpha(0);
            mScreenMainBinding.bgImage.setImageDrawable(null);
            mScreenMainBinding.bgInner.setBackgroundColor(0);
            int[] bgColors = nBg.getBgColors();
            if (bgColors.length == 1) {
                mScreenMainBinding.bgRoot.setBackgroundColor(bgColors[0]);
            } else {
                mScreenMainBinding.bgRoot.setBackground(createGradient(bgColors));
            }
        }

        if (mVerseEditor.isBGBrightnessNotSameAsBefore(nBg)) {
            Fragment frag = mPagerAdapter.getFragmentSafely(mEditorUtils.getColorsFragIndex());
            if (frag instanceof FragEditorFG) {
                ArrayList<Integer> indices = mVerseEditor.getFGIndicesAgainstBrightness(nBg.getBrightness());
                FragEditorFG fragFG = (FragEditorFG) frag;
                if (indices.size() > 0 && !indices.contains(fragFG.getSelectedItemPos())) {
                    fragFG.select(indices.get(0));
                }
            }

            final String fgColor = nBg.isDark() ? "#A6A6A6" : "#6A6A6A";
            mScreenMainBinding.watermarkTxt.setTextColor(Color.parseColor(fgColor));
        }
        mVerseEditor.setLastBG(nBg);
    }

    @Override
    public void onBGAlphaColorChange(int alphaColor) {
        mScreenMainBinding.bgInner.setBackgroundColor(alphaColor);
    }

    @Override
    public void onFGChange(EditorFG nFG) {
        int[] colors = nFG.getColors();
        mScreenMainBinding.txtArabic.setTextColor(colors[0]);
        mScreenMainBinding.txtTransl.setTextColor(colors[1]);
        mScreenMainBinding.txtRef.setTextColor(colors[2]);
    }

    @Override
    public void onOptionChange(boolean showAr, boolean showTrans, boolean showRef) {
        if (mScreenMainBinding == null) {
            return;
        }

        showTrans = showTrans && mVerseEditor.getVerse().getTranslationCount() != 0;

        mScreenMainBinding.txtArabic.setVisibility(showAr ? View.VISIBLE : View.GONE);
        mScreenMainBinding.txtTransl.setVisibility(showTrans ? View.VISIBLE : View.GONE);
        mScreenMainBinding.txtRef.setVisibility(showRef ? View.VISIBLE : View.GONE);

        Fragment frag = mPagerAdapter.getFragmentSafely(mEditorUtils.getSizeFragIndex());
        if (frag instanceof FragEditorSize) {
            FragEditorSize fragSize = (FragEditorSize) frag;
            fragSize.disableArSeekbar(!showAr);
            fragSize.disableTranslSeekbar(!showTrans);
        }
    }

    @Override
    public void onTranslChange(TranslationBook book) {
        if (mScreenMainBinding == null) {
            return;
        }

        mHandler.post(() -> onTranslChangeFinal(book));
    }

    private void onTranslChangeFinal(TranslationBook book) {
        if (book == null) {
            return;
        }

        Translation transl = book.getTranslation(mVerseEditor.getChapNo(), mVerseEditor.getVerseNo());
        mVerseEditor.getVerse().setTranslations(new ArrayList<>(Collections.singleton(transl)));

        mScreenMainBinding.txtTransl.setText(StringUtils.removeHTML(transl.getText(), false));
        mScreenMainBinding.txtTransl.setTypeface(transl.isUrdu() ? mUrduTypeface : Typeface.SANS_SERIF);

        if (mTranslShowingFirstTime) {
            mTranslShowingFirstTime = false;
            hideLoader();
            if ((transl.getText().length() + mVerseEditor.getVerse().arabicText.length()) > 650) {
                setOptionsVisibility(false, true, true);

                if (transl.getText().length() > 700) {
                    mBinding.viewPager.setCurrentItem(mEditorUtils.getSizeFragIndex(), false);
                    Toast.makeText(this, R.string.strMsgEditShareLongTransl, Toast.LENGTH_SHORT).show();
                } else {
                    mBinding.viewPager.setCurrentItem(mEditorUtils.getOptionsFragIndex(), false);
                }
            } else {
                setOptionsVisibility(true, true, true);
            }
        }
    }

    private void setOptionsVisibility(boolean showAr, boolean showTrans, boolean showRef) {
        Fragment frag = mPagerAdapter.getFragmentSafely(mEditorUtils.getOptionsFragIndex());
        if (frag instanceof FragEditorOptions) {
            ((FragEditorOptions) frag).setOptionsVisibility(showAr, showTrans, showRef);
        }
    }

    @Override
    public void onArSizeChange(float arSizeMult) {
        if (mDecorator == null || mVerseEditor == null || mScreenMainBinding == null) {
            return;
        }

        mDecorator.setTextSizeArabic(mScreenMainBinding.txtArabic, arSizeMult);
    }

    @Override
    public void onTranslSizeChange(float translSizeMult) {
        if (mDecorator == null || mVerseEditor == null || mScreenMainBinding == null) {
            return;
        }

        mDecorator.setTextSizeTransl(mScreenMainBinding.txtTransl, translSizeMult);
    }

    private void showLoader() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.show();
    }

    private void hideLoader() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult2(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri != null && mTmpImageToSave != null) {
            saveImage(uri, mTmpImageToSave);
        }
    }

    private void somethingWrongOnStart(int msgId) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(this);
        builder.setTitle(R.string.strTitleError);
        builder.setMessage(msgId);
        builder.setNeutralButton(R.string.strLabelClose, (dialog, which) -> finish());
        builder.show();
    }
}