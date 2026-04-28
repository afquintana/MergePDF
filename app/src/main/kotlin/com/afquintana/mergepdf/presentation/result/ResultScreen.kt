package com.afquintana.mergepdf.presentation.result

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afquintana.mergepdf.R
import com.afquintana.mergepdf.core.ui.MergeBlue
import com.afquintana.mergepdf.domain.model.MergeOutput
import com.afquintana.mergepdf.presentation.merge.MergeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                .padding(horizontal = 18.dp, vertical = 14.dp),
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
            Spacer(Modifier.height(18.dp))
            if (output == null) {
                Text(stringResource(R.string.no_pdfs_created))
                return@Column
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(0.72f),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        PdfOutputPreview(
                            output = output,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = output.displayName,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.output_file_details,
                            output.file.length() / 1024L,
                            output.pageCount,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            state.error?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
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
private fun PdfOutputPreview(
    output: MergeOutput,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(output.file.absolutePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(output.file.absolutePath) {
        bitmap = withContext(Dispatchers.IO) { output.renderFirstPage() }
    }

    if (bitmap == null) {
        CircularProgressIndicator(modifier = modifier.padding(28.dp))
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = output.displayName,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

private fun MergeOutput.renderFirstPage(): Bitmap? {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    return descriptor.use {
        PdfRenderer(it).use { renderer ->
            if (renderer.pageCount == 0) return null
            renderer.openPage(0).use { page ->
                val width = 360
                val height = (width * page.height / page.width.toFloat()).toInt()
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }
}
