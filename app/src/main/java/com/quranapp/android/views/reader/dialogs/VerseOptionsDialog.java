/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.quranapp.android.utils.app.AppUtils.getDeviceInformation;
import static com.quranapp.android.utils.fb.FirebaseUtils.RTDB_VERSE_REPORTS;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.peacedesign.android.utils.AppBridge;
import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.Log;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.sheet.PeaceBottomSheet;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.adapters.utility.SpinnerAdapter2;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.components.utility.SpinnerItem;
import com.quranapp.android.databinding.LytReaderVodBinding;
import com.quranapp.android.databinding.LytReaderVodItemBinding;
import com.quranapp.android.databinding.LytReportProblemBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.readerhandler.ReaderParams;
import com.quranapp.android.utils.app.AppUtils;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.univ.DateUtils;
import com.quranapp.android.utils.univ.SimpleTextWatcher;
import com.quranapp.android.views.helper.Spinner2;
import com.quranapp.android.views.reader.RecitationPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VerseOptionsDialog extends PeaceBottomSheet implements View.OnClickListener, BookmarkCallbacks {
    private final DatabaseReference mReportVerseDB;
    private ReaderPossessingActivity mActivity;
    private VODLayout mVODLayout;
    private LytReaderVodBinding mVODBinding;
    private Verse mVerse;
    private BookmarkCallbacks mVerseViewCallbacks;
    private VerseShareDialog mVSD;

    public VerseOptionsDialog() {
        mReportVerseDB = FirebaseDatabase.getInstance().getReference().child(RTDB_VERSE_REPORTS);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReaderPossessingActivity) {
            mActivity = (ReaderPossessingActivity) context;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable("verse", mVerse);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVerse = (Verse) savedInstanceState.getSerializable("verse");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        if (mActivity != null && mVerse != null) {
            mActivity.getQuranMetaSafely(quranMeta -> setupDialogTitle(dialog.getContext(), quranMeta, mVerse));
        }
        super.setupDialog(dialog, style);
    }

    @Override
    protected void setupContentView(@NonNull LinearLayout dialogLayout, PeaceBottomSheetParams params) {
        if (mActivity == null || mVerse == null) {
            return;
        }

        if (mVODBinding == null) {
            AsyncLayoutInflater inflater = new AsyncLayoutInflater(dialogLayout.getContext());
            inflater.inflate(R.layout.lyt_reader_vod, dialogLayout, (view, resid, parent) -> {
                mVODBinding = LytReaderVodBinding.bind(view);
                mVODLayout = new VODLayout(mVODBinding);

                setupContent(mActivity, mVODBinding, mVODLayout, dialogLayout, mVerse);
            });
        } else {
            setupContent(mActivity, mVODBinding, mVODLayout, dialogLayout, mVerse);
        }
    }

    private void setupContent(
            ReaderPossessingActivity actvt, LytReaderVodBinding vodBinding, VODLayout vodLayout,
            LinearLayout dialogLayout, Verse verse
    ) {
        ViewUtils.removeView(vodBinding.getRoot());
        dialogLayout.addView(vodBinding.getRoot());

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vodBinding.getRoot().getLayoutParams();
        lp.width = WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        vodBinding.getRoot().requestLayout();

        installContents(actvt, vodBinding, vodLayout, verse);

        if (!RecitationUtils.isRecitationSupported() || !(actvt instanceof ActivityReader)) {
            vodLayout.btnPlayControl.setVisibility(View.GONE);
        }
    }

    public void open(ReaderPossessingActivity actvt, Verse verse, BookmarkCallbacks verseViewCallbacks) {
        mActivity = actvt;
        mVerse = verse;
        mVerseViewCallbacks = verseViewCallbacks;

        show(actvt.getSupportFragmentManager());
    }

    @Override
    public void dismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installContents(ReaderPossessingActivity actvt, LytReaderVodBinding vodBinding, VODLayout vodLayout, Verse verse) {
        if (actvt instanceof ActivityReader) {
            ActivityReader reader = (ActivityReader) actvt;
            if (reader.mPlayer != null) {
                onVerseRecite(reader.mPlayer);
            }
        }


        boolean hasFootnotes = false;
        for (Translation translation : verse.getTranslations()) {
            if (translation.getFootnotesCount() > 0) {
                hasFootnotes = true;
                break;
            }
        }

        disableButton(vodLayout.btnFootnotes, !hasFootnotes);

        final int chapterNo = verse.getChapterNo();
        final int verseNo = verse.getVerseNo();
        onBookmarkChanged(actvt.isBookmarked(chapterNo, verseNo, verseNo));

        vodBinding.scrollView.scrollTo(0, 0);
    }

    private void setupDialogTitle(Context ctx, QuranMeta quranMeta, Verse verse) {
        String chapterName = quranMeta.getChapterName(ctx, mVerse.getChapterNo());
        String title = ctx.getString(R.string.strTitleReaderVerseInformation, chapterName, verse.getVerseNo());
        setHeaderTitle(title);
        updateHeaderTitle();
    }

    private void onVerseRecite(RecitationPlayer player) {
        final int chapterNo = player.P().getCurrChapterNo();
        final int verseNo = player.P().getCurrVerseNo();
        onVerseRecite(chapterNo, verseNo, player.isPlaying());
    }

    public void onVerseRecite(int chapterNo, int verseNo, boolean isReciting) {
        if (mVODLayout == null) {
            return;
        }

        if (mVODLayout.btnPlayControl.getVisibility() != View.VISIBLE) {
            return;
        }

        isReciting &= mVerse != null && mVerse.getChapterNo() == chapterNo && mVerse.getVerseNo() == verseNo;

        int resId = isReciting ? R.drawable.dr_icon_pause_verse : R.drawable.dr_icon_play_verse;
        mVODLayout.btnPlayControlIcon.setImageResource(resId);

        int stringRes = isReciting ? R.string.strLabelPause : R.string.strLabelPlay;
        mVODLayout.btnPlayControlText.setText(stringRes);
    }

    private void onBookmarkChanged(boolean isBookmarked) {
        if (mVODLayout == null) {
            return;
        }

        Context ctx = mVODLayout.btnBookmarkIcon.getContext();

        final int filter = ContextCompat.getColor(ctx, isBookmarked ? R.color.colorPrimary : R.color.colorIcon2);
        mVODLayout.btnBookmarkIcon.setColorFilter(filter);

        final int res = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        mVODLayout.btnBookmarkIcon.setImageResource(res);

        int stringRes = isBookmarked ? R.string.strLabelBookmarked : R.string.strLabelBookmark;
        mVODLayout.btnBookmarkText.setText(stringRes);
    }

    private void disableButton(View btn, boolean disable) {
        btn.setEnabled(!disable);
        btn.setAlpha(disable ? 0.5f : 1f);
    }

    private void initShareDialog(ReaderPossessingActivity actvt) {
        if (mVSD == null) {
            mVSD = new VerseShareDialog(actvt);
        }
    }

    public void openShareDialog(ReaderPossessingActivity actvt, int chapterNo, int verseNo) {
        initShareDialog(actvt);
        mVSD.show(chapterNo, verseNo);
    }

    public void dismissShareDialog() {
        if (mVSD == null) {
            return;
        }

        mVSD.dismiss();
    }

    private void openReportDialog(ReaderPossessingActivity actvt, Verse verse) {
        LytReportProblemBinding binding = LytReportProblemBinding.inflate(LayoutInflater.from(actvt));

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(actvt);
        builder.setTitle(R.string.strTitleReportVerse);
        builder.setView(binding.getRoot());
        builder.setDialogGravity(PeaceDialog.GRAVITY_BOTTOM); builder.setCanceledOnTouchOutside(false);
        builder.setNeutralButton(R.string.strLabelClose, null);
        builder.setPositiveButton(R.string.strLabelReport, (dialog, which) -> report(actvt, verse, binding));
        builder.setFocusOnPositive(true);

        PeaceDialog dialog = builder.create();
        setupReportDialog(actvt, dialog, binding);
        dialog.show();
    }

    private void setupReportDialog(Context ctx, PeaceDialog dialog, LytReportProblemBinding binding) {
        dialog.setButtonEnabled(DialogInterface.BUTTON_POSITIVE, false);

        final List<SpinnerItem> reasons = new ArrayList<>();

        for (String reason : ctx.getResources().getStringArray(R.array.arrReportReasons)) {
            reasons.add(new SpinnerItem(reason));
        }

        Spinner2 spinner = binding.spinner;
        spinner.setOnItemSelectedListener(new Spinner2.SimplerSpinnerItemSelectListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean enabled = position != 0 && !TextUtils.isEmpty(binding.message.getText());
                dialog.setButtonEnabled(DialogInterface.BUTTON_POSITIVE, enabled);
            }
        });

        SpinnerAdapter2<SpinnerItem> adapter = new SpinnerAdapter2<>(ctx, R.layout.lyt_simple_spinner_item, R.id.text,
                reasons);
        spinner.setAdapter(adapter);

        binding.message.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                boolean enabled = spinner.getSelectedItemPosition() != 0 && !TextUtils.isEmpty(s);
                dialog.setButtonEnabled(DialogInterface.BUTTON_POSITIVE, enabled);
            }
        });
    }

    private void report(ReaderPossessingActivity actvt, Verse verse, LytReportProblemBinding binding) {
        String key = mReportVerseDB.push().getKey();
        if (key == null) {
            return;
        }

        Map<String, Object> map = new HashMap<>();

        map.put("reason", ((SpinnerItem) binding.spinner.getSelectedItem()).getName());
        map.put("additionalMessage", String.valueOf(binding.message.getText()));
        map.put("activity", actvt.getClass().getName());
        map.put("verseInformation", getVerseInformation(actvt, verse));
        map.put("appConfigs", AppUtils.getAppConfigs(actvt));
        map.put("deviceInformation", getDeviceInformation(actvt));
        map.put("date", DateUtils.getDateTimeNow());

        Map<String, Object> readerInfo = null;
        if (actvt instanceof ActivityReader) {
            readerInfo = getReaderInformation((ActivityReader) actvt);
        }
        map.put("readerInformation", readerInfo);

        Toast.makeText(actvt, R.string.strTextSending, Toast.LENGTH_LONG).show();
        mReportVerseDB.child(key).setValue(map, (error, ref) -> {
            if (error == null) {
                Toast.makeText(actvt, R.string.strMsgVerseReportSuccess, Toast.LENGTH_LONG).show();
            } else {
                error.toException().printStackTrace();
                Log.d(error.toException());
                Toast.makeText(actvt, R.string.strMsgVerseReportFailed, Toast.LENGTH_LONG).show();
            }
        });
    }

    public Map<String, Object> getVerseInformation(ReaderPossessingActivity actvt, Verse verse) {
        Map<String, Object> map = new HashMap<>();

        map.put("chapterNo", verse.getChapterNo());
        map.put("verseNo", verse.getVerseNo());
        map.put("translations",
                verse.getTranslations().stream().map(Translation::getBookSlug).collect(Collectors.toList()));

        map.put("script", SPReader.getSavedScript(actvt));
        map.put("reciter", SPReader.getSavedRecitationSlug(actvt));

        map.put("isBookmarked", actvt.isBookmarked(verse.getChapterNo(), verse.getVerseNo(), verse.getVerseNo()));

        return map;
    }

    public Map<String, Object> getReaderInformation(ActivityReader reader) {
        ReaderParams params = reader.mReaderParams;

        Map<String, Object> map = new HashMap<>();
        map.put("readType", params.readType);
        map.put("readerStyle", params.getReaderStyle());
        map.put("verseRange", Arrays.toString(params.verseRange));
        map.put("visibleTranslSlugs", params.getVisibleTranslSlugs().toString());
        map.put("saveTranslChanges", params.saveTranslChanges);

        if (reader.mPlayer != null) {
            boolean wasPlaying = reader.mPlayer.P().previouslyPlaying;
            map.put("wasPlaying", wasPlaying);
            if (wasPlaying) {
                map.put("playingVerse", Arrays.toString(reader.mPlayer.P().currVerse));
            }
        }

        return map;
    }

    @Override
    public void onClick(View v) {
        dismiss();
        ReaderPossessingActivity actvt = mActivity;
        if (actvt == null || mVerse == null) {
            return;
        }

        int id = v.getId();
        if (id == R.id.btnPlayControl) {
            if (actvt instanceof ActivityReader) {
                ActivityReader reader = (ActivityReader) actvt;
                if (reader.mPlayer != null) {
                    reader.mPlayer.reciteControl(mVerse.getChapterNo(), mVerse.getVerseNo());
                }
            }
        } else if (id == R.id.btnFootnotes) {
            actvt.showFootnotes(mVerse);
        } else if (id == R.id.btnTafsir) {
            /*
            Intent intent = ReaderFactory.prepareTafsirIntent(actvt, mVerse.getChapterNo(), mVerse.getVerseNo());
            actvt.startActivity4Result(intent, null);
            */
        } else if (id == R.id.btnBookmark) {
            final int chapterNo = mVerse.getChapterNo();
            final int verseNo = mVerse.getVerseNo();

            boolean isBookmarked = actvt.isBookmarked(chapterNo, verseNo, verseNo);

            if (isBookmarked) {
                actvt.onBookmarkView(chapterNo, verseNo, verseNo, this);
            } else {
                actvt.addVerseToBookmark(chapterNo, verseNo, verseNo, this);
            }
        } else if (id == R.id.btnQuickEdit) {
            /* actvt.startQuickEditShare(mVerse);*/
        } else if (id == R.id.btnShare) {
            openShareDialog(actvt, mVerse.getChapterNo(), mVerse.getVerseNo());
        } else if (id == R.id.btnReport) {
            // redirect to github issues
            AppBridge.newOpener(actvt).browseLink("https://github.com/AlfaazPlus/QuranApp/issues/new?template=verse_report.yml");
        }
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        onBookmarkChanged(false);

        if (mVerseViewCallbacks != null) {
            mVerseViewCallbacks.onBookmarkRemoved(model);
        }
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        onBookmarkChanged(true);

        if (mVerseViewCallbacks != null) {
            mVerseViewCallbacks.onBookmarkAdded(model);
        }
    }

    @Override
    public void onBookmarkUpdated(BookmarkModel model) {
        if (mVerseViewCallbacks != null) {
            mVerseViewCallbacks.onBookmarkUpdated(model);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            mVODBinding = null;
        }
        if (mVSD != null) {
            mVSD.onLowMemory();
        }
    }

    private class VODLayout {
        private View btnPlayControl;
        private ImageView btnPlayControlIcon;
        private TextView btnPlayControlText;

        private View btnFootnotes;

        private ImageView btnBookmarkIcon;
        private TextView btnBookmarkText;

        public VODLayout(LytReaderVodBinding vodBinding) {
            init(vodBinding);
        }

        private void createLayout(LinearLayout options) {
            Context context = options.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            final TypedArray ids = context.getResources().obtainTypedArray(R.array.arrVODIds);
            final TypedArray icons = context.getResources().obtainTypedArray(R.array.arrVODIcons);
            final String[] labels = context.getResources().getStringArray(R.array.arrVODLabels);

            int marg = Dimen.dp2px(context, 10);
            int tint = ContextCompat.getColor(context, R.color.colorIcon);

            for (int i = 0, l = labels.length; i < l; i++) {
                int id = ids.getResourceId(i, -1);
                int icon = icons.getResourceId(i, 0);
                String label = labels[i];

                if (id == R.id.btnTafsir || id == R.id.btnQuickEdit) {
                    continue;
                }

                LytReaderVodItemBinding binding = LytReaderVodItemBinding.inflate(inflater);
                binding.icon.setImageResource(icon);
                binding.label.setText(label);
                binding.icon.setColorFilter(tint);

                ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                ViewUtils.setMargins(p, marg);

                binding.getRoot().setId(id);
                binding.getRoot().setOnClickListener(VerseOptionsDialog.this);
                options.addView(binding.getRoot(), p);
            }

            ids.recycle();
            icons.recycle();
        }

        private void collectViews(View container) {
            btnPlayControl = container.findViewById(R.id.btnPlayControl);
            btnPlayControlIcon = btnPlayControl.findViewById(R.id.icon);
            btnPlayControlText = btnPlayControl.findViewById(R.id.label);

            btnFootnotes = container.findViewById(R.id.btnFootnotes);

            View btnBookmark = container.findViewById(R.id.btnBookmark);
            btnBookmarkIcon = btnBookmark.findViewById(R.id.icon);
            btnBookmarkText = btnBookmark.findViewById(R.id.label);
        }

        public void init(LytReaderVodBinding vodBinding) {
            createLayout(vodBinding.options);
            collectViews(vodBinding.options);
        }
    }
}
