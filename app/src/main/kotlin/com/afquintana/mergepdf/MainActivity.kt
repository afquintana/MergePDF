package com.afquintana.mergepdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afquintana.mergepdf.core.ui.MergePdfTheme
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.presentation.home.HomeScreen
import com.afquintana.mergepdf.presentation.merge.MergePdfViewModel
import com.afquintana.mergepdf.presentation.merge.MergeReviewScreen
import com.afquintana.mergepdf.presentation.merge.MergeScreen
import com.afquintana.mergepdf.presentation.result.ResultScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MergePdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MergePdfTheme {
                MergePdfApp(
                    viewModel = viewModel,
                    onShare = ::sharePdf,
                    onView = ::viewPdf,
                )
            }
        }
    }

    private fun sharePdf(output: MergeOutput) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, output.toContentUri())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_pdf_chooser_title)))
    }

    private fun viewPdf(output: MergeOutput) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(output.toContentUri(), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.view_pdf_chooser_title)))
    }

    private fun MergeOutput.toContentUri(): Uri =
        FileProvider.getUriForFile(
            this@MainActivity,
            "$packageName.fileprovider",
            file,
        )
}

@Composable
private fun MergePdfApp(
    viewModel: MergePdfViewModel,
    onShare: (MergeOutput) -> Unit,
    onView: (MergeOutput) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        viewModel.onPdfsPicked(uris)
    }
    val outputFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, flags)
            }
        }
        viewModel.onOutputFolderPicked(uri)
    }

    LaunchedEffect(state.outputFolderPickerRequest) {
        if (state.outputFolderPickerRequest > 0) {
            outputFolderPicker.launch(state.initialOutputFolder)
        }
    }

    LaunchedEffect(state.message, state.error) {
        if (state.message != null || state.error != null) {
            kotlinx.coroutines.delay(3200)
            viewModel.clearMessage()
        }
    }

    val pickPdfs = { picker.launch(arrayOf("application/pdf")) }

    when (state.screen) {
        MergeScreen.Home -> HomeScreen(
            isBusy = state.isBusy,
            message = state.message,
            error = state.error,
            onPickPdf = pickPdfs,
        )

        MergeScreen.Review -> MergeReviewScreen(
            state = state,
            onBack = viewModel::goHome,
            onAddPdfs = pickPdfs,
            onRemovePdf = viewModel::removePdf,
            onPageClick = viewModel::togglePageSelection,
            onRemoveSelectedPagesChange = viewModel::setRemoveSelectedPages,
            onMerge = viewModel::requestOutputFolder,
        )

        MergeScreen.Result -> ResultScreen(
            state = state,
            onBack = viewModel::goToReview,
            onHome = viewModel::goHome,
            onShare = onShare,
            onView = onView,
        )
    }
}
