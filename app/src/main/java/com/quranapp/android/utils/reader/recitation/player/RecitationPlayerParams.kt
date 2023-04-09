package com.quranapp.android.utils.reader.recitation.player

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.quranapp.android.components.quran.QuranMeta

class RecitationPlayerParams() : Parcelable {
    /**
     * Pair<surahNo, verseNo>
     */
    var currentVerse = Pair(-1, -1)

    /**
     * Pair<surahNo, verseNo>
     */
    var firstVerse = Pair(-1, -1)

    /**s
     * Pair<surahNo, verseNo>
     */
    var lastVerse = Pair(-1, -1)

    var currentReciter: String? = null
    var previouslyPlaying = false
    var continueRange = true
    var currentRangeCompleted = true
    var currentVerseCompleted = true
    var repeatVerse = false
    var syncWithVerse = true

    var lastMediaURI: Uri? = null

    constructor(parcel: Parcel) : this() {
        currentReciter = parcel.readString()
        previouslyPlaying = parcel.readByte() != 0.toByte()
        continueRange = parcel.readByte() != 0.toByte()
        currentRangeCompleted = parcel.readByte() != 0.toByte()
        currentVerseCompleted = parcel.readByte() != 0.toByte()
        repeatVerse = parcel.readByte() != 0.toByte()
        syncWithVerse = parcel.readByte() != 0.toByte()
        lastMediaURI = ParcelCompat.readParcelable(parcel, Uri::class.java.classLoader, Uri::class.java)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(currentReciter)
        parcel.writeByte(if (previouslyPlaying) 1 else 0)
        parcel.writeByte(if (continueRange) 1 else 0)
        parcel.writeByte(if (currentRangeCompleted) 1 else 0)
        parcel.writeByte(if (currentVerseCompleted) 1 else 0)
        parcel.writeByte(if (repeatVerse) 1 else 0)
        parcel.writeByte(if (syncWithVerse) 1 else 0)
        parcel.writeParcelable(lastMediaURI, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecitationPlayerParams> {
        override fun createFromParcel(parcel: Parcel): RecitationPlayerParams {
            return RecitationPlayerParams(parcel)
        }

        override fun newArray(size: Int): Array<RecitationPlayerParams?> {
            return arrayOfNulls(size)
        }
    }

    fun getNextVerse(quranMeta: QuranMeta, curVerse: Pair<Int, Int>? = null): Pair<Int, Int>? {
        val currentVerse = curVerse ?: this.currentVerse

        val currentChapterNo = currentVerse.first
        val currentVerseNo = currentVerse.second
        val lastChapterNo = lastVerse.first
        val lastVerseNo = lastVerse.second

        if (!QuranMeta.isChapterValid(currentChapterNo) || !quranMeta.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = currentVerseNo + 1

        if (nextVerseNo > quranMeta.getChapterVerseCount(currentChapterNo)) {
            // If we are at the last verse of the chapter, go to the first verse of the next chapter if possible.
            // Otherwise, stay at the current verse.
            nextVerseNo = if (QuranMeta.isChapterValid(nextChapterNo + 1)) {
                nextChapterNo++
                1
            } else {
                quranMeta.getChapterVerseCount(currentChapterNo)
            }
        }

        if (nextChapterNo > lastChapterNo || nextVerseNo > lastVerseNo) {
            nextChapterNo = -1
            nextVerseNo = -1
        }

        if (nextChapterNo == -1 && nextVerseNo == -1) {
            return null
        }

        return Pair(nextChapterNo, nextVerseNo)
    }


    fun getPreviousVerse(quranMeta: QuranMeta): Pair<Int, Int>? {
        val currentChapterNo = currentVerse.first
        val currentVerseNo = currentVerse.second
        val firstChapterNo = firstVerse.first
        val firstVerseNo = firstVerse.second

        if (!QuranMeta.isChapterValid(currentChapterNo) || !quranMeta.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var previousChapterNo = currentChapterNo
        var previousVerseNo = currentVerseNo - 1
        if (previousVerseNo < 1) {
            // If we are at the first verse of the chapter, go to the last verse of the previous chapter if possible.
            // Otherwise, stay at the current verse.
            previousVerseNo = if (QuranMeta.isChapterValid(previousChapterNo - 1)) {
                previousChapterNo--
                quranMeta.getChapterVerseCount(previousChapterNo)
            } else {
                1
            }
        }

        // If the previous verse or chapter goes beyond the current range,
        // then return -1 for both so that the player doesn't change anything.
        if (previousChapterNo < firstChapterNo || previousVerseNo < firstVerseNo) {
            previousChapterNo = -1
            previousVerseNo = -1
        }

        if (previousChapterNo == -1 && previousVerseNo == -1) {
            return null
        }

        return Pair(previousChapterNo, previousVerseNo)
    }

    /**
     * @return Returns true if the player has previous verse within the current playable verse range.
     */
    fun hasNextVerse(quranMeta: QuranMeta): Boolean {
        val currentChapterNo = currentVerse.first
        val currentVerseNo = currentVerse.second
        val lastChapterNo = lastVerse.first
        val lastVerseNo = lastVerse.second

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !quranMeta.isVerseValid4Chapter(currentChapterNo, currentVerseNo)
        ) {
            return false
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = currentVerseNo + 1

        if (nextVerseNo > quranMeta.getChapterVerseCount(currentChapterNo)) {
            nextChapterNo += 1
            nextVerseNo = 1
        }

        return nextChapterNo <= lastChapterNo && nextVerseNo <= lastVerseNo
    }

    /**
     * @return Returns true if the player has previous verse within the current playable verse range.
     */
    fun hasPreviousVerse(quranMeta: QuranMeta): Boolean {
        val currentChapterNo = currentVerse.first
        val currentVerseNo = currentVerse.second
        val firstChapterNo = firstVerse.first
        val firstVerseNo = firstVerse.second

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !quranMeta.isVerseValid4Chapter(currentChapterNo, currentVerseNo)
        ) {
            return false
        }

        var prevChapterNo = currentChapterNo
        var prevVerseNo = currentVerseNo - 1

        if (prevVerseNo < 1) {
            prevChapterNo -= 1
            prevVerseNo = quranMeta.getChapterVerseCount(prevChapterNo)
        }

        return prevChapterNo >= firstChapterNo && prevVerseNo >= firstVerseNo
    }

    fun isCurrentVerse(verse: Pair<Int, Int>): Boolean {
        return isCurrentVerse(verse.first, verse.second)
    }

    fun isCurrentVerse(chapterNo: Int, verseNo: Int): Boolean {
        return currentVerse.first == chapterNo && currentVerse.second == verseNo
    }

    val currentChapterNo get() = currentVerse.first
    val currentVerseNo get() = currentVerse.second
}