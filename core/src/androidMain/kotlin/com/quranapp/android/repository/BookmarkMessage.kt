package com.quranapp.android.repository

import com.quranapp.android.core.R

enum class BookmarkMessage(val messageRes: Int) {
    Added(R.string.strMsgBookmarkAdded),
    AlreadyAdded(R.string.strMsgBookmarkAddedAlready),
    AddFailed(R.string.strMsgBookmarkAddFailed),
    Removed(R.string.strMsgBookmarkRemoved),
    RemoveFailed(R.string.strMsgBookmarkRemoveFailed),
}
