package com.afquintana.mergepdf.domain.model

import android.net.Uri
import java.io.File

data class SelectedPdf(
    val id: String,
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val markedForRemoval: Boolean = false,
)

data class MergeOutput(
    val file: File,
    val displayName: String,
    val pageCount: Int,
)
