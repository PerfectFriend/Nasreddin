package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.reader.computeFittedArtworkRect
import com.example.ui.theme.CoverEmblemFontFamily
import com.example.ui.theme.CoverTitleFontFamily
import kotlin.math.roundToInt

internal data class CoverTitleParts(
    val episodeBadge: String?,
    val headline: String
)

private val EPISODE_PREFIX_REGEX = Regex("^(\\d+)\\.\\s+(.+)$")

/**
 * Separates an optional numeric prefix (e.g. `"76. Speech Feeds Ears..."`) into an illuminated
 * crown medallion label (`"№ 76"`) and the pure parable name so 100% of the cartouche ribbon
 * width stays dedicated to wrapping the full parable title cleanly.
 */
internal fun splitCoverTitleAndBadge(
    rawTitle: String,
    fallbackSubtitle: String? = null
): CoverTitleParts {
    val trimmed = rawTitle.trim()
    val match = EPISODE_PREFIX_REGEX.matchEntire(trimmed)
    return if (match != null) {
        val number = match.groupValues[1]
        val body = match.groupValues[2].trim()
        CoverTitleParts(episodeBadge = "№ $number", headline = body)
    } else {
        val cleanSub = fallbackSubtitle?.trim()?.takeIf { it.isNotBlank() }
        CoverTitleParts(episodeBadge = cleanSub, headline = trimmed)
    }
}

/**
 * Pure helper computing initial responsive Eastern calligraphic font size ([CoverTitleFontFamily])
 * for the widened cover parchment scroll (`78%` cover width, multi-line wrapping).
 */
internal fun computeCoverTitleFontSp(
    titleLength: Int,
    availableWidthDp: Float
): Float {
    val base = when {
        availableWidthDp >= 280f -> 21.0f
        availableWidthDp >= 165f -> 14.5f
        availableWidthDp >= 110f -> 11.0f
        else -> 8.2f
    }
    val lengthScale = when {
        titleLength > 48 -> 0.72f
        titleLength > 34 -> 0.80f
        titleLength > 24 -> 0.90f
        else -> 1.0f
    }
    val minSp = when {
        availableWidthDp >= 280f -> 11.5f
        availableWidthDp >= 165f -> 9.0f
        else -> 6.5f
    }
    return (base * lengthScale).coerceAtLeast(minSp)
}

/**
 * Dynamically renders the localized story title inside a widened illuminated parchment scroll
 * (`78%` artwork width, centered at `y = 0.19f`, `heightIn` flexible up to `22.5%` cover height)
 * topped by a lapis-and-gold episode medallion (`№ X`) using [CoverTitleFontFamily] (El Messiri)
 * paired with [CoverEmblemFontFamily] (Philosopher Bold).
 *
 * Uses multi-line soft wrapping (`maxLines = 4`) plus an automatic `onTextLayout` scale guard so
 * even the longest localized parable title fits completely without `...` truncation.
 */
