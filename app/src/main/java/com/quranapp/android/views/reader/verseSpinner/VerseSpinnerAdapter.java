package com.quranapp.android.views.reader.verseSpinner;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.utils.extensions.ViewPaddingKt;
import com.quranapp.android.views.reader.spinner.ReaderSpinnerAdapter;
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem;

import java.util.List;

public class VerseSpinnerAdapter extends ReaderSpinnerAdapter<VerseSpinnerAdapter.VHVerseSpinner> {
    public VerseSpinnerAdapter(Context ctx, List<ReaderSpinnerItem> items) {
        super(ctx, items);
    }

    @Override
    public VerseSpinnerItem getItem(int position) {
        return (VerseSpinnerItem) super.getItem(position);
    }

    @Override
    public VerseSpinnerItem getSelectedItem() {
        return (VerseSpinnerItem) super.getSelectedItem();
    }

    @NonNull
    @Override
    public VHVerseSpinner onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHVerseSpinner(makeItemView(parent.getContext()));
    }

    private TextView makeItemView(Context context) {
        TextView txtView = new TextView(context);

        ViewPaddingKt.updatePaddings(txtView, Dimen.dp2px(context, 15), Dimen.dp2px(context, 10));

        txtView.setTextColor(ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label));

        txtView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        return txtView;
    }

    class VHVerseSpinner extends VHReaderSpinner {
        private final TextView mTextView;

        public VHVerseSpinner(@NonNull TextView itemView) {
            super(VerseSpinnerAdapter.this, itemView);
            mTextView = itemView;
        }

        @Override
        public void bind(ReaderSpinnerItem item) {
            super.bind(item);
            VerseSpinnerItem verseItem = (VerseSpinnerItem) item;
            mTextView.setText(verseItem.getLabel());
        }
    }
}
