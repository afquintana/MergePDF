package com.afquintana.mergepdf.data.di

import com.afquintana.mergepdf.core.analytics.AppAnalytics
import com.afquintana.mergepdf.core.analytics.FirebaseAppAnalytics
import com.afquintana.mergepdf.data.pdf.AndroidPdfRepository
import com.afquintana.mergepdf.domain.repository.PdfRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindModule {
    @Binds
    @Singleton
    abstract fun bindPdfRepository(repository: AndroidPdfRepository): PdfRepository

    @Binds
    @Singleton
    abstract fun bindAnalytics(analytics: FirebaseAppAnalytics): AppAnalytics
}
