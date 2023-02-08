package com.quranapp.android.views.reader;

import static com.quranapp.android.utils.reader.recitation.RecitationNotificationService.KEY_PLAYING;
import static com.quranapp.android.utils.reader.recitation.RecitationNotificationService.KEY_RECITER;
import static com.quranapp.android.utils.reader.recitation.RecitationNotificationService.KEY_TITLE;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PAUSE;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_PLAY;
import static com.quranapp.android.utils.receivers.RecitationPlayerReceiver.ACTION_STOP;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.peacedesign.android.utils.Log;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.recitation.RecitationModel;
import com.quranapp.android.exc.NoInternetException;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.interfaceUtils.PlayerVerseLoadCallback;
import com.quranapp.android.suppliments.recitation.RecitationMenu;
import com.quranapp.android.utils.reader.recitation.RecitationLoadTaskRunner;
import com.quranapp.android.utils.reader.recitation.RecitationNotificationService;
import com.quranapp.android.utils.reader.recitation.RecitationParams;
import com.quranapp.android.utils.reader.recitation.RecitationPlayerImpl;
import com.quranapp.android.utils.reader.recitation.RecitationUtils;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.sharedPrefs.SPReader;
import com.quranapp.android.utils.univ.Duration;
import com.quranapp.android.utils.univ.FileUtils;
import com.quranapp.android.utils.univ.NotifUtils;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressLint("ViewConstructor")
public class RecitationPlayer extends FrameLayout implements RecitationPlayerImpl, Destroyable {
    private static final int SEEK_LEFT = -1;
    private static final int SEEK_RIGHT = 1;
    private final int MILLIS_MULT = 100;
    private final SeekBar progressBar;
    public final TextView progressText;
    private final View loader;
    private final ImageView btnPlayControl;
    private final View btnSeekLeft;
    private final View btnSeekRight;
    private final View btnPrevVerse;
    private final View btnNextVerse;
    private final ImageView btnVerseSync;
    private final View btnMenu;
    public final FileUtils fileUtils;
    private final RecitationLoadTaskRunner mTaskRunner = new RecitationLoadTaskRunner();
    private final PlayerVerseLoadCallback mVerseLoadCallback = new PlayerVerseLoadCallback(this);
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final RecitationMenu mMenu;
    private RecitationParams P;
    public final ActivityReader mActivity;
    private MediaPlayer mediaPlayer;
    private boolean isLoadingInProgress;
    public boolean mReaderChanging;
    private Runnable mPlayerProgressRunner;
    public boolean mForceManifestFetch;

    public RecitationPlayer(@NonNull ActivityReader activityReader) {
        super(activityReader);
        mActivity = activityReader;

        fileUtils = FileUtils.newInstance(mActivity);
        P = new RecitationParams();
        mMenu = new RecitationMenu(this);

        View view = LayoutInflater.from(mActivity).inflate(R.layout.lyt_recitation_player, this, false);
        addView(view);

        progressBar = findViewById(R.id.progress);
        progressText = findViewById(R.id.progressText);
        loader = findViewById(R.id.loader);
        btnPlayControl = findViewById(R.id.playControl);
        btnSeekLeft = findViewById(R.id.seekLeft);
        btnSeekRight = findViewById(R.id.seekRight);
        btnPrevVerse = findViewById(R.id.prevVerse);
        btnNextVerse = findViewById(R.id.nextVerse);
        btnVerseSync = findViewById(R.id.verseSync);
        btnMenu = findViewById(R.id.menu);
        init();
    }

    @Override
    public void destroy() {
        release();
        mMenu.close();
        mTaskRunner.cancelAll();

        showNotification(ACTION_STOP);
    }

    public void onChapterChanged(int chapterNo, int fromVerse, int toVerse) {
        mReaderChanging = true;

        P.currVerse = new int[]{chapterNo, fromVerse};

        P.fromVerse = new int[]{chapterNo, fromVerse};
        P.toVerse = new int[]{chapterNo, toVerse};

        onReaderChanged();

        btnVerseSync.setImageAlpha(mActivity.mReaderParams.isSingleVerse() ? 0 : 255);
        btnVerseSync.setEnabled(!mActivity.mReaderParams.isSingleVerse());
    }

