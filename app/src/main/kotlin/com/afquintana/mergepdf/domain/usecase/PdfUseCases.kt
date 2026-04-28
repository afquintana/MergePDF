package com.afquintana.mergepdf.domain.usecase

import android.net.Uri
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.domain.model.SelectedPdf
import com.afquintana.mergepdf.domain.repository.PdfRepository
import javax.inject.Inject

class ReadPdfUseCase @Inject constructor(
    private val repository: PdfRepository,
) {
    suspend operator fun invoke(uri: Uri) = repository.readPdf(uri)
}

class MergePdfsUseCase @Inject constructor(
    private val repository: PdfRepository,
) {
    suspend operator fun invoke(
        pdfs: List<SelectedPdf>,
        outputFolder: Uri,
    ): MergeOutput = repository.merge(pdfs, outputFolder)
}

class SavePdfUseCase @Inject constructor(
    private val repository: PdfRepository,
) {
    suspend operator fun invoke(output: MergeOutput): Uri = repository.saveToDocuments(output)
}
