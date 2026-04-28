package com.afquintana.mergepdf.core.analytics

interface AppAnalytics {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun recordException(throwable: Throwable)
}
