package com.afquintana.mergepdf.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FirebaseAppAnalytics @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AppAnalytics {

    override fun logEvent(name: String, params: Map<String, String>) {
        runCatching {
            val bundle = Bundle().apply {
                params.forEach { (key, value) -> putString(key, value) }
            }
            FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
        }
    }

    override fun recordException(throwable: Throwable) {
        runCatching {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }
}
