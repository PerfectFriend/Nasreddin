package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ComicRepository
import com.example.model.AppLanguage
import com.example.ui.ComicViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ходжа", appName)
    }

    @Test
    fun `vault loads 25 anecdotes with 10 free and 15 premium`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        val comics = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(25, comics.size)

        // Verify first 10 are free
        val freeStories = comics.filter { it.isFree }
        assertEquals(10, freeStories.size)

        // Verify remaining 15 are premium
        val premiumStories = comics.filter { !it.isFree }
        assertEquals(15, premiumStories.size)
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
    fun `strict language filtering applies across all 25 stories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        // Russian selection
        repository.setLanguage(AppLanguage.RUSSIAN)
        val ruStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(25, ruStories.size)
        assertTrue(ruStories.all { it.language == AppLanguage.RUSSIAN })

        // Spanish selection
        repository.setLanguage(AppLanguage.SPANISH)
        val esStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(25, esStories.size)
        assertTrue(esStories.all { it.language == AppLanguage.SPANISH })

        // English selection
        repository.setLanguage(AppLanguage.ENGLISH)
        val enStories = repository.comics.value.filter { it.isBuiltIn }
        assertEquals(25, enStories.size)
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
        assertEquals(R.drawable.img_comic_ru_cover, ruComic.coverDrawableResId)
        assertEquals(R.drawable.img_comic_ru_cover, ruComic.pages[0].drawableResId)
        assertEquals(R.drawable.img_comic_ru_p1_v2, ruComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_ru_p2_v2, ruComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_ru_p3_v2, ruComic.pages[3].drawableResId)

        val esComic = repository.getBuiltInSpanishComic()
        assertEquals(R.drawable.img_comic_es_cover, esComic.coverDrawableResId)
        assertEquals(R.drawable.img_comic_es_cover, esComic.pages[0].drawableResId)
        assertEquals(R.drawable.img_comic_es_p1_v2, esComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_es_p2_v2, esComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_es_p3_v2, esComic.pages[3].drawableResId)

        val enComic = repository.getBuiltInEnglishComic()
        assertEquals(R.drawable.img_comic_en_cover, enComic.coverDrawableResId)
        assertEquals(R.drawable.img_comic_en_cover, enComic.pages[0].drawableResId)
        assertEquals(R.drawable.img_comic_en_p1_v2, enComic.pages[1].drawableResId)
        assertEquals(R.drawable.img_comic_en_p2_v2, enComic.pages[2].drawableResId)
        assertEquals(R.drawable.img_comic_en_p3_v2, enComic.pages[3].drawableResId)
    }

    @Test
    fun `all 25 stories have at least 4 pages and episodes 2 to 10 split into 4 panel crops across RU ES and EN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ComicRepository(context)

        for (lang in listOf(AppLanguage.RUSSIAN, AppLanguage.SPANISH, AppLanguage.ENGLISH)) {
            repository.setLanguage(lang)
            val allBuiltIn = repository.comics.value.filter { it.isBuiltIn }
            assertEquals(25, allBuiltIn.size)

            // Every story must strictly have >= 4 pages
            for (story in allBuiltIn) {
                assertTrue(
                    "Story ${story.storyNumber} ($lang) should have at least 4 pages, had ${story.pages.size}",
                    story.pages.size >= 4
                )
            }

            val freeComics = allBuiltIn.filter { it.isFree }
            assertEquals(10, freeComics.size)

            val coverDrawables = freeComics.mapNotNull { it.coverDrawableResId }.toSet()
            assertEquals("Expected 10 distinct covers for $lang", 10, coverDrawables.size)

            for (ep in freeComics.drop(1)) {
                assertEquals(4, ep.pages.size)
                assertEquals(4, ep.pages.mapNotNull { it.panelCrop }.toSet().size)
                for (page in ep.pages) {
                    assertNotNull(page.drawableResId)
                    assertEquals(ep.coverDrawableResId, page.drawableResId)
                }
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
        assertEquals(4, viewModel.uiState.value.activeComic?.pages?.size)

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
}
