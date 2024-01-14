/*
 * Created by Faisal Khan on (c) 31/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import static com.quranapp.android.utils.univ.StringUtils.HYPHEN;
import static com.quranapp.android.widgets.compound.PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_LEFT;
import static com.quranapp.android.widgets.compound.PeaceCompoundButton.COMPOUND_TEXT_GRAVITY_RIGHT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.AppBridge;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.WindowUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.dialog.base.PeaceDialogController;
import com.quranapp.android.R;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Footnote;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.databinding.LytVerseShareBinding;
import com.quranapp.android.utils.Log;
import com.quranapp.android.utils.extensions.ViewPaddingKt;
import com.quranapp.android.utils.univ.MessageUtils;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.widgets.checkbox.PeaceCheckBox;
import com.quranapp.android.widgets.checkbox.PeaceCheckboxGroup;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kotlin.Unit;

public class VerseShareDialog extends PeaceDialog {
    private final ReaderPossessingActivity mActivity;
    private final Pattern patternFootnoteTag = Pattern.compile("<fn.*?>(.*?)<.*?fn>");
    private VerseShareDialogController mController;

    protected VerseShareDialog(ReaderPossessingActivity activity) {
        super(activity);
        mActivity = activity;
        setOnDismissListener(dialog -> mController.mBinding = null);
    }

    @NonNull
    @Override
    protected PeaceDialogController getController(@NonNull Context context, @NonNull PeaceDialog dialog) {
        mController = new VerseShareDialogController(context, dialog);
        return mController;
    }

    @Override
    protected int resolveDialogHeight(@NonNull View decorView) {
        return WRAP_CONTENT;
    }

    public void onLowMemory() {
        mController.mBinding = null;
    }

    public void show(int chapterNo, int verseNo) {
        show();
        mController.postShow(chapterNo, verseNo);
    }

    private void showAdvancedSharing(LytVerseShareBinding binding, boolean show) {
        binding.advanced.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.advancedContainer.setVisibility(show ? View.VISIBLE : View.GONE);

        if (!show) {
            binding.selectVerses.check(R.id.currVerse);
            binding.whatsappStyling.setChecked(false);
            binding.includeAr.setChecked(true);
            binding.includeFootnotes.setChecked(false);

            binding.translsGroup.clearCheck(false);
            binding.translsGroup.checkByIndex(0, true);
        }
    }

    private class VerseShareDialogController extends PeaceDialogController {
        private LytVerseShareBinding mBinding;
        private int mChapterNo;
        private int mVerseNo;
        private boolean mPendingPostShow;

        protected VerseShareDialogController(Context context, PeaceDialog dialogInterface) {
            super(context, dialogInterface);

            setTitle(context.getString(R.string.strTitleShareVerse));
            setTitleTextAppearance(R.style.TextAppearanceCommonTitleLarge);
            setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            setButton(BUTTON_NEUTRAL, context.getText(R.string.strLabelCancel), 0, null, (dialog, which) -> hide());
            setButton(BUTTON_POSITIVE, context.getText(R.string.strLabelShare), 0, null, (dialog, which) -> {
                try {
                    share(mBinding, mActivity.mQuranMetaRef.get());
                } catch (Exception e) {e.printStackTrace();}
            });

            setDismissOnNeutral(false);
            setDismissOnPositive(false);

            setCanceledOnTouchOutside(false);
        }

        @Override
        public void installContent() {
            createLayout(getContext());
        }

        private void createLayout(Context context) {
            AsyncLayoutInflater inflater = new AsyncLayoutInflater(context);
            inflater.inflate(R.layout.lyt_verse_share, null, (view, resid, parent) -> {
                LytVerseShareBinding binding = LytVerseShareBinding.bind(view);
                mBinding = binding;

                binding.selectVerses.setOnCheckChangedListener((button, checkedId) -> {
                    showRangeInputs(binding, checkedId == R.id.verseRange);
                    return Unit.INSTANCE;
                });
                binding.advanced.setOnClickListener(v -> showAdvancedSharing(binding, true));

                setView(binding.getRoot());
                super.installContent();

                if (mPendingPostShow) {
                    postShow(mChapterNo, mVerseNo);
                    mPendingPostShow = false;
                }
            });
        }

        public void postShow(int chapterNo, int verseNo) {
            mChapterNo = chapterNo;
            mVerseNo = verseNo;

            if (mBinding == null) {
                mPendingPostShow = true;
                return;
            }

            resetScroll(mBinding.getRoot().getParent());
            showAdvancedSharing(mBinding, false);

            int verseCount = mActivity.mQuranMetaRef.get().getChapterVerseCount(chapterNo);
            mBinding.rangeInputsTitle.setText(getContext().getString(R.string.strMsgShareRange, 1, verseCount));

            makeTransls(mActivity.mTranslFactory.getAvailableTranslationBooksInfo(), mBinding.translsGroup);
        }

        private void resetScroll(ViewParent parent) {
            if (parent instanceof NestedScrollView) {
                ((NestedScrollView) parent).smoothScrollTo(0, 0);
            }
        }

        private void makeTransls(Map<String, QuranTranslBookInfo> bookInfos, PeaceCheckboxGroup translsGroup) {
            translsGroup.removeAllViews();

            if (bookInfos == null) {
                return;
            }

            int forcedTextGravity = WindowUtils.isRTL(translsGroup.getContext())
                ? COMPOUND_TEXT_GRAVITY_RIGHT
                : COMPOUND_TEXT_GRAVITY_LEFT;

            bookInfos.forEach((slug, book) -> {
                PeaceCheckBox checkBox = new PeaceCheckBox(getContext());
                ViewPaddingKt.updatePaddings(checkBox, Dimen.dp2px(getContext(), 15), Dimen.dp2px(getContext(), 10));

                checkBox.setBackgroundResource(R.drawable.dr_bg_hover_cornered);
                checkBox.setText(book.getDisplayName(true));
                checkBox.setTextAppearance(R.style.TextAppearance);
                checkBox.setForceTextGravity(forcedTextGravity);
                checkBox.setTag(slug);

                translsGroup.addView(checkBox);
            });

            translsGroup.checkByIndex(0, true);
        }

        private void showRangeInputs(LytVerseShareBinding binding, boolean show) {
            binding.rangeInputsTitle.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.rangeInputs.setVisibility(show ? View.VISIBLE : View.GONE);

            InputMethodManager imm = ContextCompat.getSystemService(getContext(), InputMethodManager.class);
            if (imm == null) {
                return;
            }

            if (show) {
                binding.fromVerse.requestFocus();
                imm.toggleSoftInput(0, 0);
            } else {
                imm.hideSoftInputFromWindow(binding.fromVerse.getWindowToken(), 0);
            }
        }

        private void share(LytVerseShareBinding binding, QuranMeta quranMeta) {
            int fromVerse = mVerseNo;
            int toVerse = mVerseNo;
            if (binding.selectVerses.getCheckedRadioId() == R.id.verseRange) {
                Editable fromVerseText = binding.fromVerse.getText();
                Editable toVerseText = binding.toVerse.getText();

                if (TextUtils.isEmpty(fromVerseText) || TextUtils.isEmpty(toVerseText)) {
                    return;
                }

                fromVerse = Integer.parseInt(fromVerseText + "");
                toVerse = Integer.parseInt(toVerseText + "");
            }

            Stream<PeaceCheckBox> stream = binding.translsGroup.getCheckedBoxes().stream();
            Set<String> slugs = stream.map(checkBox -> checkBox.getTag().toString()).collect(Collectors.toSet());

            boolean whatsappStyling = binding.whatsappStyling.isChecked();
            boolean incAr = binding.includeAr.isChecked();
            boolean incFootnotes = binding.includeFootnotes.isChecked();

            if (!quranMeta.isVerseRangeValid4Chapter(mChapterNo, fromVerse, toVerse)) {
                MessageUtils.INSTANCE.showRemovableToast(mActivity,
                    getContext().getString(R.string.strMsgEnterValidRange),
                    Toast.LENGTH_LONG);
                return;
            }

            prepareNShare(mChapterNo, fromVerse, toVerse, slugs, whatsappStyling, incAr, incFootnotes,
                fromVerse == toVerse);
        }

        private void prepareNShare(
            int chapNo, int fromVerse, int toVerse, Set<String> slugs, boolean whatsappStyling,
            boolean incAr, boolean incFootnotes, boolean isSingleVerse
        ) {

            StringBuilder sb = new StringBuilder();

            boolean proceed = !slugs.isEmpty() || incAr;
            if (proceed) {
                for (int verseNo = fromVerse; verseNo <= toVerse; verseNo++) {
                    makeVerse(sb, chapNo, verseNo, slugs, whatsappStyling, incAr, incFootnotes, isSingleVerse);

                    if (verseNo < toVerse) {
                        sb.append("\n\n");
                        StringUtils.replicate(sb, HYPHEN, 10);
                        sb.append("\n\n");
                    }
                }
            }

            Log.d(sb);

            if (sb.length() == 0) {
                return;
            }

            shareFinal(sb, getContext());
        }

        private void makeVerse(
            StringBuilder sb, int chapNo, int verseNo, Set<String> slugs, boolean whatsappStyling,
            boolean incAr, boolean incFootnotes, boolean isSingleVerse
        ) {

            List<Translation> transls = mActivity.mTranslFactory.getTranslationsSingleVerse(slugs, chapNo, verseNo);
            boolean hasMultiTransls = transls.size() > 1;

            if (!isSingleVerse || hasMultiTransls) {
                sb.append("(Quran, Verse ").append(chapNo).append(":").append(verseNo).append(")\n");
            }

            if (incAr) {
                sb.append(mActivity.mQuranRef.get().getVerse(chapNo, verseNo).arabicText);
            }

            for (int i = 0, l = transls.size(); i < l; i++) {
                sb.append(makeTranslText(transls.get(i), hasMultiTransls, incFootnotes, whatsappStyling));

                if (i < l - 1) {
                    sb.append("\n\n");
                }
            }

            if (isSingleVerse && !hasMultiTransls) {
                sb.append("\n").append(HYPHEN).append(" Quran ").append(chapNo).append(":").append(verseNo);
            }
        }

        private CharSequence makeTranslText(Translation transl, boolean inclAuthor, boolean inclFootnotes, boolean whatsappStyle) {
            StringBuilder sb = new StringBuilder();

            sb.append("\n").append(cleanHTML(transl.getText(), inclFootnotes));

            Map<Integer, Footnote> footnotes = transl.getFootnotes();
            boolean hasFootnotes = !footnotes.isEmpty();

            if (inclAuthor || (inclFootnotes && hasFootnotes)) {
                QuranTranslBookInfo bookInfo = mActivity.mTranslFactory.getTranslationBookInfo(transl.getBookSlug());
                String author = bookInfo.getDisplayName(false);
                sb.append("\n");
                italic(sb, author, whatsappStyle);
            }

            if (inclFootnotes && hasFootnotes) {
                sb.append("\n\n");
                bold(sb, "FOOTNOTES:", whatsappStyle);
                footnotes.forEach((number, footnote) -> {
                    sb.append("\n\t\t").append("[").append(number).append("] ");
                    sb.append(Html.fromHtml(footnote.text));
                });
            }
            return sb;
        }

        private void bold(StringBuilder sb, CharSequence text, boolean whatsappStyle) {
            if (whatsappStyle) {
                sb.append("*").append(text).append("*");
            } else {
                sb.append(text);
            }
        }

        private void italic(StringBuilder sb, CharSequence text, boolean whatsappStyle) {
            if (whatsappStyle) {
                sb.append("_").append(text).append("_");
            } else {
                sb.append(text);
            }
        }

        private String cleanHTML(String string, boolean inclFootnotes) {
            Matcher matcher = patternFootnoteTag.matcher(string);
            String marker = inclFootnotes ? "[$1]" : "";
            return Html.fromHtml(matcher.replaceAll(marker)).toString();
        }

        private void shareFinal(StringBuilder sb, Context context) {
            AppBridge.Sharer sharer = AppBridge.newSharer(context);
            sharer.setData(sb).setChooserTitle(context.getString(R.string.strTitleShareVerse));
            sharer.setPlatform(AppBridge.Platform.SYSTEM_SHARE);
            Intent intent = sharer.prepare();

            mActivity.startActivity4Result(intent, null);
        }
    }
}
