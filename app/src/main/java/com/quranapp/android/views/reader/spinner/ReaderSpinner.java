package com.quranapp.android.views.reader.spinner;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.databinding.LytReaderSpinnerBinding;
import com.quranapp.android.databinding.LytReaderSpinnerPopupBinding;
import com.quranapp.android.utils.extensions.ContextKt;
import com.quranapp.android.utils.simplified.SimpleTextWatcher;
import com.quranapp.android.utils.univ.PopupWindow2;
import com.quranapp.android.utils.univ.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ReaderSpinner extends FrameLayout {
    private final Drawable mPopupBG;
    private final LayoutInflater mInflater;
    protected ActivityReader mActivity;
    protected EditText mSearchBox;
    private ReaderSpinnerAdapter<?> mAdapter;
    private PopupWindow2 mPopup;
    private View mSpinnerView;
    private View mSpinnerPopupView;
    private RecyclerView mRecyclerView;
    private OnItemSelectedListener mItemSelectListener;

    public ReaderSpinner(@NonNull Context context) {
        this(context, null);
    }

    public ReaderSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReaderSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ReaderSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mInflater = LayoutInflater.from(getContext());
        mPopupBG = ContextKt.drawable(context, R.drawable.dr_bg_reader_spinner_popup);

        init(context);
    }

    private void init(Context context) {
        initThis();
        initSpinnerView(context);
        initSpinnerPopupView(context);
        initPopup();
    }

    private void initThis() {
        setOnClickListener(v -> showPopup());
    }

    private void initSpinnerView(Context context) {
        mSpinnerView = getSpinnerView(context, mInflater);
        addView(mSpinnerView);
    }

    private void initSpinnerPopupView(Context context) {
        mSpinnerPopupView = getSpinnerPopupView(mInflater);

        View btnClose = mSpinnerPopupView.findViewById(R.id.close);
        btnClose.setOnClickListener(v -> closePopup());

        TextView title = mSpinnerPopupView.findViewById(R.id.title);
        title.setText(getPopupTitle());

        mSearchBox = mSpinnerPopupView.findViewById(R.id.search);
        setupSearchBox(mSearchBox, mSpinnerPopupView.findViewById(R.id.btnClear));

        mRecyclerView = mSpinnerPopupView.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    @CallSuper
    protected void setupSearchBox(EditText searchBox, View btnClear) {
        btnClear.setVisibility(GONE);
        btnClear.setOnClickListener(v -> searchBox.setText(null));

        searchBox.setHint(getPopupSearchHint());
        searchBox.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                btnClear.setVisibility(TextUtils.isEmpty(s) ? GONE : VISIBLE);

                search(s.toString());
            }
        });
    }

    private void initPopup() {
        mPopup = new PopupWindow2();
        mPopup.setBackgroundDrawable(mPopupBG);
        mPopup.setClipBackground(true);
        mPopup.setDimBehind(.7f);
        mPopup.setAnimationStyle(R.style.PopupMenuAnimation);
        mPopup.setContentView(mSpinnerPopupView);
        mPopup.setOnDismissListener(() -> {
            if (mSearchBox != null) {
                mSearchBox.setText("");
            }
        });

        setupPopupDimensions();
    }

    protected View getSpinnerView(Context context, LayoutInflater inflater) {
        LytReaderSpinnerBinding mBinding = LytReaderSpinnerBinding.inflate(inflater);
        return mBinding.getRoot();
    }

    private View getSpinnerPopupView(LayoutInflater inflater) {
        LytReaderSpinnerPopupBinding binding = LytReaderSpinnerPopupBinding.inflate(inflater);
        return binding.getRoot();
    }

    protected String getPopupTitle() {
        return "";
    }

    protected String getPopupSearchHint() {
        return getContext().getString(R.string.strHintSearch);
    }

    private void search(String searchQuery) {
        searchQuery = StringUtils.escapeRegex(searchQuery);

        if (mAdapter == null || mRecyclerView == null) {
            return;
        }

        List<ReaderSpinnerItem> storedItems = mAdapter.getStoredItems();

        if (TextUtils.isEmpty(searchQuery)) {
            mAdapter.setSearchItems(storedItems);
            scrollToSelected();
            return;
        }

        Pattern p = Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE);

        List<ReaderSpinnerItem> foundItems = new ArrayList<>();

        for (int i = 0, l = storedItems.size(); i < l; i++) {
            ReaderSpinnerItem item = storedItems.get(i);
            boolean found = search(item, p);
            if (found) {
                foundItems.add(item);
            }
        }

        mAdapter.setSearchItems(foundItems);

        mRecyclerView.scrollToPosition(0);
    }

    protected boolean search(ReaderSpinnerItem item, Pattern pattern) {
        return false;
    }

    public void setSpinnerText(CharSequence text) {
        TextView txtView = mSpinnerView.findViewById(R.id.readerSpinnerText);
        boolean hasText = !TextUtils.isEmpty(text);
        if (hasText) {
            txtView.setText(text);
            txtView.setVisibility(VISIBLE);
        } else {
            txtView.setVisibility(GONE);
        }
    }

    protected void setSpinnerTextInternal(TextView textView, ReaderSpinnerItem item) {
        setSpinnerText(item.getLabel());
    }

    private void beforeShow() {
        scrollToSelected();
    }

    private void setupPopupDimensions() {
        mPopup.setHeight(WRAP_CONTENT);
        mPopup.setWidth(WRAP_CONTENT);
    }

    private void showPopup() {
        beforeShow();
        mPopup.showAtLocation(this, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
    }

    public void closePopup() {
        mPopup.dismiss();
    }

    public void scrollToSelected() {
        if (mRecyclerView == null) {
            return;
        }

        ReaderSpinnerItem selectedItem = mAdapter.getSelectedItem();
        if (selectedItem != null) {
            mRecyclerView.scrollToPosition(selectedItem.getPosition());
        }
    }

    void onItemSelect(ReaderSpinnerItem item, boolean invokeListener) {
        if (item != null) {
            TextView txtView = mSpinnerView.findViewById(R.id.readerSpinnerText);
            setSpinnerTextInternal(txtView, item);
        }

        if (invokeListener && mItemSelectListener != null) {
            mItemSelectListener.onItemSelect(item);
        }

        if (mPopup.isShowing()) {
            closePopup();
        }
    }

    public void setOnItemSelectListener(OnItemSelectedListener listener) {
        mItemSelectListener = listener;
    }

    public ReaderSpinnerAdapter<?> getAdapter() {
        return mAdapter;
    }

    @CallSuper
    public void setAdapter(ReaderSpinnerAdapter<?> adapter) {
        mAdapter = adapter;

        adapter.setSpinner(this);

        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(adapter);
        }
    }

    private ReaderSpinnerItem findSelectedItemForText(ReaderSpinnerAdapter<?> adapter) {
        ReaderSpinnerItem selectedItem = adapter.getSelectedItem();

        if (selectedItem == null) {
            selectedItem = adapter.getItem(0);
        }

        return selectedItem;
    }

    public void setActivity(ActivityReader activity) {
        mActivity = activity;
    }

    public interface OnItemSelectedListener {
        void onItemSelect(ReaderSpinnerItem item);
    }
}
