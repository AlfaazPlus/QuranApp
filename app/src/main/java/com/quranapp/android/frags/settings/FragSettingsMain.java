/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;
import static com.quranapp.android.activities.readerSettings.ActivitySettings.KEY_SETTINGS_DESTINATION;
import static com.quranapp.android.activities.readerSettings.ActivitySettings.SETTINGS_THEME;
import static com.quranapp.android.activities.readerSettings.ActivitySettings.SETTINGS_VOTD;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_VERSES;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_READING;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_TRANSLATION;
import static com.quranapp.android.utils.reader.TextSizeUtils.TEXT_SIZE_MAX_PROGRESS;
import static com.quranapp.android.utils.reader.TextSizeUtils.TEXT_SIZE_MIN_PROGRESS;
import static com.quranapp.android.utils.univ.Codes.SETTINGS_LAUNCHER_RESULT_CODE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.tabs.TabLayout;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.DrawableUtils;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.sheet.PeaceBottomSheet;
import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.databinding.FragSettingsMainBinding;
import com.quranapp.android.databinding.LytReaderIndexTabBinding;
import com.quranapp.android.databinding.LytReaderSettingsBinding;
import com.quranapp.android.databinding.LytReaderSettingsItemBinding;
import com.quranapp.android.databinding.LytReaderSettingsTextDecoratorBinding;
import com.quranapp.android.databinding.LytReaderSettingsTextSizeBinding;
import com.quranapp.android.databinding.LytSettingsLayoutStyleBinding;
import com.quranapp.android.databinding.LytSettingsVotdToggleBinding;
import com.quranapp.android.databinding.LytThemeExplorerBinding;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.utils.app.RecitationManager;
import com.quranapp.android.utils.app.ThemeUtils;
import com.quranapp.android.utils.reader.QuranScriptUtils;
import com.quranapp.android.utils.reader.QuranScriptUtilsKt;
import com.quranapp.android.utils.reader.TextSizeUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslFactory;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.sharedPrefs.SPVerses;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.utils.univ.SimpleSeekbarChangeListener;
import com.quranapp.android.utils.univ.SimpleTabSelectorListener;
import com.quranapp.android.utils.votd.VOTDUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.IconedTextView;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import kotlin.Pair;
import kotlin.Unit;

public class FragSettingsMain extends FragSettingsBase implements FragmentResultListener {
    private FragSettingsMainBinding mBinding;
    private LytReaderSettingsItemBinding mThemeExplorerBinding;
    private LytReaderSettingsItemBinding mVOTDToggleBinding;
    private LytReaderSettingsItemBinding mTranslExplorerBinding;
    private LytReaderSettingsItemBinding mRecitationExplorerBinding;
    private LytReaderSettingsItemBinding mScriptExplorerBinding;
    private QuranTranslFactory mTranslFactory;
    private ReaderVerseDecorator mVerseDecorator;
    private LayoutInflater mInflater;

    private boolean mIsFromReader;
    private String[] initTranslSlugs;
    private boolean saveTranslChanges;

    private LytReaderSettingsTextSizeBinding mLytTextSizeArabic;
    private LytReaderSettingsTextSizeBinding mLytTextSizeTransl;

