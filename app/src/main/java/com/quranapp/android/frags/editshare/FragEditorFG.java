/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.adapters.editor.ADPEditorFG;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.utils.extended.GapedItemDecoration;

public class FragEditorFG extends FragEditorBase {
    private ADPEditorFG mAdapter;
    private RecyclerView mRecyclerView;

    public static FragEditorFG newInstance() {
        return new FragEditorFG();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = inflater.getContext();
        RecyclerView view = new RecyclerView(ctx);
        ViewUtils.setPaddingHorizontal(view, dp2px(ctx, 8));
        ViewUtils.setPaddingVertical(view, dp2px(ctx, 10));
        view.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view;
        if (mAdapter == null && mVerseEditor != null) {
            initializeBGs(mVerseEditor);
        }
    }

    private void initializeBGs(VerseEditor editor) {
        mAdapter = new ADPEditorFG(editor);

        GridLayoutManager lm = new GridLayoutManager(getContext(), 5);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.addItemDecoration(new GapedItemDecoration(dp2px(mRecyclerView.getContext(), 3)));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(null);
    }

    public int getSelectedItemPos() {
        if (mAdapter == null) {
            return -1;
        }
        return mAdapter.getSelectedItemPos();
    }

    public void select(int index) {
        if (mAdapter == null) {
            return;
        }

        mAdapter.select(index);
    }
}
