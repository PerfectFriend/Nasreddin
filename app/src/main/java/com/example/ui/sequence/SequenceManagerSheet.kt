package com.example.ui.sequence

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.ui.reader.ComicPageArtwork
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.GaudiCoralRed
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AlphanumericComparator
import com.example.util.AppStrings

/**
 * Modal bottom sheet for inspecting, naturally sorting ([AlphanumericComparator]),
 * reordering, or pruning comic pages before saving back to [ComicViewModel].
 *
 * Awaits `sheetState.hide()` before invoking [onDismiss] or jumping to a tapped page so
 * the Compose sheet animation completes before navigation/state transitions occur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceManagerSheet(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSaveNewSequence: (List<ComicPage>) -> Unit,
    onSelectPageToRead: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var currentPages by remember(comic) { mutableStateOf(comic.pages.toList()) }

    val dismissWithHide: (() -> Unit) -> Unit = { afterHide ->
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                afterHide()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberSurfaceDark,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.sequenceSheetTitle(currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VanGoghStarGold
                    )
                    Text(
                        text = "${comic.title} (${currentPages.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberNeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { dismissWithHide(onDismiss) },
                    modifier = Modifier.testTag("close_sequence_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.close(currentLanguage),
                        tint = Color.White
                    )
                }
            }

            // Quick Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val comparator = AlphanumericComparator()
                        currentPages = currentPages.sortedWith { a, b -> comparator.compare(a.title, b.title) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_natural_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberSurfaceElevated,
                        contentColor = CyberNeonCyan
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.sortAZ(currentLanguage), fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        currentPages = currentPages.reversed()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sort_reverse_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberSurfaceElevated,
                        contentColor = VanGoghStarGold
                    )
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.sortReverse(currentLanguage), fontSize = 12.sp)
                }
            }

            // Page list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(currentPages, key = { _, page -> page.id }) { index, page ->
                    PageReorderCard(
                        page = page,
                        index = index,
                        totalCount = currentPages.size,
                        currentLanguage = currentLanguage,
                        onMoveUp = {
                            if (index > 0) {
                                val list = currentPages.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                                currentPages = list
                            }
                        },
                        onMoveDown = {
                            if (index < currentPages.size - 1) {
                                val list = currentPages.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                                currentPages = list
                            }
                        },
                        onDelete = {
                            if (currentPages.size > 1) {
                                currentPages = currentPages.filterIndexed { i, _ -> i != index }
                            }
                        },
                        onTapPage = {
                            dismissWithHide { onSelectPageToRead(index) }
                        }
                    )
                }
            }

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { dismissWithHide(onDismiss) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(AppStrings.cancel(currentLanguage), color = Color.White)
                }

                Button(
                    onClick = {
                        onSaveNewSequence(currentPages)
                        dismissWithHide(onDismiss)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_sequence_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanGoghStarGold,
                        contentColor = Color(0xFF2B1900)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.apply(currentLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PageReorderCard(
    page: ComicPage,
    index: Int,
    totalCount: Int,
    currentLanguage: AppLanguage,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onTapPage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTapPage() }
            .testTag("page_card_$index"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(VanGoghStarGold.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, VanGoghStarGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = VanGoghStarGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(54.dp, 64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
            ) {
                ComicPageArtwork(
                    page = page,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = page.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!page.dialogueText.isNullOrEmpty() || !page.narrativeText.isNullOrEmpty()) {
                    Text(
                        text = page.dialogueText ?: page.narrativeText ?: "",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = AppStrings.moveUp(currentLanguage),
                        tint = if (index > 0) CyberNeonCyan else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = AppStrings.moveDown(currentLanguage),
                        tint = if (index < totalCount - 1) CyberNeonCyan else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (totalCount > 1) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppStrings.deletePage(currentLanguage),
                            tint = GaudiCoralRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
