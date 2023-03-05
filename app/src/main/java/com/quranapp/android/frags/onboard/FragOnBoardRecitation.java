/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 31/3/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.onboard;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import com.quranapp.android.utils.app.RecitationManager;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sharedPrefs.SPAppActions;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.widgets.PageAlert;

import java.util.List;

import kotlin.Unit;

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
        if (force && !NetworkStateReceiver.isNetworkConnected(list.getContext())) {
            noInternet();
            return;
        }

        showLoader();

        RecitationManager.prepare(list.getContext(), force, () -> {
            List<RecitationModel> models = RecitationManager.getModels();

            if (models != null && models.size() > 0) {
                populateRecitation(list, models);
            } else {
                noRecitersAvailable();
            }

            SPAppActions.setFetchRecitationsForce(list.getContext(), false);
            hideLoader();
            return Unit.INSTANCE;
        });
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
    }

    private void noInternet() {
        mPageAlert.setupForNoInternet(() -> refresh(mBinding.list, true));
        mPageAlert.show(mBinding.getRoot());
    }
}
