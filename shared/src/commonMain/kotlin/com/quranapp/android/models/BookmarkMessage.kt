package com.quranapp.android.models

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strMsgBookmarkAddFailed
import com.quranapp.android.generated.resources.strMsgBookmarkAdded
import com.quranapp.android.generated.resources.strMsgBookmarkAddedAlready
import com.quranapp.android.generated.resources.strMsgBookmarkRemoveFailed
import com.quranapp.android.generated.resources.strMsgBookmarkRemoved
import org.jetbrains.compose.resources.StringResource

enum class BookmarkMessage(val messageRes: StringResource) {
    Added(Res.string.strMsgBookmarkAdded),
    AlreadyAdded(Res.string.strMsgBookmarkAddedAlready),
    AddFailed(Res.string.strMsgBookmarkAddFailed),
    Removed(Res.string.strMsgBookmarkRemoved),
    RemoveFailed(Res.string.strMsgBookmarkRemoveFailed),
}
