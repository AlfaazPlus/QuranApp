/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 3/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.activities.readerSettings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import static com.quranapp.android.utils.univ.Codes.SETTINGS_LAUNCHER_RESULT_CODE;

import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.databinding.ActivitySettingsBinding;
import com.quranapp.android.frags.settings.FragSettingsBase;
import com.quranapp.android.frags.settings.FragSettingsLanguage;
import com.quranapp.android.frags.settings.FragSettingsMain;
import com.quranapp.android.frags.settings.FragSettingsScripts;
import com.quranapp.android.frags.settings.FragSettingsTafsirs;
import com.quranapp.android.frags.settings.FragSettingsTransl;
import com.quranapp.android.frags.settings.FragSettingsTranslationsDownload;
import com.quranapp.android.frags.settings.recitations.FragSettingsRecitations;
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudio;
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudioReciter;
import com.quranapp.android.views.BoldHeader;

import java.util.Objects;

public class ActivitySettings extends BaseActivity {
    public static final String KEY_SETTINGS_DESTINATION = "key.settings_destination";
    public static final int SETTINGS_LANG = 0x1;
    public static final int SETTINGS_THEME = 0x2;
    public static final int SETTINGS_VOTD = 0x3;
    public static final int SETTINGS_TRANSLATION = 0x4;
    public static final int SETTINGS_TRANSLATION_DOWNLOAD = 0x5;
    public static final int SETTINGS_TAFSIR = 0x6;
    public static final int SETTINGS_SCRIPT = 0x7;
    public static final int SETTINGS_RECITER = 0x8;
    public static final int SETTINGS_MANAGE_AUDIO = 0x9;
    public static final int SETTINGS_MANAGE_AUDIO_RECITER = 0x10;

    private ActivitySettingsBinding mBinding;

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        if (!mBinding.header.checkSearchShown()) {
            beforeFinish();
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        beforeFinish();
        super.onPause();
    }

    private void beforeFinish() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frags_container);
        if (f instanceof FragSettingsBase) {
            Intent intent = new Intent();
            Bundle finishingResult = ((FragSettingsBase) f).getFinishingResult(this);
            if (finishingResult != null) {
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
        final Class<? extends FragSettingsBase> destFrag;
        final int destination = intent.getIntExtra(KEY_SETTINGS_DESTINATION, -1);
        switch (destination) {
            case SETTINGS_LANG:
                destFrag = FragSettingsLanguage.class;
                break;
            case SETTINGS_TRANSLATION:
                destFrag = FragSettingsTransl.class;
                break;
            case SETTINGS_TRANSLATION_DOWNLOAD:
                destFrag = FragSettingsTranslationsDownload.class;
                break;
            case SETTINGS_TAFSIR:
                destFrag = FragSettingsTafsirs.class;
                break;
            case SETTINGS_SCRIPT:
                destFrag = FragSettingsScripts.class;
                break;
            case SETTINGS_RECITER:
                destFrag = FragSettingsRecitations.class;
                break;
            case SETTINGS_MANAGE_AUDIO:
                destFrag = FragSettingsManageAudio.class;
                break;
            case SETTINGS_MANAGE_AUDIO_RECITER:
                destFrag = FragSettingsManageAudioReciter.class;
                break;
            case SETTINGS_THEME:
            case SETTINGS_VOTD:
            default:
                destFrag = FragSettingsMain.class;
                break;
        }
        return destFrag;
    }

    private void init() {
        initHeader(mBinding.header);

        getSupportFragmentManager().addOnBackStackChangedListener(this::onFragChanged);
    }

    private void onFragChanged() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frags_container);
        if (f instanceof FragSettingsBase) {
            FragSettingsBase frag = (FragSettingsBase) f;
            frag.setupHeader(this, mBinding.header);
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
