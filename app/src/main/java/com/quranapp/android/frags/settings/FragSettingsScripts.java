/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.frags.settings;

import static com.quranapp.android.activities.ActivityReader.KEY_SCRIPT_CHANGED;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.databinding.LytSettingsScriptItemBinding;
import com.quranapp.android.utils.reader.ScriptUtils;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.views.BoldHeader;

public class FragSettingsScripts extends FragSettingsBase {
    private String initScript;

    @Override
    public String getFragTitle(Context ctx) {
        return ctx.getString(R.string.strTitleScripts);
    }

    @Override
    protected View getFragView(Context ctx) {
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        ViewUtils.setPaddingVertical(list, dp2px(ctx, 10), dimen(ctx, R.dimen.dmnPadHuge));
        return list;
    }

    @Override
    public int getLayoutResource() {
        return 0;
    }

    @Override
    public void setupHeader(ActivitySettings activity, BoldHeader header) {
        super.setupHeader(activity, header);
        header.setCallback(activity::onBackPressed);
        header.disableRightBtn(true);

        header.setShowSearchIcon(false);
        header.setShowRightIcon(false);
    }

    @Override
    protected boolean shouldCreateScroller() {
        return true;
    }

    @Override
    public void onViewReady(@NonNull Context ctx, @NonNull View view, @Nullable Bundle savedInstanceState) {
        initScript = SPReader.getSavedScript(ctx);

        makeScriptOptions((LinearLayout) view, initScript);
    }

    public void makeScriptOptions(LinearLayout list, String initScript) {
        Context ctx = list.getContext();
        String[] availableScriptSlugs = ScriptUtils.availableScriptSlugs();

        for (String slug : availableScriptSlugs) {
            LytSettingsScriptItemBinding binding = LytSettingsScriptItemBinding.inflate(LayoutInflater.from(ctx), list, false);

            binding.miniInfo.setVisibility(View.GONE);

            binding.radio.setText(ScriptUtils.getScriptName(slug));
            binding.radio.setChecked(slug.equals(initScript));

            binding.preview.setText(ScriptUtils.getScriptPreviewRes(slug));
            binding.preview.setTypeface(ResUtils.getFont(ctx, ScriptUtils.getScriptFontRes(slug)));
            binding.preview.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    ResUtils.getDimenPx(ctx, ScriptUtils.getScriptFontDimenRes(slug)));

            binding.getRoot().setOnClickListener(v -> onItemClick(list, binding, slug));

            list.addView(binding.getRoot());
        }
    }

    private void onItemClick(LinearLayout list, LytSettingsScriptItemBinding binding, String slug) {
        if (binding.radio.isChecked()) {
            return;
        }

        for (int i = 0, l2 = list.getChildCount(); i < l2; i++) {
            final View child = list.getChildAt(i);
            if (child != null) {
                RadioButton radio = child.findViewById(R.id.radio);
                radio.setChecked(false);
            }
        }

        binding.radio.setChecked(true);
        if (binding.radio.isChecked()) {
            SPReader.setSavedScript(list.getContext(), slug);
        }
    }

    @Override
    public Bundle getFinishingResult(Context ctx) {
        if (!SPReader.getSavedScript(ctx).equals(initScript)) {
            Bundle data = new Bundle();
            data.putBoolean(KEY_SCRIPT_CHANGED, true);
            return data;
        }
        return null;
    }
}
