package com.afquintana.mergepdf.presentation.merge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afquintana.mergepdf.R
import com.afquintana.mergepdf.core.ui.MergeBlue
import com.afquintana.mergepdf.domain.model.PdfPagePreview
import com.afquintana.mergepdf.domain.model.SelectedPdf

@Composable
fun MergeReviewScreen(
    state: MergeUiState,
    onBack: () -> Unit,
    onAddPdfs: () -> Unit,
    onRemovePdf: (String) -> Unit,
    onPageClick: (String, Int) -> Unit,
    onRemoveSelectedPagesChange: (Boolean) -> Unit,
    onMerge: () -> Unit,
) {
    val selectedPagesCount = state.selectedPageIds.size
    val pageTiles = state.selectedPdfs.flatMap { pdf ->
        pdf.pages.map { page ->
            PdfPageTileData(
                pdf = pdf,
                page = page,
            )
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                            )
                        }
                        Text(
                            text = stringResource(R.string.screen_merge_title),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                item {
                    PdfNameList(
                        pdfs = state.selectedPdfs,
                        onRemovePdf = onRemovePdf,
                    )
                }
                pdfPageGrid(
                    pageTiles = pageTiles,
                    selectedPageIds = state.selectedPageIds,
                    onPageClick = onPageClick,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.remove_selected_pages_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = stringResource(
                            R.string.remove_selected_pages_description,
                            selectedPagesCount,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
                Switch(
                    checked = state.removeSelectedPages,
                    onCheckedChange = onRemoveSelectedPagesChange,
                )
            }
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = 45.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onAddPdfs,
                    enabled = !state.isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MergeBlue),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(
                        text = stringResource(R.string.add_pdf_button).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Button(
                    onClick = onMerge,
                    enabled = !state.isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MergeBlue),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.merge_pdf_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfNameList(
    pdfs: List<SelectedPdf>,
    onRemovePdf: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        pdfs.forEach { pdf ->
            PdfNameRow(pdf = pdf, onRemovePdf = { onRemovePdf(pdf.id) })
        }
    }
}

@Composable
private fun PdfNameRow(
    pdf: SelectedPdf,
    onRemovePdf: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pdf.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
        )
        IconButton(
            onClick = onRemovePdf,
            modifier = Modifier.size(28.dp),
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.pdfPageGrid(
    pageTiles: List<PdfPageTileData>,
    selectedPageIds: Set<String>,
    onPageClick: (String, Int) -> Unit,
) {
    items(pageTiles.chunked(3), key = { row -> row.joinToString { "${it.pdf.id}-${it.page.pageNumber}" } }) { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { tile ->
                PdfPageTile(
                    tile = tile,
                    selected = pageId(tile.pdf.id, tile.page.pageNumber) in selectedPageIds,
                    onClick = { onPageClick(tile.pdf.id, tile.page.pageNumber) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(3 - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PdfPageTile(
    tile: PdfPageTileData,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pdf = tile.pdf
    val page = tile.page
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MergeBlue else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp),
                ),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Image(
                bitmap = page.thumbnail.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        Text(
            text = stringResource(R.string.page_preview_label, page.pageNumber, pdf.name),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class PdfPageTileData(
    val pdf: SelectedPdf,
    val page: PdfPagePreview,
)
