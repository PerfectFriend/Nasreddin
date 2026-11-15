package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.GaudiCoralRed
import com.example.ui.theme.VanGoghSolarYellow
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Primary catalog home screen displaying the 100 localized Hodja Nasreddin episodes,
 * inline search & atmosphere/tier filters, Hybrid Monetization status banner,
 * SAF folder/image importers, and user-added folders.
 *
 * Reviewer notes:
 * - Language switcher uses an in-window [AnimatedVisibility] scrim instead of `DropdownMenu`
 *   (`PopupWindow`) to eliminate Android surface teardown warnings during rapid dismissal.
 * - [BackHandler] closes the language selector overlay before delegating system back events.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar with Language Switcher and Splash button
                item {
                    LibraryTopHeader(
                        currentLanguage = currentLanguage,
                        onOpenLanguageMenu = { langMenuExpanded = !langMenuExpanded },
                        onOpenSplash = onOpenSplash,
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        onPickFiles = { filesPickerLauncher.launch("image/*") }
                    )
                }

                // Hybrid Premium / Ad-Supported Banner
                item {
                    val builtInCount = comics.count { it.isBuiltIn }.coerceAtLeast(100)
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

                // Featured Comic Hero Banner (Story #1 — hidden while filtering so results stay above the fold)
                if (!isFilteringActive) {
                    val featuredComic = comics.firstOrNull { it.isBuiltIn && it.storyNumber == 1 }
                        ?: comics.firstOrNull { it.isBuiltIn }
                    if (featuredComic != null) {
                        item {
                            FeaturedComicBanner(
                                comic = featuredComic,
                                currentLanguage = currentLanguage,
                                onRead = { onOpenComic(featuredComic) },
                                onManageSequence = { onManageSequence(featuredComic) }
                            )
                        }
                    }

                    // Quick Import Action Buttons
                    item {
                        ImportActionsRow(
                            currentLanguage = currentLanguage,
                            onPickFolder = { folderPickerLauncher.launch(null) },
                            onPickFiles = { filesPickerLauncher.launch("image/*") }
                        )
                    }
                }

                // Search Bar & Atmosphere/Tier Filter Chips for 100-story catalog
                item {
                    LibrarySearchAndFiltersSection(
                        currentLanguage = currentLanguage,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSearchQueryChange = { searchQuery = it },
                        onSelectFilter = { selectedFilter = it }
                    )
                }

                // Section Title: Books in Library
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

                // Filtered Comics List (Strictly matching the active language)
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
                            onManageSequence = { onManageSequence(comic) },
                            onDelete = { onDeleteComic(comic.id) }
                        )
                    }
                }

                // Eastern Wisdom Parable Info Card
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan)
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.6f)),
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

