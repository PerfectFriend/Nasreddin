package com.example.model

/**
 * Supported application UI and comic catalog languages.
 * The repository strictly filters built-in stories to the active language.
 */
enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    RUSSIAN("ru", "Русский", "RU"),
    SPANISH("es", "Español", "ES"),
    ENGLISH("en", "English", "EN")
}

/**
 * Normalized crop bounds in [0f..1f] relative to the source drawable dimensions.
 * Used to split 2x2 comic sheets into individual single-panel fullscreen pages without asset duplication.
 */
data class PanelCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * Represents a single readable page (or cropped single panel) within a [ComicBook].
 */
data class ComicPage(
    val id: String,
    val title: String,
    val drawableResId: Int? = null,
    val uriString: String? = null,
    val narrativeText: String? = null,
    val dialogueText: String? = null,
    val artStyleNotes: String? = null,
    val sequenceNumber: Int = 0,
    val language: AppLanguage? = null,
    val panelCrop: PanelCrop? = null
)

/**
 * Represents a complete comic episode or user-imported folder/image collection.
 * Built-in stories enforce at least 4 pages per story.
 */
data class ComicBook(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val coverDrawableResId: Int? = null,
    val coverUriString: String? = null,
    val pages: List<ComicPage> = emptyList(),
    val isBuiltIn: Boolean = false,
    val sourceFolderUri: String? = null,
    val lastReadPageIndex: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val isFree: Boolean = true,
    val storyNumber: Int = 1
)

enum class ReadingMode {
    HORIZONTAL_PAGER,
    VERTICAL_SCROLL
}

enum class ReaderBackground {
    CYBER_DARK,
    PURE_BLACK,
    SEPIA_PARCHMENT
}
