package com.quranapp.android.utils.reader.recitation;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.quranapp.android.components.quran.QuranMeta;
import com.peacedesign.android.utils.Log;

public class RecitationParams implements Parcelable {
    public int[] currVerse = {-1, -1};
    public int[] fromVerse = {-1, -1};
    public int[] toVerse = {-1, -1};

    public String currentReciterSlug;
    public boolean previouslyPlaying;
    public boolean continueRange = true;
    public boolean currRangeCompleted = true;
    public boolean currVerseCompleted = true;
    public boolean repeatVerse;
    public boolean verseSync = true;
    public Uri lastMediaURI;

    public RecitationParams() {
    }

    protected RecitationParams(Parcel in) {
        currVerse = in.createIntArray();
        fromVerse = in.createIntArray();
        toVerse = in.createIntArray();
        currentReciterSlug = in.readString();
        previouslyPlaying = in.readByte() != 0;
        continueRange = in.readByte() != 0;
        currRangeCompleted = in.readByte() != 0;
        currVerseCompleted = in.readByte() != 0;
        repeatVerse = in.readByte() != 0;
        verseSync = in.readByte() != 0;
        lastMediaURI = in.readParcelable(Uri.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(currVerse);
        dest.writeIntArray(fromVerse);
        dest.writeIntArray(toVerse);
        dest.writeString(currentReciterSlug);
        dest.writeByte((byte) (previouslyPlaying ? 1 : 0));
        dest.writeByte((byte) (continueRange ? 1 : 0));
        dest.writeByte((byte) (currRangeCompleted ? 1 : 0));
        dest.writeByte((byte) (currVerseCompleted ? 1 : 0));
        dest.writeByte((byte) (repeatVerse ? 1 : 0));
        dest.writeByte((byte) (verseSync ? 1 : 0));
        dest.writeParcelable(lastMediaURI, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RecitationParams> CREATOR = new Creator<RecitationParams>() {
        @Override
        public RecitationParams createFromParcel(Parcel in) {
            return new RecitationParams(in);
        }

        @Override
        public RecitationParams[] newArray(int size) {
            return new RecitationParams[size];
        }
    };

    public int[] getFirstVerse() {
        return fromVerse;
    }

    public int[] getNextVerse(QuranMeta quranMeta) {
        final int currChapterNo = currVerse[0];
        final int currVerseNo = currVerse[1];
        final int toChapterNo = toVerse[0];
        final int toVerseNo = toVerse[1];

        Log.d(currChapterNo + ":" + currVerseNo, toChapterNo + ":" + toVerseNo);

        if (!QuranMeta.isChapterValid(currChapterNo) || !quranMeta.isVerseValid4Chapter(currChapterNo, currVerseNo)) {
            return new int[]{-1, -1};
        }

        int nextChapterNo = currChapterNo;
        int nextVerseNo = currVerseNo + 1;

        if (nextVerseNo > quranMeta.getChapterVerseCount(currChapterNo)) {
            nextChapterNo += 1;
            nextVerseNo = 1;
        }

        if (nextChapterNo > toChapterNo || nextVerseNo > toVerseNo) {
            nextChapterNo = -1;
            nextVerseNo = -1;
        }

        return new int[]{nextChapterNo, nextVerseNo};
    }

    public int[] getPreviousVerse(QuranMeta quranMeta) {
        final int currChapterNo = currVerse[0];
        final int currVerseNo = currVerse[1];
        final int fromChapterNo = fromVerse[0];
        final int fromVerseNo = fromVerse[1];

        if (!QuranMeta.isChapterValid(currChapterNo) || !quranMeta.isVerseValid4Chapter(currChapterNo, currVerseNo)) {
            return new int[]{-1, -1};
        }

        int prevChapterNo = currChapterNo;
        int prevVerseNo = currVerseNo - 1;

        if (prevVerseNo < 1) {
            prevChapterNo -= 1;
            prevVerseNo = quranMeta.getChapterVerseCount(prevChapterNo);
        }

        if (prevChapterNo < fromChapterNo || prevVerseNo < fromVerseNo) {
            prevChapterNo = -1;
            prevVerseNo = -1;
        }

        return new int[]{prevChapterNo, prevVerseNo};
    }

    /**
     * @return Returns true if the player chapter has next verse.
     */
    public boolean hasNextVerse(QuranMeta quranMeta) {
        final int currChapterNo = currVerse[0];
        final int currVerseNo = currVerse[1];
        final int toChapterNo = toVerse[0];
        final int toVerseNo = toVerse[1];

        if (!QuranMeta.isChapterValid(currChapterNo) || !quranMeta.isVerseValid4Chapter(currChapterNo, currVerseNo)) {
            return false;
        }

        int nextChapterNo = currChapterNo;
        int nextVerseNo = currVerseNo + 1;

        if (nextVerseNo > quranMeta.getChapterVerseCount(currChapterNo)) {
            nextChapterNo += 1;
            nextVerseNo = 1;
        }

        return nextChapterNo <= toChapterNo && nextVerseNo <= toVerseNo;
    }

    /**
     * @return Returns true if the player has previous verse within the current playable verse range.
     */
    public boolean hasPreviousVerse(QuranMeta quranMeta) {
        final int currChapterNo = currVerse[0];
        final int currVerseNo = currVerse[1];
        final int fromChapterNo = fromVerse[0];
        final int fromVerseNo = fromVerse[1];

        if (!QuranMeta.isChapterValid(currChapterNo) || !quranMeta.isVerseValid4Chapter(currChapterNo, currVerseNo)) {
            return false;
        }

        int prevChapterNo = currChapterNo;
        int prevVerseNo = currVerseNo - 1;

        if (prevVerseNo < 1) {
            prevChapterNo -= 1;
            prevVerseNo = quranMeta.getChapterVerseCount(prevChapterNo);
        }

        return prevChapterNo >= fromChapterNo && prevVerseNo >= fromVerseNo;
    }

    public boolean isCurrentVerse(int chapterNo, int verseNo) {
        return currVerse[0] == chapterNo && currVerse[1] == verseNo;
    }

    public int getCurrChapterNo() {
        return currVerse[0];
    }

    public int getCurrVerseNo() {
        return currVerse[1];
    }
}
