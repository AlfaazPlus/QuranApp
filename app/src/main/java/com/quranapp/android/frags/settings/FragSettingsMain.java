/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentResultListener;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_VERSES;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_PAGE;
import static com.quranapp.android.reader_managers.ReaderParams.READER_STYLE_TRANSLATION;
import static com.quranapp.android.utils.app.DownloadSourceUtils.DOWNLOAD_SRC_DEFAULT;
import static com.quranapp.android.utils.app.DownloadSourceUtils.DOWNLOAD_SRC_GITHUB;
import static com.quranapp.android.utils.app.DownloadSourceUtils.DOWNLOAD_SRC_JSDELIVR;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.TEXT_SIZE_MIN_PROGRESS;
import static com.quranapp.android.utils.reader.ReaderTextSizeUtils.getMaxProgress;
import static com.quranapp.android.utils.univ.Codes.SETTINGS_LAUNCHER_RESULT_CODE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SETTING_IS_FROM_READER;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.VERTICAL;

import com.google.android.material.tabs.TabLayout;
import com.peacedesign.android.utils.DrawableUtils;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.api.models.recitation.RecitationInfoModel;
import com.quranapp.android.api.models.recitation.RecitationTranslationInfoModel;
import com.quranapp.android.api.models.tafsir.TafsirInfoModel;
import com.quranapp.android.databinding.FragSettingsMainBinding;
import com.quranapp.android.databinding.LytReaderIndexTabBinding;
import com.quranapp.android.databinding.LytReaderSettingsItemBinding;
import com.quranapp.android.databinding.LytReaderSettingsItemSwitchBinding;
import com.quranapp.android.databinding.LytReaderSettingsTextDecoratorBinding;
import com.quranapp.android.databinding.LytReaderSettingsTextSizeBinding;
import com.quranapp.android.databinding.LytResDownloadSourceSheetBinding;
import com.quranapp.android.databinding.LytSettingsLayoutStyleBinding;
import com.quranapp.android.databinding.LytSettingsReaderBinding;
import com.quranapp.android.databinding.LytSettingsVotdToggleBinding;
import com.quranapp.android.databinding.LytThemeExplorerBinding;
import com.quranapp.android.frags.settings.appLogs.FragSettingsAppLogs;
import com.quranapp.android.frags.settings.recitations.FragSettingsRecitations;
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudio;
import com.quranapp.android.reader_managers.ReaderVerseDecorator;
import com.quranapp.android.utils.app.DownloadSourceUtils;
import com.quranapp.android.utils.app.ThemeUtils;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.LayoutParamsKt;
import com.quranapp.android.utils.gesture.HoverPushOpacityEffect;
import com.quranapp.android.utils.reader.QuranScriptUtils;
import com.quranapp.android.utils.reader.QuranScriptUtilsKt;
import com.quranapp.android.utils.reader.ReaderTextSizeUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;
import com.quranapp.android.utils.reader.recitation.RecitationManager;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.reader.tafsir.TafsirManager;
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.sharedPrefs.SPVerses;
import com.quranapp.android.utils.simplified.SimpleSeekbarChangeListener;
import com.quranapp.android.utils.simplified.SimpleTabSelectorListener;
import com.quranapp.android.utils.tafsir.TafsirUtils;
import com.quranapp.android.utils.univ.Keys;
import com.quranapp.android.utils.votd.VOTDUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.IconedTextView;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet;
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheetParams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import kotlin.Pair;
import kotlin.Unit;

