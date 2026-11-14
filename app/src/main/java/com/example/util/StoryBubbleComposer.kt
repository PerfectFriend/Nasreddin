package com.example.util

import com.example.model.AppLanguage
import com.example.model.BubbleStyle
import com.example.model.BubbleTailDirection
import com.example.model.SpeechBubble

/**
 * Splits a localized parable into 4 balanced comic-strip beats and maps each beat onto
 * dynamic [SpeechBubble] overlays positioned over the open sky/floor zones of the universal
 * 4-panel template artwork (keeping characters centered and unobstructed).
 *
 * Designed so one set of text-free visual assets serves all languages (RU / ES / EN and
 * future top-10 languages) without duplicating large raster drawables in the APK.
 */
object StoryBubbleComposer {

    /**
     * Produces a 4-element list (`index 0..3`) matching the 4 cropped panels of an episode.
     * Each page receives concise, localized speech and/or narration bubbles.
     */
    fun composeFourPanelBubbles(
        storyId: String,
        storyText: String,
        moral: String,
        language: AppLanguage
    ): List<List<SpeechBubble>> {
        val sentences = splitIntoSentences(storyText)
        val beats = distributeIntoFourBeats(sentences, storyText)

        return beats.mapIndexed { panelIndex, beatText ->
            buildPanelBubbles(
                storyId = storyId,
                panelIndex = panelIndex,
                beatText = beatText,
                moral = moral.takeIf { it.isNotBlank() && panelIndex == 3 },
                language = language
            )
        }
    }

    private fun buildPanelBubbles(
        storyId: String,
        panelIndex: Int,
        beatText: String,
        moral: String?,
        language: AppLanguage
    ): List<SpeechBubble> {
        val trimmed = beatText.trim()
        if (trimmed.isEmpty() && moral.isNullOrBlank()) return emptyList()

        val bubbles = mutableListOf<SpeechBubble>()
        val extractedQuote = extractFirstQuotedSegment(trimmed)

        if (extractedQuote != null) {
            val contextText = extractedQuote.surroundingContext
            if (contextText.isNotEmpty()) {
                bubbles.add(
                    SpeechBubble(
                        id = "${storyId}_p${panelIndex + 1}_narr",
                        text = contextText,
                        centerX = 0.50f,
                        centerY = 0.88f,
                        widthFraction = 0.88f,
                        style = BubbleStyle.NARRATION,
                        tailDirection = BubbleTailDirection.NONE
                    )
                )
            }
            val (centerX, tail) = when (panelIndex) {
                0 -> 0.42f to BubbleTailDirection.BOTTOM_LEFT
                1 -> 0.56f to BubbleTailDirection.BOTTOM_RIGHT
                2 -> 0.44f to BubbleTailDirection.BOTTOM_LEFT
                else -> 0.48f to BubbleTailDirection.BOTTOM_RIGHT
            }
            bubbles.add(
                SpeechBubble(
                    id = "${storyId}_p${panelIndex + 1}_speech",
                    text = extractedQuote.quote,
                    centerX = centerX,
                    centerY = 0.16f,
                    widthFraction = 0.80f,
                    speaker = localizedHodjaLabel(language),
                    style = if (panelIndex == 2 && !trimmed.contains("—")) BubbleStyle.THOUGHT else BubbleStyle.SPEECH,
                    tailDirection = tail
                )
            )
        } else if (trimmed.isNotEmpty()) {
            // Pure narrative beat: place setup caption at bottom, or final punchline above Hodja in sky
            val isPunchlinePanel = panelIndex == 3
            bubbles.add(
                SpeechBubble(
                    id = "${storyId}_p${panelIndex + 1}_narr",
                    text = trimmed,
                    centerX = 0.50f,
                    centerY = if (isPunchlinePanel) 0.16f else 0.87f,
                    widthFraction = 0.88f,
                    style = if (isPunchlinePanel) BubbleStyle.SPEECH else BubbleStyle.NARRATION,
                    tailDirection = if (isPunchlinePanel) BubbleTailDirection.BOTTOM_RIGHT else BubbleTailDirection.NONE
                )
            )
        }

        if (!moral.isNullOrBlank()) {
            bubbles.add(
                SpeechBubble(
                    id = "${storyId}_p4_moral",
                    text = moral,
                    centerX = 0.50f,
                    centerY = 0.89f,
                    widthFraction = 0.90f,
                    style = BubbleStyle.NARRATION,
                    tailDirection = BubbleTailDirection.NONE
                )
            )
        }

        return bubbles
    }

