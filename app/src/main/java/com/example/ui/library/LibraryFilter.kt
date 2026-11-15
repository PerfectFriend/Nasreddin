package com.example.ui.library

import com.example.model.ComicBook

/**
 * Filter categories available in the library catalog header.
 */
enum class LibraryFilter {
    ALL,
    FREE,
    UNLOCKED,
    BAZAAR,
    TEAHOUSE,
    NIGHT
}

/**
 * Filters library [comics] by [selectedFilter] and free-text [query] (matching title,
 * parable summary, or story number with optional `#` prefix).
 */
internal fun filterLibraryComics(
    comics: List<ComicBook>,
    query: String,
    selectedFilter: LibraryFilter,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int
): List<ComicBook> {
    val trimmedQuery = query.trim().lowercase()
    val numericQuery = trimmedQuery.removePrefix("#").toIntOrNull()

    return comics.filter { comic ->
        val isUnlocked = comic.isFree ||
            isPremiumUnlocked ||
            seriesUnlocked ||
            comic.storyNumber <= adUnlockedMaxStory

        val matchesFilter = when (selectedFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.FREE -> comic.isFree
            LibraryFilter.UNLOCKED -> isUnlocked
            LibraryFilter.BAZAAR -> comic.isBuiltIn && comic.themeTag == "bazaar"
            LibraryFilter.TEAHOUSE -> comic.isBuiltIn && comic.themeTag == "teahouse"
            LibraryFilter.NIGHT -> comic.isBuiltIn && comic.themeTag == "night"
        }
        if (!matchesFilter) return@filter false

        if (trimmedQuery.isEmpty()) return@filter true

        val matchesNumeric = numericQuery != null && comic.isBuiltIn && comic.storyNumber == numericQuery
        matchesNumeric ||
            comic.title.lowercase().contains(trimmedQuery) ||
            comic.description.lowercase().contains(trimmedQuery)
    }
}
