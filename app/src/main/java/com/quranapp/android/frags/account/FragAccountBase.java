/*
 * (c) Faisal Khan. Created on 27/1/2022.
 */

package com.quranapp.android.frags.account;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.account.ActivityAccount;
import com.quranapp.android.frags.BaseFragment;
import com.peacedesign.android.utils.span.TypefaceSpan2;

public abstract class FragAccountBase extends BaseFragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = inflater.getContext();

        NestedScrollView scrollView = new NestedScrollView(ctx);
        scrollView.setFadingEdgeLength(30);
        scrollView.setVerticalFadingEdgeEnabled(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        ViewUtils.setPaddingBottom(wrapper, ResUtils.getDimenPx(ctx, R.dimen.dmnPadBig));

        View mainContent = inflater.inflate(getLayoutResource(), container, false);

        wrapper.addView(mainContent);
        scrollView.addView(wrapper, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return scrollView;
    }

    protected abstract int getLayoutResource();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context ctx = view.getContext();
        View mainContent = view.findViewById(R.id.mainContent);
        prepareView(ctx, mainContent);
        onAccountViewReady(ctx, mainContent);
    }

    protected abstract void onAccountViewReady(Context ctx, View mainContent);

    private void prepareView(Context ctx, View mainContent) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Dimen.dp2px(ctx, 320), WRAP_CONTENT);
        mainContent.setLayoutParams(lp);
        ViewUtils.setPaddings(mainContent, Dimen.dp2px(ctx, 20));
    }

    protected SpannableString createLink(Context ctx, String str) {
        int colorPrimary = ContextCompat.getColor(ctx, R.color.colorPrimary);
        SpannableString spannable = new SpannableString(str);
        int flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        spannable.setSpan(new TypefaceSpan2(Typeface.DEFAULT_BOLD), 0, spannable.length(), flag);
        spannable.setSpan(new ForegroundColorSpan(colorPrimary), 0, spannable.length(), flag);
        return spannable;
    }

    protected String trim(CharSequence sequence) {
        return sequence.toString().trim();
    }

    protected ActivityAccount activity() {
        if (getActivity() instanceof ActivityAccount) {
            return (ActivityAccount) getActivity();
        }

        if (getContext() instanceof ActivityAccount) {
            return (ActivityAccount) getContext();
        }
        return null;
    }
}
