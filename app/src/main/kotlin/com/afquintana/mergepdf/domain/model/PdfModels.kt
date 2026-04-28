package com.afquintana.mergepdf.domain.model

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

data class SelectedPdf(
    val id: String,
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val pages: List<PdfPagePreview>,
)

data class PdfPagePreview(
    val pageNumber: Int,
    val thumbnail: Bitmap,
)

data class MergeOutput(
    val file: File,
    val displayName: String,
    val pageCount: Int,
)
