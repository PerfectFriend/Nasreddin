package com.example.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.example.model.ComicPage

/**
 * Interactive fullscreen comic page container supporting pinch-to-zoom (1x..5x),
 * bounded panning, double-tap zoom around the tap focal point, and single-tap HUD toggle.
 */
@Composable
fun ZoomableComicImage(
    page: ComicPage,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    // Keyed by page.id so switching pages or stories always starts at unzoomed 1f scale
    var scale by remember(page.id) { mutableFloatStateOf(1f) }
    var offset by remember(page.id) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(page.id) {
                detectTapGestures(
                    onTap = {
                        if (scale > 1f) {
                            // Reset zoom if tapped while zoomed
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            onTap()
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            // Center zoom on double tap point
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            offset = Offset(
                                (centerX - tapOffset.x) * 1.5f,
                                (centerY - tapOffset.y) * 1.5f
                            )
                        }
                    }
                )
            }
            .pointerInput(page.id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale

                    if (scale > 1f) {
                        val maxPanX = (size.width * (scale - 1)) / 2f
                        val maxPanY = (size.height * (scale - 1)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                            y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val graphicsModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }

        ComicPageArtwork(
            page = page,
            modifier = graphicsModifier,
            contentScale = ContentScale.Fit
        )
    }
}