    public FragSettingsMain() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTranslFactory != null) {
            mTranslFactory.close();
        }
    }

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleSettings);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.frag_settings_main;
    }

    @Override
    public int getPageBackgroundColor(@NonNull Context ctx) {
        return color(ctx, R.color.colorBGPageVariable);
    }

    @Override
    public void onNewArguments(@NonNull Bundle args) {
        super.onNewArguments(args);
        if (getContext() == null) {
            return;
        }

        tryLaunchThemeExplorerFromIntent(getContext(), args);
        tryLaunchVOTDToggleExplorer(getContext(), args);
    }

    @Override
    public void setupHeader(@NonNull ActivitySettings activity, @NonNull BoldHeader header) {
        super.setupHeader(activity, header);
        header.setCallback(activity::onBackPressed);
        header.setShowSearchIcon(false);
        header.setShowRightIcon(false);
    }

    @Override
    protected boolean getShouldCreateScrollerView() {
        return true;
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding = FragSettingsMainBinding.bind(view);

        mTranslFactory = new QuranTranslFactory(ctx);
        mVerseDecorator = new ReaderVerseDecorator(ctx);
        mInflater = LayoutInflater.from(ctx);

        init(ctx);

        getParentFragmentManager().setFragmentResultListener(
            String.valueOf(SETTINGS_LAUNCHER_RESULT_CODE),
            getViewLifecycleOwner(),
            this
        );
    }

    private void init(Context ctx) {
        initValues();

        mBinding.getRoot().setBackgroundColor(color(ctx, R.color.colorBGPageVariable));

        initExplorers(ctx);
        initLayoutStyle(ctx);
        initDecorator(ctx);

        setupItemsVisibility(SPReader.getSavedReaderStyle(ctx));
    }

    private void setupItemsVisibility(int readerStyle) {
        int visibilityForReadingStyle = readerStyle == READER_STYLE_READING ? GONE : VISIBLE;

        if (mTranslExplorerBinding != null) {
            mTranslExplorerBinding.getRoot().setVisibility(visibilityForReadingStyle);
        }
        if (mLytTextSizeTransl != null) {
            mLytTextSizeTransl.getRoot().setVisibility(visibilityForReadingStyle);
        }
    }

    private void initValues() {
        final Bundle args = getArgs();
        mIsFromReader = args.getBoolean(READER_KEY_SETTING_IS_FROM_READER, false);
        initTranslSlugs = args.getStringArray(READER_KEY_TRANSL_SLUGS);
        saveTranslChanges = args.getBoolean(READER_KEY_SAVE_TRANSL_CHANGES, true);
    }

    private void initExplorers(Context ctx) {
        mBinding.appSettings.getRoot().setVisibility(!mIsFromReader ? VISIBLE : GONE);

        if (!mIsFromReader) {
            iniAppSettings();
        }

        initReaderSettings(ctx);
    }

    private void iniAppSettings() {
        initAppLanguage(mBinding.appSettings.getRoot());
        initThemes(mBinding.appSettings.getRoot());
        initVOTDToggleLauncher(mBinding.appSettings.getRoot());
    }

    private void initAppLanguage(LinearLayout parent) {
        LytReaderSettingsItemBinding appLangExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater, parent,
            false);

        setupLauncherParams(R.drawable.dr_icon_language, appLangExplorerBinding);
        setupAppLangTitle(appLangExplorerBinding);

        appLangExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsLanguage.class, null));

        parent.addView(appLangExplorerBinding.getRoot());
    }

    private void setupAppLangTitle(LytReaderSettingsItemBinding binding) {
        prepareTitle(binding, R.string.strTitleAppLanguage, "");
    }

    private void initThemes(LinearLayout parent) {
        mThemeExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);
        setupLauncherParams(R.drawable.dr_icon_theme, mThemeExplorerBinding);

        setupThemeTitle();
        mThemeExplorerBinding.launcher.setOnClickListener(v -> launchThemeExplorer(v.getContext()));

        parent.addView(mThemeExplorerBinding.getRoot());

        tryLaunchThemeExplorerFromIntent(parent.getContext(), getArgs());
    }

    private void setupThemeTitle() {
        if (mThemeExplorerBinding == null) return;

        String selectedTheme = ThemeUtils.resolveThemeTextFromMode(mThemeExplorerBinding.getRoot().getContext());
        prepareTitle(mThemeExplorerBinding, R.string.strTitleTheme, selectedTheme);
    }

    private void tryLaunchThemeExplorerFromIntent(Context ctx, Bundle args) {
        if (args.getInt(KEY_SETTINGS_DESTINATION) == SETTINGS_THEME) {
            launchThemeExplorer(ctx);
        }
    }

    private void launchThemeExplorer(Context ctx) {
        PeaceBottomSheet sheetDialog = new PeaceBottomSheet();
        PeaceBottomSheet.PeaceBottomSheetParams params = sheetDialog.getDialogParams();
        params.headerTitle = getString(R.string.strTitleTheme);

        LytThemeExplorerBinding binding = LytThemeExplorerBinding.inflate(mInflater);
        binding.themeGroup.check(ThemeUtils.resolveThemeIdFromMode(ctx));
        params.setContentView(binding.getRoot());

        binding.themeGroup.setOnCheckedChangedListener((button, checkedId) -> {
            final String themeMode;
            final int mode;

            if (checkedId == R.id.themeDark) {
                themeMode = SPAppConfigs.THEME_MODE_DARK;
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.themeLight) {
                themeMode = SPAppConfigs.THEME_MODE_LIGHT;
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                themeMode = SPAppConfigs.THEME_MODE_DEFAULT;
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            SPAppConfigs.setThemeMode(ctx, themeMode);
            sheetDialog.dismiss();
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        sheetDialog.setOnDismissListener(this::setupThemeTitle);
        sheetDialog.show(getParentFragmentManager());
    }

    private void initVOTDToggleLauncher(LinearLayout parent) {
        mVOTDToggleBinding = LytReaderSettingsItemBinding.inflate(mInflater, parent, false);

        setupLauncherParams(R.drawable.dr_icon_heart_filled, mVOTDToggleBinding);
        setupVOTDToggleTitle();

        mVOTDToggleBinding.launcher.setOnClickListener(v -> launchVOTDToggleExplorer(v.getContext()));

        parent.addView(mVOTDToggleBinding.getRoot());

        tryLaunchVOTDToggleExplorer(parent.getContext(), getArgs());
    }

    private void setupVOTDToggleTitle() {
        if (mVOTDToggleBinding == null) {
            return;
        }

        String status = mVOTDToggleBinding.getRoot().getContext().getString(SPVerses.getVOTDReminderEnabled(
            mVOTDToggleBinding.getRoot().getContext()) ? R.string.strLabelOn : R.string.strLabelOff);
        prepareTitle(mVOTDToggleBinding, R.string.strTitleVOTD, status);
    }

    private void tryLaunchVOTDToggleExplorer(Context ctx, Bundle args) {
        if (args.getInt(KEY_SETTINGS_DESTINATION) == SETTINGS_VOTD) {
            launchVOTDToggleExplorer(ctx);
        }
    }

    private void launchVOTDToggleExplorer(Context ctx) {
        PeaceBottomSheet sheetDialog = new PeaceBottomSheet();
        PeaceBottomSheet.PeaceBottomSheetParams params = sheetDialog.getDialogParams();
        params.headerTitle = getString(R.string.strTitleVOTD);

        LytSettingsVotdToggleBinding binding = LytSettingsVotdToggleBinding.inflate(mInflater);
        binding.getRoot().check(SPVerses.getVOTDReminderEnabled(ctx) ? R.id.on : R.id.off);
        params.setContentView(binding.getRoot());

        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        binding.getRoot().setBeforeCheckedChangeListener((group, newButtonId) -> {
            if (newButtonId == R.id.on) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmPermission();
                    return false;
                }
                VOTDUtils.enableVOTDReminder(ctx);
            } else {
                VOTDUtils.disableVOTDReminder(ctx);
            }

            SPVerses.setVOTDReminderEnabled(ctx, newButtonId == R.id.on);
            sheetDialog.dismiss();
            return true;
        });

        sheetDialog.setOnDismissListener(this::setupVOTDToggleTitle);
        sheetDialog.show(getParentFragmentManager());
    }

    private void alarmPermission() {
        // TODO: 13-Mar-22
        // TODO: 05/02/23 Notification permission
    }

    private void initReaderSettings(Context ctx) {
        LytReaderSettingsBinding readerSettings = mBinding.readerSettings;

        initTranslExplorer(readerSettings.explorerLauncherContainer);
        if (RecitationUtils.isRecitationSupported()) {
            initRecitationExplorer(readerSettings.explorerLauncherContainer);
        }
        initScriptExplorer(readerSettings.explorerLauncherContainer);


        readerSettings.btnReset.setOnTouchListener(new HoverPushOpacityEffect());
        readerSettings.btnReset.setOnClickListener(v -> resetCheckpoint(ctx));
        readerSettings.getRoot().setVisibility(VISIBLE);
    }

    private void initTranslExplorer(LinearLayout parent) {
        mTranslExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParams(R.drawable.dr_icon_translations, mTranslExplorerBinding);
        setupTranslTitle();

        mTranslExplorerBinding.launcher.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean(READER_KEY_SAVE_TRANSL_CHANGES, saveTranslChanges);
            args.putStringArray(READER_KEY_TRANSL_SLUGS, initTranslSlugs);
            launchFrag(FragSettingsTransl.class, args);
        });

        parent.addView(mTranslExplorerBinding.getRoot());
    }

    private void setupTranslTitle() {
        if (mTranslExplorerBinding == null) return;
        Context ctx = mTranslExplorerBinding.getRoot().getContext();

        Set<String> slugs;

        if (initTranslSlugs == null) {
            slugs = SPReader.getSavedTranslations(ctx);
        } else {
            slugs = new TreeSet<>(Arrays.asList(initTranslSlugs.clone()));
        }

        // Filter out translation slugs who are downloaded.
        slugs = slugs.stream().filter(slug -> {
            if (TranslUtils.isPrebuilt(slug)) {
                return true;
            }

            return mTranslFactory.isTranslationDownloaded(slug);
        }).collect(Collectors.toSet());

        if (initTranslSlugs == null) {
            SPReader.setSavedTranslations(ctx, slugs);
        } else {
            initTranslSlugs = slugs.toArray(new String[0]);
        }

        final int size = slugs.size();
        String subtext = size <= 0 ? null : getString(R.string.strLabelSelectedCount, size);
        prepareTitle(mTranslExplorerBinding, R.string.strTitleTranslations, subtext);
    }

    private void initRecitationExplorer(LinearLayout parent) {
        mRecitationExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParams(R.drawable.dr_icon_recitation, mRecitationExplorerBinding);
        setupRecitationTitle();

        mRecitationExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsRecitations.class, null));

        parent.addView(mRecitationExplorerBinding.getRoot());
    }

    private void setupRecitationTitle() {
        if (mRecitationExplorerBinding == null) return;
        Context ctx = mRecitationExplorerBinding.getRoot().getContext();

        prepareTitle(mRecitationExplorerBinding, R.string.strTitleRecitations, null);
        RecitationManager.prepare(ctx, false, () -> {
            prepareTitle(
                mRecitationExplorerBinding,
                R.string.strTitleRecitations,
                RecitationUtils.getReciterName(SPReader.getSavedRecitationSlug(ctx))
            );

            return Unit.INSTANCE;
        });
    }

    private void initScriptExplorer(LinearLayout parent) {
        mScriptExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParams(R.drawable.dr_icon_quran_script, mScriptExplorerBinding);
        setupScriptTitle();

        mScriptExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsScripts.class, null));

        parent.addView(mScriptExplorerBinding.getRoot());
    }

    private void setupScriptTitle() {
        if (mScriptExplorerBinding == null) return;

        String subtitle = QuranScriptUtilsKt.getQuranScriptName(
            SPReader.getSavedScript(mScriptExplorerBinding.getRoot().getContext()));
        prepareTitle(mScriptExplorerBinding, R.string.strTitleScripts, subtitle);
    }

    private void setupLauncherIcon(int startIconRes, IconedTextView textView) {
        Context context = textView.getContext();
        Drawable chevronRight = ContextKt.drawable(context, R.drawable.dr_icon_chevron_right);

        if (chevronRight != null && WindowUtils.isRTL(context)) {
            chevronRight = DrawableUtils.rotate(context, chevronRight, 180);
        }

        textView.setDrawables(ContextKt.drawable(context, startIconRes), null, chevronRight, null);
    }

    @SuppressLint("RtlHardcoded")
    private void prepareTitle(LytReaderSettingsItemBinding binding, int titleRes, String subtitle) {
        Context ctx = binding.getRoot().getContext();

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        String title = ctx.getString(titleRes);
        SpannableString titleSS = new SpannableString(title);
        titleSS.setSpan(new StyleSpan(Typeface.BOLD), 0, titleSS.length(), flag);
        ssb.append(titleSS);

        if (!TextUtils.isEmpty(subtitle)) {
            SpannableString subtitleSS = new SpannableString(subtitle);
            subtitleSS.setSpan(new AbsoluteSizeSpan(dimen(ctx, R.dimen.dmnCommonSize2)), 0, subtitleSS.length(), flag);
            subtitleSS.setSpan(new ForegroundColorSpan(color(ctx, R.color.colorText3)), 0, subtitleSS.length(), flag);
            ssb.append("\n").append(subtitleSS);
        }

        binding.launcher.setText(ssb);
        binding.launcher.setGravity(WindowUtils.isRTL(ctx) ? Gravity.RIGHT : Gravity.LEFT);
    }

    private void setupLauncherParams(int startIconRes, LytReaderSettingsItemBinding launcherBinding) {
        View launcherRoot = launcherBinding.getRoot();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LayoutParamsKt.updateMarginHorizontal(params, dp2px(launcherRoot.getContext(), 10));
        LayoutParamsKt.updateMarginVertical(params, dp2px(launcherRoot.getContext(), 5));
        launcherRoot.setLayoutParams(params);

        setupLauncherIcon(startIconRes, launcherBinding.launcher);
    }

    private void initLayoutStyle(Context ctx) {
        CardView readerStyleContainer = mBinding.readerSettings.readerStyleContainer;

        int initReaderStyle = SPReader.getSavedReaderStyle(ctx);
        int readType = getArgs().getInt(Keys.READER_KEY_READ_TYPE, -1);
        if (readType == READER_READ_TYPE_VERSES) {
            readerStyleContainer.setVisibility(GONE);
            return;
        } else {
            readerStyleContainer.setVisibility(VISIBLE);
        }

        LytSettingsLayoutStyleBinding bindingLayoutStyle = LytSettingsLayoutStyleBinding.inflate(mInflater);
        bindingLayoutStyle.title.setText(R.string.strTitleReaderLayoutStyle);
        String[] labels = strArray(ctx, R.array.arrReaderStyle);
        int[] styles = {READER_STYLE_TRANSLATION, READER_STYLE_READING};

        for (int i = 0, l = labels.length; i < l; i++) {
            TabLayout.Tab tab = bindingLayoutStyle.tabLayout.newTab();
            LytReaderIndexTabBinding binding = LytReaderIndexTabBinding.inflate(mInflater);
            tab.setCustomView(binding.getRoot());
            binding.tabTitle.setText(labels[i]);
            tab.setTag(styles[i]);

            bindingLayoutStyle.tabLayout.addTab(tab, styles[i] == initReaderStyle);
        }


        bindingLayoutStyle.tabLayout.addOnTabSelectedListener(new SimpleTabSelectorListener() {
            @Override
            public void onTabSelected(@NonNull TabLayout.Tab tab) {
                Integer style = (Integer) tab.getTag();

                if (style == null) {
                    return;
                }

                SPReader.setSavedReaderStyle(ctx, style);
                setupItemsVisibility(style);
            }
        });

        readerStyleContainer.addView(bindingLayoutStyle.getRoot());
    }

    private void initDecorator(Context ctx) {
        LytReaderSettingsTextDecoratorBinding binding = LytReaderSettingsTextDecoratorBinding.inflate(mInflater,
            mBinding.readerSettings.getRoot(), true);

        boolean isLandscape = WindowUtils.isLandscapeMode(ctx);
        int orientation = isLandscape ? HORIZONTAL : VERTICAL;
        binding.lytTextSizeCont.setOrientation(orientation);

        initTextSizeArabic(binding, isLandscape);
        initTextSizeTransl(binding, isLandscape);
    }

    private void initTextSizeArabic(LytReaderSettingsTextDecoratorBinding parent, boolean isLandscape) {
        mLytTextSizeArabic = parent.lytTextSizeArabic;

        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) mLytTextSizeArabic.getRoot().getLayoutParams();
        p.width = isLandscape ? 0 : MATCH_PARENT;
        if (isLandscape) {
            p.weight = 1;
        }
        mLytTextSizeArabic.getRoot().setLayoutParams(p);

        mLytTextSizeArabic.title.setText(R.string.strTitleReaderTextSizeArabic);

        AppCompatSeekBar seekbar = mLytTextSizeArabic.seekbar;
        seekbar.setMax(TEXT_SIZE_MAX_PROGRESS - TEXT_SIZE_MIN_PROGRESS);

        setProgressAndTextArabic(SPReader.getSavedTextSizeMultArabic(parent.getRoot().getContext()));

        setupTextSizeArabicPreview();

        seekbar.setOnSeekBarChangeListener(new SimpleSeekbarChangeListener() {
            @Override
            public void onProgressChanged(@NonNull SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + TEXT_SIZE_MIN_PROGRESS;
                final String text = progress + "%";
                mLytTextSizeArabic.progressText.setText(text);
                demonstrateTextSizeArabic(progress);
            }

            @Override
            public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
                final int progress = TEXT_SIZE_MIN_PROGRESS + seekBar.getProgress();
                SPReader.setSavedTextSizeMultArabic(seekBar.getContext(), TextSizeUtils.calculateMultiplier(progress));
            }
        });
    }

    private void setupTextSizeArabicPreview() {
        if (mLytTextSizeArabic == null) {
            return;
        }
        Context ctx = mLytTextSizeArabic.getRoot().getContext();

        mVerseDecorator.refresh();
        mVerseDecorator.refreshQuranTextFonts(mVerseDecorator.isKFQPCScript() ? new Pair<>(1, 1) : null);

        final String savedScript = SPReader.getSavedScript(ctx);

        int scriptPreviewRes = QuranScriptUtilsKt.getScriptPreviewRes(savedScript);
        mLytTextSizeArabic.demoText.setText(scriptPreviewRes);
        mVerseDecorator.setTextSizeArabic(mLytTextSizeArabic.demoText);
        if (QuranScriptUtilsKt.isKFQPCScript(savedScript)) {
            mLytTextSizeArabic.demoText.setTypeface(
                ContextKt.getFont(ctx, QuranScriptUtilsKt.getQuranScriptFontRes(savedScript))
            );
        } else {
            mVerseDecorator.setFontArabic(mLytTextSizeArabic.demoText, -1);
        }
    }

    private void initTextSizeTransl(LytReaderSettingsTextDecoratorBinding parent, boolean isLandscape) {
        mLytTextSizeTransl = parent.lytTextSizeTransl;

        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) mLytTextSizeTransl.getRoot().getLayoutParams();
        p.width = isLandscape ? 0 : MATCH_PARENT;
        if (isLandscape) {
            p.weight = 1;
            p.leftMargin = Dimen.dp2px(parent.getRoot().getContext(), 15);
        } else {
            p.topMargin = Dimen.dp2px(parent.getRoot().getContext(), 15);
        }

        mLytTextSizeTransl.getRoot().setLayoutParams(p);

        mLytTextSizeTransl.title.setText(R.string.strTitleReaderTextSizeTransl);

        AppCompatSeekBar seekbar = mLytTextSizeTransl.seekbar;
        seekbar.setMax(TEXT_SIZE_MAX_PROGRESS - TEXT_SIZE_MIN_PROGRESS);

        setProgressAndTextTransl(SPReader.getSavedTextSizeMultTransl(parent.getRoot().getContext()));

        mLytTextSizeTransl.demoText.setText(getString(R.string.strPreviewTextTransl));
        mVerseDecorator.setTextSizeTransl(mLytTextSizeTransl.demoText);

        seekbar.setOnSeekBarChangeListener(new SimpleSeekbarChangeListener() {
            @Override
            public void onProgressChanged(@NonNull SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + TEXT_SIZE_MIN_PROGRESS;
                final String text = progress + "%";
                mLytTextSizeTransl.progressText.setText(text);
                demonstrateTextSizeTransl(progress);
            }

            @Override
            public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
                final int progress = TEXT_SIZE_MIN_PROGRESS + seekBar.getProgress();
                SPReader.setSavedTextSizeMultTransl(seekBar.getContext(), TextSizeUtils.calculateMultiplier(progress));
            }
        });
    }

    private void setProgressAndTextArabic(float multiplier) {
        if (mLytTextSizeArabic == null) return;
        mLytTextSizeArabic.seekbar.setProgress(TextSizeUtils.calculateProgress(multiplier));
        final String text = TextSizeUtils.calculateProgressText(multiplier) + "%";
        mLytTextSizeArabic.progressText.setText(text);
    }

    private void demonstrateTextSizeArabic(int progress) {
        if (mLytTextSizeArabic == null) {
            return;
        }

        final float size = mVerseDecorator.getTextSizeArabic() * TextSizeUtils.calculateMultiplier(progress);
        mLytTextSizeArabic.demoText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void setProgressAndTextTransl(float multiplier) {
        if (mLytTextSizeTransl == null) return;
        mLytTextSizeTransl.seekbar.setProgress(TextSizeUtils.calculateProgress(multiplier));
        final String text = TextSizeUtils.calculateProgressText(multiplier) + "%";
        mLytTextSizeTransl.progressText.setText(text);
    }

    private void demonstrateTextSizeTransl(int progress) {
        if (mLytTextSizeTransl == null) {
            return;
        }

        final float size = mVerseDecorator.getTextSizeTransl() * TextSizeUtils.calculateMultiplier(progress);
        mLytTextSizeTransl.demoText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void resetToDefault(Context ctx) {
        SPReader.setSavedTextSizeMultArabic(ctx, TextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT);
        SPReader.setSavedTextSizeMultTransl(ctx, TextSizeUtils.TEXT_SIZE_MULT_TRANS_DEFAULT);
        SPReader.setSavedScript(ctx, QuranScriptUtils.SCRIPT_DEFAULT);
        initTranslSlugs = TranslUtils.defaultTranslationSlugs().toArray(new String[0]);

        RecitationManager.prepare(ctx, false, () -> {
            List<RecitationModel> models = RecitationManager.getModels();

            if (models != null) {
                for (RecitationModel model : models) {
                    if (model != null) {
                        SPReader.setSavedRecitationSlug(ctx, model.getSlug());
                        break;
                    }
                }

                setupRecitationTitle();
            }

            return Unit.INSTANCE;
        });

        setupTranslTitle();
        setupScriptTitle();
        setupTextSizeArabicPreview();

        demonstrateTextSizeArabic(TextSizeUtils.TEXT_SIZE_DEFAULT_PROGRESS);
        demonstrateTextSizeTransl(TextSizeUtils.TEXT_SIZE_DEFAULT_PROGRESS);
        setProgressAndTextArabic(TextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT);
        setProgressAndTextTransl(TextSizeUtils.TEXT_SIZE_MULT_TRANS_DEFAULT);
    }

    private void resetCheckpoint(Context ctx) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
        builder.setTitle(R.string.strTitleResetReaderSettings);
        builder.setMessage(R.string.strMsgResetReaderSettings);
        builder.setButtonsDirection(PeaceDialog.STACKED);
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setDialogGravity(PeaceDialog.GRAVITY_BOTTOM);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setPositiveButton(R.string.strLabelReset, color(ctx, R.color.colorSecondary),
            (dialog1, which) -> resetToDefault(ctx));
        builder.setFocusOnPositive(true);
        builder.show();
    }

    @Override
    public Bundle getFinishingResult(@NonNull Context ctx) {
        Bundle data = new Bundle();
        data.putStringArray(READER_KEY_TRANSL_SLUGS, initTranslSlugs);
        if (saveTranslChanges && initTranslSlugs != null) {
            SPReader.setSavedTranslations(ctx, new TreeSet<>(Arrays.asList(initTranslSlugs)));
        }
        return data;
    }

    @Override
    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        if (!String.valueOf(SETTINGS_LAUNCHER_RESULT_CODE).equals(requestKey)) {
            return;
        }

        initTranslSlugs = result.getStringArray(READER_KEY_TRANSL_SLUGS);
        setupTranslTitle();

        // Update the args so that it can reflect when this fragment is recreated.
        Bundle args = getArgs();
        args.putStringArray(READER_KEY_TRANSL_SLUGS, initTranslSlugs);
        setArguments(args);
    }
}
