package com.afquintana.mergepdf.data.pdf

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.domain.model.PdfPagePreview
import com.afquintana.mergepdf.domain.model.SelectedPdf
import com.afquintana.mergepdf.domain.repository.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
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
        val pdfInfo = resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                PdfInfo(
                    pageCount = renderer.pageCount,
                    pages = renderer.renderFirstPagePreview(),
                )
            }
        } ?: error("Unable to open the PDF")

        SelectedPdf(
            id = uri.toString(),
            uri = uri,
            name = resolver.displayName(uri).orEmpty().ifBlank { "document.pdf" },
            pageCount = pdfInfo.pageCount,
            pages = pdfInfo.pages,
        )
    }

    override suspend fun renderPages(pdf: SelectedPdf): List<PdfPagePreview> =
        withContext(Dispatchers.IO) {
            resolver.openFileDescriptor(pdf.uri, "r")?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.renderPages()
                }
            } ?: error("Unable to open the PDF")
        }

    override suspend fun merge(
        pdfs: List<SelectedPdf>,
        excludedPages: Map<String, Set<Int>>,
        outputFolder: Uri,
    ): MergeOutput =
        withContext(Dispatchers.IO) {
            check(pdfs.size >= 2) { "Select at least two PDFs" }

            val outputDir = File(context.cacheDir, "merge_outputs").apply { mkdirs() }
            val targetFolder = DocumentFile.fromTreeUri(context, outputFolder)
                ?: error("Unable to open the selected folder")
            val timestamp = System.currentTimeMillis()
            val fileName = "UnirPDF_$timestamp.pdf"
            val file = File(outputDir, fileName)
            var outputPageCount = 0

            PDDocument().use { target ->
                val sourceDocuments = mutableListOf<PDDocument>()
                try {
                    pdfs.forEach { pdf ->
                        val excluded = excludedPages[pdf.id].orEmpty()
                        val source = resolver.openInputStream(pdf.uri)?.use { input ->
                            PDDocument.load(input)
                        } ?: error("Unable to read ${pdf.name}")
                        sourceDocuments += source

                        (1..source.numberOfPages)
                            .filterNot { pageNumber -> pageNumber in excluded }
                            .forEach { pageNumber ->
                                val sourcePage = source.getPage(pageNumber - 1)
                                val importedPage = target.importPage(sourcePage)
                                importedPage.mediaBox = sourcePage.mediaBox
                                importedPage.cropBox = sourcePage.cropBox
                                importedPage.rotation = sourcePage.rotation
                                outputPageCount++
                            }
                    }
                    check(outputPageCount > 0) { "No pages to merge" }
                    target.save(file)
                } finally {
                    sourceDocuments.forEach { source ->
                        runCatching { source.close() }
                    }
                }
            }

            targetFolder.copyPdf(file, fileName)
            MergeOutput(
                file = file,
                displayName = fileName,
                pageCount = outputPageCount,
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
                "UnirPDF",
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
                "${Environment.DIRECTORY_DOCUMENTS}/UnirPDF",
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

    private fun PdfRenderer.renderPages(): List<PdfPagePreview> {
        if (pageCount == 0) {
            val emptyPage = Bitmap.createBitmap(320, 452, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
            return listOf(PdfPagePreview(pageNumber = 1, thumbnail = emptyPage))
        }

        return (0 until pageCount).map { index ->
            openPage(index).use { page ->
                val width = 320
                val height = (width * page.height / page.width.toFloat()).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(Color.WHITE)
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                PdfPagePreview(pageNumber = index + 1, thumbnail = bitmap)
            }
        }
    }

    private fun PdfRenderer.renderFirstPagePreview(): List<PdfPagePreview> {
        if (pageCount == 0) {
            val emptyPage = Bitmap.createBitmap(320, 452, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
            return listOf(PdfPagePreview(pageNumber = 1, thumbnail = emptyPage))
        }

        return listOf(renderPage(0))
    }

    private fun PdfRenderer.renderPage(index: Int): PdfPagePreview =
        openPage(index).use { page ->
            val width = 320
            val height = (width * page.height / page.width.toFloat()).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(Color.WHITE)
                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            PdfPagePreview(pageNumber = index + 1, thumbnail = bitmap)
        }

    private data class PdfInfo(
        val pageCount: Int,
        val pages: List<PdfPagePreview>,
    )
}
