package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.FontResolver


open class ReaderProviderViewModel(application: Application) : AndroidViewModel(application) {
    val controller = RecitationController.getInstance(application)
    val userRepository = DatabaseProvider.getUserRepository(application)
    val repository = DatabaseProvider.getQuranRepository(application)
    val fontResolver = FontResolver.getInstance(application)
}