package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ComicBook
import com.example.ui.ComicViewModel
import com.example.ui.library.ComicLibraryScreen
import com.example.ui.monetization.AdSlotOverlayDialog
import com.example.ui.monetization.PaywallDialog
import com.example.ui.parchment.ParchmentStoryDialog
import com.example.ui.reader.ComicLoreSheet
import com.example.ui.reader.ComicReaderScreen
import com.example.ui.sequence.SequenceManagerSheet
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.CyberComicsTheme
import com.example.ui.theme.CyberMidnight

/**
 * Single-activity Compose host enabling edge-to-edge rendering and delegating all catalog,
 * localization, reader, and monetization state to [ComicViewModel].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ComicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CyberComicsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberMidnight
                ) {
                    ComicAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Root state-driven router switching between:
 * 1. Launch [SplashScreen] (`uiState.isSplashActive == true`)
 * 2. Fullscreen [ComicReaderScreen] (`uiState.activeComic != null`)
 * 3. Home [ComicLibraryScreen] (`uiState.activeComic == null`)
 *
 * Monetization dialogs ([PaywallDialog] and [AdSlotOverlayDialog]) remain scoped at this root level
 * so users can unlock locked next/picked stories seamlessly without leaving or tearing down the reader.
 */
@Composable
fun ComicAppContent(viewModel: ComicViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sequenceModalComic by remember { mutableStateOf<ComicBook?>(null) }

    // Instant launch splash screen showing Hodja preaching from top of minaret
    if (uiState.isSplashActive) {
        SplashScreen(
            currentLanguage = uiState.currentLanguage,
            onSelectLanguage = { lang ->
                viewModel.setLanguage(lang)
            },
            onStartReading = {
                viewModel.dismissSplash(openReaderDirectly = true)
            },
            onEnterLibrary = {
                viewModel.dismissSplash(openReaderDirectly = false)
            }
        )
        return
    }

    val activeComic = uiState.activeComic

    if (activeComic != null) {
        // Fullscreen Comic Reader Mode (keyed by activeComic.id so pager/zoom state resets cleanly on story switch)
        key(activeComic.id) {
            ComicReaderScreen(
                comic = activeComic,
                allComics = uiState.comics,
                isPremiumUnlocked = uiState.isPremiumUnlocked,
                seriesUnlocked = uiState.seriesUnlocked,
                adUnlockedMaxStory = uiState.adUnlockedMaxStory,
                initialPageIndex = uiState.currentPageIndex,
                currentLanguage = uiState.currentLanguage,
                readingMode = uiState.readingMode,
                readerBackground = uiState.readerBackground,
                controlsVisible = uiState.isFullscreenControlsVisible,
                isThumbnailDrawerOpen = uiState.isThumbnailDrawerOpen,
                showSpeechBubbles = uiState.showSpeechBubbles,
                onPageChanged = { newPage ->
                    viewModel.setCurrentPage(newPage)
                },
                onToggleControls = {
                    viewModel.toggleControlsVisibility()
                },
                onToggleThumbnails = { open ->
                    viewModel.toggleThumbnailDrawer(open)
                },
                onToggleSpeechBubbles = {
                    viewModel.toggleSpeechBubbles()
                },
                onOpenSequenceManager = {
                    viewModel.toggleSequenceSheet(true)
                },
                onOpenLore = {
                    viewModel.toggleLoreSheet(true)
                },
                onChangeReadingMode = { mode ->
                    viewModel.setReadingMode(mode)
                },
                onChangeBackground = { bg ->
                    viewModel.setReaderBackground(bg)
                },
                onSelectComic = { selected ->
                    viewModel.openComic(selected, 0)
                },
                onPreviousComic = {
                    viewModel.openPreviousComic()
                },
                onNextComic = {
                    viewModel.openNextComic()
                },
                onClose = {
                    viewModel.closeComic()
                }
            )
        }

        // Overlay Lore Sheet for dialogue and sermon text
        if (uiState.isLoreSheetOpen) {
            val currentPage = activeComic.pages.getOrNull(uiState.currentPageIndex)
            ComicLoreSheet(
                comic = activeComic,
                currentPage = currentPage,
                currentLanguage = uiState.currentLanguage,
                onDismiss = { viewModel.toggleLoreSheet(false) }
            )
        }

        // Overlay Sequence Manager Sheet while inside reader
        if (uiState.isSequenceSheetOpen) {
            SequenceManagerSheet(
                comic = activeComic,
                currentLanguage = uiState.currentLanguage,
                onDismiss = { viewModel.toggleSequenceSheet(false) },
                onSaveNewSequence = { newPages ->
                    viewModel.reorderPages(activeComic.id, newPages)
                },
                onSelectPageToRead = { pageIdx ->
                    viewModel.setCurrentPage(pageIdx)
                    viewModel.toggleSequenceSheet(false)
                }
            )
        }
    } else {
        // Library Screen
        ComicLibraryScreen(
            comics = uiState.comics,
            currentLanguage = uiState.currentLanguage,
            isPremiumUnlocked = uiState.isPremiumUnlocked,
            seriesUnlocked = uiState.seriesUnlocked,
            adUnlockedMaxStory = uiState.adUnlockedMaxStory,
            isLoading = uiState.isLoading,
            snackbarMessage = uiState.snackbarMessage,
            onClearSnackbar = { viewModel.clearSnackbar() },
            onSelectLanguage = { lang ->
                viewModel.setLanguage(lang)
            },
            onOpenSplash = {
                viewModel.showSplash()
            },
            onOpenPaywall = {
                viewModel.openPaywall()
            },
            onOpenComic = { comic ->
                viewModel.openComic(comic)
            },
            onOpenParchment = { comic ->
                viewModel.openParchment(comic)
            },
            onManageSequence = { comic ->
                sequenceModalComic = comic
            },
            onDeleteComic = { comicId ->
                viewModel.deleteComic(comicId)
            },
            onImportFolder = { uri ->
                viewModel.importFolder(uri)
            },
            onImportMultipleImages = { uris ->
                viewModel.importMultipleImages(uris)
            }
        )

        // Sequence Manager Sheet opened from library
        val modalComic = sequenceModalComic
        if (modalComic != null) {
            SequenceManagerSheet(
                comic = modalComic,
                currentLanguage = uiState.currentLanguage,
                onDismiss = { sequenceModalComic = null },
                onSaveNewSequence = { newPages ->
                    viewModel.reorderPages(modalComic.id, newPages)
                },
                onSelectPageToRead = { pageIdx ->
                    sequenceModalComic = null
                    viewModel.openComic(modalComic, pageIdx)
                }
            )
        }
    }

    // Illuminated Parchment Story Scroll opened from Library
    val parchmentComic = uiState.parchmentComic
    if (parchmentComic != null) {
        ParchmentStoryDialog(
            comic = parchmentComic,
            currentLanguage = uiState.currentLanguage,
            onDismiss = { viewModel.closeParchment() }
        )
    }

    // Hybrid Paywall Modal Dialog
    if (uiState.isPaywallOpen) {
        val totalBuiltIn = uiState.comics.count { it.isBuiltIn }.coerceAtLeast(50)
        PaywallDialog(
            currentLanguage = uiState.currentLanguage,
            seriesUnlocked = uiState.seriesUnlocked,
            adUnlockedMaxStory = uiState.adUnlockedMaxStory,
            totalStoriesCount = totalBuiltIn,
            onBuyPro = { viewModel.buyFullVersion() },
            onBuySeries = { viewModel.buySeriesPack() },
            onWatchAd = { viewModel.startRewardedAdUnlock() },
            onRestore = { viewModel.restorePurchases() },
            onDismiss = { viewModel.closePaywall() }
        )
    }

    // Rewarded / Interstitial Ad Slot Overlay
    if (uiState.isAdSlotOpen) {
        AdSlotOverlayDialog(
            currentLanguage = uiState.currentLanguage,
            isRewarded = uiState.isRewardedAdMode,
            onComplete = { viewModel.completeAdSlot() },
            onUpgradePro = { viewModel.buyFullVersion() },
            onDismiss = { viewModel.dismissAdSlot() }
        )
    }
}
