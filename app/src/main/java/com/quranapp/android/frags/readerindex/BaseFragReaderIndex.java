package com.quranapp.android.frags.readerindex;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReaderIndexPage;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.databinding.FragReaderIndexBinding;
import com.quranapp.android.frags.BaseFragment;
import com.quranapp.android.interfaceUtils.readerIndex.FragReaderIndexCallback;
import com.quranapp.android.utils.thread.runner.RunnableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseRunnableTask;
import com.quranapp.android.views.helper.RecyclerView2;

import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseFragReaderIndex extends BaseFragment implements FragReaderIndexCallback {
    protected final Handler mHandler = new Handler(Looper.getMainLooper());
    private final RunnableTaskRunner mTaskRunner = new RunnableTaskRunner(mHandler);
    private final AtomicReference<QuranMeta> mQuranMetaRef = new AtomicReference<>();
    protected FragReaderIndexBinding mBinding;
    private boolean mIsReversed;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ActivityReaderIndexPage) {
            ((ActivityReaderIndexPage) context).addToCallbacks(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_reader_index, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBinding = FragReaderIndexBinding.bind(view);
        // ViewUtils.setBounceOverScrollRV(mBinding.list);

        mBinding.loader.setVisibility(View.VISIBLE);

        QuranMeta.prepareInstance(view.getContext(), quranMeta -> {
            mQuranMetaRef.set(quranMeta);

            mTaskRunner.runAsync(new BaseRunnableTask() {
                @Override
                public void postExecute() {
                    mBinding.loader.setVisibility(View.GONE);
                }

                @Override
                public void runTask() {
                    initList(mBinding.list, view.getContext());
                }

                @Override
                public void onComplete() {}
            });
        });
    }

    @CallSuper
    protected void initList(RecyclerView2 list, Context ctx) {
        RecyclerView.ItemAnimator animator = list.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    @Override
    public void scrollToTop(boolean smooth) {
        if (mBinding != null && mBinding.list.getLayoutManager() != null) {
            if (smooth) {
                mBinding.list.smoothScrollToPosition(0);
            } else {
                mBinding.list.scrollToPosition(0);
            }
        }
    }

    @Override
    public void sort(Context ctx) {
        mTaskRunner.runAsync(new BaseRunnableTask() {
            @Override
            public void preExecute() {
                mBinding.loader.setVisibility(View.VISIBLE);
            }

            @Override
            public void postExecute() {
                mBinding.loader.setVisibility(View.GONE);
            }

            @Override
            public void runTask() {
                resetAdapter(mBinding.list, ctx, !mIsReversed);
            }

            @Override
            public void onComplete() {}
        });
    }

    @CallSuper
    protected void resetAdapter(RecyclerView2 list, Context ctx, boolean reverse) {
        mIsReversed = reverse;
    }

    public QuranMeta getQuranMeta() {
        return mQuranMetaRef.get();
    }
}
