package com.afquintana.mergepdf.core.ui

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StringProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun get(@StringRes stringRes: Int, vararg args: Any): String =
        context.getString(stringRes, *args)
}
