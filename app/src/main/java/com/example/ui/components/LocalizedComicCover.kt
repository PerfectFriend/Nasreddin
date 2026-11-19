package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.ui.theme.DisplayTitleFontFamily
import com.example.ui.theme.ParchmentHeadingFontFamily
import kotlin.math.roundToInt

/**
 * Pure helper computing responsive serif title font size inside the ornate blank top cartouche
 * of generated Eastern covers (`x = 0.22..0.78`, `y = 0.112..0.240`).
 */
internal fun computeCoverTitleFontSp(
    titleLength: Int,
    availableWidthDp: Float
): Float {
    val base = when {
        availableWidthDp >= 280f -> 20.5f
        availableWidthDp >= 180f -> 15.0f
        availableWidthDp >= 115f -> 10.5f
        else -> 7.8f
    }
    val lengthScale = when {
        titleLength > 48 -> 0.72f
        titleLength > 34 -> 0.80f
        titleLength > 24 -> 0.90f
        else -> 1.0f
    }
    val minSp = if (availableWidthDp >= 180f) 11.5f else 6.5f
    return (base * lengthScale).coerceAtLeast(minSp)
}

/**
 * Dynamically renders the localized story title inside the dedicated top cream-parchment
 * cartouche of every generated Eastern cover (`3:4` aspect ratio, cartouche centered around
 * `x = 0.50f`, `y = 0.175f`, spanning `56%` width and `12.6%` height).
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

        // Blank cartouche interior lies reliably within x = 0.22..0.78, y = 0.114..0.236
        val cartoucheWidthDp = (fittedWidthDp * 0.54f).coerceAtLeast(16.dp)
        val cartoucheHeightDp = (fittedHeightDp * 0.122f).coerceAtLeast(10.dp)
        val fontSizeSp = computeCoverTitleFontSp(title.length, fittedWidthDp.value)
        val lineHeightSp = (fontSizeSp * 1.15f)
        val showSub = !subtitle.isNullOrBlank() && fittedWidthDp.value >= 150f

        Box(
            modifier = Modifier
                .offset { IntOffset(fitted.leftPx.roundToInt(), fitted.topPx.roundToInt()) }
                .size(fittedWidthDp, fittedHeightDp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment { size, space, _ ->
                        val cx = (space.width * 0.50f - size.width / 2f).roundToInt()
                        val cy = (space.height * 0.175f - size.height / 2f).roundToInt()
                        IntOffset(cx, cy)
                    })
                    .size(cartoucheWidthDp, cartoucheHeightDp)
                    .padding(horizontal = if (fittedWidthDp.value >= 140f) 6.dp else 2.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showSub && subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = ParchmentHeadingFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSizeSp * 0.48f).coerceIn(8f, 11.5f).sp,
                        color = Color(0xFF7A4812),
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = title,
                    fontFamily = DisplayTitleFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSp.sp,
                    lineHeight = lineHeightSp.sp,
                    color = Color(0xFF281406),
                    maxLines = if (showSub) 2 else 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
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