    public void onJuzChanged(int juzNo) {
        mReaderChanging = true;

        final QuranMeta quranMeta = mActivity.mQuranMetaRef.get();

        final int[] chaptersInJuz = quranMeta.getChaptersInJuz(juzNo);
        final int firstChapter = chaptersInJuz[0];
        final int firstVerse = quranMeta.getVersesOfChapterInJuz(juzNo, firstChapter)[0];
        final int lastChapter = chaptersInJuz[chaptersInJuz.length - 1];

        final int[] lastChapFromToVerses = quranMeta.getVersesOfChapterInJuz(juzNo, lastChapter);
        final int lastVerse = lastChapFromToVerses[1];

        P.fromVerse = new int[]{firstChapter, firstVerse};
        P.toVerse = new int[]{lastChapter, lastVerse};
        P.currVerse = P.fromVerse;

        onReaderChanged();

        btnVerseSync.setImageAlpha(255);
        btnVerseSync.setEnabled(true);
    }

    private void onReaderChanged() {
        P.currRangeCompleted = true;
        P.currVerseCompleted = true;

        release();
        mMenu.close();
        mTaskRunner.cancelAll();
        setupPlayControlBtn(false);
        updateProgressBar();
        updateTimelineText();
        reveal();
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        PlayerSavedState playerSS = new PlayerSavedState(super.onSaveInstanceState());

        playerSS.recitationParams = P;
        return playerSS;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof PlayerSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        PlayerSavedState playerSS = (PlayerSavedState) state;
        super.onRestoreInstanceState(playerSS.getSuperState());

        P = playerSS.recitationParams;
        if (P.lastMediaURI != null) {
            prepareMediaPlayer(P.lastMediaURI, P.currentReciterSlug, P.getCurrChapterNo(), P.getCurrVerseNo(),
                    P.previouslyPlaying);
        }
    }

    private void init() {
        setId(R.id.recitationPlayer);
        setSaveEnabled(true);

        // intercept touch
        setOnTouchListener((v, event) -> true);

        initManagers();

        setupRepeat(SPReader.getRecitationRepeatVerse(getContext()));
        setupContinuePlaying(SPReader.getRecitationContinueChapter(getContext()));
        setupVerseSync(SPReader.getRecitationScrollSync(getContext()), false);
        updateProgressBar();
        updateTimelineText();

        initControls();
    }

    private void initManagers() {
    }

