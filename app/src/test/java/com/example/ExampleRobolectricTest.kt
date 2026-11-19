package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ComicRepository
import com.example.model.AppLanguage
import com.example.model.BubbleStyle
import com.example.model.PanelCrop
import com.example.ui.ComicViewModel
import com.example.ui.components.computeCoverTitleFontSp
import com.example.ui.library.ComicLibraryScreen
import com.example.ui.library.LibraryFilter
import com.example.ui.library.filterLibraryComics
import com.example.ui.reader.ComicPanelCropCache
import com.example.ui.reader.computeAdaptiveBubbleTypography
import com.example.ui.reader.computeFittedArtworkRect
import com.example.ui.theme.CyberComicsTheme
import com.example.util.AppStrings
import com.example.util.FolderImporter
import com.example.util.StoryBubbleComposer
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
    fun `vault loads 100 anecdotes with 10 free and 90 premium`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val comics = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(100, comics.size)

        // Verify first 10 are free
        val freeStories = comics.filter { it.isFree }
        assertEquals(10, freeStories.size)

        // Verify remaining 90 are premium
        val premiumStories = comics.filter { !it.isFree }
        assertEquals(90, premiumStories.size)
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
    fun `strict language filtering applies across all 100 stories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        // Russian selection
        repository.setLanguage(AppLanguage.RUSSIAN)
        val ruStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(100, ruStories.size)
        assertTrue(ruStories.all { it.language == AppLanguage.RUSSIAN })

        // Spanish selection
        repository.setLanguage(AppLanguage.SPANISH)
        val esStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(100, esStories.size)
        assertTrue(esStories.all { it.language == AppLanguage.SPANISH })

        // English selection
        repository.setLanguage(AppLanguage.ENGLISH)
        val enStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(100, enStories.size)
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
    fun `all 100 stories have at least 4 pages and episodes 2 to 10 include cover plus 4 panel crops across RU ES and EN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        val themedCovers = setOf(
            R.drawable.img_cover_mosque,
            R.drawable.img_cover_bazaar,
            R.drawable.img_cover_teahouse,
            R.drawable.img_cover_night
        )

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val allBuiltIn = repository.comics.value.filter { it.isBuiltIn }
            assertEquals(100, allBuiltIn.size)

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
    fun `viewModel switches directly between stories and handles hybrid ad and PRO unlock seamlessly`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ComicViewModel(app)

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

        // Watch rewarded ad to unlock next 5 stories (11..15) and open pending Story 11 automatically
        viewModel.startRewardedAdUnlock()
        assertFalse(viewModel.uiState.value.isPaywallOpen)
        assertTrue(viewModel.uiState.value.isAdSlotOpen)
        assertTrue(viewModel.uiState.value.isRewardedAdMode)

        viewModel.completeAdSlot()
        assertFalse(viewModel.uiState.value.isAdSlotOpen)
        assertEquals(15, viewModel.uiState.value.adUnlockedMaxStory)
        assertEquals(11, viewModel.uiState.value.activeComic?.storyNumber)

        // Story 16 is still locked until next ad / series / PRO
        val story16 = viewModel.uiState.value.comics.first { it.storyNumber == 16 }
        viewModel.openComic(story16, 0)
        assertTrue(viewModel.uiState.value.isPaywallOpen)

        // Buy series pack ($0.99) -> unlocks Story 16 immediately ad-free
        viewModel.buySeriesPack()
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

        // Must fall back to English default and still serve all 100 built-in stories safely
        assertEquals(AppLanguage.ENGLISH, repository.currentLanguage.value)
        val builtIn = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(100, builtIn.size)
        assertEquals(0, repository.comics.value.count { !it.isBuiltIn })
    }

    @Test
    fun `stories 11 to 100 share 3 thematic cover templates and 3 panel sheets across RU ES EN with localized cartouche titles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val expectedUniversalCovers = setOf(
            R.drawable.img_cover_bazaar,
            R.drawable.img_cover_teahouse,
            R.drawable.img_cover_night
        )
        val expectedUniversalDrawables = setOf(
            R.drawable.img_universal_hodja_template,
            R.drawable.img_universal_hodja_teahouse,
            R.drawable.img_universal_hodja_night
        )
        val collectTitlesByStory = mutableMapOf<AppLanguage, List<String>>()

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val premiumStories = repository.comics.value.filter { it.isBuiltIn && it.storyNumber >= 11 }
            assertEquals(90, premiumStories.size)

            val usedCovers = premiumStories.mapNotNull { it.coverDrawableResId }.toSet()
            assertEquals(
                "Stories 11..100 ($lang) should utilize all 3 thematic Eastern covers",
                expectedUniversalCovers,
                usedCovers
            )

            val firstBubbleTexts = mutableListOf<String>()
            for (story in premiumStories) {
                assertTrue(story.coverDrawableResId in expectedUniversalCovers)
                assertEquals(5, story.pages.size)
                assertEquals(story.coverDrawableResId, story.pages[0].drawableResId)
                assertEquals(story.title, story.pages[0].coverTitle)
                assertTrue(story.fullStoryText.isNotBlank())

                val panelPages = story.pages.drop(1)
                assertEquals(4, panelPages.mapNotNull { it.panelCrop }.toSet().size)

                for (page in panelPages) {
                    assertTrue(page.drawableResId in expectedUniversalDrawables)
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

        // Same 3 universal artwork sheets, yet completely distinct localized bubble text per language
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
    fun `new teahouse and night templates decode panels cleanly and ad batches unlock up to story 100`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)
        val crop = PanelCrop(left = 0.020f, top = 0.018f, right = 0.490f, bottom = 0.490f)

        val teahousePanel = ComicPanelCropCache.getOrDecodePanel(
            context.resources,
            R.drawable.img_universal_hodja_teahouse,
            crop
        )
        val nightPanel = ComicPanelCropCache.getOrDecodePanel(
            context.resources,
            R.drawable.img_universal_hodja_night,
            crop
        )
        assertNotNull(teahousePanel)
        assertNotNull(nightPanel)
        assertTrue(teahousePanel!!.width > 0 && teahousePanel.height > 0)
        assertTrue(nightPanel!!.width > 0 && nightPanel.height > 0)

        // Verify explicit theme mapping and cyclic fallback when theme is absent
        assertEquals(R.drawable.img_universal_hodja_teahouse, repository.resolveUniversalTemplateDrawable("teahouse", 99))
        assertEquals(R.drawable.img_universal_hodja_night, repository.resolveUniversalTemplateDrawable("night", 99))
        assertEquals(R.drawable.img_universal_hodja_template, repository.resolveUniversalTemplateDrawable("bazaar", 99))
        assertEquals(R.drawable.img_universal_hodja_teahouse, repository.resolveUniversalTemplateDrawable(null, 2))

        // Ad batches advance sequentially past 50 all the way to 100
        var unlockedMax = repository.adUnlockedMaxStory.value
        assertEquals(10, unlockedMax)
        repeat(18) {
            unlockedMax = repository.unlockNextAdBatch()
        }
        assertEquals(100, unlockedMax)
        // Must cap cleanly at catalog max 100
        assertEquals(100, repository.unlockNextAdBatch())
    }

    @Test
    fun `library search and tier atmosphere filters locate stories accurately across 100 parables`() {
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

        // Filter: NIGHT matches night-themed episodes including #100
        val nightStories = filterLibraryComics(
            comics = allComics,
            query = "",
            selectedFilter = LibraryFilter.NIGHT,
            isPremiumUnlocked = true,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertTrue(nightStories.any { it.storyNumber == 100 })
        assertTrue(nightStories.all { it.themeTag == "night" })

        // Numeric query with '#' prefix resolves exact episode #98
        val byNumber = filterLibraryComics(
            comics = allComics,
            query = "#98",
            selectedFilter = LibraryFilter.ALL,
            isPremiumUnlocked = true,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertEquals(listOf(98), byNumber.map { it.storyNumber })

        // Keyword query matches localized RU content cleanly and unknown words return empty
        val byKeyword = filterLibraryComics(
            comics = allComics,
            query = "финиковой",
            selectedFilter = LibraryFilter.BAZAAR,
            isPremiumUnlocked = false,
            seriesUnlocked = false,
            adUnlockedMaxStory = 10
        )
        assertEquals(listOf(51), byKeyword.map { it.storyNumber })

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
    fun `dialogue-rich second-half parables 79 and 90 parse cleanly across RU ES and EN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val builtIn = repository.comics.value.filter { it.isBuiltIn }
            for (storyNumber in listOf(79, 90)) {
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
}
