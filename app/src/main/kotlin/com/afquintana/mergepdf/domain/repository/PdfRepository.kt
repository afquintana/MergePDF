package com.afquintana.mergepdf.domain.repository

import android.net.Uri
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.domain.model.PdfPagePreview
import com.afquintana.mergepdf.domain.model.SelectedPdf

interface PdfRepository {
    suspend fun readPdf(uri: Uri): SelectedPdf
    suspend fun renderPages(pdf: SelectedPdf): List<PdfPagePreview>
    suspend fun merge(
        pdfs: List<SelectedPdf>,
        excludedPages: Map<String, Set<Int>>,
        outputFolder: Uri,
    ): MergeOutput
    suspend fun saveToDocuments(output: MergeOutput): Uri
}
