package com.example.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BubbleStyle
import com.example.model.BubbleTailDirection
import com.example.model.SpeechBubble
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Exact rendered sub-rectangle (in pixels) of a comic image inside its viewport container.
 */
internal data class FittedArtworkRect(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float
)

/**
 * Computes the true visible artwork bounds inside `[containerWidthPx] x [containerHeightPx]`
 * so normalized [SpeechBubble] coordinates stay anchored onto the illustration itself rather
 * than floating inside black letterbox bars.
 */
internal fun computeFittedArtworkRect(
    containerWidthPx: Float,
    containerHeightPx: Float,
    imageAspectRatio: Float,
    isCropScale: Boolean
): FittedArtworkRect {
    if (containerWidthPx <= 0f || containerHeightPx <= 0f) {
        return FittedArtworkRect(0f, 0f, 0f, 0f)
    }
    if (isCropScale || imageAspectRatio <= 0f) {
        return FittedArtworkRect(0f, 0f, containerWidthPx, containerHeightPx)
    }
    val containerAspect = containerWidthPx / containerHeightPx
    return if (imageAspectRatio > containerAspect) {
        val fittedHeight = containerWidthPx / imageAspectRatio
        FittedArtworkRect(
            leftPx = 0f,
            topPx = (containerHeightPx - fittedHeight) / 2f,
            widthPx = containerWidthPx,
            heightPx = fittedHeight
        )
    } else {
        val fittedWidth = containerHeightPx * imageAspectRatio
        FittedArtworkRect(
            leftPx = (containerWidthPx - fittedWidth) / 2f,
            topPx = 0f,
            widthPx = fittedWidth,
            heightPx = containerHeightPx
        )
    }
}

/**
 * Computed visual typography parameters for a single comic balloon so verbose
 * RU/ES sentences remain legible without clipping on narrow smartphone screens.
 */
internal data class BubbleTypographySpec(
    val fontSizeSp: Float,
    val lineHeightSp: Float,
    val maxLines: Int,
    val horizontalPaddingDp: Float
)

/**
 * Computes responsive font size, line height, and line budget based on character length,
 * available balloon width, and bubble style (dialogue balloon vs narration plaque).
 */
internal fun computeAdaptiveBubbleTypography(
    textLength: Int,
    maxBubbleWidthDp: Float,
    style: BubbleStyle
): BubbleTypographySpec {
    val isCaption = style == BubbleStyle.NARRATION
    val baseFont = if (isCaption) 11.5f else 12.5f
    val baseLine = if (isCaption) 15.0f else 16.0f
    val narrowFactor = if (maxBubbleWidthDp < 210f) 0.92f else 1.0f

    val lengthScale = when {
        textLength > 150 -> 0.84f
        textLength > 105 -> 0.90f
        textLength > 70 -> 0.95f
        else -> 1.0f
    }

    val combinedScale = (narrowFactor * lengthScale).coerceAtLeast(0.80f)
    val fontSizeSp = (baseFont * combinedScale).coerceAtLeast(9.5f)
    val lineHeightSp = (baseLine * combinedScale).coerceAtLeast(12.2f)
    val maxLines = when {
        textLength > 140 -> 7
        textLength > 85 -> 6
        else -> 5
    }
    val horizontalPaddingDp = if (maxBubbleWidthDp < 200f) 9f else 12f

    return BubbleTypographySpec(
        fontSizeSp = fontSizeSp,
        lineHeightSp = lineHeightSp,
        maxLines = maxLines,
        horizontalPaddingDp = horizontalPaddingDp
    )
}

/**
 * Dynamically renders comic speech balloons, thought clouds, and narration captions
 * anchored inside the fitted comic artwork rectangle. Automatically scales with
 * [ZoomableComicImage] pinch-to-zoom and panning gestures.
 */
