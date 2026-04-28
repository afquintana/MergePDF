package com.afquintana.mergepdf.data.pdf

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.domain.model.SelectedPdf
import com.afquintana.mergepdf.domain.repository.PdfRepository
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidPdfRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PdfRepository {

    private val resolver: ContentResolver = context.contentResolver

    override suspend fun readPdf(uri: Uri): SelectedPdf = withContext(Dispatchers.IO) {
        val pageCount = resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
        } ?: error("Unable to open the PDF")

        SelectedPdf(
            id = uri.toString(),
            uri = uri,
            name = resolver.displayName(uri).orEmpty().ifBlank { "document.pdf" },
            pageCount = pageCount,
        )
    }

    override suspend fun merge(pdfs: List<SelectedPdf>, outputFolder: Uri): MergeOutput =
        withContext(Dispatchers.IO) {
            val activePdfs = pdfs.filterNot { it.markedForRemoval }
            check(activePdfs.size >= 2) { "Select at least two PDFs" }

            val outputDir = File(context.cacheDir, "merge_outputs").apply { mkdirs() }
            val targetFolder = DocumentFile.fromTreeUri(context, outputFolder)
                ?: error("Unable to open the selected folder")
            val timestamp = System.currentTimeMillis()
            val fileName = "MergePDF_$timestamp.pdf"
            val file = File(outputDir, fileName)

            val streams = activePdfs.map { pdf ->
                resolver.openInputStream(pdf.uri) ?: error("Unable to read ${pdf.name}")
            }
            try {
                PDFMergerUtility().apply {
                    destinationFileName = file.absolutePath
                    streams.forEach(::addSource)
                    mergeDocuments(null)
                }
            } finally {
                streams.forEach { stream -> runCatching { stream.close() } }
            }

            targetFolder.copyPdf(file, fileName)
            MergeOutput(
                file = file,
                displayName = fileName,
                pageCount = activePdfs.sumOf { it.pageCount },
            )
        }

    private fun DocumentFile.copyPdf(source: File, displayName: String) {
        findFile(displayName)?.delete()
        val document = createFile("application/pdf", displayName)
            ?: error("Unable to create the output file")
        resolver.openOutputStream(document.uri)?.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Unable to save the file")
    }

    override suspend fun saveToDocuments(output: MergeOutput): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "MergePDF",
            ).apply { mkdirs() }
            val target = File(directory, output.displayName)
            output.file.copyTo(target, overwrite = true)
            return@withContext Uri.fromFile(target)
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, output.displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/MergePDF",
            )
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create the output file")

        resolver.openOutputStream(uri)?.use { outputStream ->
            output.file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: error("Unable to save the file")

        uri
    }

    private fun ContentResolver.displayName(uri: Uri): String? =
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
}