@Composable
private fun PremiumPromoBanner(
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    unlockedCount: Int,
    totalBuiltInCount: Int,
    currentLanguage: AppLanguage,
    onOpenPaywall: () -> Unit
) {
    val isFullyAdFree = isPremiumUnlocked || seriesUnlocked
    val hasAdUnlockedStories = unlockedCount > 10 && !isFullyAdFree

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { if (!isPremiumUnlocked) onOpenPaywall() }
            .testTag("premium_promo_banner"),
        shape = RoundedCornerShape(14.dp),
        color = if (isFullyAdFree) CyberSurfaceDark else Color(0xFF261D0A),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isFullyAdFree) CyberNeonCyan.copy(alpha = 0.2f) else VanGoghStarGold.copy(alpha = 0.25f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = when {
                            isPremiumUnlocked -> AppStrings.unlockedStatusBanner(currentLanguage)
                            seriesUnlocked -> AppStrings.seriesUnlockedStatusBanner(currentLanguage)
                            else -> AppStrings.unlockFullVaultBanner(currentLanguage)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold
                    )
                    Text(
                        text = if (isFullyAdFree) {
                            AppStrings.unlockedStatusSubtext(currentLanguage, totalBuiltInCount)
                        } else {
                            AppStrings.hybridBannerSubtext(
                                lang = currentLanguage,
                                unlockedCount = unlockedCount,
                                totalCount = totalBuiltInCount,
                                isAdSupportedUnlocked = hasAdUnlockedStories
                            )
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            if (!isPremiumUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VanGoghStarGold,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = AppStrings.paywallSalePrice(),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2B1700),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTopHeader(
    currentLanguage: AppLanguage,
    onOpenLanguageMenu: () -> Unit,
    onOpenSplash: () -> Unit,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit
) {
    Surface(
        color = CyberSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(VanGoghStarGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF241500),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.appTitle(currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = VanGoghStarGold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = AppStrings.appSubtitle(currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberNeonCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language selector pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberMidnight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { onOpenLanguageMenu() }
                        .padding(end = 4.dp)
                        .testTag("library_lang_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLanguage.name.take(2),
                            fontWeight = FontWeight.Bold,
                            color = VanGoghStarGold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Splash screen button
                IconButton(
                    onClick = onOpenSplash,
                    modifier = Modifier.testTag("header_splash_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = AppStrings.splashTooltip(currentLanguage),
                        tint = VanGoghSolarYellow
                    )
                }

                IconButton(
                    onClick = onPickFolder,
                    modifier = Modifier.testTag("header_add_folder")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = AppStrings.loadFolderTitle(currentLanguage),
                        tint = VanGoghStarGold
                    )
                }
                IconButton(
                    onClick = onPickFiles,
                    modifier = Modifier.testTag("header_add_files")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = AppStrings.selectFilesTitle(currentLanguage),
                        tint = CyberNeonCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedComicBanner(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    onRead: () -> Unit,
    onManageSequence: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("featured_comic_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, VanGoghStarGold.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                if (comic.coverDrawableResId != null) {
                    Image(
                        painter = painterResource(id = comic.coverDrawableResId),
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberSurfaceElevated.copy(alpha = 0.8f),
                                    CyberSurfaceElevated
                                )
                            )
                        )
                )

                // Free Edition Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${AppStrings.freeBadge(currentLanguage)} • ${AppStrings.officialEdition(currentLanguage, comic.pages.size)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = VanGoghStarGold
                )
                Text(
                    text = comic.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberNeonCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = comic.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRead,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .testTag("featured_read_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanGoghStarGold,
                            contentColor = Color(0xFF2E1700)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (comic.lastReadPageIndex > 0) {
                                AppStrings.continueReading(currentLanguage, comic.lastReadPageIndex + 1)
                            } else {
                                AppStrings.readButton(currentLanguage)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onManageSequence,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("featured_sequence_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberMidnight,
                            contentColor = CyberNeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.pageSequence(currentLanguage, comic.pages.size),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportActionsRow(
    currentLanguage: AppLanguage,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPickFolder() }
                .testTag("import_folder_card"),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberNeonCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.loadFolderTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = AppStrings.loadFolderSub(currentLanguage),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPickFiles() }
                .testTag("import_files_card"),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(VanGoghStarGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = VanGoghStarGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.selectFilesTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = AppStrings.selectFilesSub(currentLanguage),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ComicListItemCard(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    onRead: () -> Unit,
    onManageSequence: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnlockedViaAd = !comic.isFree && !isPremiumUnlocked && !seriesUnlocked && comic.storyNumber <= adUnlockedMaxStory
    val isLocked = !comic.isFree && !isPremiumUnlocked && !seriesUnlocked && comic.storyNumber > adUnlockedMaxStory

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onRead() }
            .testTag("comic_item_${comic.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLocked) VanGoghStarGold.copy(alpha = 0.5f) else CyberSurfaceElevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (comic.coverDrawableResId != null) {
                    Image(
                        painter = painterResource(id = comic.coverDrawableResId),
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (comic.coverUriString != null) {
                    AsyncImage(
                        model = comic.coverUriString,
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = AppStrings.unlockCtaChip(currentLanguage),
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comic.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tier badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    when {
                        comic.isFree -> {
                            Text(
                                text = AppStrings.freeBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        isPremiumUnlocked || seriesUnlocked -> {
                            Text(
                                text = AppStrings.proUnlockedBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        isUnlockedViaAd -> {
                            Text(
                                text = AppStrings.adUnlockedBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text(
                                text = AppStrings.premiumBadge(currentLanguage),
                                color = VanGoghStarGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = " • ${AppStrings.pagesCountSuffix(currentLanguage, comic.pages.size)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                if (comic.lastReadPageIndex > 0) {
                    Text(
                        text = AppStrings.stoppedAtPage(currentLanguage, comic.lastReadPageIndex + 1),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onRead,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isLocked) VanGoghStarGold else VanGoghStarGold.copy(alpha = 0.2f),
                            contentColor = if (isLocked) Color(0xFF281500) else VanGoghStarGold
                        )
                    ) {
                        if (isLocked) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (isLocked) AppStrings.unlockCtaChip(currentLanguage) else AppStrings.readButton(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onManageSequence,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberSurfaceElevated,
                            contentColor = CyberNeonCyan
                        )
                    ) {
                        Text(AppStrings.pageOrderButton(currentLanguage), fontSize = 11.sp)
                    }
                }
            }

            if (!comic.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = AppStrings.deleteComic(currentLanguage),
                        tint = GaudiCoralRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WisdomCard(currentLanguage: AppLanguage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberMidnight),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = AppStrings.wisdomCardTitle(currentLanguage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = VanGoghStarGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AppStrings.wisdomCardText(currentLanguage),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun LibrarySearchAndFiltersSection(
    currentLanguage: AppLanguage,
    searchQuery: String,
    selectedFilter: LibraryFilter,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input"),
            singleLine = true,
            placeholder = {
                Text(
                    text = AppStrings.searchStoriesPlaceholder(currentLanguage),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = CyberNeonCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("library_search_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = AppStrings.clearSearchTooltip(currentLanguage),
                            tint = VanGoghStarGold
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurfaceDark,
                unfocusedContainerColor = CyberSurfaceDark,
                focusedBorderColor = CyberNeonCyan,
                unfocusedBorderColor = CyberSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = VanGoghStarGold
            )
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFilter(filter) },
                    modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}"),
                    label = {
                        Text(
                            text = AppStrings.libraryFilterLabel(currentLanguage, filter),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CyberSurfaceDark,
                        labelColor = Color.White.copy(alpha = 0.8f),
                        selectedContainerColor = VanGoghStarGold,
                        selectedLabelColor = Color(0xFF281500)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CyberSurfaceElevated,
                        selectedBorderColor = VanGoghStarGold
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyFilterCard(
    currentLanguage: AppLanguage,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("library_empty_filter_card"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = VanGoghStarGold.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = AppStrings.emptySearchTitle(currentLanguage),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppStrings.emptySearchSub(currentLanguage),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onReset,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("library_reset_filters_btn"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CyberNeonCyan.copy(alpha = 0.16f),
                    contentColor = CyberNeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = AppStrings.resetFiltersAction(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
