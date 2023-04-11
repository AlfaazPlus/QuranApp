/*
 * (c) Faisal Khan. Created on 20/9/2021.
 */

package com.quranapp.android.frags.editshare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.quranapp.android.adapters.editor.ADPEditorBG;
import com.quranapp.android.components.editor.EditorBG;
import com.quranapp.android.components.editor.VerseEditor;
import com.quranapp.android.utils.extended.GapedItemDecoration;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.extensions.ViewPaddingKt;

import java.io.InputStream;

public class FragEditorBG extends FragEditorBase {
    private ADPEditorBG mAdapter;
    private RecyclerView mRecyclerView;

    public static FragEditorBG newInstance() {
        return new FragEditorBG();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = inflater.getContext();
        RecyclerView view = new RecyclerView(ctx);
        ViewPaddingKt.updatePaddingHorizontal(view, ContextKt.dp2px(ctx, 8));
        ViewPaddingKt.updatePaddingVertical(view, ContextKt.dp2px(ctx, 10));
        view.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view;
        if (mVerseEditor != null) {
            initializeBGs(mVerseEditor);
        }
    }

    private void initializeBGs(VerseEditor editor) {
        mAdapter = new ADPEditorBG(this, editor);

        GridLayoutManager lm = new GridLayoutManager(getContext(), 5);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.addItemDecoration(new GapedItemDecoration(ContextKt.dp2px(mRecyclerView.getContext(), 3)));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(null);

        mRecyclerView.post(() -> mAdapter.select(1));
    }

    @Override
    protected void onActivityResult2(@NonNull ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK) {
            return;
        }

        Intent data = result.getData();
        if (data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    FragmentActivity activity = requireActivity();

                    InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);

                    mVerseEditor.getBGs().add(1, EditorBG.createWith(drawable));
                    mRecyclerView.post(() -> mAdapter.selectNew(1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
