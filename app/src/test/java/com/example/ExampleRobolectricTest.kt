package com.example

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AndroidSoundtrackPlayer
import com.example.audio.ClosedLoopPlaylistState
import com.example.audio.SoundtrackController
import com.example.data.ComicRepository
import com.example.model.AppLanguage
import com.example.model.BubbleStyle
import com.example.model.PanelCrop
import com.example.monetization.AdShowResult
import com.example.monetization.AdsGateway
import com.example.monetization.BillingEvent
import com.example.monetization.BillingGateway
import com.example.monetization.SkuCatalog
import com.example.monetization.StoreProductPrices
import com.example.security.EntitlementSnapshot
import com.example.security.EntitlementStore
import com.example.ui.ComicViewModel
import com.example.ui.components.CoverCartoucheTitleOverlay
import com.example.ui.components.computeCoverTitleFontSp
import com.example.ui.components.splitCoverTitleAndBadge
import com.example.ui.library.ComicLibraryScreen
import com.example.ui.library.LibraryFilter
import com.example.ui.library.filterLibraryComics
import com.example.ui.monetization.PaywallDialog
import com.example.ui.reader.ComicPanelCropCache
import com.example.ui.reader.computeAdaptiveBubbleTypography
import com.example.ui.reader.computeFittedArtworkRect
import com.example.ui.theme.CyberComicsTheme
import com.example.util.AppStrings
import com.example.util.FolderImporter
import com.example.util.StoryBubbleComposer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ходжа", appName)
    }

    @Test
    fun `vault loads 50 anecdotes with 10 free and 40 premium`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val comics = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(50, comics.size)

        // Verify first 10 are free
        val freeStories = comics.filter { it.isFree }
        assertEquals(10, freeStories.size)

        // Verify remaining 40 are premium
        val premiumStories = comics.filter { !it.isFree }
        assertEquals(40, premiumStories.size)
    }

    @Test
    fun `unlocking premium grants access to full vault`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        assertFalse(repository.isPremiumUnlocked.value)

        repository.unlockPremium()
        assertTrue(repository.isPremiumUnlocked.value)
    }

    @Test
    fun `strict language filtering applies across all 50 stories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        // Russian selection
        repository.setLanguage(AppLanguage.RUSSIAN)
        val ruStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(50, ruStories.size)
        assertTrue(ruStories.all { it.language == AppLanguage.RUSSIAN })

        // Spanish selection
        repository.setLanguage(AppLanguage.SPANISH)
        val esStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(50, esStories.size)
        assertTrue(esStories.all { it.language == AppLanguage.SPANISH })

        // English selection
        repository.setLanguage(AppLanguage.ENGLISH)
        val enStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(50, enStories.size)
        assertTrue(enStories.all { it.language == AppLanguage.ENGLISH })
    }

    @Test
    fun `canonical hodja reference asset is bundled and non-empty`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = context.assets.open("hodja_reference.jpg").use { it.readBytes() }
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `all built-in comic covers and pages use consistent canonical hodja drawables across all languages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val ruComic = repository.getBuiltInRussianComic()
        assertEquals(R.drawable.img_cover_mosque, ruComic.coverDrawableResId)
        assertEquals(R.drawable.img_cover_mosque, ruComic.pages[0].drawableResId)
        assertEquals(ruComic.title, ruComic.pages[0].coverTitle)
        assertEquals(R.drawable.img_comic_ru_p1_v2, ruComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_ru_p2_v2, ruComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_ru_p3_v2, ruComic.pages[3].drawableResId)
        assertTrue(ruComic.fullStoryText.isNotBlank() && ruComic.moralText.isNotBlank())

        val esComic = repository.getBuiltInSpanishComic()
        assertEquals(R.drawable.img_cover_mosque, esComic.coverDrawableResId)
        assertEquals(R.drawable.img_cover_mosque, esComic.pages[0].drawableResId)
        assertEquals(esComic.title, esComic.pages[0].coverTitle)
        assertEquals(R.drawable.img_comic_es_p1_v2, esComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_es_p2_v2, esComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_es_p3_v2, esComic.pages[3].drawableResId)
        assertTrue(esComic.fullStoryText.isNotBlank() && esComic.moralText.isNotBlank())

        val enComic = repository.getBuiltInEnglishComic()
        assertEquals(R.drawable.img_cover_mosque, enComic.coverDrawableResId)
        assertEquals(R.drawable.img_cover_mosque, enComic.pages[0].drawableResId)
        assertEquals(enComic.title, enComic.pages[0].coverTitle)
        assertEquals(R.drawable.img_comic_en_p1_v2, enComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_en_p2_v2, enComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_en_p3_v2, enComic.pages[3].drawableResId)
        assertTrue(enComic.fullStoryText.isNotBlank() && enComic.moralText.isNotBlank())
    }

    @Test
    fun `all 50 stories have at least 4 pages and episodes 2 to 10 include cover plus 4 panel crops across RU ES and EN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        val themedCovers = setOf(
            R.drawable.img_cover_mosque,
            R.drawable.img_cover_bazaar,
            R.drawable.img_cover_teahouse,
            R.drawable.img_cover_night,
            R.drawable.img_cover_court,
            R.drawable.img_cover_caravan,
            R.drawable.img_cover_lake,
            R.drawable.img_cover_garden
        )

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val allBuiltIn = repository.comics.value.filter { it.isBuiltIn }
            assertEquals(50, allBuiltIn.size)

            // Every story must strictly have >= 4 pages
            for (story in allBuiltIn) {
                assertTrue(
                    "Story ${story.storyNumber} ($lang) should have at least 4 pages, had ${story.pages.size}",
                    story.pages.size >= 4
                )
            }

            val freeComics = allBuiltIn.filter { it.isFree }
            assertEquals(10, freeComics.size)

            for (ep in freeComics.drop(1)) {
                assertEquals(5, ep.pages.size)
                assertEquals(ep.coverDrawableResId, ep.pages[0].drawableResId)
                assertTrue(ep.coverDrawableResId in themedCovers)
                assertEquals(ep.title, ep.pages[0].coverTitle)

                val panelPages = ep.pages.drop(1)
                assertEquals(4, panelPages.mapNotNull { it.panelCrop }.toSet().size)
                val panelSheetResIds = panelPages.mapNotNull { it.drawableResId }.toSet()
                assertEquals(1, panelSheetResIds.size)
            }
        }
    }

    @Test
    fun `viewModel switches directly between stories and requires verified Billing and Ad callbacks without bypass`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeBilling = FakeBillingGateway()
        val fakeAds = FakeAdsGateway()
        val viewModel = ComicViewModel(app)
        viewModel.attachMonetizationGatewaysForTest(fakeBilling, fakeAds)
        viewModel.setLanguage(AppLanguage.ENGLISH)

        // Open Story 1 directly via splash CTA
        viewModel.dismissSplash(openReaderDirectly = true)
        assertEquals(1, viewModel.uiState.value.activeComic?.storyNumber)

        // Move directly to Story 2 and Story 3
        viewModel.openNextComic()
        assertEquals(2, viewModel.uiState.value.activeComic?.storyNumber)
        assertEquals(0, viewModel.uiState.value.currentPageIndex)
        assertEquals(5, viewModel.uiState.value.activeComic?.pages?.size)

        viewModel.openNextComic()
        assertEquals(3, viewModel.uiState.value.activeComic?.storyNumber)

        // Move back to Story 2
        viewModel.openPreviousComic()
        assertEquals(2, viewModel.uiState.value.activeComic?.storyNumber)

        // Jump straight to Story 10 then attempt Story 11 -> triggers Hybrid Paywall without closing reader
        val story10 = viewModel.uiState.value.comics.first { it.storyNumber == 10 }
        viewModel.openComic(story10, 0)
        assertEquals(10, viewModel.uiState.value.activeComic?.storyNumber)

        viewModel.openNextComic()
        assertTrue(viewModel.uiState.value.isPaywallOpen)
        assertEquals(10, viewModel.uiState.value.activeComic?.storyNumber)

        // Closing rewarded ad early MUST NOT grant access
        viewModel.startRewardedAdUnlock()
        assertTrue(viewModel.uiState.value.isAdSlotOpen)
        assertTrue(viewModel.uiState.value.isAdLoading)
        fakeAds.finishRewarded(AdShowResult.ClosedWithoutReward)
        assertFalse(viewModel.uiState.value.isAdSlotOpen)
        assertEquals(10, viewModel.uiState.value.adUnlockedMaxStory)
        assertTrue(viewModel.uiState.value.isPaywallOpen)

        // Verified RewardEarned callback unlocks next 5 stories (11..15) and opens Story 11
        viewModel.startRewardedAdUnlock()
        fakeAds.finishRewarded(AdShowResult.RewardEarned)
        assertFalse(viewModel.uiState.value.isAdSlotOpen)
        assertEquals(15, viewModel.uiState.value.adUnlockedMaxStory)
        assertEquals(11, viewModel.uiState.value.activeComic?.storyNumber)

        // Story 16 is still locked; clicking Series Pack launches Billing without instant free unlock
        val story16 = viewModel.uiState.value.comics.first { it.storyNumber == 16 }
        viewModel.openComic(story16, 0)
        assertTrue(viewModel.uiState.value.isPaywallOpen)

        viewModel.buySeriesPack()
        assertEquals(SkuCatalog.SKU_SERIES_VOLUME1, fakeBilling.lastLaunchedSku)
        assertFalse("Must never unlock series before verified Billing confirmation", viewModel.uiState.value.seriesUnlocked)

        // Pending payment keeps series locked with informative status
        fakeBilling.emitEvent(BillingEvent.PurchasePending)
        assertFalse(viewModel.uiState.value.seriesUnlocked)
        assertNotNull(viewModel.uiState.value.paywallStatusMessage)

        // Verified Play PurchaseGranted unlocks Series Pack immediately and opens pending Story 16
        fakeBilling.emitEvent(BillingEvent.PurchaseGranted(SkuCatalog.SKU_SERIES_VOLUME1))
        assertFalse(viewModel.uiState.value.isPaywallOpen)
        assertTrue(viewModel.uiState.value.seriesUnlocked)
        assertEquals(16, viewModel.uiState.value.activeComic?.storyNumber)
    }

    @Test
    fun `viewModel snackbars strictly follow selected app language across EN ES and RU`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)

        viewModel.setLanguage(AppLanguage.ENGLISH)
        assertEquals("Language switched to English (EN)", viewModel.uiState.value.snackbarMessage)
        viewModel.clearSnackbar()
        val enComic = viewModel.uiState.value.comics.first()
        viewModel.reorderPages(enComic.id, enComic.pages.reversed())
        assertEquals("Page sequence saved (${enComic.pages.size} pages)", viewModel.uiState.value.snackbarMessage)
        viewModel.clearSnackbar()

        viewModel.setLanguage(AppLanguage.SPANISH)
        val esComic = viewModel.uiState.value.comics.first()
        viewModel.reorderPages(esComic.id, esComic.pages)
        assertEquals("Orden de páginas guardado (${esComic.pages.size} págs.)", viewModel.uiState.value.snackbarMessage)
        viewModel.clearSnackbar()

        viewModel.setLanguage(AppLanguage.RUSSIAN)
        val ruComic = viewModel.uiState.value.comics.first()
        viewModel.reorderPages(ruComic.id, ruComic.pages)
        assertEquals("Порядок страниц сохранен (${ruComic.pages.size} стр.)", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `panel crop rectangle calculation handles normal and out-of-bounds coordinates safely`() {
        val normalRect = ComicPanelCropCache.computeRegionRect(
            width = 1000,
            height = 1000,
            crop = PanelCrop(left = 0.5f, top = 0.5f, right = 0.98f, bottom = 0.98f)
        )
        assertEquals(500, normalRect.left)
        assertEquals(500, normalRect.top)
        assertEquals(980, normalRect.right)
        assertEquals(980, normalRect.bottom)

        // Inverted / out-of-range coordinates must clamp cleanly without throwing IllegalArgumentException
        val clampedRect = ComicPanelCropCache.computeRegionRect(
            width = 800,
            height = 600,
            crop = PanelCrop(left = 1.5f, top = -0.5f, right = 0.1f, bottom = 2.0f)
        )
        assertTrue(clampedRect.left in 0 until clampedRect.right)
        assertTrue(clampedRect.top in 0 until clampedRect.bottom)
        assertTrue(clampedRect.right <= 800)
        assertTrue(clampedRect.bottom <= 600)
    }

    @Test
    fun `panel crop decoding extracts bounded bitmap from 2x2 sheet and falls back cleanly on invalid resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crop = PanelCrop(left = 0.015f, top = 0.082f, right = 0.496f, bottom = 0.532f)

        // Valid 2x2 episode sheet decodes and hits cache cleanly
        val cropped = ComicPanelCropCache.getOrDecodePanel(context.resources, R.drawable.img_ep02_ru, crop)
        assertNotNull(cropped)
        assertTrue(cropped!!.width > 0 && cropped.height > 0)

        val cachedAgain = ComicPanelCropCache.getOrDecodePanel(context.resources, R.drawable.img_ep02_ru, crop)
        assertNotNull(cachedAgain)

        // Invalid resource ID must degrade gracefully to null instead of crashing
        val invalid = ComicPanelCropCache.getOrDecodePanel(context.resources, 0, crop)
        assertNull(invalid)
    }

    @Test
    fun `repository degrades gracefully when persisted custom comics JSON or saved language is corrupted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("cyber_comics_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("saved_language", "CORRUPTED_LANG_CODE")
            .putString("custom_comics_json", "{not_valid_json_array!!!")
            .commit()

        val repository = ComicRepository(context)

        // Must fall back to English default and still serve all 50 built-in stories safely
        assertEquals(AppLanguage.ENGLISH, repository.currentLanguage.value)
        val builtIn = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(50, builtIn.size)
        assertEquals(0, repository.comics.value.count { !it.isBuiltIn })
    }

    @Test
    fun `all 40 premium stories 11 to 50 have unique bespoke sheets, rotate across 7 covers, and localize bubbles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val expectedCovers = setOf(
            R.drawable.img_cover_bazaar,
            R.drawable.img_cover_teahouse,
            R.drawable.img_cover_night,
            R.drawable.img_cover_court,
            R.drawable.img_cover_caravan,
            R.drawable.img_cover_lake,
            R.drawable.img_cover_garden
        )
        val collectTitlesByStory = mutableMapOf<AppLanguage, List<String>>()

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val premiumStories = repository.comics.value.filter { it.isBuiltIn && it.storyNumber >= 11 }
            assertEquals(40, premiumStories.size)

            val usedCovers = premiumStories.mapNotNull { it.coverDrawableResId }.toSet()
            assertEquals(
                "Stories 11..50 ($lang) should utilize all 7 non-mosque Eastern covers",
                expectedCovers,
                usedCovers
            )

            // Every story #11..#50 must point to its own completely unique bespoke sheet drawable
            val bespokeDrawables = premiumStories.map { story ->
                val panelRes = story.pages.drop(1).mapNotNull { it.drawableResId }.toSet()
                assertEquals(1, panelRes.size)
                panelRes.first()
            }
            assertEquals(
                "Stories 11..50 must each have a distinct unique bespoke artwork drawable",
                40,
                bespokeDrawables.toSet().size
            )

            val firstBubbleTexts = mutableListOf<String>()
            for (story in premiumStories) {
                assertTrue(story.coverDrawableResId in expectedCovers)
                assertEquals(5, story.pages.size)
                assertEquals(story.coverDrawableResId, story.pages[0].drawableResId)
                assertEquals(story.title, story.pages[0].coverTitle)
                assertTrue(story.fullStoryText.isNotBlank())
                assertTrue(story.moralText.isNotBlank())

                val panelPages = story.pages.drop(1)
                assertEquals(4, panelPages.mapNotNull { it.panelCrop }.toSet().size)

                val expectedSheet = repository.resolveStoryBespokeSheetDrawable(story.storyNumber)
                assertNotNull("Story ${story.storyNumber} must map to a bespoke sheet", expectedSheet)
                for (page in panelPages) {
                    assertEquals(expectedSheet, page.drawableResId)
                    assertTrue(
                        "Story ${story.storyNumber} ($lang) page ${page.sequenceNumber} must have dynamic bubbles",
                        page.bubbles.isNotEmpty()
                    )
                    for (bubble in page.bubbles) {
                        assertTrue(bubble.text.isNotBlank())
                        assertTrue(bubble.centerX in 0f..1f)
                        assertTrue(bubble.centerY in 0f..1f)
                    }
                }
                firstBubbleTexts.add(panelPages.first().bubbles.first().text)
            }
            collectTitlesByStory[lang] = firstBubbleTexts
        }

        // Same language-agnostic visual sheets, yet completely distinct localized bubble text per language
        assertNotEquals(collectTitlesByStory[AppLanguage.RUSSIAN], collectTitlesByStory[AppLanguage.ENGLISH])
        assertNotEquals(collectTitlesByStory[AppLanguage.SPANISH], collectTitlesByStory[AppLanguage.ENGLISH])
    }

    @Test
    fun `computeFittedArtworkRect centers artwork accurately for ContentScale Fit and Crop`() {
        // Vertical 1000x2000 container displaying a square 1:1 image -> letterboxed vertically at y=500..1500
        val portraitFit = computeFittedArtworkRect(
            containerWidthPx = 1000f,
            containerHeightPx = 2000f,
            imageAspectRatio = 1.0f,
            isCropScale = false
        )
        assertEquals(0f, portraitFit.leftPx, 0.01f)
        assertEquals(500f, portraitFit.topPx, 0.01f)
        assertEquals(1000f, portraitFit.widthPx, 0.01f)
        assertEquals(1000f, portraitFit.heightPx, 0.01f)

        // Wide 2000x1000 container displaying a portrait 0.5f image -> pillarboxed horizontally at x=750..1250
        val tabletFit = computeFittedArtworkRect(
            containerWidthPx = 2000f,
            containerHeightPx = 1000f,
            imageAspectRatio = 0.5f,
            isCropScale = false
        )
        assertEquals(750f, tabletFit.leftPx, 0.01f)
        assertEquals(0f, tabletFit.topPx, 0.01f)
        assertEquals(500f, tabletFit.widthPx, 0.01f)
        assertEquals(1000f, tabletFit.heightPx, 0.01f)

        // Crop fills full container bounds
        val cropRect = computeFittedArtworkRect(
            containerWidthPx = 1200f,
            containerHeightPx = 800f,
            imageAspectRatio = 0.75f,
            isCropScale = true
        )
        assertEquals(0f, cropRect.leftPx, 0.01f)
        assertEquals(0f, cropRect.topPx, 0.01f)
        assertEquals(1200f, cropRect.widthPx, 0.01f)
        assertEquals(800f, cropRect.heightPx, 0.01f)
    }

    @Test
    fun `storyBubbleComposer splits English contractions cleanly and toggles visibility in ViewModel`() {
        val sampleEn = "Hodja wasn't riding it. 'Thank Heaven!' he cheered. Everyone laughed loudly."
        val sentences = StoryBubbleComposer.splitIntoSentences(sampleEn)
        assertEquals(3, sentences.size)
        assertEquals("Hodja wasn't riding it.", sentences[0])

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)
        assertTrue(viewModel.uiState.value.showSpeechBubbles)
        viewModel.toggleSpeechBubbles()
        assertFalse(viewModel.uiState.value.showSpeechBubbles)
        viewModel.toggleSpeechBubbles()
        assertTrue(viewModel.uiState.value.showSpeechBubbles)
    }

    @Test
    fun `golden reference parables 12 and 22 maintain stable 5-page layout and localized morals`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val builtIn = repository.comics.value.filter { it.isBuiltIn }

            // Golden story #12 ("Потерянный осел") and #22 ("Ты тоже прав")
            for (storyNumber in listOf(12, 22)) {
                val story = builtIn.first { it.storyNumber == storyNumber }
                assertEquals(5, story.pages.size)
                assertTrue(story.fullStoryText.isNotBlank())
                assertTrue(story.moralText.isNotBlank())

                // Should extract at least one dialogue/thought SPEECH bubble across the 4 scenes
                val allBubbles = story.pages.flatMap { it.bubbles }
                assertTrue(
                    "Story $storyNumber ($lang) must contain at least one speech/thought bubble",
                    allBubbles.any { it.style == BubbleStyle.SPEECH || it.style == BubbleStyle.THOUGHT }
                )

                // Final panel (index 4) must carry localized moral plaque in RU, ES, and EN
                val moralBubble = story.pages[4].bubbles.firstOrNull { it.id.endsWith("_moral") }
                assertNotNull("Story $storyNumber ($lang) page 5 must have localized moral bubble", moralBubble)
                assertEquals(BubbleStyle.NARRATION, moralBubble!!.style)
                assertTrue(moralBubble.text.length > 15)
            }
        }
    }

    @Test
    fun `adaptive bubble typography scales down cleanly for verbose RU ES text on narrow screens`() {
        val tabletShort = computeAdaptiveBubbleTypography(
            textLength = 28,
            maxBubbleWidthDp = 320f,
            style = BubbleStyle.SPEECH
        )
        val narrowLong = computeAdaptiveBubbleTypography(
            textLength = 165,
            maxBubbleWidthDp = 180f,
            style = BubbleStyle.SPEECH
        )

        assertEquals(12.5f, tabletShort.fontSizeSp, 0.01f)
        assertEquals(5, tabletShort.maxLines)

        assertTrue(narrowLong.fontSizeSp < tabletShort.fontSizeSp)
        assertTrue(narrowLong.fontSizeSp >= 9.5f)
        assertEquals(7, narrowLong.maxLines)
        assertEquals(9f, narrowLong.horizontalPaddingDp, 0.01f)
    }

    @Test
    fun `folder importer labels strictly localize across RU ES EN and handle empty selection safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Empty selection returns null safely across any language
        assertNull(FolderImporter.importFromSelectedUris(context, emptyList(), AppLanguage.SPANISH))

        assertTrue(AppStrings.importedPageTitle(AppLanguage.RUSSIAN, 1, "img").startsWith("Стр. 1:"))
        assertTrue(AppStrings.importedPageTitle(AppLanguage.SPANISH, 1, "img").startsWith("Pág. 1:"))
        assertTrue(AppStrings.importedPageTitle(AppLanguage.ENGLISH, 1, "img").startsWith("Page 1:"))

        assertEquals("Cómic importado", AppStrings.importedComicFallbackTitle(AppLanguage.SPANISH))
        assertEquals("Imported Comic", AppStrings.importedComicFallbackTitle(AppLanguage.ENGLISH))
        assertEquals("Импортированный комикс", AppStrings.importedComicFallbackTitle(AppLanguage.RUSSIAN))
    }

    @Test
    fun `bespoke sheets decode all 4 quadrant panels cleanly and ad batches cap at story 50`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        val quadrantCrops = listOf(
            PanelCrop(left = 0.020f, top = 0.018f, right = 0.490f, bottom = 0.490f),
            PanelCrop(left = 0.510f, top = 0.018f, right = 0.980f, bottom = 0.490f),
            PanelCrop(left = 0.020f, top = 0.510f, right = 0.490f, bottom = 0.982f),
            PanelCrop(left = 0.510f, top = 0.510f, right = 0.980f, bottom = 0.982f)
        )

        for (sheetRes in listOf(R.drawable.img_story_011, R.drawable.img_story_034, R.drawable.img_story_050)) {
            for (crop in quadrantCrops) {
                val panel = ComicPanelCropCache.getOrDecodePanel(context.resources, sheetRes, crop)
                assertNotNull(panel)
                assertTrue(panel!!.width > 0 && panel.height > 0)
            }
        }

        // Verify all 40 bespoke sheets #11..#50 resolve via map lookup and null outside range
        val mapped = (11..50).mapNotNull { repository.resolveStoryBespokeSheetDrawable(it) }
        assertEquals(40, mapped.toSet().size)
        assertNull(repository.resolveStoryBespokeSheetDrawable(10))
        assertNull(repository.resolveStoryBespokeSheetDrawable(51))

        // Ad batches advance sequentially from 10 to 50 and cap cleanly at 50
        var unlockedMax = repository.adUnlockedMaxStory.value
        assertEquals(10, unlockedMax)
        repeat(8) {
            unlockedMax = repository.unlockNextAdBatch()
        }
        assertEquals(50, unlockedMax)
        assertEquals(50, repository.unlockNextAdBatch())
    }

    @Test
    fun `library search and tier atmosphere filters locate stories accurately across 50 parables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        repository.setLanguage(AppLanguage.RUSSIAN)
        val allComics = repository.comics.value

        // Filter: FREE returns only stories 1..10
        val freeOnly = filterLibraryComics(
            comics = allComics,
            query = "",
            selectedFilter = LibraryFilter.FREE,
            isPremiumUnlocked = false,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertEquals(10, freeOnly.size)

        // Filter: UNLOCKED expands cleanly when adUnlockedMaxStory = 15
        val unlocked15 = filterLibraryComics(
            comics = allComics,
            query = "",
            selectedFilter = LibraryFilter.UNLOCKED,
            isPremiumUnlocked = false,
            seriesUnlocked = false,
            adUnlockedMaxStory = 15
        )
        assertEquals(15, unlocked15.size)

        // Filter: NIGHT matches night-themed episodes including #50
        val nightStories = filterLibraryComics(
            comics = allComics,
            query = "",
            selectedFilter = LibraryFilter.NIGHT,
            isPremiumUnlocked = true,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertTrue(nightStories.any { it.storyNumber == 50 })
        assertTrue(nightStories.all { it.themeTag == "night" })

        // Numeric query with '#' prefix resolves exact episode #48
        val byNumber = filterLibraryComics(
            comics = allComics,
            query = "#48",
            selectedFilter = LibraryFilter.ALL,
            isPremiumUnlocked = true,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertEquals(listOf(48), byNumber.map { it.storyNumber })

        // Keyword query matches localized RU content cleanly and unknown words return empty
        val byKeyword = filterLibraryComics(
            comics = allComics,
            query = "терновник",
            selectedFilter = LibraryFilter.BAZAAR,
            isPremiumUnlocked = false,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertEquals(listOf(19), byKeyword.map { it.storyNumber })

        val noMatch = filterLibraryComics(
            comics = allComics,
            query = "несбыточный_запрос_xyz",
            selectedFilter = LibraryFilter.TEAHOUSE,
            isPremiumUnlocked = false,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `dialogue-rich Volume 1 parables 39, 44, and 48 parse cleanly across RU ES and EN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val builtIn = repository.comics.value.filter { it.isBuiltIn }
            for (storyNumber in listOf(39, 44, 48)) {
                val story = builtIn.first { it.storyNumber == storyNumber }
                assertEquals(5, story.pages.size)
                val allBubbles = story.pages.flatMap { it.bubbles }
                assertTrue(
                    "Story $storyNumber ($lang) must contain speech bubbles",
                    allBubbles.any { it.style == BubbleStyle.SPEECH || it.style == BubbleStyle.THOUGHT }
                )
                assertNotNull(
                    "Story $storyNumber ($lang) page 5 must include localized moral",
                    story.pages[4].bubbles.firstOrNull { it.id.endsWith("_moral") }
                )
            }
        }
    }

    @Test
    fun `computeCoverTitleFontSp scales title typography gracefully across thumbnails and fullscreen covers`() {
        val thumbShort = computeCoverTitleFontSp(titleLength = 16, availableWidthDp = 72f)
        val thumbLong = computeCoverTitleFontSp(titleLength = 52, availableWidthDp = 72f)
        val fullShort = computeCoverTitleFontSp(titleLength = 16, availableWidthDp = 320f)
        val fullLong = computeCoverTitleFontSp(titleLength = 52, availableWidthDp = 320f)

        assertTrue(fullShort > thumbShort)
        assertTrue(thumbShort > thumbLong)
        assertTrue(fullShort > fullLong)
        assertTrue(fullLong >= 11.5f)
        assertTrue(thumbLong >= 6.5f)

        val split = splitCoverTitleAndBadge("76. Speech Feeds Ears, Warm Spoons Feed the Heart", "Ходжа")
        assertEquals("№ 76", split.episodeBadge)
        assertEquals("Speech Feeds Ears, Warm Spoons Feed the Heart", split.headline)

        val customSplit = splitCoverTitleAndBadge("Samarkand Nights", "Custom Edition")
        assertEquals("Custom Edition", customSplit.episodeBadge)
        assertEquals("Samarkand Nights", customSplit.headline)
    }

    @Test
    fun `cover cartouche separates crown badge and displays longest RU ES EN parable titles without clipping`() {
        val longestRu = "79. Кто первый заговорит — платит за чай"
        val longestEs = "2. El olor del guiso y el tintineo de monedas"
        val longestEn = "76. Speech Feeds Ears, Warm Spoons Feed the Heart"

        composeTestRule.setContent {
            CyberComicsTheme {
                Box(modifier = Modifier.size(300.dp, 400.dp)) {
                    CoverCartoucheTitleOverlay(title = longestRu, subtitle = "Базар")
                }
                Box(modifier = Modifier.size(300.dp, 400.dp)) {
                    CoverCartoucheTitleOverlay(title = longestEs, subtitle = "Bazar")
                }
                Box(modifier = Modifier.size(180.dp, 240.dp)) {
                    CoverCartoucheTitleOverlay(title = longestEn, subtitle = "Teahouse")
                }
            }
        }

        composeTestRule.onNodeWithText("№ 79").assertIsDisplayed()
        composeTestRule.onNodeWithText("Кто первый заговорит — платит за чай").assertIsDisplayed()
        composeTestRule.onNodeWithText("№ 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("El olor del guiso y el tintineo de monedas").assertIsDisplayed()
        composeTestRule.onNodeWithText("№ 76").assertIsDisplayed()
        composeTestRule.onNodeWithText("Speech Feeds Ears, Warm Spoons Feed the Heart").assertIsDisplayed()
    }

    @Test
    fun `clicking Parchment button in Library opens ParchmentStoryDialog in active language and closes cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)
        viewModel.setLanguage(AppLanguage.RUSSIAN)
        viewModel.clearSnackbar()
        viewModel.dismissSplash(openReaderDirectly = false)

        composeTestRule.setContent {
            CyberComicsTheme {
                ComicAppContent(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("featured_parchment_btn"))
        composeTestRule.onNodeWithTag("featured_parchment_btn").performClick()

        composeTestRule.onNodeWithTag("parchment_dialog_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parchment_story_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parchment_story_body").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parchment_moral_card").performScrollTo().assertIsDisplayed()

        // Switch language to Spanish while inspecting catalog logic -> parchment remains valid & localized
        viewModel.setLanguage(AppLanguage.SPANISH)
        composeTestRule.waitForIdle()
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.parchmentComic?.language)

        composeTestRule.onNodeWithTag("close_parchment_dialog").performClick()
        composeTestRule.onNodeWithTag("parchment_dialog_root").assertDoesNotExist()
    }

    @Test
    fun `clicking Parchment button in Reader opens illuminated ParchmentStoryDialog`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)
        viewModel.setLanguage(AppLanguage.ENGLISH)
        viewModel.dismissSplash(openReaderDirectly = true)

        composeTestRule.setContent {
            CyberComicsTheme {
                ComicAppContent(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("reader_parchment_button").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("parchment_dialog_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parchment_story_body").assertIsDisplayed()

        composeTestRule.onNodeWithTag("parchment_done_btn").performClick()
        composeTestRule.onNodeWithTag("parchment_dialog_root").assertDoesNotExist()
    }

    @Test
    fun `library UI displays empty search card on zero results and restores catalog on reset click`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        repository.setLanguage(AppLanguage.RUSSIAN)
        val comics = repository.comics.value

        composeTestRule.setContent {
            CyberComicsTheme {
                ComicLibraryScreen(
                    comics = comics,
                    currentLanguage = AppLanguage.RUSSIAN,
                    isPremiumUnlocked = false,
                    seriesUnlocked = false,
                    adUnlockedMaxStory = 10,
                    isLoading = false,
                    snackbarMessage = null,
                    onClearSnackbar = {},
                    onSelectLanguage = {},
                    onOpenSplash = {},
                    onOpenPaywall = {},
                    onOpenComic = {},
                    onManageSequence = {},
                    onDeleteComic = {},
                    onImportFolder = {},
                    onImportMultipleImages = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("library_search_input"))

        composeTestRule.onNodeWithTag("library_search_input")
            .performTextInput("несбыточный_запрос_xyz")

        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("library_empty_filter_card"))

        composeTestRule.onNodeWithTag("library_empty_filter_card")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("library_reset_filters_btn"))

        composeTestRule.onNodeWithTag("library_reset_filters_btn")
            .performClick()

        composeTestRule.onNodeWithTag("library_empty_filter_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("featured_comic_banner"))
        composeTestRule.onNodeWithTag("featured_comic_banner").assertIsDisplayed()
    }

    @Test
    fun `all 6 compressed OGG soundtrack resources exist with valid OggS headers and compact total size`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tracks = AndroidSoundtrackPlayer.DEFAULT_TRACKS
        assertEquals(6, tracks.size)

        var totalBytes = 0L
        for (resId in tracks) {
            val bytes = context.resources.openRawResource(resId).use { it.readBytes() }
            assertTrue("Track $resId should be non-empty", bytes.size > 1024)
            assertEquals('O'.code.toByte(), bytes[0])
            assertEquals('g'.code.toByte(), bytes[1])
            assertEquals('g'.code.toByte(), bytes[2])
            assertEquals('S'.code.toByte(), bytes[3])
            totalBytes += bytes.size
        }

        // Compressed 6-track 18-minute OGG Vorbis loop must stay compact (< 7 MB total)
        assertTrue("Expected compact OGG soundtrack < 7 MB, was $totalBytes bytes", totalBytes < 7L * 1024L * 1024L)
    }

    @Test
    fun `closedLoopPlaylistState rotates cleanly around 6 tracks and caps consecutive error retries`() {
        val trackIds = listOf(101, 102, 103, 104, 105, 106)
        val loop = ClosedLoopPlaylistState(trackIds)

        assertEquals(0, loop.currentIndex)
        assertEquals(101, loop.currentTrackResId())

        val visitedIndices = mutableListOf(loop.currentIndex)
        repeat(7) {
            loop.advanceToNextTrack()
            visitedIndices.add(loop.currentIndex)
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 0, 1), visitedIndices)

        // Recovering playback resets consecutive errors
        assertTrue(loop.onTrackErrorShouldRetry())
        assertEquals(2, loop.currentIndex)
        loop.markPlaybackStarted()
        assertEquals(0, loop.consecutiveErrors)

        // Failing all 6 tracks in a row stops retrying safely
        repeat(5) { assertTrue(loop.onTrackErrorShouldRetry()) }
        assertFalse(loop.onTrackErrorShouldRetry())
    }

    @Test
    fun `viewModel soundtrack pauses on background onStop and AdSlot and persists mute toggle`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeBilling = FakeBillingGateway()
        val fakeAds = FakeAdsGateway()
        val viewModel = ComicViewModel(app)
        viewModel.attachMonetizationGatewaysForTest(fakeBilling, fakeAds)
        var requestedPlaying = false
        var released = false
        val fakeController = object : SoundtrackController {
            override val currentTrackIndex: Int = 0
            override val isActivelyPlaying: Boolean get() = requestedPlaying
            override fun updatePlaybackState(shouldBePlaying: Boolean) {
                requestedPlaying = shouldBePlaying
            }
            override fun skipToNextTrack() {}
            override fun release() {
                released = true
            }
        }
        viewModel.attachSoundtrackControllerForTest(fakeController)

        // Splash is active -> music stays silent until dismissed
        assertTrue(viewModel.uiState.value.isMusicEnabled)
        assertFalse(requestedPlaying)

        // Dismiss splash -> soundtrack starts playing softly in foreground
        viewModel.dismissSplash(openReaderDirectly = false)
        assertTrue(requestedPlaying)

        // App moves to background (onStop) -> soundtrack pauses immediately; resumes on onStart
        viewModel.onAppForegroundStateChanged(false)
        assertFalse(requestedPlaying)
        viewModel.onAppForegroundStateChanged(true)
        assertTrue(requestedPlaying)

        // Sponsor ad break -> pauses soundtrack until ad dismissed
        viewModel.startRewardedAdUnlock()
        assertFalse(requestedPlaying)
        fakeAds.finishRewarded(AdShowResult.ClosedWithoutReward)
        assertTrue(requestedPlaying)

        // Toggle off -> pauses playback and persists in SharedPreferences
        viewModel.toggleMusicEnabled(showToast = false)
        assertFalse(viewModel.uiState.value.isMusicEnabled)
        assertFalse(requestedPlaying)

        val freshRepo = ComicRepository(app)
        assertFalse(freshRepo.isMusicEnabled.value)

        // Restore toggle on cleanly
        viewModel.setMusicEnabled(true)
        assertTrue(requestedPlaying)
        assertFalse(released)
    }

    @Test
    fun `library and reader music toggle buttons switch soundtrack preference cleanly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)
        viewModel.setLanguage(AppLanguage.RUSSIAN)
        viewModel.setMusicEnabled(true)
        viewModel.clearSnackbar()
        viewModel.dismissSplash(openReaderDirectly = false)

        composeTestRule.setContent {
            CyberComicsTheme {
                ComicAppContent(viewModel = viewModel)
            }
        }

        // Open Story #1 via Featured card and toggle music OFF in Reader HUD
        composeTestRule.onNodeWithTag("library_lazy_column")
            .performScrollToNode(hasTestTag("featured_read_btn"))
        composeTestRule.onNodeWithTag("featured_read_btn").performClick()

        composeTestRule.onNodeWithTag("reader_music_toggle").assertIsDisplayed().performClick()
        assertFalse(viewModel.uiState.value.isMusicEnabled)

        // Return to Library and toggle music back ON from Library header
        composeTestRule.onNodeWithTag("reader_back_button").performClick()
        composeTestRule.onNodeWithTag("library_music_toggle").assertIsDisplayed().performClick()
        assertTrue(viewModel.uiState.value.isMusicEnabled)
    }

    @Test
    fun `entitlementStore encrypts unlocks with AES-GCM, scrubs legacy plaintext flags, and fails closed on tampering`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_encrypted_entitlements", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("premium_unlocked", true)
            .putBoolean("series_unlocked", true)
            .putInt("ad_unlocked_max_story", 20)
            .commit()

        val store = EntitlementStore(prefs)
        // Legacy plaintext PRO/Series flags must NOT automatically unlock elevated privileges
        val initial = store.load()
        assertFalse(initial.isPremiumUnlocked)
        assertFalse(initial.seriesUnlocked)
        assertEquals(20, initial.adUnlockedMaxStory)
        assertFalse(prefs.contains("premium_unlocked"))
        assertFalse(prefs.contains("series_unlocked"))

        // Verified save persists cleanly across reloads
        assertTrue(store.save(EntitlementSnapshot(isPremiumUnlocked = true, seriesUnlocked = true, adUnlockedMaxStory = 35)))
        val reloaded = store.load()
        assertTrue(reloaded.isPremiumUnlocked)
        assertTrue(reloaded.seriesUnlocked)
        assertEquals(35, reloaded.adUnlockedMaxStory)

        // Corrupting ciphertext breaks GCM authentication tag and fails strictly closed
        val storedCipher = prefs.getString(EntitlementStore.KEY_ENCRYPTED_ENTITLEMENTS, "")!!
        val tampered = storedCipher.dropLast(4) + "AAAA"
        prefs.edit().putString(EntitlementStore.KEY_ENCRYPTED_ENTITLEMENTS, tampered).commit()

        val afterTamper = store.load()
        assertFalse(afterTamper.isPremiumUnlocked)
        assertFalse(afterTamper.seriesUnlocked)
        assertEquals(EntitlementSnapshot.DEFAULT_FREE_MAX_STORY, afterTamper.adUnlockedMaxStory)
    }

    @Test
    fun `billing restore reconciles Play owned SKUs and revokes unowned privileges while Paywall shows store prices`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeBilling = FakeBillingGateway()
        val fakeAds = FakeAdsGateway()
        val viewModel = ComicViewModel(app)
        viewModel.attachMonetizationGatewaysForTest(fakeBilling, fakeAds)
        viewModel.setLanguage(AppLanguage.ENGLISH)

        // Store localized prices flow into UiState & PaywallDialog
        fakeBilling.emitPrices(StoreProductPrices(proFormattedPrice = "€4.99", seriesFormattedPrice = "€0.99"))
        viewModel.restorePurchases()
        fakeBilling.emitEvent(BillingEvent.RestoredPurchases(setOf(SkuCatalog.SKU_PRO_LIFETIME), userInitiated = true))
        assertTrue(viewModel.uiState.value.isPremiumUnlocked)
        assertEquals("€4.99", viewModel.uiState.value.proPriceLabel)

        // Authoritative restore returning empty (e.g., refunded purchase) revokes PRO safely
        fakeBilling.emitEvent(BillingEvent.RestoredPurchases(emptySet(), userInitiated = false))
        assertFalse(viewModel.uiState.value.isPremiumUnlocked)

        composeTestRule.setContent {
            CyberComicsTheme {
                PaywallDialog(
                    currentLanguage = AppLanguage.ENGLISH,
                    seriesUnlocked = false,
                    adUnlockedMaxStory = 10,
                    totalStoriesCount = 50,
                    proPriceLabel = "€4.99",
                    seriesPriceLabel = "€0.99",
                    isBusy = false,
                    statusMessage = "Payment pending",
                    onBuyPro = {},
                    onBuySeries = {},
                    onWatchAd = {},
                    onRestore = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("€4.99").assertIsDisplayed()
        composeTestRule.onNodeWithTag("paywall_status_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("paywall_buy_series_button").performScrollTo().assertIsDisplayed()
    }

    private class FakeBillingGateway : BillingGateway {
        private val _productPrices = MutableStateFlow(StoreProductPrices())
        override val productPrices: StateFlow<StoreProductPrices> = _productPrices.asStateFlow()

        private val _isReady = MutableStateFlow(true)
        override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

        private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 16)
        override val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

        var lastLaunchedSku: String? = null

        override fun connect() {}
        override fun queryOwnedPurchases(userInitiated: Boolean) {}
        override fun launchPurchase(activity: Activity?, sku: String) {
            lastLaunchedSku = sku
        }
        override fun destroy() {}

        fun emitPrices(prices: StoreProductPrices) {
            _productPrices.value = prices
        }

        fun emitEvent(event: BillingEvent) {
            _events.tryEmit(event)
        }
    }

    private class FakeAdsGateway : AdsGateway {
        private val _isRewardedReady = MutableStateFlow(true)
        override val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

        private val _isInterstitialReady = MutableStateFlow(false)
        override val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

        private var pendingRewardCallback: ((AdShowResult) -> Unit)? = null

        override fun initialize() {}
        override fun preloadRewardedAd() {}
        override fun preloadInterstitialAd() {}

        override fun showRewardedAd(activity: Activity?, onResult: (AdShowResult) -> Unit) {
            pendingRewardCallback = onResult
        }

        override fun showInterstitialAd(activity: Activity?, onClosed: () -> Unit) {
            onClosed()
        }

        fun finishRewarded(result: AdShowResult) {
            val cb = pendingRewardCallback
            pendingRewardCallback = null
            cb?.invoke(result)
        }
    }
}
