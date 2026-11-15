package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ComicRepository
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.model.ReaderBackground
import com.example.model.ReadingMode
import com.example.util.AlphanumericComparator
import com.example.util.AppStrings
import com.example.util.FolderImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single immutable UI state snapshot observed by [com.example.MainActivity] via lifecycle-aware flows.
 *
 * Hybrid access truth table for any [ComicBook]:
 * - `comic.isFree == true` (Stories 1..10) -> always unlocked & ad-free.
 * - `isPremiumUnlocked == true` (PRO $4.99) -> all stories unlocked & zero ads.
 * - `seriesUnlocked == true` ($0.99 pack) -> built-in series (11..100) unlocked & ad-free.
 * - `comic.storyNumber <= adUnlockedMaxStory` -> unlocked free via Rewarded Ad (periodic sponsor breaks apply).
 */
data class UiState(
    val comics: List<ComicBook> = emptyList(),
    val activeComic: ComicBook? = null,
    val currentPageIndex: Int = 0,
    val readingMode: ReadingMode = ReadingMode.HORIZONTAL_PAGER,
    val readerBackground: ReaderBackground = ReaderBackground.CYBER_DARK,
    val isFullscreenControlsVisible: Boolean = true,
    val isSequenceSheetOpen: Boolean = false,
    val isLoreSheetOpen: Boolean = false,
    val isThumbnailDrawerOpen: Boolean = false,
    val isSplashActive: Boolean = true,
    val isPaywallOpen: Boolean = false,
    val isPremiumUnlocked: Boolean = false,
    val seriesUnlocked: Boolean = false,
    val adUnlockedMaxStory: Int = 10,
    val isAdSlotOpen: Boolean = false,
    val isRewardedAdMode: Boolean = false,
    val pendingLockedComic: ComicBook? = null,
    val adSupportedReadsSinceBreak: Int = 0,
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val showSpeechBubbles: Boolean = true,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
) {
    fun isComicUnlocked(comic: ComicBook): Boolean {
        return comic.isFree ||
            isPremiumUnlocked ||
            seriesUnlocked ||
            comic.storyNumber <= adUnlockedMaxStory
    }
}

/**
 * Coordinates library catalog state, fullscreen reader navigation, page sequence persistence,
 * and Hybrid Monetization unlock flows (PRO / Series Pack / Rewarded Ad batches).
 */
class ComicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ComicRepository(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentLanguage.collect { lang ->
                _uiState.update { it.copy(currentLanguage = lang) }
            }
        }

        viewModelScope.launch {
            repository.isPremiumUnlocked.collect { unlocked ->
                _uiState.update { it.copy(isPremiumUnlocked = unlocked) }
            }
        }

        viewModelScope.launch {
            repository.seriesUnlocked.collect { unlocked ->
                _uiState.update { it.copy(seriesUnlocked = unlocked) }
            }
        }

        viewModelScope.launch {
            repository.adUnlockedMaxStory.collect { maxStory ->
                _uiState.update { it.copy(adUnlockedMaxStory = maxStory) }
            }
        }

        viewModelScope.launch {
            repository.comics.collect { list ->
                _uiState.update { state ->
                    val updatedActive = state.activeComic?.let { active ->
                        list.find { it.id == active.id } ?: active
                    }
                    state.copy(comics = list, activeComic = updatedActive)
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        _uiState.update { it.copy(snackbarMessage = AppStrings.languageSwitchedMessage(language)) }
    }

    fun dismissSplash(openReaderDirectly: Boolean = false) {
        _uiState.update { it.copy(isSplashActive = false) }
        if (openReaderDirectly) {
            val defaultComic = _uiState.value.comics.firstOrNull { it.isBuiltIn && it.language == _uiState.value.currentLanguage }
                ?: _uiState.value.comics.firstOrNull()
            if (defaultComic != null) {
                openComic(defaultComic, 0)
            }
        }
    }

    fun showSplash() {
        _uiState.update { it.copy(isSplashActive = true) }
    }

    fun openPaywall() {
        _uiState.update { it.copy(isPaywallOpen = true, isAdSlotOpen = false) }
    }

    fun closePaywall() {
        _uiState.update { it.copy(isPaywallOpen = false, pendingLockedComic = null) }
    }

    fun buyFullVersion() {
        repository.unlockPremium()
        val lang = _uiState.value.currentLanguage
        val pending = _uiState.value.pendingLockedComic
        _uiState.update {
            it.copy(
                isPaywallOpen = false,
                isAdSlotOpen = false,
                isPremiumUnlocked = true,
                pendingLockedComic = null,
                snackbarMessage = AppStrings.purchaseSuccessMessage(lang)
            )
        }
        if (pending != null) {
            openComic(pending, 0)
        }
    }

    fun buySeriesPack() {
        repository.unlockSeriesPack()
        val lang = _uiState.value.currentLanguage
        val pending = _uiState.value.pendingLockedComic
        val builtInCount = _uiState.value.comics.count { it.isBuiltIn }.coerceAtLeast(100)
        _uiState.update {
            it.copy(
                isPaywallOpen = false,
                isAdSlotOpen = false,
                seriesUnlocked = true,
                pendingLockedComic = null,
                snackbarMessage = AppStrings.seriesPurchaseSuccessMessage(lang, builtInCount)
            )
        }
        if (pending != null) {
            openComic(pending, 0)
        }
    }

    fun startRewardedAdUnlock() {
        _uiState.update {
            it.copy(
                isPaywallOpen = false,
                isAdSlotOpen = true,
                isRewardedAdMode = true
            )
        }
    }

    fun completeAdSlot() {
        val state = _uiState.value
        val lang = state.currentLanguage
        if (state.isRewardedAdMode) {
            val newMax = repository.unlockNextAdBatch()
            val pending = state.pendingLockedComic
            _uiState.update {
                it.copy(
                    isAdSlotOpen = false,
                    isRewardedAdMode = false,
                    adUnlockedMaxStory = newMax,
                    pendingLockedComic = null,
                    snackbarMessage = AppStrings.adBatchUnlockedMessage(lang, newMax)
                )
            }
            if (pending != null && pending.storyNumber <= newMax) {
                openComic(pending, 0)
            }
        } else {
            _uiState.update {
                it.copy(
                    isAdSlotOpen = false,
                    isRewardedAdMode = false
                )
            }
        }
    }

    fun dismissAdSlot() {
        _uiState.update {
            it.copy(
                isAdSlotOpen = false,
                isRewardedAdMode = false,
                pendingLockedComic = null
            )
        }
    }

    fun restorePurchases() {
        val restored = repository.restorePurchases()
        val lang = _uiState.value.currentLanguage
        val msg = if (restored) {
            AppStrings.purchaseSuccessMessage(lang)
        } else {
            when (lang) {
                AppLanguage.RUSSIAN -> "Предыдущие покупки не найдены"
                AppLanguage.SPANISH -> "No se encontraron compras anteriores"
                AppLanguage.ENGLISH -> "No prior purchases found"
            }
        }
        _uiState.update { it.copy(isPaywallOpen = false, snackbarMessage = msg) }
    }

    fun openComic(comic: ComicBook, startPageIndex: Int? = null) {
        // Hybrid Gated Check: free (1..10), ad-unlocked batch, series pack, or PRO
        if (!repository.isComicUnlocked(comic)) {
            _uiState.update { it.copy(pendingLockedComic = comic, isPaywallOpen = true) }
            return
        }

        val state = _uiState.value
        val isAdSupportedStory = !comic.isFree && !state.isPremiumUnlocked && !state.seriesUnlocked
        val nextAdCount = if (isAdSupportedStory) state.adSupportedReadsSinceBreak + 1 else state.adSupportedReadsSinceBreak
        // Show an interstitial sponsor break only every 4th ad-supported story read to avoid reader fatigue
        val showInterstitialBreak = isAdSupportedStory && nextAdCount >= AD_BREAK_STORY_INTERVAL

        val pageIdx = startPageIndex ?: comic.lastReadPageIndex.coerceIn(0, (comic.pages.size - 1).coerceAtLeast(0))
        _uiState.update {
            it.copy(
                isSplashActive = false,
                activeComic = comic,
                currentPageIndex = pageIdx,
                isFullscreenControlsVisible = true,
                isThumbnailDrawerOpen = false,
                isLoreSheetOpen = false,
                isSequenceSheetOpen = false,
                pendingLockedComic = null,
                adSupportedReadsSinceBreak = if (showInterstitialBreak) 0 else nextAdCount,
                isAdSlotOpen = showInterstitialBreak,
                isRewardedAdMode = false
            )
        }
    }

    fun closeComic() {
        val active = _uiState.value.activeComic
        val page = _uiState.value.currentPageIndex
        if (active != null) {
            repository.updateReadingProgress(active.id, page)
        }
        _uiState.update {
            it.copy(activeComic = null)
        }
    }

    fun openNextComic() {
        val state = _uiState.value
        val active = state.activeComic ?: return
        val list = state.comics
        val idx = list.indexOfFirst { it.id == active.id }
        if (idx in 0 until list.lastIndex) {
            repository.updateReadingProgress(active.id, state.currentPageIndex)
            openComic(list[idx + 1], startPageIndex = 0)
        }
    }

    fun openPreviousComic() {
        val state = _uiState.value
        val active = state.activeComic ?: return
        val list = state.comics
        val idx = list.indexOfFirst { it.id == active.id }
        if (idx > 0) {
            repository.updateReadingProgress(active.id, state.currentPageIndex)
            openComic(list[idx - 1], startPageIndex = 0)
        }
    }

    fun setCurrentPage(index: Int) {
        val active = _uiState.value.activeComic ?: return
        val clamped = index.coerceIn(0, (active.pages.size - 1).coerceAtLeast(0))
        _uiState.update { it.copy(currentPageIndex = clamped) }
        repository.updateReadingProgress(active.id, clamped)
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(isFullscreenControlsVisible = !it.isFullscreenControlsVisible) }
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.update { it.copy(readingMode = mode) }
    }

    fun setReaderBackground(bg: ReaderBackground) {
        _uiState.update { it.copy(readerBackground = bg) }
    }

    fun toggleSequenceSheet(open: Boolean) {
        _uiState.update { it.copy(isSequenceSheetOpen = open) }
    }

    fun toggleLoreSheet(open: Boolean) {
        _uiState.update { it.copy(isLoreSheetOpen = open) }
    }

    fun toggleThumbnailDrawer(open: Boolean) {
        _uiState.update { it.copy(isThumbnailDrawerOpen = open) }
    }

    fun toggleSpeechBubbles() {
        _uiState.update { it.copy(showSpeechBubbles = !it.showSpeechBubbles) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun importFolder(uri: Uri) {
        viewModelScope.launch {
            val lang = _uiState.value.currentLanguage
            _uiState.update { it.copy(isLoading = true) }
            val imported = withContext(Dispatchers.IO) {
                FolderImporter.importFromFolderTreeUri(getApplication(), uri, lang)
            }
            if (imported != null && imported.pages.isNotEmpty()) {
                repository.addCustomComic(imported)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = AppStrings.folderImportedMessage(lang, imported.title, imported.pages.size)
                    )
                }
                openComic(imported, 0)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = AppStrings.folderEmptyMessage(lang)
                    )
                }
            }
        }
    }

    fun importMultipleImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val lang = _uiState.value.currentLanguage
            _uiState.update { it.copy(isLoading = true) }
            val imported = withContext(Dispatchers.IO) {
                FolderImporter.importFromSelectedUris(getApplication(), uris, lang)
            }
            if (imported != null && imported.pages.isNotEmpty()) {
                repository.addCustomComic(imported)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = AppStrings.imagesImportedMessage(lang, imported.pages.size)
                    )
                }
                openComic(imported, 0)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = AppStrings.imagesImportFailedMessage(lang)
                    )
                }
            }
        }
    }

    fun reorderPages(comicId: String, newPages: List<ComicPage>) {
        val comic = _uiState.value.comics.find { it.id == comicId } ?: return
        val updatedPages = newPages.mapIndexed { idx, page ->
            page.copy(sequenceNumber = idx + 1)
        }
        val updatedComic = comic.copy(
            pages = updatedPages,
            coverUriString = updatedPages.firstOrNull()?.uriString ?: comic.coverUriString
        )
        repository.updateComic(updatedComic)
        val lang = _uiState.value.currentLanguage
        _uiState.update {
            it.copy(
                // Preserve activeComic unless we actually reordered the comic currently open in the reader
                activeComic = if (it.activeComic?.id == comicId) updatedComic else it.activeComic,
                currentPageIndex = it.currentPageIndex.coerceIn(0, (updatedPages.size - 1).coerceAtLeast(0)),
                snackbarMessage = AppStrings.sequenceSavedMessage(lang, updatedPages.size)
            )
        }
    }

    fun sortPagesAlphabetically(comicId: String, reverse: Boolean = false) {
        val comic = _uiState.value.comics.find { it.id == comicId } ?: return
        val comparator = AlphanumericComparator()
        val sorted = if (!reverse) {
            comic.pages.sortedWith { a, b -> comparator.compare(a.title, b.title) }
        } else {
            comic.pages.sortedWith { a, b -> comparator.compare(b.title, a.title) }
        }
        reorderPages(comicId, sorted)
    }

    fun deletePage(comicId: String, pageId: String) {
        val comic = _uiState.value.comics.find { it.id == comicId } ?: return
        val remaining = comic.pages.filter { it.id != pageId }
        reorderPages(comicId, remaining)
    }

    fun deleteComic(comicId: String) {
        repository.deleteComic(comicId)
        if (_uiState.value.activeComic?.id == comicId) {
            closeComic()
        }
        val lang = _uiState.value.currentLanguage
        _uiState.update { it.copy(snackbarMessage = AppStrings.comicDeletedMessage(lang)) }
    }

    companion object {
        private const val AD_BREAK_STORY_INTERVAL = 4
    }
}
