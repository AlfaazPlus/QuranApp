/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 31/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.onboard;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.quranapp.android.utils.univ.ExceptionCause.REQUIRE_RECITATION_FORCE_LOAD;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.quranapp.android.R;
import com.quranapp.android.adapters.recitation.ADPRecitations;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.databinding.LytOnboardRecitationsBinding;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.recitation.LoadRecitationsTask;
import com.quranapp.android.widgets.PageAlert;

import java.util.List;

public class FragOnBoardRecitation extends FragOnBoardBase {
    private final CallableTaskRunner<String> mRecitationTaskRunner = new CallableTaskRunner<>();
    private LytOnboardRecitationsBinding mBinding;
    private PageAlert mPageAlert;

    public static FragOnBoardRecitation newInstance() {
        return new FragOnBoardRecitation();
    }

    @Override
    public void onDestroy() {
        mRecitationTaskRunner.cancel();
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LytOnboardRecitationsBinding.inflate(inflater, container, false).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding = LytOnboardRecitationsBinding.bind(view);

        initPageAlert(view.getContext());
        initRecitations(mBinding.list);
    }

    private void initRecitations(RecyclerView list) {
        list.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        list.setLayoutManager(new LinearLayoutManager(list.getContext()));

        RecyclerView.ItemAnimator itemAnimator = list.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        refresh(list, false);
    }

    private void populateRecitation(RecyclerView list, List<RecitationModel> items) {
        ADPRecitations adapter = new ADPRecitations();
        adapter.setModels(items);
        list.setAdapter(adapter);
    }

    private void refresh(RecyclerView list, boolean force) {
        LoadRecitationsTaskOnBoard loadManifestTask = new LoadRecitationsTaskOnBoard(list, force);
        if (force) {
            if (!NetworkStateReceiver.isNetworkConnected(list.getContext())) {
                noInternet();
                return;
            }

            showLoader();
            RecitationUtils.prepareRecitationsManifestUrlFB(uri -> {
                loadManifestTask.setUrl(uri.toString());
                mRecitationTaskRunner.callAsync(loadManifestTask);
            }, loadManifestTask::onFailed);
        } else {
            mRecitationTaskRunner.callAsync(loadManifestTask);
        }
    }

    private void initPageAlert(Context ctx) {
        mPageAlert = new PageAlert(ctx);
    }

    private void noRecitersAvailable() {
        showAlert(R.string.strMsgRecitationsNoAvailable, R.string.strLabelRefresh,
                () -> refresh(mBinding.list, true));
    }

    private void showLoader() {
        hideAlert();
        mBinding.loader.setVisibility(VISIBLE);
    }

    private void hideLoader() {
        mBinding.loader.setVisibility(GONE);
    }

    private void showAlert(int msgRes, int btnRes, Runnable action) {
        hideLoader();

        mPageAlert.setIcon(null);
        mPageAlert.setMessage("", mBinding.getRoot().getContext().getString(msgRes));
        mPageAlert.setActionButton(btnRes, action);
        mPageAlert.show(mBinding.getRoot());
    }

    private void hideAlert() {
        mPageAlert.remove();
        //        mBinding.pageAlert.getRoot().setVisibility(GONE);
    }

    private void noInternet() {
        mPageAlert.setupForNoInternet(() -> refresh(mBinding.list, true));
        mPageAlert.show(mBinding.getRoot());
    }

    private class LoadRecitationsTaskOnBoard extends LoadRecitationsTask {
        private final RecyclerView mList;

        public LoadRecitationsTaskOnBoard(RecyclerView list, boolean force) {
            super(list.getContext(), null, force);
            mList = list;
        }

        @Override
        public void preExecute() {
            showLoader();
        }

        @Override
        protected void onComplete(@NonNull List<RecitationModel> items) {
            if (items.size() > 0) {
                populateRecitation(mList, items);
            } else {
                noRecitersAvailable();
            }
            SPAppActions.setFetchRecitationsForce(mList.getContext(), false);
            hideLoader();
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            super.onFailed(e);
            if (e.getMessage() != null && e.getMessage().contains(REQUIRE_RECITATION_FORCE_LOAD)) {
                refresh(mList, true);
            } else {
                hideLoader();
            }
        }
    }
}
