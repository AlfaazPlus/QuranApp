package com.quranapp.android.views.reader.verseSpinner;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import static com.quranapp.android.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_JUMP_PATTERN;

import com.quranapp.android.R;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.views.reader.spinner.ReaderSpinner;
import com.quranapp.android.views.reader.spinner.ReaderSpinnerAdapter;
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerseSpinner extends ReaderSpinner {
    public VerseSpinner(@NonNull Context context) {
        super(context);
    }

    public VerseSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerseSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean search(ReaderSpinnerItem item, Pattern pattern) {
        return pattern.matcher(item.getLabel()).find();
    }

    @Override
    protected void setupSearchBox(EditText searchBox, View btnClear) {
        super.setupSearchBox(searchBox, btnClear);
        searchBox.setImeOptions(EditorInfo.IME_ACTION_GO);
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO && mActivity != null) {
                try {navigate(v.getText());} catch (NumberFormatException e) {e.printStackTrace();}
                closePopup();
            }
            return true;
        });
    }

    private void navigate(CharSequence searchQuery) throws NumberFormatException {
        if (mActivity.mReaderParams.readType == READER_READ_TYPE_JUZ) {
            final Matcher m = VERSE_JUMP_PATTERN.matcher(searchQuery);
            if (m.find()) {
                MatchResult r = m.toMatchResult();
                if (r.groupCount() >= 2) {
                    int chapNo = Integer.parseInt(r.group(1));
                    int verseNo = Integer.parseInt(r.group(2));
                    mActivity.handleVerseSpinnerSelectedVerseNo(chapNo, verseNo);
                }
            }
        } else {
            final Chapter chapter = mActivity.mReaderParams.currChapter;

            if (chapter == null) {
                return;
            }

            int chapNo = chapter.getChapterNumber();
            int verseNo = Integer.parseInt(searchQuery.toString());
            mActivity.handleVerseSpinnerSelectedVerseNo(chapNo, verseNo);
        }
    }

    @Override
    protected String getPopupTitle() {
        return getContext().getString(R.string.strTitleReaderJumpToVerse);
    }

    @Override
    protected String getPopupSearchHint() {
        return getContext().getString(R.string.strHintSearchVerse);
    }

    @Override
    public void setAdapter(ReaderSpinnerAdapter<?> adapter) {
        super.setAdapter(adapter);

        if (mActivity == null || mSearchBox == null) {
            return;
        }

        int inputType = mActivity.mReaderParams.readType == READER_READ_TYPE_JUZ ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_CLASS_NUMBER;
        mSearchBox.setInputType(inputType);
    }
}
