/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.quranapp.android.activities.ActivityReader.KEY_RECITER_CHANGED;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.adapters.recitation.ADPRecitations;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.databinding.FragSettingsTranslBinding;
import com.quranapp.android.exc.NoInternetException;
import com.quranapp.android.interfaceUtils.RecitationExplorerImpl;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.recitation.LoadRecitationsTask;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.views.BoldHeader;
import com.quranapp.android.widgets.PageAlert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

public class FragSettingsRecitation extends FragSettingsBase implements RecitationExplorerImpl {
    private final CallableTaskRunner<String> mTaskRunner = new CallableTaskRunner<>();
    private String mSavedRecitation;
    private FragSettingsTranslBinding mBinding;
    private ADPRecitations mAdapter;
    private List<RecitationModel> mModels;
    private PageAlert mPageAlert;

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleRecitations);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.frag_settings_transl;
    }

    @Override
    public void onDestroy() {
        mTaskRunner.cancel();
        super.onDestroy();
    }

    @Override
    public Bundle getFinishingResult(Context ctx) {
        if (!SPReader.getSavedRecitationSlug(ctx).equals(mSavedRecitation)) {
            Bundle data = new Bundle();
            data.putBoolean(KEY_RECITER_CHANGED, true);
            return data;
        }
        return null;
    }

    @Override
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        super.setupHeader(activity, header);
        header.setCallback(new BoldHeader.BoldHeaderCallback() {
            @Override
            public void onBackIconClick() {
                activity.onBackPressed();
            }

            @Override
            public void onRightIconClick() {
                refresh(header.getContext(), true);
            }

            @Override
            public void onSearchRequest(EditText searchBox, CharSequence newText) {
                search(newText);
            }
        });

        header.setShowSearchIcon(false);
        header.setShowRightIcon(false);
        header.disableRightBtn(false);

        header.setSearchHint(R.string.strHintSearchReciter);
        header.setRightIconRes(R.drawable.dr_icon_refresh, activity.getString(R.string.strLabelRefresh));
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        mSavedRecitation = SPReader.getSavedRecitationSlug(ctx);
        mBinding = FragSettingsTranslBinding.bind(view);

        init(ctx);
    }

    private void init(Context ctx) {
        refresh(ctx, SPAppActions.getFetchRecitationsForce(ctx));
    }

    private void initPageAlert(Context ctx) {
        mPageAlert = new PageAlert(ctx);
    }

    private void refresh(Context ctx, boolean force) {
        if (!mTaskRunner.isDone()) {
            if (force) {
                mTaskRunner.cancel();
            } else {
                return;
            }
        }

        LoadRecitationsTask2 loadManifestTask = new LoadRecitationsTask2(ctx, force);
        if (force) {
            if (!NetworkStateReceiver.isNetworkConnected(ctx)) {
                noInternet(ctx);
                return;
            }

            showLoader();
            RecitationUtils.prepareRecitationsManifestUrlFB(uri -> {
                loadManifestTask.setUrl(uri.toString());
                mTaskRunner.callAsync(loadManifestTask);
            }, loadManifestTask::onFailed);
        } else {
            mTaskRunner.callAsync(loadManifestTask);
        }
    }

    private void search(CharSequence query) {
        if (mAdapter == null || mModels == null) return;

        if (TextUtils.isEmpty(query)) {
            if (mAdapter.getItemCount() != mModels.size()) {
                resetAdapter(mModels);
            }
            return;
        }

        Pattern pattern = Pattern.compile(StringUtils.escapeRegex(String.valueOf(query)), CASE_INSENSITIVE | DOTALL);

        List<RecitationModel> found = new ArrayList<>();
        for (RecitationModel model : mModels) {
            String reciter = model.getReciter();

            if (TextUtils.isEmpty(reciter)) {
                continue;
            }

            if (pattern.matcher(reciter).find()) {
                found.add(model);
            }
        }

        resetAdapter(found);
    }

    private void populateTransls(Context ctx, List<RecitationModel> models) {
        mModels = models;

        LinearLayoutManager layoutManager = new LinearLayoutManager(ctx);
        mBinding.list.setLayoutManager(layoutManager);

        RecyclerView.ItemAnimator itemAnimator = mBinding.list.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        mAdapter = new ADPRecitations();
        resetAdapter(models);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(true);
            header.setShowRightIcon(true);
        }
    }

    private void resetAdapter(List<RecitationModel> models) {
        mAdapter.setModels(models);
        mBinding.list.setAdapter(mAdapter);
    }

    @Override
    public String getSavedReciter() {
        return mSavedRecitation;
    }

    @Override
    public void setSavedReciter(String slug) {
        mSavedRecitation = slug;
    }

    private void noRecitersAvailable(Context ctx) {
        showAlert(ctx, 0, R.string.strMsgRecitationsNoAvailable, R.string.strLabelRefresh,
                () -> refresh(ctx, true));
    }

    private void showLoader() {
        hideAlert();
        mBinding.loader.setVisibility(VISIBLE);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowRightIcon(false);
            header.setShowSearchIcon(false);
        }
    }

    private void hideLoader() {
        mBinding.loader.setVisibility(GONE);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowRightIcon(true);
            header.setShowSearchIcon(true);
        }
    }

    private void showAlert(Context ctx, int titleRes, int msgRes, int btnRes, Runnable action) {
        hideLoader();

        if (mPageAlert == null) {
            initPageAlert(ctx);
        }

        mPageAlert.setIcon(null);
        mPageAlert.setMessage(titleRes > 0 ? ctx.getString(titleRes) : "", ctx.getString(msgRes));
        mPageAlert.setActionButton(btnRes, action);
        mPageAlert.show(mBinding.container);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(false);
            header.setShowRightIcon(true);
        }
    }

    private void hideAlert() {
        if (mPageAlert == null) {
            return;
        }
        mPageAlert.remove();
    }

    private void noInternet(Context ctx) {
        if (mPageAlert == null) {
            initPageAlert(ctx);
        }
        mPageAlert.setupForNoInternet(() -> refresh(ctx, true));
        mPageAlert.show(mBinding.container);

        if (getActivity() instanceof ActivitySettings) {
            BoldHeader header = ((ActivitySettings) getActivity()).getHeader();
            header.setShowSearchIcon(false);
            header.setShowRightIcon(true);
        }
    }

    private class LoadRecitationsTask2 extends LoadRecitationsTask {
        public LoadRecitationsTask2(Context ctx, boolean force) {
            super(ctx, FragSettingsRecitation.this, force);
        }

        @Override
        public void preExecute() {
            showLoader();
        }

        @Override
        protected void onComplete(@NonNull List<RecitationModel> items) {
            if (items.size() > 0) {
                populateTransls(getContext(), items);
            } else {
                noRecitersAvailable(getContext());
            }
            SPAppActions.setFetchRecitationsForce(getContext(), false);
            hideLoader();
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            super.onFailed(e);
            if (e instanceof CancellationException) {
                hideLoader();
                return;
            } else if (e instanceof NoInternetException || e.getCause() instanceof NoInternetException) {
                noInternet(getContext());
                return;
            }

            if (e.getMessage() != null && e.getMessage().contains(REQUIRE_RECITATION_FORCE_LOAD)) {
                refresh(getContext(), true);
            } else {
                showAlert(getContext(), R.string.strTitleOops, R.string.strMsgRecitationsLoadFailed, R.string.strLabelRetry,
                        () -> refresh(getContext(), true));
            }
        }
    }
}
