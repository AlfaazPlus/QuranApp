package com.quranapp.android.frags.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.FragMainBinding;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.utils.app.UpdateManager;
import com.quranapp.android.views.VOTDView;
import com.quranapp.android.views.homepage.FeatureProphetsLayout;
import com.quranapp.android.views.homepage.FeatureReadingLayout;
import com.quranapp.android.views.homepage.FeatureTopicsLayout;
import com.quranapp.android.views.homepage.FeaturedDuaLayout;
import com.quranapp.android.views.homepage.QuranEtiquetteLayout;
import com.quranapp.android.views.homepage.ReadHistoryLayout;
import com.quranapp.android.views.homepage.QuranSolutionVersesLayout;

public class FragMain extends BaseFragment {
    private FragMainBinding mBinding;
    private AsyncLayoutInflater mAsyncInflater;
    private ReadHistoryLayout mReadHistoryLayout;
    private VOTDView mVotdView;
    private UpdateManager mUpdateManager;

    public FragMain() {
    }

    @Override
    public boolean networkReceiverRegistrable() {
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUpdateManager != null) {
            mUpdateManager.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUpdateManager != null) {
            mUpdateManager.onResume();
        }

        Context context = getContext();
        if (context == null) {
            return;
        }

        QuranMeta.prepareInstance(context, quranMeta -> {
            if (mVotdView != null) {
                mVotdView.post(() -> mVotdView.refresh(quranMeta));
            }
            if (mReadHistoryLayout != null) {
                mReadHistoryLayout.post(() -> mReadHistoryLayout.refresh(quranMeta));
            }
        });
    }

    @Override
    public void onDestroy() {
        if (mVotdView != null) {
            mVotdView.destroy();
        }

        if (mReadHistoryLayout != null) {
            mReadHistoryLayout.destroy();
        }

        super.onDestroy();
    }

    public static FragMain newInstance() {
        return new FragMain();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAsyncInflater = new AsyncLayoutInflater(inflater.getContext());

        mBinding = FragMainBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mBinding == null) {
            mBinding = FragMainBinding.bind(view);
        }


        mUpdateManager = new UpdateManager(view.getContext(), mBinding.appUpdateContainer);
        // If update is not critical, proceed to load the rest of the content
        if (!mUpdateManager.check4Update()) {
            QuranMeta.prepareInstance(view.getContext(), quranMeta -> initContent(view, quranMeta));
        }
    }

    private void initContent(View root, QuranMeta quranMeta) {
        initVOTD(root, quranMeta);
        //        initReadHistory(root, quranMeta);
    }

    private void initVOTD(View root, QuranMeta quranMeta) {
        mVotdView = new VOTDView(root.getContext());
        mVotdView.setId(R.id.homepageVOTD);
        mBinding.container.addView(mVotdView, resolvePosReadHistory(root));
        mVotdView.refresh(quranMeta);
        mVotdView.post(() -> initReadHistory(root, quranMeta));
    }

    private void initReadHistory(View root, QuranMeta quranMeta) {
        mReadHistoryLayout = new ReadHistoryLayout(root.getContext());
        mReadHistoryLayout.setId(R.id.homepageReadHistoryLayout);
        mBinding.container.addView(mReadHistoryLayout, resolvePosReadHistory(root));
        mReadHistoryLayout.post(() -> {
            mReadHistoryLayout.refresh(quranMeta);
            initFeaturedReading(root, quranMeta);
        });
    }

    private void initFeaturedReading(View root, QuranMeta quranMeta) {
        FeatureReadingLayout readingLayout = new FeatureReadingLayout(root.getContext());
        readingLayout.setId(R.id.homepageReadingLayout);
        mBinding.container.addView(readingLayout, resolvePosFeaturedReading(root));
        readingLayout.post(() -> {
            readingLayout.refresh(quranMeta);
            initFeaturedDua(root, quranMeta);
        });
    }

    private void initFeaturedDua(View root, QuranMeta quranMeta) {
        FeaturedDuaLayout duaLayout = new FeaturedDuaLayout(root.getContext());
        duaLayout.setId(R.id.homepageDuaLayout);
        mBinding.container.addView(duaLayout, resolvePosFeaturedDua(root));
        duaLayout.post(() -> {
            duaLayout.refresh(quranMeta);
            initSolutionVerses(root, quranMeta);
        });
    }

    private void initSolutionVerses(View root, QuranMeta quranMeta) {
        QuranSolutionVersesLayout layout = new QuranSolutionVersesLayout(root.getContext());
        layout.setId(R.id.homepageSituationVersesLayout);
        mBinding.container.addView(layout, resolvePosSituationVerses(root));
        layout.post(() -> {
            layout.refresh(quranMeta);
            initEtiquetteVerses(root, quranMeta);
        });
    }

    private void initEtiquetteVerses(View root, QuranMeta quranMeta) {
        QuranEtiquetteLayout layout = new QuranEtiquetteLayout(root.getContext());
        layout.setId(R.id.homepageEtiquetteVersesLayout);
        mBinding.container.addView(layout, resolvePosEtiquetteVerses(root));
        layout.post(() -> {
            layout.refresh(quranMeta);
            initFeaturedTopics(root, quranMeta);
        });
    }

    private void initFeaturedTopics(View root, QuranMeta quranMeta) {
        FeatureTopicsLayout topicsLayout = new FeatureTopicsLayout(root.getContext());
        topicsLayout.setId(R.id.homepageTopicsLayout);
        mBinding.container.addView(topicsLayout, resolvePosFeaturedTopics(root));
        topicsLayout.post(() -> {
            topicsLayout.refresh(quranMeta);
            initFeaturedProphets(root, quranMeta);
        });
    }

    private void initFeaturedProphets(View root, QuranMeta quranMeta) {
        FeatureProphetsLayout prophetsLayout = new FeatureProphetsLayout(root.getContext());
        prophetsLayout.setId(R.id.homepageProphetsLayout);
        mBinding.container.addView(prophetsLayout, resolvePosFeaturedProphets(root));
        prophetsLayout.post(() -> prophetsLayout.refresh(quranMeta));
    }

    public static int resolvePosUpdateCont() {
        return 0;
    }

    private int resolvePosVOTD(View root) {
        int pos = resolvePosUpdateCont() + 1;
        if (root.findViewById(R.id.appUpdateContainer) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosReadHistory(View root) {
        int pos = resolvePosVOTD(root) + 1;
        if (root.findViewById(R.id.homepageVOTD) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosFeaturedReading(View root) {
        int pos = resolvePosReadHistory(root) + 1;
        if (root.findViewById(R.id.homepageReadHistoryLayout) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosFeaturedDua(View root) {
        int pos = resolvePosFeaturedReading(root) + 1;
        if (root.findViewById(R.id.homepageReadingLayout) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosSituationVerses(View root) {
        int pos = resolvePosFeaturedDua(root) + 1;
        if (root.findViewById(R.id.homepageDuaLayout) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosEtiquetteVerses(View root) {
        int pos = resolvePosSituationVerses(root) + 1;
        if (root.findViewById(R.id.homepageSituationVersesLayout) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosFeaturedTopics(View root) {
        int pos = resolvePosEtiquetteVerses(root) + 1;
        if (root.findViewById(R.id.homepageEtiquetteVersesLayout) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosFeaturedProphets(View root) {
        int pos = resolvePosFeaturedTopics(root) + 1;
        if (root.findViewById(R.id.homepageTopicsLayout) == null) {
            pos--;
        }
        return pos;
    }
}
