package com.afquintana.mergepdf.core.file

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutputFolderStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getLastFolder(): Uri? =
        preferences.getString(KEY_LAST_FOLDER_URI, null)?.let(Uri::parse)

    fun saveLastFolder(uri: Uri) {
        preferences.edit().putString(KEY_LAST_FOLDER_URI, uri.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "merge_pdf_output_folder"
        const val KEY_LAST_FOLDER_URI = "last_folder_uri"
    }
}
