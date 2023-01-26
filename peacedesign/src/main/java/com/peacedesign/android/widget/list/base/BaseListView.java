package com.peacedesign.android.widget.list.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.Dimen;

@SuppressWarnings({"rawtypes", "unchecked"})
@SuppressLint("UnknownNullness")
public class BaseListView extends ScrollView {
    public static final int DEFAULT_ITEM_ANIMATOR = -1;
    public ViewGroup mContainer;
    private BaseListAdapter mAdapter;
    private OnItemClickListener mOnItemClickListener;
    private boolean mSupportsShowAnimation;
    private int mItemAnimator = DEFAULT_ITEM_ANIMATOR;

    public BaseListView(@NonNull Context context) {
        this(context, null);
    }

    public BaseListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        initThis();
        initContainer(context);
    }

    private void initThis() {
        setVerticalScrollBarEnabled(false);
        setClipToPadding(false);
    }

    private void initContainer(Context context) {
        mContainer = createItemsContainer(context);

        int padV = Dimen.dp2px(getContext(),10);
        mContainer.setPadding(0, padV, 0, padV);

        addView(mContainer);
    }

    @NonNull
    protected ViewGroup createItemsContainer(@NonNull Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);


        return container;
    }

    @Nullable
    public BaseListItem getItem(int position) {
        if (mAdapter != null) {
            return mAdapter.getItem(position);
        }
        return null;
    }

    public int getItemsCount() {
        if (mAdapter != null) {
            return mAdapter.getItemsCount();
        }
        return 0;
    }

    protected void onCreate() {
        create();
    }

    private void create() {
        mContainer.removeAllViews();

        if (mAdapter == null) {
            return;
        }

        setContainer(mContainer);
        for (int i = 0, count = mAdapter.getItemsCount(); i < count; i++) {
            onCreateItem(mAdapter.getItem(i), i);
        }
    }


    protected void onCreateItem(BaseListItem item, int position) {
        mAdapter.onCreateItem(item, position);
        View itemView = item.getItemView();

        if (mSupportsShowAnimation) {
            initPendingItemAnimation(itemView, position);
        }
    }

    protected void setContainer(ViewGroup container) {
        mAdapter.setContainer(container);
    }

    public BaseListAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(BaseListAdapter adapter) {
        mAdapter = adapter;
        if (adapter != null) {
            adapter.setListView(this);
        }
        onCreate();
    }

    private Animation resolveAnimation() {
        if (mItemAnimator == 0) return null;
        try {
            return AnimationUtils.loadAnimation(getContext(), mItemAnimator);
        } catch (Exception e) {
            return AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        }
    }

    private void initPendingItemAnimation(View itemView, int itemIndex) {
        Animation animation = resolveAnimation();
        if (animation == null) return;
        animation.setFillAfter(true);
        animation.setFillBefore(true);
        animation.setStartOffset(itemIndex * 40);
        animation.setDuration(300);
        itemView.post(() -> itemView.startAnimation(animation));
    }

    public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    protected void dispatchItemClick(@NonNull BaseListItem item) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(item);
        }
    }

    public void setSupportsShowAnimation(boolean b) {
        mSupportsShowAnimation = b;
    }

    public void setItemAnimator(@AnimRes int animationRes) {
        mItemAnimator = animationRes;
    }

    public interface OnItemClickListener {
        void onItemClick(@NonNull BaseListItem item);
    }
}