    internal fun splitIntoSentences(text: String): List<String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < cleaned.length) {
            val ch = cleaned[i]
            if (ch == '«' || ch == '»' || ch == '"' || ch == '\'') {
                // Treat apostrophes inside English contractions (wasn't / Hodja's) as non-quote delimiters
                val isContractionApostrophe = ch == '\'' &&
                    i > 0 && cleaned[i - 1].isLetter() &&
                    i < cleaned.lastIndex && cleaned[i + 1].isLetter()
                if (!isContractionApostrophe) {
                    inQuotes = when (ch) {
                        '«' -> true
                        '»' -> false
                        else -> !inQuotes
                    }
                }
            }
            current.append(ch)

            val isSentenceEnd = (ch == '.' || ch == '!' || ch == '?') &&
                !inQuotes &&
                (i == cleaned.lastIndex || cleaned[i + 1].isWhitespace() || cleaned[i + 1] == '»' || cleaned[i + 1] == '\'' || cleaned[i + 1] == '"')

            if (isSentenceEnd) {
                val sentence = current.toString().trim()
                if (sentence.isNotEmpty()) {
                    result.add(sentence)
                }
                current.clear()
            }
            i++
        }

        val trailing = current.toString().trim()
        if (trailing.isNotEmpty()) {
            result.add(trailing)
        }
        return result
    }

    private fun distributeIntoFourBeats(sentences: List<String>, rawText: String): List<String> {
        if (sentences.isEmpty()) return listOf(rawText, rawText, rawText, rawText)
        if (sentences.size <= 4) {
            return List(4) { idx ->
                sentences.getOrNull(idx) ?: sentences.last()
            }
        }
        // Pack sentences evenly across 4 panels without losing ending punchlines
        val chunks = MutableList(4) { mutableListOf<String>() }
        sentences.forEachIndexed { idx, sentence ->
            val bucket = (idx * 4) / sentences.size
            chunks[bucket.coerceIn(0, 3)].add(sentence)
        }
        return chunks.mapIndexed { idx, list ->
            if (list.isNotEmpty()) list.joinToString(" ") else sentences[idx.coerceAtMost(sentences.lastIndex)]
        }
    }

    private data class QuotedExtraction(val quote: String, val surroundingContext: String)

    private fun extractFirstQuotedSegment(text: String): QuotedExtraction? {
        val guillemetStart = text.indexOf('«')
        val guillemetEnd = if (guillemetStart >= 0) text.indexOf('»', guillemetStart + 1) else -1
        if (guillemetStart >= 0 && guillemetEnd > guillemetStart) {
            val quote = text.substring(guillemetStart + 1, guillemetEnd).trim()
            val before = text.substring(0, guillemetStart).trim().trimEnd(':', '—', '-')
            val after = text.substring(guillemetEnd + 1).trim().trimStart(',', '—', '-', ' ')
            val context = listOf(before, after).filter { it.isNotEmpty() }.joinToString(" ")
            if (quote.isNotEmpty()) return QuotedExtraction(quote, context)
        }

        // Single-quoted speech pattern used in English anecdotes
        val quoteRegex = Regex("(?<!\\p{L})'([^']{4,})'(?!\\p{L})")
        val match = quoteRegex.find(text)
        if (match != null) {
            val quote = match.groupValues[1].trim()
            val before = text.substring(0, match.range.first).trim().trimEnd(':')
            val after = text.substring(match.range.last + 1).trim().trimStart(',', ' ')
            val context = listOf(before, after).filter { it.isNotEmpty() }.joinToString(" ")
            if (quote.isNotEmpty()) return QuotedExtraction(quote, context)
        }
        return null
    }

    private fun localizedHodjaLabel(language: AppLanguage): String? = null
}
