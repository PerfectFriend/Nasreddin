package com.example.ui.reader

import androidx.compose.runtime.Composable
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.ui.parchment.ParchmentStoryDialog

/**
 * Presents the full parable narrative, dialogue lines, and moral written in the selected
 * language on an illuminated Eastern parchment scroll.
 */
@Composable
fun ComicLoreSheet(
    comic: ComicBook,
    currentPage: ComicPage?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    ParchmentStoryDialog(
        comic = comic,
        currentPage = currentPage,
        currentLanguage = currentLanguage,
        onDismiss = onDismiss
    )
}
