package com.afquintana.mergepdf.presentation.result

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afquintana.mergepdf.R
import com.afquintana.mergepdf.core.ui.MergeBlue
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.presentation.merge.MergeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_RESULT_PREVIEW_PAGES = 60

@Composable
fun ResultScreen(
    state: MergeUiState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShare: (MergeOutput) -> Unit,
    onView: (MergeOutput) -> Unit,
) {
    val output = state.output

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                    Text(
                        text = stringResource(R.string.screen_results_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                IconButton(onClick = onHome) {
                    Icon(
                        Icons.Rounded.Home,
                        contentDescription = stringResource(R.string.content_description_home),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (output == null) {
                Text(stringResource(R.string.no_pdfs_created))
                return@Column
            }

            ResultPagesGrid(
                output = output,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            state.error?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onShare(output) },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MergeBlue),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Text(
                        text = stringResource(R.string.share_button),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = { onView(output) },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MergeBlue),
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null)
                    Text(
                        text = stringResource(R.string.view_button),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultPagesGrid(
    output: MergeOutput,
    modifier: Modifier = Modifier,
) {
    var previews by remember(output.file.absolutePath) { mutableStateOf<List<ResultPagePreview>?>(null) }

    LaunchedEffect(output.file.absolutePath) {
        previews = withContext(Dispatchers.IO) {
            output.renderPages(limit = MAX_RESULT_PREVIEW_PAGES)
        }
    }

    if (previews == null) {
        CircularProgressIndicator(
            modifier = modifier.padding(28.dp),
            color = MergeBlue,
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(previews.orEmpty(), key = { it.pageNumber }) { preview ->
            ResultPageTile(preview = preview)
        }
        val remainingPages = (output.pageCount - MAX_RESULT_PREVIEW_PAGES).coerceAtLeast(0)
        if (remainingPages > 0) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Text(
                    text = stringResource(R.string.remaining_pages_message, remainingPages),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ResultPageTile(preview: ResultPagePreview) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp),
                ),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Image(
                bitmap = preview.thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        Text(
            text = preview.pageNumber.toString(),
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun MergeOutput.renderPages(limit: Int): List<ResultPagePreview> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    return descriptor.use {
        PdfRenderer(it).use { renderer ->
            val count = renderer.pageCount.coerceAtMost(limit)
            (0 until count).map { index ->
                renderer.openPage(index).use { page ->
                    val width = 320
                    val height = (width * page.height / page.width.toFloat()).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                    ResultPagePreview(pageNumber = index + 1, thumbnail = bitmap)
                }
            }
        }
    }
}

private data class ResultPagePreview(
    val pageNumber: Int,
    val thumbnail: Bitmap,
)
