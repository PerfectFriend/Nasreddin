package com.example.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidSoundtrackPlayer
import com.example.audio.SoundtrackController
import com.example.data.ComicRepository
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.model.ReaderBackground
import com.example.model.ReadingMode
import com.example.monetization.AdMobAdsManager
import com.example.monetization.AdShowResult
import com.example.monetization.AdsGateway
import com.example.monetization.BillingEvent
import com.example.monetization.BillingGateway
import com.example.monetization.PlayBillingManager
import com.example.monetization.SkuCatalog
import com.example.util.AlphanumericComparator
import com.example.util.AppStrings
import com.example.util.FolderImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 * - `isPremiumUnlocked == true` (PRO via Play Billing) -> all stories unlocked & zero ads.
 * - `seriesUnlocked == true` (Volume 1 Series via Play Billing) -> built-in series (11..50) unlocked & ad-free.
 * - `comic.storyNumber <= adUnlockedMaxStory` -> unlocked free via verified AdMob Rewarded Ad (periodic interstitial breaks apply).
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
    val isAdLoading: Boolean = false,
    val isBillingBusy: Boolean = false,
    val proPriceLabel: String? = null,
    val seriesPriceLabel: String? = null,
    val paywallStatusMessage: String? = null,
    val pendingLockedComic: ComicBook? = null,
    val adSupportedReadsSinceBreak: Int = 0,
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val showSpeechBubbles: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val parchmentComic: ComicBook? = null
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
 * and Play Billing / AdMob Hybrid Monetization unlock flows.
 */
class ComicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ComicRepository(application)
    private var soundtrackController: SoundtrackController = AndroidSoundtrackPlayer(application)
    private var billingGateway: BillingGateway = PlayBillingManager(application)
    private var adsGateway: AdsGateway = AdMobAdsManager(application)
    private var billingObserverJob: Job? = null
    private var isAppInForeground: Boolean = true

    private val _uiState = MutableStateFlow(UiState(isMusicEnabled = repository.isMusicEnabled.value))
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
            repository.isMusicEnabled.collect { enabled ->
                _uiState.update { it.copy(isMusicEnabled = enabled) }
                syncSoundtrackPlayback()
            }
        }

        viewModelScope.launch {
            repository.comics.collect { list ->
                _uiState.update { state ->
                    val updatedActive = state.activeComic?.let { active ->
                        list.find { it.id == active.id } ?: active
                    }
                    val updatedParchment = state.parchmentComic?.let { p ->
                        list.find { it.storyNumber == p.storyNumber && it.isBuiltIn == p.isBuiltIn }
                            ?: list.find { it.id == p.id }
                            ?: p
                    }
                    state.copy(
                        comics = list,
                        activeComic = updatedActive,
                        parchmentComic = updatedParchment
                    )
                }
            }
        }

        observeBillingGateway(billingGateway)
        billingGateway.connect()
        adsGateway.initialize()
    }

    private fun observeBillingGateway(gateway: BillingGateway) {
        billingObserverJob?.cancel()
        billingObserverJob = viewModelScope.launch {
            launch {
                gateway.productPrices.collect { prices ->
                    _uiState.update {
                        it.copy(
                            proPriceLabel = prices.proFormattedPrice,
                            seriesPriceLabel = prices.seriesFormattedPrice
                        )
                    }
                }
            }
            launch {
                gateway.events.collect { event ->
                    handleBillingEvent(event)
                }
            }
        }
    }

    private fun handleBillingEvent(event: BillingEvent) {
        val lang = _uiState.value.currentLanguage
        when (event) {
            is BillingEvent.PurchaseGranted -> {
                val pending = _uiState.value.pendingLockedComic
                val builtInCount = _uiState.value.comics.count { it.isBuiltIn }.coerceAtLeast(50)
                when (event.sku) {
                    SkuCatalog.SKU_PRO_LIFETIME -> {
                        repository.unlockPremium()
                        _uiState.update {
                            it.copy(
                                isPaywallOpen = false,
                                isAdSlotOpen = false,
                                isBillingBusy = false,
                                paywallStatusMessage = null,
                                isPremiumUnlocked = true,
                                pendingLockedComic = null,
                                snackbarMessage = AppStrings.purchaseSuccessMessage(lang)
                            )
                        }
                    }
                    SkuCatalog.SKU_SERIES_VOLUME1 -> {
                        repository.unlockSeriesPack()
                        _uiState.update {
                            it.copy(
                                isPaywallOpen = false,
                                isAdSlotOpen = false,
                                isBillingBusy = false,
                                paywallStatusMessage = null,
                                seriesUnlocked = true,
                                pendingLockedComic = null,
                                snackbarMessage = AppStrings.seriesPurchaseSuccessMessage(lang, builtInCount)
                            )
                        }
                    }
                }
                if (pending != null) {
                    openComic(pending, 0)
                }
            }
            is BillingEvent.RestoredPurchases -> {
                val restored = repository.reconcilePlayEntitlements(
                    ownedSkus = event.grantedSkus,
                    authoritativelySynced = true
                )
                val pending = _uiState.value.pendingLockedComic
                _uiState.update { state ->
                    val statusMsg = when {
                        restored -> AppStrings.purchaseSuccessMessage(lang)
                        event.userInitiated -> AppStrings.billingNoPurchasesFound(lang)
                        else -> state.paywallStatusMessage
                    }
                    state.copy(
                        isBillingBusy = false,
                        isPaywallOpen = if (restored) false else state.isPaywallOpen,
                        paywallStatusMessage = if (restored) null else statusMsg,
                        pendingLockedComic = if (restored) null else state.pendingLockedComic,
                        snackbarMessage = if (restored || event.userInitiated) statusMsg else state.snackbarMessage
                    )
                }
                if (restored && pending != null) {
                    openComic(pending, 0)
                }
            }
            BillingEvent.PurchasePending -> {
                val msg = AppStrings.billingPurchasePending(lang)
                _uiState.update {
                    it.copy(
                        isBillingBusy = false,
                        paywallStatusMessage = msg,
                        snackbarMessage = msg
                    )
                }
            }
            BillingEvent.UserCancelled -> {
                val msg = AppStrings.billingCancelled(lang)
                _uiState.update {
                    it.copy(
                        isBillingBusy = false,
                        paywallStatusMessage = msg
                    )
                }
            }
            is BillingEvent.Error -> {
                val msg = AppStrings.billingUnavailableError(lang)
                _uiState.update {
                    it.copy(
                        isBillingBusy = false,
                        paywallStatusMessage = msg,
                        snackbarMessage = if (!it.isPaywallOpen) msg else it.snackbarMessage
                    )
                }
            }
        }
        syncSoundtrackPlayback()
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        _uiState.update { it.copy(snackbarMessage = AppStrings.languageSwitchedMessage(language)) }
    }

    fun toggleMusicEnabled(showToast: Boolean = true) {
        val next = !_uiState.value.isMusicEnabled
        setMusicEnabled(next, showToast = showToast)
    }

    fun setMusicEnabled(enabled: Boolean, showToast: Boolean = false) {
        repository.setMusicEnabled(enabled)
        _uiState.update { state ->
            state.copy(
                isMusicEnabled = enabled,
                snackbarMessage = if (showToast) {
                    AppStrings.musicEnabledStatusMessage(state.currentLanguage, enabled)
                } else {
                    state.snackbarMessage
                }
            )
        }
        syncSoundtrackPlayback()
    }

    fun onAppForegroundStateChanged(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground) {
            billingGateway.queryOwnedPurchases(userInitiated = false)
        }
        syncSoundtrackPlayback()
    }

    internal fun attachSoundtrackControllerForTest(controller: SoundtrackController) {
        soundtrackController.release()
        soundtrackController = controller
        syncSoundtrackPlayback()
    }

    internal fun attachMonetizationGatewaysForTest(
        billing: BillingGateway,
        ads: AdsGateway
    ) {
        billingObserverJob?.cancel()
        billingGateway.destroy()
        billingGateway = billing
        adsGateway = ads
        observeBillingGateway(billing)
        billing.connect()
        ads.initialize()
    }

    private fun syncSoundtrackPlayback() {
        val state = _uiState.value
        val shouldPlay = isAppInForeground &&
            state.isMusicEnabled &&
            !state.isSplashActive &&
            !state.isAdSlotOpen
        soundtrackController.updatePlaybackState(shouldPlay)
    }

    override fun onCleared() {
        billingObserverJob?.cancel()
        billingGateway.destroy()
        soundtrackController.release()
        super.onCleared()
    }

    fun dismissSplash(openReaderDirectly: Boolean = false) {
        _uiState.update { it.copy(isSplashActive = false) }
        syncSoundtrackPlayback()
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
        syncSoundtrackPlayback()
    }

    fun openPaywall() {
        _uiState.update {
            it.copy(
                isPaywallOpen = true,
                isAdSlotOpen = false,
                paywallStatusMessage = null
            )
        }
        syncSoundtrackPlayback()
    }

    fun closePaywall() {
        _uiState.update {
            it.copy(
                isPaywallOpen = false,
                isBillingBusy = false,
                paywallStatusMessage = null,
                pendingLockedComic = null
            )
        }
    }

    fun buyFullVersion(activity: Activity? = null) {
        if (_uiState.value.isBillingBusy) return
        _uiState.update { it.copy(isBillingBusy = true, paywallStatusMessage = null) }
        billingGateway.launchPurchase(activity, SkuCatalog.SKU_PRO_LIFETIME)
    }

    fun buySeriesPack(activity: Activity? = null) {
        if (_uiState.value.isBillingBusy) return
        _uiState.update { it.copy(isBillingBusy = true, paywallStatusMessage = null) }
        billingGateway.launchPurchase(activity, SkuCatalog.SKU_SERIES_VOLUME1)
    }

    fun startRewardedAdUnlock(activity: Activity? = null) {
        if (_uiState.value.isAdLoading) return
        _uiState.update {
            it.copy(
                isPaywallOpen = false,
                isAdSlotOpen = true,
                isRewardedAdMode = true,
                isAdLoading = true,
                paywallStatusMessage = null
            )
        }
        syncSoundtrackPlayback()

        adsGateway.showRewardedAd(activity) { result ->
            val state = _uiState.value
            val lang = state.currentLanguage
            when (result) {
                AdShowResult.RewardEarned -> {
                    val newMax = repository.unlockNextAdBatch()
                    val pending = state.pendingLockedComic
                    _uiState.update {
                        it.copy(
                            isAdSlotOpen = false,
                            isRewardedAdMode = false,
                            isAdLoading = false,
                            adUnlockedMaxStory = newMax,
                            pendingLockedComic = null,
                            snackbarMessage = AppStrings.adBatchUnlockedMessage(lang, newMax)
                        )
                    }
                    syncSoundtrackPlayback()
                    if (pending != null && pending.storyNumber <= newMax) {
                        openComic(pending, 0, activity)
                    }
                }
                AdShowResult.ClosedWithoutReward -> {
                    val msg = AppStrings.rewardedAdClosedEarly(lang)
                    _uiState.update {
                        it.copy(
                            isAdSlotOpen = false,
                            isRewardedAdMode = false,
                            isAdLoading = false,
                            isPaywallOpen = state.pendingLockedComic != null,
                            paywallStatusMessage = msg,
                            snackbarMessage = msg
                        )
                    }
                    syncSoundtrackPlayback()
                }
                is AdShowResult.FailedToShow -> {
                    val msg = AppStrings.rewardedAdNotReadyError(lang)
                    _uiState.update {
                        it.copy(
                            isAdSlotOpen = false,
                            isRewardedAdMode = false,
                            isAdLoading = false,
                            isPaywallOpen = state.pendingLockedComic != null,
                            paywallStatusMessage = msg,
                            snackbarMessage = msg
                        )
                    }
                    syncSoundtrackPlayback()
                }
            }
        }
    }

    fun dismissAdSlot() {
        _uiState.update {
            it.copy(
                isAdSlotOpen = false,
                isRewardedAdMode = false,
                isAdLoading = false,
                pendingLockedComic = null
            )
        }
        syncSoundtrackPlayback()
    }

    fun restorePurchases() {
        if (_uiState.value.isBillingBusy) return
        _uiState.update { it.copy(isBillingBusy = true, paywallStatusMessage = null) }
        billingGateway.queryOwnedPurchases(userInitiated = true)
    }

    fun openComic(comic: ComicBook, startPageIndex: Int? = null, activity: Activity? = null) {
        // Hybrid Gated Check: free (1..10), ad-unlocked batch, series pack, or PRO
        if (!repository.isComicUnlocked(comic)) {
            _uiState.update {
                it.copy(
                    pendingLockedComic = comic,
                    isPaywallOpen = true,
                    paywallStatusMessage = null
                )
            }
            return
        }

        val state = _uiState.value
        val isAdSupportedStory = !comic.isFree && !state.isPremiumUnlocked && !state.seriesUnlocked
        val nextAdCount = if (isAdSupportedStory) state.adSupportedReadsSinceBreak + 1 else state.adSupportedReadsSinceBreak
        val shouldTriggerInterstitial = isAdSupportedStory &&
            nextAdCount >= AD_BREAK_STORY_INTERVAL &&
            activity != null &&
            adsGateway.isInterstitialReady.value

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
                adSupportedReadsSinceBreak = if (isAdSupportedStory && nextAdCount >= AD_BREAK_STORY_INTERVAL) 0 else nextAdCount,
                isAdSlotOpen = shouldTriggerInterstitial,
                isRewardedAdMode = false,
                isAdLoading = false
            )
        }
        syncSoundtrackPlayback()

        if (shouldTriggerInterstitial) {
            adsGateway.showInterstitialAd(activity) {
                _uiState.update { it.copy(isAdSlotOpen = false, isRewardedAdMode = false) }
                syncSoundtrackPlayback()
            }
        } else if (isAdSupportedStory && !adsGateway.isInterstitialReady.value) {
            adsGateway.preloadInterstitialAd()
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

    fun openNextComic(activity: Activity? = null) {
        val state = _uiState.value
        val active = state.activeComic ?: return
        val list = state.comics
        val idx = list.indexOfFirst { it.id == active.id }
        if (idx in 0 until list.lastIndex) {
            repository.updateReadingProgress(active.id, state.currentPageIndex)
            openComic(list[idx + 1], startPageIndex = 0, activity = activity)
        }
    }

    fun openPreviousComic(activity: Activity? = null) {
        val state = _uiState.value
        val active = state.activeComic ?: return
        val list = state.comics
        val idx = list.indexOfFirst { it.id == active.id }
        if (idx > 0) {
            repository.updateReadingProgress(active.id, state.currentPageIndex)
            openComic(list[idx - 1], startPageIndex = 0, activity = activity)
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

    fun openParchment(comic: ComicBook) {
        if (!repository.isComicUnlocked(comic)) {
            _uiState.update { it.copy(pendingLockedComic = comic, isPaywallOpen = true) }
            return
        }
        _uiState.update { it.copy(parchmentComic = comic) }
    }

    fun closeParchment() {
        _uiState.update { it.copy(parchmentComic = null) }
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
