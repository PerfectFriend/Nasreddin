package com.example.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.VanGoghSolarYellow
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Modal bottom sheet presenting the active comic page's narrative text, dialogue/moral,
 * and Eastern folklore context while reading.
 *
 * Awaits `sheetState.hide()` before invoking [onDismiss] so Compose Material 3 tears down
 * the bottom sheet window cleanly without abrupt detached-surface warnings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicLoreSheet(
    comic: ComicBook,
    currentPage: ComicPage?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val dismissSafely: () -> Unit = {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismiss()
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = VanGoghStarGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.sermonTextTooltip(currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VanGoghStarGold
                    )
                }
                IconButton(
                    onClick = dismissSafely,
                    modifier = Modifier.testTag("close_lore_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.close(currentLanguage),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Page Details
            if (currentPage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentPage.title,
                                fontWeight = FontWeight.Bold,
                                color = CyberNeonCyan,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(VanGoghStarGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = AppStrings.pageCounter(currentLanguage, currentPage.sequenceNumber, comic.pages.size),
                                    color = VanGoghStarGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!currentPage.narrativeText.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentPage.narrativeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 22.sp
                            )
                        }

                        if (!currentPage.dialogueText.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberMidnight.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CyberNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = AppStrings.comicDialoguesLabel(currentLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentPage.dialogueText,
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = VanGoghSolarYellow,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Eastern Wisdom & Folklore Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberMidnight.copy(alpha = 0.9f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.wisdomCardTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        color = VanGoghStarGold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.wisdomCardText(currentLanguage),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
