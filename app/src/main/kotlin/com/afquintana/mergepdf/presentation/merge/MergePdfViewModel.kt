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
    private val mergePdfs: MergePdfsUseCase,
    private val savePdf: SavePdfUseCase,
    private val analytics: AppAnalytics,
    private val strings: StringProvider,
    private val outputFolderStore: OutputFolderStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState

    private var pendingMerge: List<SelectedPdf> = emptyList()

    fun onPdfsPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isBusy = true, error = null, message = null) }
                val existingIds = _uiState.value.selectedPdfs.map { it.id }.toSet()
                val newPdfs = uris
                    .filterNot { it.toString() in existingIds }
                    .map { uri -> readPdf(uri) }
                analytics.logEvent("pdfs_selected", mapOf("files" to newPdfs.size.toString()))
                _uiState.update { state ->
                    state.copy(
                        screen = MergeScreen.Review,
                        selectedPdfs = state.selectedPdfs + newPdfs,
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

    fun toggleRemoval(pdfId: String) {
        _uiState.update { state ->
            state.copy(
                selectedPdfs = state.selectedPdfs.map { pdf ->
                    if (pdf.id == pdfId) {
                        pdf.copy(markedForRemoval = !pdf.markedForRemoval)
                    } else {
                        pdf
                    }
                },
            )
        }
    }

    fun requestOutputFolder() {
        val activePdfs = _uiState.value.selectedPdfs.filterNot { it.markedForRemoval }
        if (activePdfs.size < 2) {
            _uiState.update { it.copy(error = strings.get(R.string.error_invalid_merge_selection)) }
            return
        }

        pendingMerge = activePdfs
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
            return
        }

        outputFolderStore.saveLastFolder(folderUri)
        pendingMerge = emptyList()
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isBusy = true, error = null, message = null) }
                val output = mergePdfs(selectedPdfs, folderUri)
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
}

data class MergeUiState(
    val screen: MergeScreen = MergeScreen.Home,
    val selectedPdfs: List<SelectedPdf> = emptyList(),
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