    private void initControls() {
        progressBar.setSaveEnabled(false);
        progressBar.setSaveFromParentEnabled(false);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean previouslyPlaying;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    seek(progress * MILLIS_MULT);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                previouslyPlaying = P.previouslyPlaying;
                pauseMedia();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (previouslyPlaying) {
                    playMedia();
                }
            }
        });
        btnPlayControl.setOnClickListener(v -> {
            if (!isLoadingInProgress) {
                playControl();
            }
        });
        btnSeekLeft.setOnClickListener(v -> {
            if (!isLoadingInProgress) {
                seek(SEEK_LEFT);
            }
        });
        btnSeekRight.setOnClickListener(v -> {
            if (!isLoadingInProgress) {
                seek(SEEK_RIGHT);
            }
        });
        btnPrevVerse.setOnClickListener(v -> recitePreviousVerse());
        btnNextVerse.setOnClickListener(v -> reciteNextVerse());
        btnVerseSync.setImageAlpha(0);
        btnVerseSync.setEnabled(false);
        btnVerseSync.setOnClickListener(v -> {
            if (!mActivity.mReaderParams.isSingleVerse()) {
                setupVerseSync(!P.verseSync, true);
            }
        });
        btnMenu.setOnClickListener(this::openPlayerMenu);
    }

    public void setupVerseSync(boolean sync, boolean fromUser) {
        P.verseSync = sync;
        btnVerseSync.setSelected(P.verseSync);
        btnVerseSync.setImageResource(P.verseSync ? R.drawable.dr_icon_locked : R.drawable.dr_icon_unlocked);

        if (mActivity != null && P.verseSync && isPlaying()) {
            mActivity.onVerseRecite(P.getCurrChapterNo(), P.getCurrVerseNo(), true);
        }

        SPReader.setRecitationVerseSync(getContext(), P.verseSync);

        if (fromUser) {
            popMiniMsg(P.verseSync ? "Verse sync : ON" : "Verse sync : OFF", Toast.LENGTH_SHORT);
        }
    }

    private void setupRepeat(boolean repeat) {
        P.repeatVerse = repeat;

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(P.repeatVerse);
        }

        SPReader.setRecitationRepeatVerse(getContext(), P.repeatVerse);
    }

    private void setupContinuePlaying(boolean continuePlaying) {
        P.continueRange = continuePlaying;

        SPReader.setRecitationContinueChapter(getContext(), P.continueRange);
    }

    private void openPlayerMenu(View anchorView) {
        mMenu.open(anchorView);
    }

    public void prepareMediaPlayer(Uri audioURI, String slug, int chapterNo, int verseNo, boolean play) {
        if (!mActivity.mReaderParams.isVerseInValidRange(chapterNo, verseNo)) {
            return;
        }

        mVerseLoadCallback.postLoad();
        release();

        mediaPlayer = MediaPlayer.create(getContext(), audioURI);

        if (mediaPlayer == null) {
            popMiniMsg("Something happened wrong while playing the audio", Duration.DURATION_LONG);
            return;
        }

        P.lastMediaURI = audioURI;
        P.currentReciterSlug = slug;

        P.currVerse = new int[]{chapterNo, verseNo};

        mediaPlayer.setNextMediaPlayer(null);
        mediaPlayer.setLooping(P.repeatVerse);
        mediaPlayer.setOnPreparedListener(mp -> {
            progressBar.setMax(mp.getDuration() / MILLIS_MULT);
            updateProgressBar();
            updateTimelineText();
            if (play) {
                playMedia();
            }
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.d(what, extra);
            return true;
        });
        mediaPlayer.setOnInfoListener((mp, what, extra) -> {
            Log.d(what, extra);
            return true;
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            P.currVerseCompleted = true;

            release();

            QuranMeta quranMeta = mActivity.mQuranMetaRef.get();

            boolean continueNext = P.continueRange && P.hasNextVerse(quranMeta);

            boolean verseValid = quranMeta.isVerseValid4Chapter(P.getCurrChapterNo(), P.getCurrVerseNo() + 1);
            boolean continueNextSingle = P.continueRange && mActivity.mReaderParams.isSingleVerse() && verseValid;

            P.currRangeCompleted = !continueNext && !continueNextSingle;

            if (continueNext || continueNextSingle) {
                reciteNextVerse();
            } else {
                pauseMedia();
            }
        });
    }

    public RecitationParams P() {
        return P;
    }

    @Override
    public void playControl() {
        if (P.currRangeCompleted && P.currVerseCompleted) {
            restartRange();
        } else if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                pauseMedia();
            } else {
                playMedia();
            }
        } else {
            restartVerse();
        }
    }

    @Override
    public void playMedia() {
        if (P.verseSync && !mActivity.mReaderParams.isSingleVerse()) {
            reveal();
        }

        if (mediaPlayer != null) {
            mediaPlayer.start();
            runAudioProgress();

            P.currRangeCompleted = false;
            P.currVerseCompleted = false;
        }

        if (mActivity != null) {
            mActivity.onVerseRecite(P.getCurrChapterNo(), P.getCurrVerseNo(), true);
        }

        showNotification(ACTION_PLAY);

        Log.d("Curr - " + P.currVerse[0] + ":" + P.currVerse[1],
                P.fromVerse[0] + ":" + P.fromVerse[1] + " - " + P.toVerse[0] + ":" + P.toVerse[1]);

        setupPlayControlBtn(true);
        P.previouslyPlaying = true;
    }

    @Override
    public void pauseMedia() {
        if (isPlaying()) {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
            mHandler.removeCallbacksAndMessages(null);
        }

        if (mActivity != null) {
            mActivity.onVerseRecite(P.getCurrChapterNo(), P.getCurrVerseNo(), false);
        }

        showNotification(ACTION_PAUSE);
        setupPlayControlBtn(false);
        P.previouslyPlaying = false;
    }

    private void setupPlayControlBtn(boolean playing) {
        final int res = playing ? R.drawable.dr_icon_pause2 : R.drawable.dr_icon_play2;
        btnPlayControl.setImageResource(res);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void seek(int amountOrDirection) {
        if (mediaPlayer == null || isLoadingInProgress) {
            return;
        }

        P.currVerseCompleted = false;

        final int seekAmount;
        if (amountOrDirection == SEEK_RIGHT) {
            seekAmount = 5000;
        } else if (amountOrDirection == SEEK_LEFT) {
            seekAmount = -5000;
        } else {
            seekAmount = amountOrDirection;
        }

        boolean fromBtnClick = amountOrDirection == SEEK_LEFT || amountOrDirection == SEEK_RIGHT;

        int seekFinal = seekAmount;
        if (fromBtnClick) {
            seekFinal += mediaPlayer.getCurrentPosition();
            seekFinal = Math.max(seekFinal, 0);
            seekFinal = Math.min(seekFinal, mediaPlayer.getDuration());
        }

        mediaPlayer.seekTo(seekFinal);
        updateTimelineText();

        if (fromBtnClick) {
            updateProgressBar();
        }
    }

    @Override
    public void updateProgressBar() {
        final int progress;
        if (mediaPlayer != null) {
            progress = mediaPlayer.getCurrentPosition() / MILLIS_MULT;
        } else {
            progress = 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(progress, true);
        } else {
            progressBar.setProgress(progress);
        }
    }

    @Override
    public void updateTimelineText() {
        final String text;
        if (mediaPlayer != null) {
            text = formatTime(mediaPlayer.getCurrentPosition()) + " / " + formatTime(mediaPlayer.getDuration());
        } else {
            text = formatTime(0) + " / " + formatTime(0);
        }

        progressText.setText(text);
    }

    @Override
    public void setRepeat(boolean repeat) {
        setupRepeat(repeat);
    }

    @Override
    public void setContinueChapter(boolean continueChapter) {
        setupContinuePlaying(continueChapter);
    }

    private String formatTime(int millis) {
        final long m = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(millis));
        final long s = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(millis));
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void runAudioProgress() {
        if (mediaPlayer == null) {
            return;
        }

        // remove only progressRunner, not verse load runner
        mHandler.removeCallbacks(mPlayerProgressRunner);
        mPlayerProgressRunner = new Runnable() {
            @Override
            public void run() {
                updateProgressBar();
                updateTimelineText();

                if (isPlaying()) {
                    mHandler.postDelayed(this, MILLIS_MULT);
                }
            }
        };
        mHandler.post(mPlayerProgressRunner);
    }

    public void setupOnLoadingInProgress(boolean inProgress) {
        isLoadingInProgress = inProgress;

        if (inProgress) {
            loader.setVisibility(VISIBLE);
            btnPlayControl.setVisibility(GONE);
        } else {
            loader.setVisibility(GONE);
            btnPlayControl.setVisibility(VISIBLE);
        }

        disableActions(inProgress);
        disableActions(inProgress);
    }

    private void disableActions(boolean disable) {
        final float alpha = disable ? 0.6f : 1f;

        progressBar.setAlpha(alpha);
        progressBar.setEnabled(!disable);

        btnSeekLeft.setAlpha(alpha);
        btnSeekLeft.setEnabled(!disable);

        btnSeekRight.setAlpha(alpha);
        btnSeekRight.setEnabled(!disable);

        btnPrevVerse.setAlpha(alpha);
        btnPrevVerse.setEnabled(!disable);

        btnNextVerse.setAlpha(alpha);
        btnNextVerse.setEnabled(!disable);
    }

    @SuppressWarnings("unchecked")
    public void reveal() {
        final View parent = (View) getParent();
        final ViewGroup.LayoutParams params = parent.getLayoutParams();
        if (params instanceof CoordinatorLayout.LayoutParams) {
            final CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) params;
            final CoordinatorLayout.Behavior<View> b = p.getBehavior();
            if (b instanceof HideBottomViewOnScrollBehavior) {
                final HideBottomViewOnScrollBehavior<View> behavior = (HideBottomViewOnScrollBehavior<View>) b;
                behavior.slideUp(parent);
            }
        }

        mActivity.mBinding.readerHeader.setExpanded(true, true);
    }

    @SuppressWarnings("unchecked")
    public void conceal() {
        final View parent = (View) getParent();
        final ViewGroup.LayoutParams params = parent.getLayoutParams();
        if (params instanceof CoordinatorLayout.LayoutParams) {
            final CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) params;
            final CoordinatorLayout.Behavior<View> b = p.getBehavior();
            if (b instanceof HideBottomViewOnScrollBehavior) {
                final HideBottomViewOnScrollBehavior<View> behavior = (HideBottomViewOnScrollBehavior<View>) b;
                behavior.slideDown(parent);
            }
        }

        mActivity.mBinding.readerHeader.setExpanded(false);
    }

    public boolean isReciting(int chapterNo, int verseNo) {
        return P.isCurrentVerse(chapterNo, verseNo) && isPlaying();
    }

    @Override
    public void recitePreviousVerse() {
        if (isLoadingInProgress) {
            return;
        }

        final int chapterNo;
        final int verseNo;

        if (mActivity.mReaderParams.isSingleVerse()) {
            chapterNo = P.getCurrChapterNo();
            verseNo = P.getCurrVerseNo() - 1;
            mActivity.onVerseReciteOrJump(chapterNo, verseNo, true);
        } else {
            final int[] previousVerseJUZ = P.getPreviousVerse(mActivity.mQuranMetaRef.get());
            chapterNo = previousVerseJUZ[0];
            verseNo = previousVerseJUZ[1];
        }


        Log.d("PREV VERSE - " + chapterNo + ":" + verseNo);
        reciteVerse(chapterNo, verseNo);
    }

    @Override
    public void reciteNextVerse() {
        if (isLoadingInProgress) {
            return;
        }

        final int chapterNo;
        final int verseNo;

        if (mActivity.mReaderParams.isSingleVerse()) {
            chapterNo = P.getCurrChapterNo();
            verseNo = P.getCurrVerseNo() + 1;
            mActivity.onVerseReciteOrJump(chapterNo, verseNo, true);
        } else {
            final int[] nextVerseJUZ = P.getNextVerse(mActivity.mQuranMetaRef.get());
            chapterNo = nextVerseJUZ[0];
            verseNo = nextVerseJUZ[1];
        }

        Log.d("NEXT VERSE - " + chapterNo + ":" + verseNo);
        reciteVerse(chapterNo, verseNo);
    }

    @Override
    public void restartRange() {
        if (isLoadingInProgress) {
            return;
        }

        final int[] firstVerse = P.getFirstVerse();
        reciteVerse(firstVerse[0], firstVerse[1]);
    }

    @Override
    public void restartVerse() {
        if (isLoadingInProgress) {
            return;
        }
        reciteVerse(P.getCurrChapterNo(), P.getCurrVerseNo());
    }

    @Override
    public void reciteControl(int chapterNo, int verseNo) {
        if (isLoadingInProgress) {
            return;
        }

        if (P.isCurrentVerse(chapterNo, verseNo) && mediaPlayer != null) {
            playControl();
        } else {
            reciteVerse(chapterNo, verseNo);
        }
    }

    @Override
    public void reciteVerse(int chapterNo, int verseNo) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        if (!mActivity.mReaderParams.isVerseInValidRange(chapterNo, verseNo)) {
            return;
        }

        if (isLoadingInProgress || !mActivity.mQuranMetaRef.get().isVerseValid4Chapter(chapterNo, verseNo)) {
            return;
        }

        mVerseLoadCallback.preLoad();
        RecitationUtils.obtainRecitationModel(getContext(), mForceManifestFetch, model -> {
            // Saved recitation slug successfully fetched, now proceed.
            mForceManifestFetch = false;

            if (model != null) {
                reciteVerseOnSlugAvailable(model, chapterNo, verseNo);
            } else {
                mVerseLoadCallback.onFailed(null, null);
                mVerseLoadCallback.postLoad();
                destroy();
                pauseMedia();
            }
        });
    }

    private void reciteVerseOnSlugAvailable(RecitationModel model, int chapterNo, int verseNo) {
        final File audioFile = fileUtils.getRecitationAudioFile(model.getSlug(), chapterNo, verseNo);

        // Check If the audio file exists.
        if (audioFile.exists() && audioFile.length() > 0) {
            if (mTaskRunner.isPending(model.getSlug(), chapterNo, verseNo)) {
                pauseMedia();
                mVerseLoadCallback.preLoad();
                mTaskRunner.addCallback(model.getSlug(), chapterNo, verseNo, mVerseLoadCallback);
            } else {
                Uri audioURI = fileUtils.getFileURI(audioFile);
                prepareMediaPlayer(audioURI, model.getSlug(), chapterNo, verseNo, true);
            }
        } else {
            if (!NetworkStateReceiver.isNetworkConnected(getContext())) {
                popMiniMsg(getContext().getString(R.string.strMsgNoInternet), Toast.LENGTH_LONG);
                destroy();
                pauseMedia();
                return;
            }

            pauseMedia();
            loadVerse(model, chapterNo, verseNo, mVerseLoadCallback);
        }

        loadVersesAhead(mActivity.mQuranMetaRef.get(), model, chapterNo, verseNo);
    }

    private void loadVersesAhead(QuranMeta quranMeta, RecitationModel model, int chapterNo, int firstVerseToLoad) {
        if (!NetworkStateReceiver.isNetworkConnected(getContext())) {
            return;
        }

        //        int loadAheadCount = P.continueRange ? 5 : 1;
        int loadAheadCount = 2;
        int l = Math.min(mActivity.mQuranMetaRef.get().getChapterVerseCount(chapterNo) - firstVerseToLoad,
                loadAheadCount - 1);
        // Start from 1 so that the current verse is skipped.
        for (int i = 1; i <= l; i++) {
            int verseToLoad = firstVerseToLoad + i;

            if (!quranMeta.isVerseValid4Chapter(chapterNo, verseToLoad)) {
                continue;
            }

            final File verseFile = fileUtils.getRecitationAudioFile(model.getSlug(), chapterNo, verseToLoad);

            if (mTaskRunner.isPending(model.getSlug(), chapterNo, verseToLoad)) {
                continue;
            }

            if (verseFile.exists() && verseFile.length() > 0) {
                continue;
            }

            Log.d("Loading ahead verse - " + chapterNo + ":" + verseToLoad);

            boolean created = fileUtils.createFile(verseFile);
            if (!created) {
                continue;
            }

            loadVerse(model, chapterNo, verseToLoad, new PlayerVerseLoadCallback(null) {
                @Override
                public void onFailed(Exception e, File file) {
                    //noinspection ResultOfMethodCallIgnored
                    verseFile.delete();
                }
            });
        }
    }

    private void loadVerse(RecitationModel model, int chapterNo, int verseNo, PlayerVerseLoadCallback callback) {
        if (callback != null) {
            callback.preLoad();
        }

        final File verseFile = fileUtils.getRecitationAudioFile(model.getSlug(), chapterNo, verseNo);

        if (verseFile.exists() && verseFile.length() > 0) {
            mTaskRunner.publishVerseLoadStatus(verseFile, callback, true, null);
            return;
        }

        if (!NetworkStateReceiver.isNetworkConnected(getContext())) {
            mTaskRunner.publishVerseLoadStatus(verseFile, callback, false, new NoInternetException());
            return;
        }

        if (!fileUtils.createFile(verseFile)) {
            mTaskRunner.publishVerseLoadStatus(verseFile, callback, false, null);
            return;
        }

        String url = RecitationUtils.prepareAudioUrl(model, chapterNo, verseNo);
        mTaskRunner.addTask(verseFile, url, model.getSlug(), chapterNo, verseNo, callback);
    }

    public void popMiniMsg(String msg, int duration) {
        NotifUtils.showRemovableText(getContext(), msg, duration);
    }

    private void showNotification(String action) {
        if (ACTION_STOP.equals(action) && !RecitationNotificationService.isInstanceCreated()) {
            return;
        }

        Intent i = new Intent(getContext(), RecitationNotificationService.class);
        i.setAction(action);

        if (!ACTION_STOP.equals(action)) {
            QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
            String title = quranMeta.getChapterName(getContext(),
                    P.getCurrChapterNo()) + " : Verse " + P.getCurrVerseNo();
            String reciter = RecitationUtils.getReciterName(P.currentReciterSlug);
            i.putExtra(KEY_TITLE, title);
            i.putExtra(KEY_RECITER, reciter);
            i.putExtra(KEY_PLAYING, isPlaying());
            ContextCompat.startForegroundService(mActivity, i);
        } else {
            mActivity.startService(i);
        }
    }

    private static class PlayerSavedState extends BaseSavedState {
        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<PlayerSavedState> CREATOR = new Parcelable.Creator<PlayerSavedState>() {
            public PlayerSavedState createFromParcel(Parcel in) {
                return new PlayerSavedState(in);
            }

            public PlayerSavedState[] newArray(int size) {
                return new PlayerSavedState[size];
            }
        };
        RecitationParams recitationParams;

        private PlayerSavedState(Parcelable superState) {
            super(superState);
        }

        private PlayerSavedState(Parcel in) {
            super(in);
            recitationParams = in.readParcelable(RecitationParams.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(recitationParams, flags);
        }
    }
}
