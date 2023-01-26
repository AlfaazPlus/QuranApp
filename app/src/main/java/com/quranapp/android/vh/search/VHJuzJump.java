package com.quranapp.android.vh.search;

import com.quranapp.android.components.search.JuzJumpModel;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.databinding.LytReaderJuzSpinnerItemBinding;
import com.quranapp.android.utils.reader.factory.ReaderFactory;

public class VHJuzJump extends VHSearchResultBase {
    private final LytReaderJuzSpinnerItemBinding mBinding;

    public VHJuzJump(LytReaderJuzSpinnerItemBinding binding, boolean applyMargins) {
        super(binding.getRoot());
        mBinding = binding;

        setupJumperView(binding.getRoot(), applyMargins);
    }

    @Override
    public void bind(SearchResultModelBase parentModel, int pos) {
        JuzJumpModel model = (JuzJumpModel) parentModel;

        mBinding.juzSerial.setText(model.juzSerial);

        mBinding.getRoot().setOnClickListener(v -> ReaderFactory.startJuz(v.getContext(), model.juzNo));
    }
}
