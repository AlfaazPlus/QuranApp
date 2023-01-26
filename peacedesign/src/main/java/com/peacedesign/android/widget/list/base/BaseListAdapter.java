package com.peacedesign.android.widget.list.base;

import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.ADD;
import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.REMOVE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.peacedesign.android.utils.ArrayListPro;

import java.util.ArrayList;

public abstract class BaseListAdapter<LI extends BaseListItem> {
    private final Context mContext;
    private final ArrayListPro<LI> mItems = new ArrayListPro<>();
    private final int mAnimationDuration = 300;
    private ViewGroup mContainer;
    private boolean mItemsAnimatable = true;
    private BaseListView mListView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public BaseListAdapter(@NonNull Context context) {
        mContext = context;

        mItems.addObserver((observable, updateType) -> {
            if (updateType == REMOVE || updateType == ADD) {
                for (int i = 0; i < mItems.size(); i++) {
                    mItems.get(i).setPosition(i);
                }
            }
        });
    }

    void setListView(@NonNull BaseListView listView) {
        mListView = listView;
    }

    void setContainer(@NonNull ViewGroup container) {
        mContainer = container;
    }

    @NonNull
    public final Context getContext() {
        return mContext;
    }

    protected void onCreateItem(@NonNull LI item, int index) {
        View itemView = onCreateItemView(item, index);
        if (itemView == null) return;

        handler.post(() -> itemView.setSelected(item.isSelected()));

        itemView.setOnClickListener(v -> dispatchClick(item));
        item.setItemView(itemView);
        item.setAdapter(this);
        onAppendItemView(getContainerInternal(), itemView, index);
    }

    protected View onCreateItemView(@NonNull LI item, int position) {
        return new BaseListItemView(getContext(), item);
    }

    protected void onAppendItemView(@NonNull ViewGroup container, @NonNull View itemView, int position) {
        container.addView(itemView, position);
    }

    public void drainItems() {
        mItems.clear();
        if (mContainer != null) {
            mContainer.removeAllViews();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setItems(@NonNull ArrayList items) {
        boolean oldAnimateItemsFlag = mItemsAnimatable;
        mItemsAnimatable = false;
        drainItems();
        mItemsAnimatable = oldAnimateItemsFlag;

        mItems.addAll(items);

        if (mListView != null) {
            mListView.onCreate();
        }
    }

    public void addItem(@NonNull LI item) {
        addItem(item, mItems.size());
    }

    public void addItem(@NonNull LI item, int index) {
        item.setPosition(index);

        mItems.add(index, item);

        if (isLaidOut()) {
            onCreateItem(item, index);
            if (mItemsAnimatable) {
                View itemView = item.getItemView();
                if (itemView != null) {
                    addViewWithAnimation(itemView);
                }
            }
        }
    }

    private boolean isLaidOut() {
        return mContainer != null && mContainer.isLaidOut();
    }

    public void removeItem(int index) {
        removeItem(mItems.get(index));
    }

    public void removeItem(@NonNull LI item) {
        View view = item.getItemView();
        if (view != null) {
            remove(view, item, mItemsAnimatable);
        }
    }

    private void remove(View view, LI item, boolean animate) {
        if (animate) removeWithAnimation(view, item);
        else removeFinal(view, item);
    }

    void removeFinal(View view, LI item) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        removeMenuItemInternal(item);
    }

    private void removeWithAnimation(View view, LI item) {
        ViewGroup.LayoutParams params = view.getLayoutParams();

        ValueAnimator alphaAnimator = new ValueAnimator();
        alphaAnimator.setFloatValues(view.getAlpha(), 0f);

        alphaAnimator.addUpdateListener(animation -> view.setAlpha((float) animation.getAnimatedValue()));

        ValueAnimator heightAnimator = new ValueAnimator();
        heightAnimator.setIntValues(view.getHeight(), 0);
        heightAnimator.addUpdateListener(animation -> {
            params.height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(heightAnimator).with(alphaAnimator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                removeFinal(view, item);
            }
        });
        animatorSet.setDuration(mAnimationDuration).start();
    }

    private void removeMenuItemInternal(@NonNull LI item) {
        mItems.remove(item);
    }

    private void addViewWithAnimation(View itemView) {
        itemView.measure(0, 0);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        params.height = 0;

        ValueAnimator alphaAnimator = new ValueAnimator();
        alphaAnimator.setFloatValues(0, itemView.getAlpha());

        alphaAnimator.addUpdateListener(animation -> itemView.setAlpha((float) animation.getAnimatedValue()));

        ValueAnimator heightAnimator = new ValueAnimator();
        heightAnimator.setIntValues(0, itemView.getMeasuredHeight());
        heightAnimator.addUpdateListener(animation -> {
            params.height = (int) animation.getAnimatedValue();
            itemView.requestLayout();
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(heightAnimator).with(alphaAnimator);
        animatorSet.setDuration(mAnimationDuration).start();
    }

    @NonNull
    public LI getItem(int index) {
        int count = getItemsCount();
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(String.format("Adapter indexOutOfBound. Index: %d, Size: %d ", index, count));
        }
        return mItems.get(index);
    }

    public int getItemsCount() {
        return mItems.size();
    }

    private ViewGroup getContainerInternal() {
        return mContainer;
    }

    protected void dispatchClick(@NonNull LI item) {
        if (mListView != null) {
            mListView.dispatchItemClick(item);
        }
    }

    public void notifyItemChanged(int position) {
        View itemView = getItem(position).getItemView();
        if (itemView instanceof BaseListItemView) {
            ((BaseListItemView) itemView).notifyForChange();
        }
    }
}
