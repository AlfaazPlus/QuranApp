package com.quranapp.android.vh.search;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.search.SearchResultModelBase;

public class VHSearchResultBase extends RecyclerView.ViewHolder {
    public VHSearchResultBase(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(SearchResultModelBase model, int pos) {
    }

    protected void setupJumperView(View view, boolean applyMargins) {
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
        if (applyMargins) {
            ViewUtils.setMarginHorizontal(params, Dimen.dp2px(view.getContext(), 10));
            params.bottomMargin = Dimen.dp2px(view.getContext(), 10);
        }

        view.setLayoutParams(params);
        view.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered_onlylight);
    }
}
