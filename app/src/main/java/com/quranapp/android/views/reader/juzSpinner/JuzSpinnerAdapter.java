package com.quranapp.android.views.reader.juzSpinner;

import android.content.Context;
import android.view.View;
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

public class JuzSpinnerAdapter extends ReaderSpinnerAdapter<JuzSpinnerAdapter.VHJuzSpinner> {
    public JuzSpinnerAdapter(Context ctx, List<ReaderSpinnerItem> items) {
        super(ctx, items);
    }

    @Override
    public JuzSpinnerItem getItem(int position) {
        return (JuzSpinnerItem) super.getItem(position);
    }

    @Override
    public JuzSpinnerItem getItemFromStoredList(int position) {
        return (JuzSpinnerItem) super.getItemFromStoredList(position);
    }

    @Override
    public JuzSpinnerItem getSelectedItem() {
        return (JuzSpinnerItem) super.getSelectedItem();
    }

    @NonNull
    @Override
    public VHJuzSpinner onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHJuzSpinner(makeItemView(parent.getContext()));
    }

    private TextView makeItemView(Context context) {
        TextView txtView = new TextView(context);

        ViewPaddingKt.updatePaddings(txtView, Dimen.dp2px(context, 15), Dimen.dp2px(context, 10));

        txtView.setTextColor(ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label));

        txtView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        return txtView;
    }

    class VHJuzSpinner extends VHReaderSpinner {
        public VHJuzSpinner(View view) {
            super(JuzSpinnerAdapter.this, view);
        }

        @Override
        public void bind(ReaderSpinnerItem item) {
            super.bind(item);
            JuzSpinnerItem juzItem = (JuzSpinnerItem) item;
            ((TextView) itemView).setText(juzItem.getLabel());
        }
    }
}
