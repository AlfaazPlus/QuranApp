/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import static com.quranapp.android.utils.univ.Codes.SETTINGS_LAUNCHER_RESULT_CODE;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_SAVE_TRANSL_CHANGES;
import static com.quranapp.android.utils.univ.Keys.READER_KEY_TRANSL_SLUGS;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.adapters.transl.ADPTransls;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.transls.TranslBaseModel;
import com.quranapp.android.components.transls.TranslModel;
import com.quranapp.android.components.transls.TranslTitleModel;
import com.quranapp.android.databinding.FragSettingsTranslBinding;
import com.quranapp.android.interfaceUtils.OnTranslSelectionChangeListener;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.PageAlert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class FragSettingsTransl extends FragSettingsBase implements OnTranslSelectionChangeListener<TranslModel> {
    private final CallableTaskRunner<List<TranslBaseModel>> mTaskRunner = new CallableTaskRunner<>();

    private FragSettingsTranslBinding mBinding;
    private ADPTransls mAdapter;
    private PageAlert mPageAlert;

    private boolean saveTranslChanges;

    private Set<String> mTranslSlugs;

    public FileUtils mFileUtils;

    @Override
    public void onDestroy() {
        mTaskRunner.cancel();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        beforeFinish();
        super.onPause();
    }

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleTranslations);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.frag_settings_transl;
    }

    @Override
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        super.setupHeader(activity, header);

        header.setCallback(new BoldHeader.BoldHeaderCallback() {
            @Override
            public void onBackIconClick() {
                activity.getOnBackPressedDispatcher().onBackPressed();
            }

            @Override
            public void onRightIconClick() {
                launchFrag(FragSettingsTranslationsDownload.class, null);
            }

            @Override
            public void onSearchRequest(EditText searchBox, CharSequence newText) {
                search(newText);
            }
        });

        header.setShowSearchIcon(false);
        header.setShowRightIcon(true);
        header.disableRightBtn(false);

        header.setSearchHint(R.string.strHintSearchTranslation);
        header.setRightIconRes(R.drawable.dr_icon_download, activity.getString(R.string.strTitleDownloadTranslations));
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        mFileUtils = FileUtils.newInstance(ctx);
        mBinding = FragSettingsTranslBinding.bind(view);

        final Bundle args = getArgs();
        final String[] requestedTranslSlugs = args.getStringArray(READER_KEY_TRANSL_SLUGS);
        if (requestedTranslSlugs == null) {
            mTranslSlugs = SPReader.getSavedTranslations(ctx);
        } else {
            mTranslSlugs = new TreeSet<>(Arrays.asList(requestedTranslSlugs));
        }

        saveTranslChanges = args.getBoolean(READER_KEY_SAVE_TRANSL_CHANGES, true);

        initTranslations(ctx);
    }

    private void initPageAlert(Context ctx) {
        mPageAlert = new PageAlert(ctx);
        mPageAlert.setMessage(ctx.getString(R.string.strMsgTranslNoDownloads), null);
        mPageAlert.setActionButton(R.string.strTitleDownloadTranslations,
                () -> launchFrag(FragSettingsTranslationsDownload.class, null));
    }

    private void initTranslations(Context ctx) {
        mBinding.list.setLayoutManager(new LinearLayoutManager(ctx));
        refreshTransls(ctx);
    }

    private void refreshTransls(Context ctx) {
        mTaskRunner.callAsync(new LoadTranslsTask(mFileUtils, mTranslSlugs) {
            @Override
            public void preExecute() {
                super.preExecute();
                if (getActivity() instanceof ActivitySettings) {
                    mBinding.loader.setVisibility(VISIBLE);
                    ((ActivitySettings) getActivity()).getHeader().setShowSearchIcon(false);
                }

                if (mPageAlert != null) {
                    mPageAlert.remove();
                }
            }

            @Override
            public void onComplete(List<TranslBaseModel> translItems) {
                if (!translItems.isEmpty()) {
                    populateTransls(ctx, translItems);
                } else {
                    if (mPageAlert == null) {
                        initPageAlert(ctx);
                    }
                    mPageAlert.show(mBinding.container);
                }
            }

            @Override
            public void postExecute() {
                super.postExecute();
                mBinding.loader.setVisibility(GONE);
            }
        });
    }

    private void populateTransls(Context ctx, List<TranslBaseModel> translItems) {
        if (mAdapter != null && mAdapter.getStoredModels().equals(translItems)) {
            return;
        }

        mAdapter = new ADPTransls(ctx, translItems, this);
        mBinding.list.setAdapter(mAdapter);

        if (getActivity() instanceof ActivitySettings) {
            ((ActivitySettings) getActivity()).getHeader().setShowSearchIcon(true);
        }
    }

    private void search(CharSequence query) {
        if (mAdapter == null) return;

        List<TranslBaseModel> storedModels = mAdapter.getStoredModels();
        if (TextUtils.isEmpty(query)) {
            if (mAdapter.getItemCount() != mAdapter.getStoredItemCount()) {
                mAdapter.setModels(storedModels);
                mBinding.list.setAdapter(mAdapter);
            }
            return;
        }

        Pattern pattern = Pattern.compile(StringUtils.escapeRegex(String.valueOf(query)), CASE_INSENSITIVE | DOTALL);

        List<TranslBaseModel> found = new ArrayList<>();
        for (TranslBaseModel model : storedModels) {
            if (model instanceof TranslTitleModel) {
                found.add(model);
            }

            if (!(model instanceof TranslModel)) {
                continue;
            }

            QuranTranslBookInfo bookInfo = ((TranslModel) model).getBookInfo();

            String bookName = bookInfo.getBookName();
            String authorName = bookInfo.getAuthorName();
            String langName = bookInfo.getLangName();


            if (TextUtils.isEmpty(bookName) && TextUtils.isEmpty(authorName) && TextUtils.isEmpty(langName)) {
                continue;
            }

            if (pattern.matcher(bookName + authorName + langName).find()) {
                found.add(model);
            }
        }

        mAdapter.setModels(found);
        mBinding.list.setAdapter(mAdapter);
    }

    @Override
    public boolean onSelectionChanged(Context ctx, TranslModel translModel, boolean isSelected) {
        boolean succeed = TranslUtils.resolveSelectionChange(ctx, mTranslSlugs, translModel, isSelected,
                saveTranslChanges);
        if (succeed) {
            // Update the args so that it can reflect when this fragment is recreated.
            Bundle args = getArgs();
            args.putStringArray(READER_KEY_TRANSL_SLUGS, mTranslSlugs.toArray(new String[0]));
            setArguments(args);
        }

        return succeed;
    }

    @Override
    public Bundle getFinishingResult(Context ctx) {
        if (mTranslSlugs == null) {
            return null;
        }

        Bundle data = new Bundle();
        data.putStringArray(READER_KEY_TRANSL_SLUGS, mTranslSlugs.toArray(new String[0]));
        return data;
    }

    private void beforeFinish() {
        if (mTranslSlugs == null || getContext() == null) {
            return;
        }

        getParentFragmentManager().setFragmentResult(
                String.valueOf(SETTINGS_LAUNCHER_RESULT_CODE),
                getFinishingResult(getContext())
        );
    }

    public abstract static class LoadTranslsTask extends BaseCallableTask<List<TranslBaseModel>> {
        private final FileUtils mFileUtils;
        private final Set<String> mTranslSlugs;
        private QuranTranslationFactory mTranslFactory;

        public LoadTranslsTask(FileUtils fileUtils, Set<String> translSlugs) {
            mFileUtils = fileUtils;
            mTranslSlugs = translSlugs;
        }

        @CallSuper
        @Override
        public void preExecute() {
            mTranslFactory = new QuranTranslationFactory(mFileUtils.getContext());
        }

        @CallSuper
        @Override
        public void postExecute() {
            if (mTranslFactory != null) {
                mTranslFactory.close();
            }
        }

        @Override
        public List<TranslBaseModel> call() throws Exception {
            return getTranslationsFromDatabase();
        }

        private List<TranslBaseModel> getTranslationsFromDatabase() {
            List<TranslBaseModel> translItems = new ArrayList<>();

            Map<String, List<QuranTranslBookInfo>> languageAndInfo = new HashMap<>();
            for (QuranTranslBookInfo bookInfo : mTranslFactory.getAvailableTranslationBooksInfo().values()) {
                List<QuranTranslBookInfo> listOfLang = languageAndInfo.get(bookInfo.getLangCode());

                if (listOfLang == null) {
                    listOfLang = new ArrayList<>();
                    languageAndInfo.put(bookInfo.getLangCode(), listOfLang);
                }

                listOfLang.add(bookInfo);
            }

            languageAndInfo.forEach((langCode, listOfBooks) -> {
                TranslTitleModel translTitleModel = new TranslTitleModel(langCode, null);
                translItems.add(translTitleModel);

                for (QuranTranslBookInfo book : listOfBooks) {
                    TranslModel model = new TranslModel(book);
                    model.setChecked(isSelected(book.getSlug()));
                    translTitleModel.setLangName(book.getLangName());
                    translItems.add(model);
                }
            });

            return translItems;
        }

        public boolean isSelected(String translSlug) {
            return mTranslSlugs.contains(translSlug);
        }
    }
}