@Composable
fun CoverCartoucheTitleOverlay(
    title: String,
    subtitle: String? = null,
    imageAspectRatio: Float = 0.75f,
    isCropScale: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (title.isBlank()) return
    val density = LocalDensity.current
    val parts = remember(title, subtitle) { splitCoverTitleAndBadge(title, subtitle) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val fitted = computeFittedArtworkRect(
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            imageAspectRatio = imageAspectRatio,
            isCropScale = isCropScale
        )
        val fittedWidthDp = with(density) { fitted.widthPx.toDp() }
        val fittedHeightDp = with(density) { fitted.heightPx.toDp() }

        val displayHeadline = if (fittedWidthDp.value >= 100f) parts.headline else title
        val initialFontSp = computeCoverTitleFontSp(displayHeadline.length, fittedWidthDp.value)
        val minAutoSp = when {
            fittedWidthDp.value >= 280f -> 10.0f
            fittedWidthDp.value >= 165f -> 8.0f
            else -> 4.8f
        }
        var adjustedFontSp by remember(displayHeadline, initialFontSp) {
            mutableFloatStateOf(initialFontSp)
        }

        val bannerWidthDp = (fittedWidthDp * 0.78f).coerceAtLeast(20.dp)
        val minBannerHeightDp = (fittedHeightDp * 0.118f).coerceAtLeast(10.dp)
        val maxBannerHeightDp = (fittedHeightDp * 0.228f).coerceAtLeast(16.dp)
        val lineHeightSp = adjustedFontSp * 1.12f
        val showCrownBadge = fittedWidthDp.value >= 115f && !parts.episodeBadge.isNullOrBlank()
        val isCompactThumb = fittedWidthDp.value < 105f

        Box(
            modifier = Modifier
                .offset { IntOffset(fitted.leftPx.roundToInt(), fitted.topPx.roundToInt()) }
                .size(fittedWidthDp, fittedHeightDp)
        ) {
            // 1. Wide illuminated parchment scroll banner spanning 78% width across the upper frieze
            Box(
                modifier = Modifier
                    .align(Alignment { size, space, _ ->
                        val cx = (space.width * 0.50f - size.width / 2f).roundToInt()
                        val cy = (space.height * 0.188f - size.height / 2f).roundToInt()
                        IntOffset(cx, cy)
                    })
                    .width(bannerWidthDp)
                    .heightIn(min = minBannerHeightDp, max = maxBannerHeightDp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(if (isCompactThumb) 4.dp else 11.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF5FCF2DA),
                                Color(0xF5F6E6BE),
                                Color(0xF5EDD59F)
                            )
                        )
                    )
                    .border(
                        width = if (isCompactThumb) 0.7.dp else 1.4.dp,
                        color = Color(0xFFC28923),
                        shape = RoundedCornerShape(if (isCompactThumb) 4.dp else 11.dp)
                    )
                    .padding(
                        start = if (isCompactThumb) 3.dp else 10.dp,
                        end = if (isCompactThumb) 3.dp else 10.dp,
                        top = when {
                            showCrownBadge && fittedWidthDp.value >= 250f -> 11.dp
                            showCrownBadge -> 8.dp
                            isCompactThumb -> 2.dp
                            else -> 7.dp
                        },
                        bottom = if (isCompactThumb) 2.dp else 7.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayHeadline,
                    fontFamily = CoverTitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = adjustedFontSp.sp,
                    lineHeight = lineHeightSp.sp,
                    color = Color(0xFF211004),
                    softWrap = true,
                    maxLines = 4,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    onTextLayout = { layoutResult ->
                        val hasOverflow = layoutResult.didOverflowHeight ||
                            layoutResult.didOverflowWidth ||
                            layoutResult.hasVisualOverflow
                        if (hasOverflow && adjustedFontSp > minAutoSp) {
                            adjustedFontSp = (adjustedFontSp * 0.88f).coerceAtLeast(minAutoSp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Lapis-and-Gold crown medallion resting gracefully above the parchment scroll
            if (showCrownBadge) {
                Surface(
                    color = Color(0xFF0C1C3E),
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, Color(0xFFE7BD56)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment { size, space, _ ->
                        val cx = (space.width * 0.50f - size.width / 2f).roundToInt()
                        val cy = (space.height * 0.082f - size.height / 2f).roundToInt()
                        IntOffset(cx, cy)
                    })
                ) {
                    Text(
                        text = parts.episodeBadge.orEmpty(),
                        fontFamily = CoverEmblemFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (fittedWidthDp.value >= 250f) 11.5.sp else 9.sp,
                        letterSpacing = 0.9.sp,
                        color = Color(0xFFFAE18E),
                        maxLines = 1,
                        modifier = Modifier.padding(
                            horizontal = if (fittedWidthDp.value >= 250f) 12.dp else 7.dp,
                            vertical = if (fittedWidthDp.value >= 250f) 2.5.dp else 1.dp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Renders an ornate Eastern book cover (resource or custom URI) downsampled via Coil when dimensions
 * are given, and overlays the localized title dynamically into the cover's top cartouche.
 */
@Composable
fun LocalizedComicCover(
    title: String,
    subtitle: String? = null,
    drawableResId: Int? = null,
    uriString: String? = null,
    targetWidthPx: Int? = null,
    targetHeightPx: Int? = null,
    showCartoucheTitle: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data: Any? = drawableResId ?: uriString

    Box(modifier = modifier) {
        if (data != null) {
            if (targetWidthPx != null && targetHeightPx != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(data)
                        .size(targetWidthPx, targetHeightPx)
                        .crossfade(false)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            } else if (drawableResId != null) {
                Image(
                    painter = painterResource(id = drawableResId),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            } else {
                AsyncImage(
                    model = uriString,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }

        if (showCartoucheTitle && drawableResId != null) {
            CoverCartoucheTitleOverlay(
                title = title,
                subtitle = subtitle,
                imageAspectRatio = 0.75f,
                isCropScale = contentScale == ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Subtle vector Eastern filigree trim overlay drawn along the perimeter of fitted comic plates
 * so full-bleed scenes and panel crops share a unified gilded miniature presentation.
 */
@Composable
fun EasternPanelTrimOverlay(
    imageAspectRatio: Float,
    isCropScale: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val fitted = computeFittedArtworkRect(
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            imageAspectRatio = imageAspectRatio,
            isCropScale = isCropScale
        )
        if (fitted.widthPx > 40f && fitted.heightPx > 40f) {
            val wDp = with(density) { fitted.widthPx.toDp() }
            val hDp = with(density) { fitted.heightPx.toDp() }
            Canvas(
                modifier = Modifier
                    .offset { IntOffset(fitted.leftPx.roundToInt(), fitted.topPx.roundToInt()) }
                    .size(wDp, hDp)
            ) {
                val outerInset = 1.5.dp.toPx()
                val innerInset = 5.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFD7A843),
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(size.width - outerInset * 2f, size.height - outerInset * 2f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xCC1CA3A3),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(size.width - innerInset * 2f, size.height - innerInset * 2f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(width = 0.9.dp.toPx())
                )

                // Golden corner diamond medallions
                val diamondOffset = 6.dp.toPx()
                val r = 3.5.dp.toPx()
                val corners = listOf(
                    Offset(diamondOffset, diamondOffset),
                    Offset(size.width - diamondOffset, diamondOffset),
                    Offset(diamondOffset, size.height - diamondOffset),
                    Offset(size.width - diamondOffset, size.height - diamondOffset)
                )
                for (c in corners) {
                    val path = Path().apply {
                        moveTo(c.x, c.y - r)
                        lineTo(c.x + r, c.y)
                        lineTo(c.x, c.y + r)
                        lineTo(c.x - r, c.y)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFEBC563), style = Fill)
                    drawPath(path = path, color = Color(0xFF0B1938), style = Stroke(width = 0.9.dp.toPx()))
                }
            }
        }
    }
}