@Composable
fun ComicBubbleOverlay(
    bubbles: List<SpeechBubble>,
    imageAspectRatio: Float,
    isCropScale: Boolean,
    modifier: Modifier = Modifier
) {
    if (bubbles.isEmpty()) return

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

        Box(
            modifier = Modifier
                .offset { IntOffset(fitted.leftPx.roundToInt(), fitted.topPx.roundToInt()) }
                .size(fittedWidthDp, fittedHeightDp)
        ) {
            bubbles.forEach { bubble ->
                val maxBubbleWidthDp = (fittedWidthDp * bubble.widthFraction.coerceIn(0.32f, 0.94f))
                    .coerceAtLeast(120.dp)
                val cappedWidthDp = min(maxBubbleWidthDp.value, 360f)

                val alignmentBiasX = bubble.centerX.coerceIn(0.08f, 0.92f)
                val alignmentBiasY = bubble.centerY.coerceIn(0.08f, 0.92f)

                val align = Alignment { size, space, _ ->
                    val targetX = ((space.width * alignmentBiasX) - size.width / 2f)
                        .roundToInt()
                        .coerceIn(6, (space.width - size.width - 6).coerceAtLeast(6))
                    val targetY = ((space.height * alignmentBiasY) - size.height / 2f)
                        .roundToInt()
                        .coerceIn(6, (space.height - size.height - 6).coerceAtLeast(6))
                    IntOffset(targetX, targetY)
                }

                Box(
                    modifier = Modifier
                        .align(align)
                        .widthIn(max = cappedWidthDp.dp)
                ) {
                    ComicBalloonView(
                        bubble = bubble,
                        maxBubbleWidthDp = cappedWidthDp
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicBalloonView(
    bubble: SpeechBubble,
    maxBubbleWidthDp: Float
) {
    val fillColor = when (bubble.style) {
        BubbleStyle.SPEECH -> Color(0xFFFFF8E7)
        BubbleStyle.NARRATION -> Color(0xE60D132B)
        BubbleStyle.THOUGHT -> Color(0xFFE6FCFF)
    }
    val strokeColor = when (bubble.style) {
        BubbleStyle.SPEECH -> Color(0xFF19142B)
        BubbleStyle.NARRATION -> Color(0xFFF5C451)
        BubbleStyle.THOUGHT -> Color(0xFF00BCD4)
    }
    val textColor = when (bubble.style) {
        BubbleStyle.SPEECH -> Color(0xFF1A1426)
        BubbleStyle.NARRATION -> Color(0xFFFFF7E2)
        BubbleStyle.THOUGHT -> Color(0xFF062933)
    }

    val typographySpec = computeAdaptiveBubbleTypography(
        textLength = bubble.text.length,
        maxBubbleWidthDp = maxBubbleWidthDp,
        style = bubble.style
    )

    val isCaption = bubble.style == BubbleStyle.NARRATION
    val cornerDp = if (isCaption) 10.dp else 16.dp
    val tailReserveBottom = if (
        bubble.tailDirection == BubbleTailDirection.BOTTOM_LEFT ||
        bubble.tailDirection == BubbleTailDirection.BOTTOM_RIGHT
    ) 10.dp else 0.dp
    val tailReserveTop = if (
        bubble.tailDirection == BubbleTailDirection.TOP_LEFT ||
        bubble.tailDirection == BubbleTailDirection.TOP_RIGHT
    ) 10.dp else 0.dp

    Box {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = if (isCaption) 1.5.dp.toPx() else 2.dp.toPx()
            val radiusPx = cornerDp.toPx()
            val bottomTrim = tailReserveBottom.toPx()
            val topTrim = tailReserveTop.toPx()
            val boxHeight = (size.height - bottomTrim - topTrim).coerceAtLeast(16f)

            drawRoundRect(
                color = fillColor,
                topLeft = Offset(0f, topTrim),
                size = Size(size.width, boxHeight),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Fill
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(0f, topTrim),
                size = Size(size.width, boxHeight),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )

            // Draw crisp comic speech tail triangle when pointing toward a character
            if (bubble.tailDirection != BubbleTailDirection.NONE && size.width > 48f) {
                val tailPath = Path()
                val tipOffset = 10.dp.toPx()
                when (bubble.tailDirection) {
                    BubbleTailDirection.BOTTOM_LEFT -> {
                        val anchorX = size.width * 0.28f
                        val baseY = topTrim + boxHeight - strokePx / 2f
                        tailPath.moveTo(anchorX, baseY)
                        tailPath.lineTo(anchorX - 10.dp.toPx(), baseY + tipOffset)
                        tailPath.lineTo(anchorX + 12.dp.toPx(), baseY)
                    }
                    BubbleTailDirection.BOTTOM_RIGHT -> {
                        val anchorX = size.width * 0.72f
                        val baseY = topTrim + boxHeight - strokePx / 2f
                        tailPath.moveTo(anchorX - 12.dp.toPx(), baseY)
                        tailPath.lineTo(anchorX + 10.dp.toPx(), baseY + tipOffset)
                        tailPath.lineTo(anchorX, baseY)
                    }
                    BubbleTailDirection.TOP_LEFT -> {
                        val anchorX = size.width * 0.28f
                        val baseY = topTrim + strokePx / 2f
                        tailPath.moveTo(anchorX, baseY)
                        tailPath.lineTo(anchorX - 10.dp.toPx(), 0f)
                        tailPath.lineTo(anchorX + 12.dp.toPx(), baseY)
                    }
                    BubbleTailDirection.TOP_RIGHT -> {
                        val anchorX = size.width * 0.72f
                        val baseY = topTrim + strokePx / 2f
                        tailPath.moveTo(anchorX - 12.dp.toPx(), baseY)
                        tailPath.lineTo(anchorX + 10.dp.toPx(), 0f)
                        tailPath.lineTo(anchorX, baseY)
                    }
                    BubbleTailDirection.NONE -> Unit
                }
                drawPath(path = tailPath, color = fillColor, style = Fill)
                drawPath(path = tailPath, color = strokeColor, style = Stroke(width = strokePx))
            }
        }

        Column(
            modifier = Modifier
                .padding(
                    start = typographySpec.horizontalPaddingDp.dp,
                    end = typographySpec.horizontalPaddingDp.dp,
                    top = 7.dp + tailReserveTop,
                    bottom = 7.dp + tailReserveBottom
                )
        ) {
            if (!bubble.speaker.isNullOrBlank()) {
                Text(
                    text = bubble.speaker,
                    color = Color(0xFF00798C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Text(
                text = bubble.text,
                color = textColor,
                fontSize = typographySpec.fontSizeSp.sp,
                lineHeight = typographySpec.lineHeightSp.sp,
                fontWeight = if (isCaption) FontWeight.Medium else FontWeight.Bold,
                fontStyle = if (bubble.style == BubbleStyle.THOUGHT) FontStyle.Italic else FontStyle.Normal,
                textAlign = if (isCaption) TextAlign.Start else TextAlign.Center,
                maxLines = typographySpec.maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
