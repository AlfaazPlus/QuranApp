package com.quranapp.android.utils.reader.recitation;

public interface RecitationPlayerImpl {
    void playControl();

    void playMedia();

    void pauseMedia();

    boolean isPlaying();

    void seek(int amountOrDirection);

    void updateProgressBar();

    void updateTimelineText();

    void recitePreviousVerse();

    void reciteNextVerse();

    void restartRange();

    void restartVerse();

    void reciteControl(int chapterNo, int verseNo);

    void reciteVerse(int chapterNo, int verseNo);

    void setRepeat(boolean repeat);

    void setContinueChapter(boolean continueChapter);
}