public class FragSettingsMain extends FragSettingsBase implements FragmentResultListener {
    private FragSettingsMainBinding mBinding;
    private LytReaderSettingsItemBinding mTranslExplorerBinding;
    private LytReaderSettingsItemBinding mTafsirExplorerBinding;
    private LytReaderSettingsItemBinding mRecitationExplorerBinding;
    private LytReaderSettingsItemBinding mScriptExplorerBinding;
    private QuranTranslationFactory mTranslFactory;
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
        return ContextKt.color(ctx, R.color.colorBGPageVariable);
    }

    @Override
    public void onNewArguments(@NonNull Bundle args) {
        super.onNewArguments(args);
        if (getContext() == null) {
            return;
        }
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

        mTranslFactory = new QuranTranslationFactory(ctx);
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

        mBinding.getRoot().setBackgroundColor(ContextKt.color(ctx, R.color.colorBGPageVariable));

        initExplorers(ctx);
        initLayoutStyle(ctx);
        initDecorator(ctx);

        setupItemsVisibility(SPReader.getSavedReaderStyle(ctx));
    }

    private void setupItemsVisibility(int readerStyle) {
    }

    private void initValues() {
        final Bundle args = getArgs();
        mIsFromReader = args.getBoolean(READER_KEY_SETTING_IS_FROM_READER, false);
        initTranslSlugs = args.getStringArray(READER_KEY_TRANSL_SLUGS);
        saveTranslChanges = args.getBoolean(READER_KEY_SAVE_TRANSL_CHANGES, true);
    }

    private void initExplorers(Context ctx) {
        mBinding.appSettings.getRoot().setVisibility(!mIsFromReader ? VISIBLE : GONE);
        mBinding.otherSettings.getRoot().setVisibility(!mIsFromReader ? VISIBLE : GONE);

        if (!mIsFromReader) {
            iniAppSettings();
        }

        initReaderSettings(ctx);

        if (!mIsFromReader) {
            iniOtherSettings();
        }
    }

    private void iniAppSettings() {
        initAppLanguage(mBinding.appSettings.getRoot());
        initThemes(mBinding.appSettings.getRoot());
        initVOTDToggleLauncher(mBinding.appSettings.getRoot());
        initArabicTextToggle(mBinding.appSettings.getRoot());
    }

    private void initAppLanguage(LinearLayout parent) {
        LytReaderSettingsItemBinding appLangExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater, parent,
            false);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_language, appLangExplorerBinding);
        setupAppLangTitle(appLangExplorerBinding);

        appLangExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsLanguage.class, null));

        parent.addView(appLangExplorerBinding.getRoot());
    }

    private void setupAppLangTitle(LytReaderSettingsItemBinding binding) {
        Context ctx = binding.getRoot().getContext();
        final String[] availableLocalesValues = ContextKt.getStringArray(ctx, R.array.availableLocalesValues);
        final String[] availableLocaleNames = ContextKt.getStringArray(ctx, R.array.availableLocalesNames);

        String selectedLanguage = SPAppConfigs.getLocale(ctx);
        String selectedLanguageName = "";

        for (int i = 0, l = availableLocalesValues.length; i < l; i++) {
            if (Objects.equals(availableLocalesValues[i], selectedLanguage)) {
                selectedLanguageName = availableLocaleNames[i];
                break;
            }
        }

        prepareTitle(binding.launcher, R.string.strTitleAppLanguage, selectedLanguageName);
    }

    private void initThemes(LinearLayout parent) {
        LytReaderSettingsItemBinding binding = LytReaderSettingsItemBinding.inflate(mInflater);
        setupLauncherParamsAndIcon(R.drawable.dr_icon_theme, binding);

        setupThemeTitle(binding);
        binding.launcher.setOnClickListener(v -> launchThemeExplorer(v.getContext(), binding));

        parent.addView(binding.getRoot());
    }

    private void setupThemeTitle(LytReaderSettingsItemBinding binding) {
        String selectedTheme = ThemeUtils.resolveThemeTextFromMode(binding.getRoot().getContext());
        prepareTitle(binding.launcher, R.string.strTitleTheme, selectedTheme);
    }

    private void launchThemeExplorer(Context ctx, LytReaderSettingsItemBinding parentBinding) {
        PeaceBottomSheet sheetDialog = new PeaceBottomSheet();
        PeaceBottomSheetParams params = sheetDialog.getParams();
        params.setHeaderTitle(getString(R.string.strTitleTheme));

        LytThemeExplorerBinding binding = LytThemeExplorerBinding.inflate(mInflater);
        binding.themeGroup.check(ThemeUtils.resolveThemeIdFromMode(ctx));
        params.setContentView(binding.getRoot());

        binding.themeGroup.setOnCheckChangedListener((button, checkedId) -> {
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

            return Unit.INSTANCE;
        });

        sheetDialog.setOnDismissListener(() -> setupThemeTitle(parentBinding));
        sheetDialog.show(getParentFragmentManager());
    }

    private void initVOTDToggleLauncher(LinearLayout parent) {
        LytReaderSettingsItemBinding binding = LytReaderSettingsItemBinding.inflate(mInflater, parent, false);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_heart_filled, binding);
        setupVOTDToggleTitle(binding);

        binding.launcher.setOnClickListener(v -> launchVOTDToggleExplorer(v.getContext(), binding));

        parent.addView(binding.getRoot());
    }

    private void setupVOTDToggleTitle(LytReaderSettingsItemBinding binding) {
        Context context = binding.getRoot().getContext();

        String status = context.getString(
            VOTDUtils.isVOTDTrulyEnabled(context)
                ? R.string.strLabelOn
                : R.string.strLabelOff
        );
        prepareTitle(binding.launcher, R.string.strTitleVOTD, status);
    }

    private void launchVOTDToggleExplorer(Context ctx, LytReaderSettingsItemBinding parentBinding) {
        AlarmManager alarmManager = ContextCompat.getSystemService(ctx, AlarmManager.class);

        PeaceBottomSheet sheetDialog = new PeaceBottomSheet();
        PeaceBottomSheetParams params = sheetDialog.getParams();
        params.setHeaderTitle(getString(R.string.strTitleVOTD));

        LytSettingsVotdToggleBinding binding = LytSettingsVotdToggleBinding.inflate(mInflater);

        if (VOTDUtils.isVOTDTrulyEnabled(ctx)) {
            binding.getRoot().check(R.id.on);
        } else {
            binding.getRoot().check(R.id.off);
        }

        params.setContentView(binding.getRoot());

        binding.getRoot().setBeforeCheckChangeListener((group, newButtonId) -> {
            if (newButtonId == R.id.on) {
                if (alarmManager == null) {
                    Toast.makeText(ctx, R.string.msgAlarmManagerNotAvailable, Toast.LENGTH_LONG).show();
                    return false;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !NotificationManagerCompat.from(
                    ctx).areNotificationsEnabled()) {
                    requestNotificationPermission(ctx);
                    return false;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    requestAlarmPermission(ctx);
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

        sheetDialog.setOnDismissListener(() -> setupVOTDToggleTitle(parentBinding));
        sheetDialog.show(getParentFragmentManager());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestNotificationPermission(Context ctx) {
        Toast.makeText(ctx, R.string.msgVerseReminderNotifPermission, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestAlarmPermission(Context ctx) {
        Toast.makeText(ctx, R.string.msgVerseReminderAlarmPermission, Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
    }

    private void initArabicTextToggle(LinearLayout parent) {
        Context context = parent.getContext();

        LytReaderSettingsItemSwitchBinding binding = LytReaderSettingsItemSwitchBinding.inflate(
            mInflater, parent, true
        );
        setupLauncherParams(binding.getRoot());

        binding.container.setOnClickListener(v -> binding.switcher.toggle());
        binding.switcher.setChecked(SPReader.getArabicTextEnabled(context));

        binding.switcher.setOnCheckedChangeListener((buttonView, isChecked) ->
            SPReader.setArabicTextEnabled(context, isChecked));

        prepareTitle(binding.text, R.string.titleArabicTextToggle, context.getString(R.string.msgArabicTextToggle));
    }

    private void initReaderSettings(Context ctx) {
        LytSettingsReaderBinding readerSettings = mBinding.readerSettings;

        initTranslExplorer(readerSettings.explorerLauncherContainer);
        initTafsirExplorer(readerSettings.explorerLauncherContainer);
        initScriptExplorer(readerSettings.explorerLauncherContainer);
        if (RecitationUtils.isRecitationSupported()) {
            initRecitationExplorer(readerSettings.explorerLauncherContainer);
            initManageAudioExplorer(readerSettings.explorerLauncherContainer);
        }
        readerSettings.btnReset.setOnTouchListener(new HoverPushOpacityEffect());
        readerSettings.btnReset.setOnClickListener(v -> resetCheckpoint(ctx));
        readerSettings.getRoot().setVisibility(VISIBLE);
    }

    private void initTranslExplorer(LinearLayout parent) {
        mTranslExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_translations, mTranslExplorerBinding);
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
        String subtext = size == 0 ? null : getString(R.string.strLabelSelectedCount, size);
        prepareTitle(mTranslExplorerBinding.launcher, R.string.strTitleTranslations, subtext);
    }

    private void initTafsirExplorer(LinearLayout parent) {
        mTafsirExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_tafsir, mTafsirExplorerBinding);
        setupTafsirTitle();

        mTafsirExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsTafsirs.class, null));

        parent.addView(mTafsirExplorerBinding.getRoot());
    }

    private void initScriptExplorer(LinearLayout parent) {
        mScriptExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_quran_script, mScriptExplorerBinding);
        setupScriptTitle();

        mScriptExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsScripts.class, null));

        parent.addView(mScriptExplorerBinding.getRoot());
    }


    private void setupScriptTitle() {
        if (mScriptExplorerBinding == null) return;

        String subtitle = QuranScriptUtilsKt.getQuranScriptName(
            SPReader.getSavedScript(mScriptExplorerBinding.getRoot().getContext())
        );
        prepareTitle(mScriptExplorerBinding.launcher, R.string.strTitleSelectScripts, subtitle);
    }

    private void setupTafsirTitle() {
        if (mTafsirExplorerBinding == null) return;
        Context ctx = mTafsirExplorerBinding.getRoot().getContext();

        prepareTitle(mTafsirExplorerBinding.launcher, R.string.strTitleSelectTafsir, null);

        TafsirManager.prepare(ctx, false, () -> {
            prepareTitle(
                mTafsirExplorerBinding.launcher,
                R.string.strTitleSelectTafsir,
                TafsirUtils.getTafsirName(SPReader.getSavedTafsirKey(ctx))
            );

            return Unit.INSTANCE;
        });
    }

    private void initRecitationExplorer(LinearLayout parent) {
        mRecitationExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_recitation, mRecitationExplorerBinding);
        setupRecitationTitle();

        mRecitationExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsRecitations.class, null));

        parent.addView(mRecitationExplorerBinding.getRoot());
    }

    private void setupRecitationTitle() {
        if (mRecitationExplorerBinding == null) return;
        Context ctx = mRecitationExplorerBinding.getRoot().getContext();

        prepareTitle(mRecitationExplorerBinding.launcher, R.string.strTitleSelectReciter, null);
        RecitationManager.prepare(ctx, false, () -> {
            RecitationManager.prepareTranslations(ctx, false, () -> {
                prepareTitle(
                    mRecitationExplorerBinding.launcher,
                    R.string.strTitleSelectReciter,
                    RecitationManager.getCurrentReciterNameForAudioOption(ctx)
                );

                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }

    private void initManageAudioExplorer(LinearLayout parent) {
        com.quranapp.android.databinding.LytReaderSettingsItemBinding mManageAudioExplorerBinding = LytReaderSettingsItemBinding.inflate(
            mInflater);

        setupLauncherParamsAndIcon(R.drawable.dr_icon_download, mManageAudioExplorerBinding);

        prepareTitle(mManageAudioExplorerBinding.launcher, R.string.titleManageAudio,
            getString(R.string.downloadRecitations));

        mManageAudioExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsManageAudio.class, null));

        parent.addView(mManageAudioExplorerBinding.getRoot());
    }

    private void setupLauncherIcon(int startIconRes, IconedTextView textView) {
        Context context = textView.getContext();
        Drawable chevronRight = ContextKt.drawable(context, R.drawable.dr_icon_chevron_right);

        if (WindowUtils.isRTL(context)) chevronRight = DrawableUtils.rotate(context, chevronRight, 180);

        textView.setDrawables(ContextKt.drawable(context, startIconRes), null, chevronRight, null);
    }

    @SuppressLint("RtlHardcoded")
    private void prepareTitle(TextView txtView, int titleRes, String subtitle) {
        Context ctx = txtView.getContext();

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int flag = SPAN_EXCLUSIVE_EXCLUSIVE;

        String title = ctx.getString(titleRes);
        SpannableString titleSS = new SpannableString(title);
        titleSS.setSpan(new StyleSpan(Typeface.BOLD), 0, titleSS.length(), flag);
        ssb.append(titleSS);

        if (!TextUtils.isEmpty(subtitle)) {
            SpannableString subtitleSS = new SpannableString(subtitle);
            subtitleSS.setSpan(new AbsoluteSizeSpan(ContextKt.getDimenPx(ctx, R.dimen.dmnCommonSize2)), 0,
                subtitleSS.length(), flag);
            subtitleSS.setSpan(new ForegroundColorSpan(ContextKt.color(ctx, R.color.colorText3)), 0,
                subtitleSS.length(), flag);
            ssb.append("\n").append(subtitleSS);
        }

        txtView.setText(ssb);
        txtView.setGravity(WindowUtils.isRTL(ctx) ? Gravity.RIGHT : Gravity.LEFT);
    }

    private void setupLauncherParamsAndIcon(int startIconRes, LytReaderSettingsItemBinding launcherBinding) {
        setupLauncherParams(launcherBinding.getRoot());
        setupLauncherIcon(startIconRes, launcherBinding.launcher);
    }

    private void setupLauncherParams(View layout) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LayoutParamsKt.updateMarginHorizontal(params, ContextKt.dp2px(layout.getContext(), 10));
        LayoutParamsKt.updateMarginVertical(params, ContextKt.dp2px(layout.getContext(), 5));
        layout.setLayoutParams(params);
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
        String[] labels = ContextKt.getStringArray(ctx, R.array.arrReaderStyle);
        int[] styles = {READER_STYLE_TRANSLATION, READER_STYLE_PAGE};

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
        seekbar.setMax(getMaxProgress());

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
                SPReader.setSavedTextSizeMultArabic(
                    seekBar.getContext(),
                    ReaderTextSizeUtils.calculateMultiplier(
                        ReaderTextSizeUtils.normalizeProgress(seekbar.getProgress()))
                );
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

        mLytTextSizeArabic.demoText.setText(QuranScriptUtilsKt.getScriptPreviewText(savedScript));
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
            p.leftMargin = ContextKt.dp2px(parent.getRoot().getContext(), 15);
        } else {
            p.topMargin = ContextKt.dp2px(parent.getRoot().getContext(), 15);
        }

        mLytTextSizeTransl.getRoot().setLayoutParams(p);

        mLytTextSizeTransl.title.setText(R.string.strTitleReaderTextSizeTransl);

        AppCompatSeekBar seekbar = mLytTextSizeTransl.seekbar;
        seekbar.setMax(getMaxProgress());

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
                SPReader.setSavedTextSizeMultTransl(
                    seekBar.getContext(),
                    ReaderTextSizeUtils.calculateMultiplier(
                        ReaderTextSizeUtils.normalizeProgress(seekbar.getProgress()))
                );
            }
        });
    }

    private void setProgressAndTextArabic(float multiplier) {
        if (mLytTextSizeArabic == null) return;

        mLytTextSizeArabic.seekbar.setProgress(ReaderTextSizeUtils.calculateProgress(multiplier));
        mLytTextSizeArabic.seekbar.invalidate();
        final String text = ReaderTextSizeUtils.calculateProgressText(multiplier) + "%";
        mLytTextSizeArabic.progressText.setText(text);
    }

    private void demonstrateTextSizeArabic(int progress) {
        if (mLytTextSizeArabic == null) {
            return;
        }

        final float size = mVerseDecorator.getTextSizeArabic() * ReaderTextSizeUtils.calculateMultiplier(progress);
        mLytTextSizeArabic.demoText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void setProgressAndTextTransl(float multiplier) {
        if (mLytTextSizeTransl == null) return;
        mLytTextSizeTransl.seekbar.setProgress(ReaderTextSizeUtils.calculateProgress(multiplier));
        final String text = ReaderTextSizeUtils.calculateProgressText(multiplier) + "%";
        mLytTextSizeTransl.progressText.setText(text);
    }

    private void demonstrateTextSizeTransl(int progress) {
        if (mLytTextSizeTransl == null) {
            return;
        }

        final float size = mVerseDecorator.getTextSizeTransl() * ReaderTextSizeUtils.calculateMultiplier(progress);
        mLytTextSizeTransl.demoText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void iniOtherSettings() {
        initDownloadSource(mBinding.otherSettings.getRoot());
        initLogLauncher(mBinding.otherSettings.getRoot());
    }

    private void initDownloadSource(LinearLayout parent) {
        LytReaderSettingsItemBinding binding = LytReaderSettingsItemBinding.inflate(mInflater);
        setupLauncherParamsAndIcon(R.drawable.dr_icon_download, binding);

        setupDownloadSrcTitle(binding);
        binding.launcher.setOnClickListener(v -> launchDownloadSourceSelector(v.getContext(), binding));

        parent.addView(binding.getRoot());
    }

    private void launchDownloadSourceSelector(Context ctx, LytReaderSettingsItemBinding parentBinding) {
        PeaceBottomSheet sheetDialog = new PeaceBottomSheet();
        PeaceBottomSheetParams params = sheetDialog.getParams();
        params.setHeaderTitle(getString(R.string.titleResourceDownloadSource));

        LytResDownloadSourceSheetBinding binding = LytResDownloadSourceSheetBinding.inflate(mInflater);

        binding.downloadSrcGroup.check(
            (DOWNLOAD_SRC_GITHUB.equals(SPAppConfigs.getResourceDownloadSrc(ctx)))
                ? R.id.srcGithub
                : R.id.srcJsDelivr
        );

        params.setContentView(binding.getRoot());

        binding.downloadSrcGroup.setOnCheckChangedListener((button, checkedId) -> {
            final String downloadSrc;

            if (checkedId == R.id.srcGithub) {
                downloadSrc = DOWNLOAD_SRC_GITHUB;
            } else if (checkedId == R.id.srcJsDelivr) {
                downloadSrc = DOWNLOAD_SRC_JSDELIVR;
            } else {
                downloadSrc = DOWNLOAD_SRC_DEFAULT;
            }

            SPAppConfigs.setResourceDownloadSrc(ctx, downloadSrc);
            DownloadSourceUtils.resetDownloadSourceBaseUrl(ctx);

            sheetDialog.dismiss();

            return Unit.INSTANCE;
        });

        sheetDialog.setOnDismissListener(() -> setupDownloadSrcTitle(parentBinding));
        sheetDialog.show(getParentFragmentManager());
    }

    private void setupDownloadSrcTitle(LytReaderSettingsItemBinding binding) {
        final String selectedSource = DownloadSourceUtils.getCurrentSourceName(
            binding.getRoot().getContext()
        );
        prepareTitle(binding.launcher, R.string.titleResourceDownloadSource, selectedSource);
    }

    private void initLogLauncher(LinearLayout parent) {
        Context ctx = parent.getContext();

        LytReaderSettingsItemBinding logExplorerBinding = LytReaderSettingsItemBinding.inflate(mInflater, parent,
            false);

        setupLauncherParamsAndIcon(R.drawable.icon_log, logExplorerBinding);
        prepareTitle(
            logExplorerBinding.launcher,
            R.string.appLogs,
            TextUtils.concat(ctx.getString(R.string.crashLogs), ", ", ctx.getString(R.string.suppressedLogs)).toString()
        );

        logExplorerBinding.launcher.setOnClickListener(v -> launchFrag(FragSettingsAppLogs.class, null));

        parent.addView(logExplorerBinding.getRoot());
    }

    private void resetToDefault(Context ctx) {
        SPReader.setSavedTextSizeMultArabic(ctx, ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT);
        SPReader.setSavedTextSizeMultTransl(ctx, ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT);
        SPReader.setSavedTextSizeMultTafsir(ctx, ReaderTextSizeUtils.TEXT_SIZE_MULT_TAFSIR_DEFAULT);
        SPReader.setSavedScript(ctx, QuranScriptUtils.SCRIPT_DEFAULT);

        initTranslSlugs = TranslUtils.defaultTranslationSlugs().toArray(new String[0]);

        TafsirManager.prepare(ctx, false, () -> {
            Map<String, List<TafsirInfoModel>> tafsirModels = TafsirManager.getModels();

            if (tafsirModels != null) {
                for (List<TafsirInfoModel> tafsirList : tafsirModels.values()) {
                    for (TafsirInfoModel model : tafsirList) {
                        SPReader.setSavedRecitationSlug(ctx, model.getKey());
                        break;
                    }
                }

                setupTafsirTitle();
            }

            return Unit.INSTANCE;
        });

        RecitationManager.prepare(ctx, false, () -> {
            List<RecitationInfoModel> models = RecitationManager.getModels();

            if (models != null) {
                for (RecitationInfoModel model : models) {
                    if (model != null) {
                        SPReader.setSavedRecitationSlug(ctx, model.getSlug());
                        break;
                    }
                }

                setupRecitationTitle();
            }

            return Unit.INSTANCE;
        });

        RecitationManager.prepareTranslations(ctx, false, () -> {
            List<RecitationTranslationInfoModel> models = RecitationManager.getTranslationModels();

            if (models != null) {
                for (RecitationTranslationInfoModel model : models) {
                    if (model != null) {
                        SPReader.setSavedRecitationTranslationSlug(ctx, model.getSlug());
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

        demonstrateTextSizeArabic(ReaderTextSizeUtils.TEXT_SIZE_DEFAULT_PROGRESS);
        demonstrateTextSizeTransl(ReaderTextSizeUtils.TEXT_SIZE_DEFAULT_PROGRESS);
        setProgressAndTextArabic(ReaderTextSizeUtils.TEXT_SIZE_MULT_AR_DEFAULT);
        setProgressAndTextTransl(ReaderTextSizeUtils.TEXT_SIZE_MULT_TRANSL_DEFAULT);
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
        builder.setPositiveButton(R.string.strLabelReset, ContextKt.color(ctx, R.color.colorSecondary),
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
