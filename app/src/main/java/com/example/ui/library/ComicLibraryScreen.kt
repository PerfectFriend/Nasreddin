package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Primary catalog home screen displaying the 50 curated Volume 1 Hodja Nasreddin episodes,
 * inline search & atmosphere/tier filters, Hybrid Monetization status banner,
 * SAF folder/image importers, and user-added folders.
 *
 * Modular UI sections live in:
 * - [LibraryHeaderAndBanners.kt] (header, promo banner, hero card, import cards, wisdom card)
 * - [LibrarySearchAndFilters.kt] (search field, filter chips, empty-filter recovery card)
 * - [LibraryCatalogCards.kt] (downsampled story list items)
 * - [LibraryFilter.kt] (pure filter & search predicate)
 *
 * Reviewer notes:
 * - Language switcher uses an in-window [AnimatedVisibility] scrim instead of `DropdownMenu`
 *   (`PopupWindow`) to eliminate Android surface teardown warnings during rapid dismissal.
 * - [BackHandler] closes the language selector overlay before delegating system back events.
 */
@Composable
fun ComicLibraryScreen(
    comics: List<ComicBook>,
    currentLanguage: AppLanguage,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    isLoading: Boolean,
    snackbarMessage: String?,
    onClearSnackbar: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenSplash: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenComic: (ComicBook) -> Unit,
    onOpenParchment: (ComicBook) -> Unit = {},
    onManageSequence: (ComicBook) -> Unit,
    onDeleteComic: (String) -> Unit,
    onImportFolder: (Uri) -> Unit,
    onImportMultipleImages: (List<Uri>) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }

    val filteredComics = remember(
        comics,
        searchQuery,
        selectedFilter,
        isPremiumUnlocked,
        seriesUnlocked,
        adUnlockedMaxStory
    ) {
        filterLibraryComics(
            comics = comics,
            query = searchQuery,
            selectedFilter = selectedFilter,
            isPremiumUnlocked = isPremiumUnlocked,
            seriesUnlocked = seriesUnlocked,
            adUnlockedMaxStory = adUnlockedMaxStory
        )
    }

    val isFilteringActive = searchQuery.isNotBlank() || selectedFilter != LibraryFilter.ALL

    BackHandler(enabled = langMenuExpanded) {
        langMenuExpanded = false
    }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onClearSnackbar()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onImportFolder(treeUri)
        }
    }

    val filesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportMultipleImages(uris)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CyberMidnight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_lazy_column"),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    LibraryTopHeader(
                        currentLanguage = currentLanguage,
                        onOpenLanguageMenu = { langMenuExpanded = !langMenuExpanded },
                        onOpenSplash = onOpenSplash,
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        onPickFiles = { filesPickerLauncher.launch("image/*") }
                    )
                }

                item {
                    val builtInCount = comics.count { it.isBuiltIn }.coerceAtLeast(50)
                    val unlockedCount = if (isPremiumUnlocked || seriesUnlocked) {
                        builtInCount
                    } else {
                        adUnlockedMaxStory.coerceIn(10, builtInCount)
                    }
                    PremiumPromoBanner(
                        isPremiumUnlocked = isPremiumUnlocked,
                        seriesUnlocked = seriesUnlocked,
                        unlockedCount = unlockedCount,
                        totalBuiltInCount = builtInCount,
                        currentLanguage = currentLanguage,
                        onOpenPaywall = onOpenPaywall
                    )
                }

                if (!isFilteringActive) {
                    val featuredComic = comics.firstOrNull { it.isBuiltIn && it.storyNumber == 1 }
                        ?: comics.firstOrNull { it.isBuiltIn }
                    if (featuredComic != null) {
                        item {
                            FeaturedComicBanner(
                                comic = featuredComic,
                                currentLanguage = currentLanguage,
                                onRead = { onOpenComic(featuredComic) },
                                onOpenParchment = { onOpenParchment(featuredComic) },
                                onManageSequence = { onManageSequence(featuredComic) }
                            )
                        }
                    }

                    item {
                        ImportActionsRow(
                            currentLanguage = currentLanguage,
                            onPickFolder = { folderPickerLauncher.launch(null) },
                            onPickFiles = { filesPickerLauncher.launch("image/*") }
                        )
                    }
                }

                item {
                    LibrarySearchAndFiltersSection(
                        currentLanguage = currentLanguage,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSearchQueryChange = { searchQuery = it },
                        onSelectFilter = { selectedFilter = it }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.collectionHeader(currentLanguage, filteredComics.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (filteredComics.isEmpty()) {
                    item {
                        EmptyFilterCard(
                            currentLanguage = currentLanguage,
                            onReset = {
                                searchQuery = ""
                                selectedFilter = LibraryFilter.ALL
                            }
                        )
                    }
                } else {
                    items(filteredComics, key = { it.id }) { comic ->
                        ComicListItemCard(
                            comic = comic,
                            currentLanguage = currentLanguage,
                            isPremiumUnlocked = isPremiumUnlocked,
                            seriesUnlocked = seriesUnlocked,
                            adUnlockedMaxStory = adUnlockedMaxStory,
                            onRead = { onOpenComic(comic) },
                            onOpenParchment = { onOpenParchment(comic) },
                            onManageSequence = { onManageSequence(comic) },
                            onDelete = { onDeleteComic(comic.id) }
                        )
                    }
                }

                item {
                    WisdomCard(currentLanguage = currentLanguage)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberNeonCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = VanGoghStarGold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = AppStrings.loadingImages(currentLanguage),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // In-window Language Menu Overlay (avoids native PopupWindow BLASTBufferQueue teardown warnings)
            AnimatedVisibility(
                visible = langMenuExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { langMenuExpanded = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 60.dp, end = 16.dp)
                            .widthIn(min = 210.dp, max = 260.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {},
                        shape = RoundedCornerShape(14.dp),
                        color = CyberSurfaceDark,
                        border = BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.6f)),
                        shadowElevation = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            listOf(
                                AppLanguage.RUSSIAN to "🇷🇺 Русский (RU)",
                                AppLanguage.SPANISH to "🇪🇸 Español (ES)",
                                AppLanguage.ENGLISH to "🇬🇧 English (EN)"
                            ).forEach { (lang, label) ->
                                val isSelected = currentLanguage == lang
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            langMenuExpanded = false
                                            onSelectLanguage(lang)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) VanGoghStarGold else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = VanGoghStarGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
