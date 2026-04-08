/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.activities.readerSettings;

import static com.quranapp.android.utils.univ.Codes.SETTINGS_LAUNCHER_RESULT_CODE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.databinding.ActivitySettingsBinding;
import com.quranapp.android.frags.settings.FragSettingsBase;
import com.quranapp.android.views.BoldHeader;

import java.util.Objects;

public class ActivitySettingsOld extends BaseActivity {
    public static final String KEY_SETTINGS_DESTINATION = "key.settings_destination";
    public static final int SETTINGS_SCRIPT = 0x7;

    private ActivitySettingsBinding mBinding;

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    private void beforeFinish() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frags_container);
        if (f instanceof FragSettingsBase) {
            Bundle finishingResult = ((FragSettingsBase) f).getFinishingResult(this);

            if (finishingResult != null) {
                Intent intent = new Intent();
                intent.putExtras(finishingResult);
                setResult(SETTINGS_LAUNCHER_RESULT_CODE, intent);
            }
        }
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_settings;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frags_container);

        if (f instanceof FragSettingsBase) {
            if (!Objects.equals(f.getClass().getSimpleName(), resolveInitialFrag(intent).getSimpleName())) {
                initIntent(intent, true);
            } else {
                Bundle args = intent.getExtras();
                
                if (args != null) {
                    ((FragSettingsBase) f).onNewArguments(args);
                }
            }
        }
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivitySettingsBinding.bind(activityView);
        if (savedInstanceState == null) {
            initIntent(getIntent(), false);
        } else {
            onFragChanged();
        }
        init();
    }

    private void initIntent(Intent intent, boolean isNewIntent) {
        initFrag(resolveInitialFrag(intent), intent.getExtras(), isNewIntent);
    }

    private void initFrag(Class<? extends FragSettingsBase> frag, Bundle args, boolean isNewIntent) {
        if (frag == null) return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();
        t.add(R.id.frags_container, frag, args, frag.getSimpleName());
        t.setReorderingAllowed(true);
        if (isNewIntent) {
            t.addToBackStack(frag.getSimpleName());
        } else {
            t.runOnCommit(this::onFragChanged);
        }
        t.commit();
    }

    private Class<? extends FragSettingsBase> resolveInitialFrag(Intent intent) {
        Class<? extends FragSettingsBase> destFrag = null;
        final int destination = intent.getIntExtra(KEY_SETTINGS_DESTINATION, -1);


        return destFrag;
    }

    private void init() {
        initHeader(mBinding.header);

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(this::onFragChanged);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!mBinding.header.checkSearchShown()) {

                    if (fm.getBackStackEntryCount() == 0) {
                        beforeFinish();
                    }

                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void onFragChanged() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frags_container);
        if (f instanceof FragSettingsBase) {
            FragSettingsBase frag = (FragSettingsBase) f;
//            frag.setupHeader(this, mBinding.header);
            mBinding.getRoot().setBackgroundColor(frag.getPageBackgroundColor(this));
        }
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(this::onBackPressed);
        header.setBGColor(R.color.colorBGPage);
    }

    public BoldHeader getHeader() {
        return mBinding.header;
    }
}
