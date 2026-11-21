package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.model.PanelCrop
import com.example.util.AppStrings
import com.example.util.StoryBubbleComposer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Single source of truth for the built-in anecdote comic catalog, language selection,
 * reading progress, user-imported folders, and Hybrid Monetization unlock state.
 *
 * Design invariants:
 * 1. Only stories matching [currentLanguage] are exposed in [comics] (strict language filtering).
 * 2. Every built-in story strictly provides at least 4 readable pages (cover + 4 panels = 5 for vault):
 *    - Story #1 uses a themed localized Eastern cover plus 3 full-page comic scenes (4 pages).
 *    - Stories #2..#10 prepend a localized Eastern cover page and split each 2x2 comic sheet into 4 panel pages (5 pages).
 *    - Stories #11..#50 prepend a localized Eastern cover page paired with unique bespoke 4-panel scenes and dynamic localized bubbles (5 pages).
 */
class ComicRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cyber_comics_prefs", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(detectInitialLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isPremiumUnlocked = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM_UNLOCKED, false))
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val _seriesUnlocked = MutableStateFlow(prefs.getBoolean(KEY_SERIES_UNLOCKED, false))
    val seriesUnlocked: StateFlow<Boolean> = _seriesUnlocked.asStateFlow()

    private val _adUnlockedMaxStory = MutableStateFlow(prefs.getInt(KEY_AD_UNLOCKED_MAX_STORY, 10))
    val adUnlockedMaxStory: StateFlow<Int> = _adUnlockedMaxStory.asStateFlow()

    private val _comics = MutableStateFlow<List<ComicBook>>(emptyList())
    val comics: StateFlow<List<ComicBook>> = _comics.asStateFlow()

    // Parsed once and cached in memory so switching languages or reloading progress avoids repeated asset I/O
    private val vaultJsonArray: JSONArray? by lazy {
        try {
            val jsonString = context.assets.open("nasreddin_anecdotes_vault.json").bufferedReader().use { it.readText() }
            JSONArray(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse bundled anecdote vault JSON", e)
            null
        }
    }

    init {
        loadAllComics()
    }

    private fun detectInitialLanguage(): AppLanguage {
        val savedLang = prefs.getString(KEY_SAVED_LANG, null)
        if (savedLang != null) {
            return try {
                AppLanguage.valueOf(savedLang)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid persisted language '$savedLang', falling back to ENGLISH", e)
                AppLanguage.ENGLISH
            }
        }
        val systemLang = Locale.getDefault().language.lowercase()
        return when {
            systemLang.startsWith("ru") || systemLang.startsWith("be") || systemLang.startsWith("uk") || systemLang.startsWith("kk") -> AppLanguage.RUSSIAN
            systemLang.startsWith("es") -> AppLanguage.SPANISH
            else -> AppLanguage.ENGLISH
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString(KEY_SAVED_LANG, language.name).apply()
        loadAllComics()
    }

    fun unlockPremium(): Boolean {
        _isPremiumUnlocked.value = true
        prefs.edit().putBoolean(KEY_PREMIUM_UNLOCKED, true).apply()
        loadAllComics()
        return true
    }

    fun unlockSeriesPack(): Boolean {
        _seriesUnlocked.value = true
        prefs.edit().putBoolean(KEY_SERIES_UNLOCKED, true).apply()
        loadAllComics()
        return true
    }

    fun unlockNextAdBatch(batchSize: Int = 5, maxStory: Int? = null): Int {
        val catalogMax = maxStory ?: (_comics.value.filter { it.isBuiltIn }.maxOfOrNull { it.storyNumber } ?: 50)
        val nextMax = (_adUnlockedMaxStory.value + batchSize).coerceAtMost(catalogMax)
        _adUnlockedMaxStory.value = nextMax
        prefs.edit().putInt(KEY_AD_UNLOCKED_MAX_STORY, nextMax).apply()
        return nextMax
    }

    fun isComicUnlocked(comic: ComicBook): Boolean {
        return comic.isFree ||
            _isPremiumUnlocked.value ||
            _seriesUnlocked.value ||
            comic.storyNumber <= _adUnlockedMaxStory.value
    }

    fun restorePurchases(): Boolean {
        val wasPremium = prefs.getBoolean(KEY_PREMIUM_UNLOCKED, false)
        val wasSeries = prefs.getBoolean(KEY_SERIES_UNLOCKED, false)
        if (wasPremium || wasSeries) {
            _isPremiumUnlocked.value = wasPremium
            _seriesUnlocked.value = wasSeries
            loadAllComics()
            return true
        }
        return false
    }

    /**
     * Strictly displays ONLY comics matching the selected language.
     * First 10 stories are free, subsequent 15 stories are premium.
     */
    fun loadAllComics() {
        val list = mutableListOf<ComicBook>()
        val lang = _currentLanguage.value

        val storiesFromVault = loadStoriesFromVault(lang)

        // Story 1 has high-res generated visual comic pages
        val featuredStory = when (lang) {
            AppLanguage.RUSSIAN -> getBuiltInRussianComic()
            AppLanguage.SPANISH -> getBuiltInSpanishComic()
            AppLanguage.ENGLISH -> getBuiltInEnglishComic()
        }

        // Add featured story first
        list.add(featuredStory)

        // Add stories 2 through catalog end (50 curated Volume 1 stories)
        for (story in storiesFromVault.drop(1)) {
            list.add(story)
        }

        // Add user-imported custom folders
        list.addAll(loadCustomComics())
        _comics.value = list
    }

    private fun loadStoriesFromVault(lang: AppLanguage): List<ComicBook> {
        val result = mutableListOf<ComicBook>()
        try {
            val array = vaultJsonArray ?: return result

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val seq = obj.optInt("sequence", i + 1)
                val isFree = obj.optBoolean("isFree", seq <= 10)
                val id = obj.optString("id", "anecdote_$seq")

                val title = when (lang) {
                    AppLanguage.SPANISH -> obj.optString("title_es")
                    AppLanguage.ENGLISH -> obj.optString("title_en")
                    AppLanguage.RUSSIAN -> obj.optString("title_ru")
                }

                val text = when (lang) {
                    AppLanguage.SPANISH -> obj.optString("text_es")
                    AppLanguage.ENGLISH -> obj.optString("text_en")
                    AppLanguage.RUSSIAN -> obj.optString("text_ru")
                }

                val moral = when (lang) {
                    AppLanguage.RUSSIAN -> obj.optString("moral_ru")
                    AppLanguage.SPANISH -> obj.optString("moral_es")
                    AppLanguage.ENGLISH -> obj.optString("moral_en")
                }

                val rawTheme = obj.optString("theme", "")
                val normalizedTheme = resolveNormalizedTheme(rawTheme, seq)
                val coverDrawable = resolveStoryCoverDrawable(normalizedTheme, seq)
                val episodeDrawable = getEpisodeDrawable(seq, lang)
                val panelSheetDrawable = resolveStoryBespokeSheetDrawable(seq) ?: coverDrawable

                val coverPage = ComicPage(
                    id = "${id}_cover",
                    title = AppStrings.coverPageTitle(lang, seq, title),
                    drawableResId = coverDrawable,
                    narrativeText = text,
                    dialogueText = moral.takeIf { it.isNotEmpty() },
                    sequenceNumber = 1,
                    language = lang,
                    coverTitle = "$seq. $title",
                    coverSubtitle = AppStrings.parchmentEpisodeBadge(lang, seq)
                )

                val panelPages = if (episodeDrawable != null) {
                    val quadrantCrops = listOf(
                        PanelCrop(left = 0.015f, top = 0.082f, right = 0.496f, bottom = 0.532f),
                        PanelCrop(left = 0.504f, top = 0.082f, right = 0.985f, bottom = 0.532f),
                        PanelCrop(left = 0.015f, top = 0.539f, right = 0.496f, bottom = 0.986f),
                        PanelCrop(left = 0.504f, top = 0.539f, right = 0.985f, bottom = 0.986f)
                    )
                    quadrantCrops.mapIndexed { idx, crop ->
                        val step = idx + 1
                        ComicPage(
                            id = "${id}_p$step",
                            title = "$title • ${AppStrings.panelStepLabel(lang, step)}",
                            drawableResId = episodeDrawable,
                            narrativeText = text,
                            dialogueText = moral.takeIf { it.isNotEmpty() },
                            sequenceNumber = step + 1,
                            language = lang,
                            panelCrop = crop
                        )
                    }
                } else {
                    // Bespoke text-free 2x2 sheets (#11..#50) serve all languages while
                    // StoryBubbleComposer renders localized dialogue balloons dynamically.
                    val universalQuadrantCrops = listOf(
                        PanelCrop(left = 0.020f, top = 0.018f, right = 0.490f, bottom = 0.490f),
                        PanelCrop(left = 0.510f, top = 0.018f, right = 0.980f, bottom = 0.490f),
                        PanelCrop(left = 0.020f, top = 0.510f, right = 0.490f, bottom = 0.982f),
                        PanelCrop(left = 0.510f, top = 0.510f, right = 0.980f, bottom = 0.982f)
                    )
                    val bubbleBeats = StoryBubbleComposer.composeFourPanelBubbles(
                        storyId = id,
                        storyText = text,
                        moral = moral,
                        language = lang
                    )
                    universalQuadrantCrops.mapIndexed { idx, crop ->
                        val step = idx + 1
                        ComicPage(
                            id = "${id}_p$step",
                            title = "$title • ${AppStrings.panelStepLabel(lang, step)}",
                            drawableResId = panelSheetDrawable,
                            narrativeText = if (step == 4 && moral.isNotEmpty()) moral else text,
                            dialogueText = moral.takeIf { it.isNotEmpty() },
                            sequenceNumber = step + 1,
                            language = lang,
                            panelCrop = crop,
                            bubbles = bubbleBeats.getOrElse(idx) { emptyList() }
                        )
                    }
                }

                val pages = listOf(coverPage) + panelPages

                val subtitleText = when (lang) {
                    AppLanguage.RUSSIAN -> if (isFree) "Ходжа Насреддин" else "Ходжа Насреддин • Премиум"
                    AppLanguage.SPANISH -> if (isFree) "Nasreddin Hodja" else "Nasreddin Hodja • Premium"
                    AppLanguage.ENGLISH -> if (isFree) "Hodja Nasreddin" else "Hodja Nasreddin • Premium"
                }

                val savedProgress = prefs.getInt("progress_$id", 0)

                result.add(
                    ComicBook(
                        id = id,
                        title = "$seq. $title",
                        subtitle = subtitleText,
                        description = text,
                        coverDrawableResId = coverDrawable,
                        pages = pages,
                        isBuiltIn = true,
                        language = lang,
                        isFree = isFree,
                        storyNumber = seq,
                        themeTag = normalizedTheme,
                        lastReadPageIndex = savedProgress.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
                        fullStoryText = text,
                        moralText = moral
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading stories from vault for language=$lang", e)
        }
        return result
    }

    private fun resolveNormalizedTheme(theme: String?, sequence: Int): String {
        return when (theme?.trim()?.lowercase()) {
            "teahouse" -> "teahouse"
            "night" -> "night"
            "bazaar" -> "bazaar"
            else -> when ((sequence - 1).mod(3)) {
                0 -> "bazaar"
                1 -> "teahouse"
                else -> "night"
            }
        }
    }

    /**
     * Resolves a story-specific text-free 2x2 illustrated plate for stories #11..#50 so each
     * episode depicts its own plot beats while retaining localized RU/ES/EN dialogue bubbles.
     */
    internal fun resolveStoryBespokeSheetDrawable(sequence: Int): Int? = BESPOKE_STORY_DRAWABLES[sequence]

    /**
     * Resolves an ornate Eastern-framed cover template with a blank title cartouche
     * matching each story's atmosphere across 8 curated visual scenes ("mosque" for Story #1,
     * plus theme-matched courtroom, caravan, lakeside, garden, bazaar, teahouse, and night plates).
     */
    internal fun resolveStoryCoverDrawable(theme: String?, sequence: Int): Int {
        if (sequence == 1) return R.drawable.img_cover_mosque
        return when (resolveNormalizedTheme(theme, sequence)) {
            "teahouse" -> when (sequence.mod(3)) {
                0 -> R.drawable.img_cover_garden
                1 -> R.drawable.img_cover_court
                else -> R.drawable.img_cover_teahouse
            }
            "night" -> when (sequence.mod(3)) {
                0 -> R.drawable.img_cover_lake
                1 -> R.drawable.img_cover_night
                else -> R.drawable.img_cover_caravan
            }
            else -> when (sequence.mod(3)) {
                0 -> R.drawable.img_cover_caravan
                1 -> R.drawable.img_cover_bazaar
                else -> R.drawable.img_cover_court
            }
        }
    }

    private fun getEpisodeDrawable(seq: Int, lang: AppLanguage): Int? {
        return when (seq) {
            2 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep02_ru
                AppLanguage.SPANISH -> R.drawable.img_ep02_es
                AppLanguage.ENGLISH -> R.drawable.img_ep02_en
            }
            3 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep03_ru
                AppLanguage.SPANISH -> R.drawable.img_ep03_es
                AppLanguage.ENGLISH -> R.drawable.img_ep03_en
            }
            4 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep04_ru
                AppLanguage.SPANISH -> R.drawable.img_ep04_es
                AppLanguage.ENGLISH -> R.drawable.img_ep04_en
            }
            5 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep05_ru
                AppLanguage.SPANISH -> R.drawable.img_ep05_es
                AppLanguage.ENGLISH -> R.drawable.img_ep05_en
            }
            6 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep06_ru
                AppLanguage.SPANISH -> R.drawable.img_ep06_es
                AppLanguage.ENGLISH -> R.drawable.img_ep06_en
            }
            7 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep07_ru
                AppLanguage.SPANISH -> R.drawable.img_ep07_es
                AppLanguage.ENGLISH -> R.drawable.img_ep07_en
            }
            8 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep08_ru
                AppLanguage.SPANISH -> R.drawable.img_ep08_es
                AppLanguage.ENGLISH -> R.drawable.img_ep08_en
            }
            9 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep09_ru
                AppLanguage.SPANISH -> R.drawable.img_ep09_es
                AppLanguage.ENGLISH -> R.drawable.img_ep09_en
            }
            10 -> when (lang) {
                AppLanguage.RUSSIAN -> R.drawable.img_ep10_ru
                AppLanguage.SPANISH -> R.drawable.img_ep10_es
                AppLanguage.ENGLISH -> R.drawable.img_ep10_en
            }
            else -> null
        }
    }

    fun addCustomComic(comic: ComicBook) {
        val current = _comics.value.filter { it.id != comic.id }.toMutableList()
        current.add(comic)
        _comics.value = current
        saveCustomComics(current.filter { !it.isBuiltIn })
    }

    fun updateComic(updatedComic: ComicBook) {
        val current = _comics.value.map {
            if (it.id == updatedComic.id) updatedComic else it
        }
        _comics.value = current
        saveCustomComics(current.filter { !it.isBuiltIn })
    }

    fun updateReadingProgress(comicId: String, pageIndex: Int) {
        val current = _comics.value.map {
            if (it.id == comicId) it.copy(lastReadPageIndex = pageIndex) else it
        }
        _comics.value = current
        saveCustomComics(current.filter { !it.isBuiltIn })
        if (comicId.startsWith("builtin_") || comicId.startsWith("anecdote_")) {
            prefs.edit().putInt("progress_$comicId", pageIndex).apply()
        }
    }

    fun deleteComic(comicId: String) {
        if (comicId.startsWith("builtin_") || comicId.startsWith("anecdote_")) return
        val current = _comics.value.filter { it.id != comicId }
        _comics.value = current
        saveCustomComics(current.filter { !it.isBuiltIn })
    }

    fun getBuiltInRussianComic(): ComicBook {
        val savedProgress = prefs.getInt("progress_$BUILT_IN_RU_ID", 0)
        val storyTitle = "1. Проповедь в мечети"

        val pages = listOf(
            ComicPage(
                id = "builtin_ru_cover",
                title = "Обложка • Акшехир",
                drawableResId = R.drawable.img_cover_mosque,
                narrativeText = "Под звёздным куполом древнего Акшехира мудрец Ходжа Насреддин прибывает в Великую Мечеть со своим верным осликом.",
                sequenceNumber = 1,
                language = AppLanguage.RUSSIAN,
                coverTitle = storyTitle,
                coverSubtitle = AppStrings.parchmentEpisodeBadge(AppLanguage.RUSSIAN, 1)
            ),
            ComicPage(
                id = "builtin_ru_page_1",
                title = "Часть I • «Не знаем»",
                drawableResId = R.drawable.img_comic_ru_p1_v2,
                narrativeText = "Однажды Ходжа Насреддин взошёл на кафедру в Акшехире и обратился к верующим: «Знаете ли вы, что я сейчас вам скажу?» – «Нет, не знаем», – ответили верующие. Тогда Ходжа сказал: «Если не знаете, то зачем мне вам что-то говорить?» После чего он сошёл с кафедры и отправился в далекий путь.",
                dialogueText = "Ходжа: «Знаете ли вы, что я вам скажу?»\nПрихожане: «Нет, не знаем!»\nХоджа: «Если не знаете, зачем мне говорить?»",
                sequenceNumber = 2,
                language = AppLanguage.RUSSIAN
            ),
            ComicPage(
                id = "builtin_ru_page_2",
                title = "Часть II • «Знаем»",
                drawableResId = R.drawable.img_comic_ru_p2_v2,
                narrativeText = "Прошло время, и Ходжа снова взошёл на кафедру и задал правоверным тот же вопрос. «Знаем», – ответили Насреддину верующие. «Ну, если знаете, значит, мне нет надобности и говорить», – сказал Ходжа и опять удалился. Правоверные поразились словам Ходжи, но решили, что когда он снова задаст свой удивительный вопрос, то они ответят: «Одни из нас знают, а другие – нет».",
                dialogueText = "Ходжа: «Знаете ли вы, что я вам скажу?»\nПрихожане: «Знаем!»\nХоджа: «Раз знаете, мне незачем говорить!»\n(Шепот в толпе): «В следующий раз ответим: одни знают, другие нет!»",
                sequenceNumber = 3,
                language = AppLanguage.RUSSIAN
            ),
            ComicPage(
                id = "builtin_ru_page_3",
                title = "Часть III • «Великий финал»",
                drawableResId = R.drawable.img_comic_ru_p3_v2,
                narrativeText = "И вновь Ходжа взошёл на кафедру, и в третий раз обратился к народу со своим вопросом. «Одни из нас знают, другие нет», – был ему ответ. И тогда Ходжа, лицо которого при этом было совершенно серьёзным, воскликнул: «Прекрасно! Раз так, тогда пусть те из вас, кто знает, расскажут тем, которые не знают».",
                dialogueText = "Ходжа: «Знаете ли вы, что я вам скажу?»\nПрихожане: «Одни из нас знают, а другие нет!»\nХоджа: «Прекрасно! Пусть те, кто знает, расскажут тем, кто не знает!»",
                sequenceNumber = 4,
                language = AppLanguage.RUSSIAN
            )
        )

        return ComicBook(
            id = BUILT_IN_RU_ID,
            title = storyTitle,
            subtitle = "Ходжа Насреддин",
            description = "Классический суфийский анекдот о мудром Ходже Насреддине. Иллюстрированный комикс и полная притча на пергаменте на русском языке.",
            coverDrawableResId = R.drawable.img_cover_mosque,
            pages = pages,
            isBuiltIn = true,
            language = AppLanguage.RUSSIAN,
            isFree = true,
            storyNumber = 1,
            lastReadPageIndex = savedProgress.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            fullStoryText = pages.drop(1).mapNotNull { it.narrativeText }.joinToString("\n\n"),
            moralText = "Истинная мудрость рождается в диалоге: знающие делятся светом понимания с теми, кто лишь начинает путь."
        )
    }

    fun getBuiltInSpanishComic(): ComicBook {
        val savedProgress = prefs.getInt("progress_$BUILT_IN_ES_ID", 0)
        val storyTitle = "1. El sermón en la mezquita"

        val pages = listOf(
            ComicPage(
                id = "builtin_es_cover",
                title = "Portada • Akşehir",
                drawableResId = R.drawable.img_cover_mosque,
                narrativeText = "Bajo la bóveda estrellada de Akşehir, el sabio Nasreddin Hodja llega a la Gran Mezquita junto a su fiel borrico.",
                sequenceNumber = 1,
                language = AppLanguage.SPANISH,
                coverTitle = storyTitle,
                coverSubtitle = AppStrings.parchmentEpisodeBadge(AppLanguage.SPANISH, 1)
            ),
            ComicPage(
                id = "builtin_es_page_1",
                title = "Parte I • «No lo sabemos»",
                drawableResId = R.drawable.img_comic_es_p1_v2,
                narrativeText = "Una vez, Nasreddin Hodja subió al púlpito en Akşehir y se dirigió a los fieles: «¿Saben lo que les voy a decir?» – «No, no lo sabemos», respondieron los creyentes. Entonces Hodja dijo: «Si no lo saben, ¿para qué decirles algo?» Tras lo cual descendió del púlpito y emprendió un largo viaje.",
                dialogueText = "– ¿Saben lo que les voy a decir?\n– ¡No, no lo sabemos!\n– Si no lo saben, ¿para qué hablarles?",
                sequenceNumber = 2,
                language = AppLanguage.SPANISH
            ),
            ComicPage(
                id = "builtin_es_page_2",
                title = "Parte II • «Lo sabemos»",
                drawableResId = R.drawable.img_comic_es_p2_v2,
                narrativeText = "Pasó el tiempo, y Hodja volvió a subir al púlpito y formuló a los creyentes la misma pregunta. «Sí, lo sabemos», respondieron a Nasreddin. «Bueno, si ya lo saben, no hay necesidad de que hable», dijo Hodja y volvió a retirarse. Los creyentes acordaron una estrategia: «La próxima vez responderemos: unos lo sabemos, y otros no».",
                dialogueText = "– ¿Saben lo que voy a decir?\n– ¡Lo sabemos!\n– Bueno, si ya lo saben, no tengo por qué hablar...\n(Plan de la multitud): La próxima vez diremos: unos lo sabemos y otros no.",
                sequenceNumber = 3,
                language = AppLanguage.SPANISH
            ),
            ComicPage(
                id = "builtin_es_page_3",
                title = "Parte III • «Gran Final»",
                drawableResId = R.drawable.img_comic_es_p3_v2,
                narrativeText = "Y una vez más Hodja subió al púlpito, y por tercera vez se dirigió al pueblo con su pregunta. «Unos de nosotros lo sabemos, y otros no», fue la respuesta. Y entonces Hodja, con el rostro completamente serio, exclamó: «¡Excelente! Siendo así, ¡que los que saben se lo cuenten a los que no saben!»",
                dialogueText = "Hodja: «¿Saben lo que les voy a decir?»\nFieles: «¡Unos lo saben y otros no!»\nHodja: «¡Excelente! ¡Que los que saben se lo cuenten a los que no saben!»",
                sequenceNumber = 4,
                language = AppLanguage.SPANISH
            )
        )

        return ComicBook(
            id = BUILT_IN_ES_ID,
            title = storyTitle,
            subtitle = "Nasreddin Hodja",
            description = "Anécdota sufí clásica del sabio Nasreddin Hodja. Cómic ilustrado y relato completo en pergamino en español.",
            coverDrawableResId = R.drawable.img_cover_mosque,
            pages = pages,
            isBuiltIn = true,
            language = AppLanguage.SPANISH,
            isFree = true,
            storyNumber = 1,
            lastReadPageIndex = savedProgress.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            fullStoryText = pages.drop(1).mapNotNull { it.narrativeText }.joinToString("\n\n"),
            moralText = "La verdadera sabiduría florece al compartirse: quienes ya conocen la luz del saber deben guiar a quienes aún la buscan."
        )
    }

    fun getBuiltInEnglishComic(): ComicBook {
        val savedProgress = prefs.getInt("progress_$BUILT_IN_EN_ID", 0)
        val storyTitle = "1. The Sermon in the Mosque"

        val pages = listOf(
            ComicPage(
                id = "builtin_en_cover",
                title = "Cover • Akşehir",
                drawableResId = R.drawable.img_cover_mosque,
                narrativeText = "Beneath the starry sky of Akşehir, the wise Hodja Nasreddin arrives at the Grand Mosque beside his faithful donkey.",
                sequenceNumber = 1,
                language = AppLanguage.ENGLISH,
                coverTitle = storyTitle,
                coverSubtitle = AppStrings.parchmentEpisodeBadge(AppLanguage.ENGLISH, 1)
            ),
            ComicPage(
                id = "builtin_en_page_1",
                title = "Part I • «We Do Not Know»",
                drawableResId = R.drawable.img_comic_en_p1_v2,
                narrativeText = "One day, Hodja Nasreddin ascended the pulpit in Akşehir and asked: 'Do you know what I will say to you?' – 'No, we do not know', replied the congregation. Hodja said: 'If you do not know, then why should I speak to you?' And he stepped down and walked away into the distance.",
                dialogueText = "– Do you know what I will say to you?\n– No, we do not know!\n– If you do not know, why should I speak to you?",
                sequenceNumber = 2,
                language = AppLanguage.ENGLISH
            ),
            ComicPage(
                id = "builtin_en_page_2",
                title = "Part II • «We Know»",
                drawableResId = R.drawable.img_comic_en_p2_v2,
                narrativeText = "Time passed, and Hodja ascended the pulpit again with the same question. 'Yes, we know!', shouted the believers. 'Well, if you know, there is no need for me to speak', Hodja smiled and walked away again. The crowd plotted: 'Next time, let us answer: Some of us know, and some do not!'.",
                dialogueText = "– Do you know what I will say?\n– Yes, we know!\n– Well, if you know, then there is no need for me to speak!\n(Crowd whisper): Next time we shall say: Some know, and some do not!",
                sequenceNumber = 3,
                language = AppLanguage.ENGLISH
            ),
            ComicPage(
                id = "builtin_en_page_3",
                title = "Part III • «The Grand Climax»",
                drawableResId = R.drawable.img_comic_en_p3_v2,
                narrativeText = "And again Hodja ascended the pulpit and asked his question. 'Some of us know, and some do not', was their clever answer. Then Hodja, with a deadpan serious face, proclaimed: 'Wonderful! In that case, let those of you who know tell those who do not!'",
                dialogueText = "– Do you know what I will say?\n– Some of us know, and some do not!\n– Wonderful! Then let those of you who know tell those who do not!",
                sequenceNumber = 4,
                language = AppLanguage.ENGLISH
            )
        )

        return ComicBook(
            id = BUILT_IN_EN_ID,
            title = storyTitle,
            subtitle = "Hodja Nasreddin",
            description = "The classic Sufi wisdom tale of Hodja Nasreddin. Illustrated comic panels paired with the full parchment story in English.",
            coverDrawableResId = R.drawable.img_cover_mosque,
            pages = pages,
            isBuiltIn = true,
            language = AppLanguage.ENGLISH,
            isFree = true,
            storyNumber = 1,
            lastReadPageIndex = savedProgress.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            fullStoryText = pages.drop(1).mapNotNull { it.narrativeText }.joinToString("\n\n"),
            moralText = "True wisdom grows only when shared: those who already understand must pass their lantern to those who have yet to learn."
        )
    }

    private fun saveCustomComics(list: List<ComicBook>) {
        try {
            val array = JSONArray()
            for (comic in list) {
                val obj = JSONObject()
                obj.put("id", comic.id)
                obj.put("title", comic.title)
                obj.put("subtitle", comic.subtitle)
                obj.put("description", comic.description)
                obj.put("coverUriString", comic.coverUriString)
                obj.put("sourceFolderUri", comic.sourceFolderUri)
                obj.put("lastReadPageIndex", comic.lastReadPageIndex)
                obj.put("dateAdded", comic.dateAdded)

                val pagesArray = JSONArray()
                for (p in comic.pages) {
                    val pObj = JSONObject()
                    pObj.put("id", p.id)
                    pObj.put("title", p.title)
                    pObj.put("uriString", p.uriString)
                    pObj.put("narrativeText", p.narrativeText)
                    pObj.put("dialogueText", p.dialogueText)
                    pObj.put("sequenceNumber", p.sequenceNumber)
                    pagesArray.put(pObj)
                }
                obj.put("pages", pagesArray)
                array.put(obj)
            }
            prefs.edit().putString(KEY_CUSTOM_COMICS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize custom comics", e)
        }
    }

    private fun loadCustomComics(): List<ComicBook> {
        val result = mutableListOf<ComicBook>()
        val raw = prefs.getString(KEY_CUSTOM_COMICS, null) ?: return result
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pages = mutableListOf<ComicPage>()
                val pagesArray = obj.optJSONArray("pages")
                if (pagesArray != null) {
                    for (j in 0 until pagesArray.length()) {
                        val pObj = pagesArray.getJSONObject(j)
                        pages.add(
                            ComicPage(
                                id = pObj.optString("id"),
                                title = pObj.optString("title"),
                                uriString = pObj.optString("uriString").takeIf { it.isNotEmpty() },
                                narrativeText = pObj.optString("narrativeText").takeIf { it.isNotEmpty() },
                                dialogueText = pObj.optString("dialogueText").takeIf { it.isNotEmpty() },
                                sequenceNumber = pObj.optInt("sequenceNumber", j + 1)
                            )
                        )
                    }
                }
                result.add(
                    ComicBook(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Comic"),
                        subtitle = obj.optString("subtitle"),
                        description = obj.optString("description"),
                        coverUriString = obj.optString("coverUriString").takeIf { it.isNotEmpty() },
                        sourceFolderUri = obj.optString("sourceFolderUri").takeIf { it.isNotEmpty() },
                        lastReadPageIndex = obj.optInt("lastReadPageIndex", 0),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                        pages = pages,
                        isBuiltIn = false,
                        isFree = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize custom comics", e)
        }
        return result
    }

    companion object {
        private const val TAG = "ComicRepository"
        const val BUILT_IN_ID = "builtin_sermon_in_mosque_ru"
        const val BUILT_IN_RU_ID = "builtin_sermon_in_mosque_ru"
        const val BUILT_IN_ES_ID = "builtin_sermon_in_mosque_es"
        const val BUILT_IN_EN_ID = "builtin_sermon_in_mosque_en"
        private const val KEY_CUSTOM_COMICS = "custom_comics_json"
        private const val KEY_SAVED_LANG = "saved_app_language"
        private const val KEY_PREMIUM_UNLOCKED = "premium_unlocked"
        private const val KEY_SERIES_UNLOCKED = "series_unlocked"
        private const val KEY_AD_UNLOCKED_MAX_STORY = "ad_unlocked_max_story"

        private val BESPOKE_STORY_DRAWABLES: Map<Int, Int> = mapOf(
            11 to R.drawable.img_story_011,
            12 to R.drawable.img_story_012,
            13 to R.drawable.img_story_013,
            14 to R.drawable.img_story_014,
            15 to R.drawable.img_story_015,
            16 to R.drawable.img_story_016,
            17 to R.drawable.img_story_017,
            18 to R.drawable.img_story_018,
            19 to R.drawable.img_story_019,
            20 to R.drawable.img_story_020,
            21 to R.drawable.img_story_021,
            22 to R.drawable.img_story_022,
            23 to R.drawable.img_story_023,
            24 to R.drawable.img_story_024,
            25 to R.drawable.img_story_025,
            26 to R.drawable.img_story_026,
            27 to R.drawable.img_story_027,
            28 to R.drawable.img_story_028,
            29 to R.drawable.img_story_029,
            30 to R.drawable.img_story_030,
            31 to R.drawable.img_story_031,
            32 to R.drawable.img_story_032,
            33 to R.drawable.img_story_033,
            34 to R.drawable.img_story_034,
            35 to R.drawable.img_story_035,
            36 to R.drawable.img_story_036,
            37 to R.drawable.img_story_037,
            38 to R.drawable.img_story_038,
            39 to R.drawable.img_story_039,
            40 to R.drawable.img_story_040,
            41 to R.drawable.img_story_041,
            42 to R.drawable.img_story_042,
            43 to R.drawable.img_story_043,
            44 to R.drawable.img_story_044,
            45 to R.drawable.img_story_045,
            46 to R.drawable.img_story_046,
            47 to R.drawable.img_story_047,
            48 to R.drawable.img_story_048,
            49 to R.drawable.img_story_049,
            50 to R.drawable.img_story_050
        )
    }
}
