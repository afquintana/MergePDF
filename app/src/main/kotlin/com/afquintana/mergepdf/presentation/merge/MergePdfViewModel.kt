package com.afquintana.mergepdf.presentation.merge

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afquintana.mergepdf.R
import com.afquintana.mergepdf.core.analytics.AppAnalytics
import com.afquintana.mergepdf.core.file.OutputFolderStore
import com.afquintana.mergepdf.core.ui.StringProvider
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.domain.model.SelectedPdf
import com.afquintana.mergepdf.domain.usecase.MergePdfsUseCase
import com.afquintana.mergepdf.domain.usecase.ReadPdfUseCase
import com.afquintana.mergepdf.domain.usecase.RenderPdfPagesUseCase
import com.afquintana.mergepdf.domain.usecase.SavePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    private val readPdf: ReadPdfUseCase,
    private val renderPdfPages: RenderPdfPagesUseCase,
    private val mergePdfs: MergePdfsUseCase,
    private val savePdf: SavePdfUseCase,
    private val analytics: AppAnalytics,
    private val strings: StringProvider,
    private val outputFolderStore: OutputFolderStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState

    private var pendingMerge: List<SelectedPdf> = emptyList()
    private var pendingExcludedPages: Map<String, Set<Int>> = emptyMap()

    fun onPdfsPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isBusy = true, error = null, message = null) }
                val existingIds = _uiState.value.selectedPdfs.map { it.id }.toSet()
                val newPdfs = uris
                    .filterNot { it.toString() in existingIds }
                    .map { uri -> readPdf(uri) }
                val selectedPdfs = preparePagePreviews(_uiState.value.selectedPdfs + newPdfs)
                analytics.logEvent("pdfs_selected", mapOf("files" to newPdfs.size.toString()))
                _uiState.update {
                    it.copy(
                        screen = MergeScreen.Review,
                        selectedPdfs = selectedPdfs,
                        isBusy = false,
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun goHome() {
        _uiState.value = MergeUiState()
    }

    fun goToReview() {
        _uiState.update { it.copy(screen = MergeScreen.Review) }
    }

    fun removePdf(pdfId: String) {
        _uiState.update { state ->
            state.copy(
                selectedPdfs = state.selectedPdfs.filterNot { it.id == pdfId },
                selectedPageIds = state.selectedPageIds.filterNot {
                    it.substringBeforeLast(PAGE_ID_SEPARATOR) == pdfId
                }.toSet(),
            )
        }
    }

    fun togglePageSelection(pdfId: String, pageNumber: Int) {
        val pageId = pageId(pdfId, pageNumber)
        _uiState.update { state ->
            val selectedPages = if (pageId in state.selectedPageIds) {
                state.selectedPageIds - pageId
            } else {
                state.selectedPageIds + pageId
            }
            state.copy(selectedPageIds = selectedPages)
        }
    }

    fun setRemoveSelectedPages(remove: Boolean) {
        _uiState.update { it.copy(removeSelectedPages = remove) }
    }

    fun requestOutputFolder() {
        val state = _uiState.value
        val selectedPdfs = state.selectedPdfs
        if (selectedPdfs.size < 2) {
            _uiState.update { it.copy(error = strings.get(R.string.error_invalid_merge_selection)) }
            return
        }
        val excludedPages = if (state.removeSelectedPages) {
            state.selectedPageIds
                .mapNotNull { selectedPageId ->
                    val pdfId = selectedPageId.substringBeforeLast(PAGE_ID_SEPARATOR)
                    val pageNumber = selectedPageId.substringAfterLast(PAGE_ID_SEPARATOR).toIntOrNull()
                    pageNumber?.let { pdfId to it }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, pageNumbers) -> pageNumbers.toSet() }
        } else {
            emptyMap()
        }
        val remainingPages = selectedPdfs.sumOf { pdf ->
            pdf.pageCount - excludedPages[pdf.id].orEmpty().size
        }
        if (remainingPages <= 0) {
            _uiState.update { it.copy(error = strings.get(R.string.error_empty_merge_output)) }
            return
        }

        pendingMerge = selectedPdfs
        pendingExcludedPages = excludedPages
        _uiState.update {
            it.copy(
                outputFolderPickerRequest = it.outputFolderPickerRequest + 1,
                initialOutputFolder = outputFolderStore.getLastFolder(),
                error = null,
            )
        }
    }

    fun onOutputFolderPicked(folderUri: Uri?) {
        val selectedPdfs = pendingMerge
        if (selectedPdfs.isEmpty()) return
        if (folderUri == null) {
            pendingMerge = emptyList()
            pendingExcludedPages = emptyMap()
            return
        }

        outputFolderStore.saveLastFolder(folderUri)
        pendingMerge = emptyList()
        val excludedPages = pendingExcludedPages
        pendingExcludedPages = emptyMap()
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isBusy = true, error = null, message = null) }
                val output = mergePdfs(selectedPdfs, excludedPages, folderUri)
                analytics.logEvent(
                    "pdfs_merged",
                    mapOf(
                        "files" to selectedPdfs.size.toString(),
                        "pages" to output.pageCount.toString(),
                    ),
                )
                _uiState.update {
                    it.copy(
                        screen = MergeScreen.Result,
                        output = output,
                        isBusy = false,
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun saveOutput(output: MergeOutput) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isBusy = true, message = null) }
                savePdf(output)
                analytics.logEvent("pdf_saved")
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = strings.get(R.string.message_pdf_saved, output.displayName),
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private fun showError(throwable: Throwable) {
        analytics.recordException(throwable)
        _uiState.update {
            it.copy(
                isBusy = false,
                error = strings.get(R.string.error_generic_pdf_processing),
            )
        }
    }

    private suspend fun preparePagePreviews(pdfs: List<SelectedPdf>): List<SelectedPdf> {
        val totalPages = pdfs.sumOf { it.pageCount }
        return if (totalPages > MAX_PREVIEW_PAGES) {
            pdfs.map { pdf -> pdf.copy(pages = pdf.pages.take(1)) }
        } else {
            pdfs.map { pdf ->
                if (pdf.pages.size >= pdf.pageCount) {
                    pdf
                } else {
                    pdf.copy(pages = renderPdfPages(pdf))
                }
            }
        }
    }

    private companion object {
        const val MAX_PREVIEW_PAGES = 50
    }
}

data class MergeUiState(
    val screen: MergeScreen = MergeScreen.Home,
    val selectedPdfs: List<SelectedPdf> = emptyList(),
    val selectedPageIds: Set<String> = emptySet(),
    val removeSelectedPages: Boolean = false,
    val output: MergeOutput? = null,
    val outputFolderPickerRequest: Long = 0L,
    val initialOutputFolder: Uri? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

enum class MergeScreen {
    Home,
    Review,
    Result,
}

private const val PAGE_ID_SEPARATOR = "#"

fun pageId(pdfId: String, pageNumber: Int): String = "$pdfId$PAGE_ID_SEPARATOR$pageNumber"
