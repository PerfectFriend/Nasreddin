package com.example.ui.reader

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.model.ComicPage
import com.example.model.PanelCrop
import kotlin.math.roundToInt

/**
 * Thread-safe bounded in-memory cache for single-panel bitmap crops.
 *
 * Uses [BitmapRegionDecoder] so only the requested quadrant of a 2x2 comic sheet is decoded,
 * avoiding 4x memory spikes and keeping APK size compact.
 * Max capacity of 32 entries (~8 stories * 4 panels) keeps heap usage bounded (~25-35 MB worst case).
 */
internal object ComicPanelCropCache {
    private val cache = object : LruCache<String, ImageBitmap>(32) {}

    fun getOrDecodePanel(
        resources: Resources,
        resId: Int,
        crop: PanelCrop
    ): ImageBitmap? {
        val key = "${resId}_${crop.left}_${crop.top}_${crop.right}_${crop.bottom}"
        cache.get(key)?.let { return it }

        return try {
            val decoded = decodeRegion(resources, resId, crop)
            if (decoded != null) {
                cache.put(key, decoded)
            }
            decoded
        } catch (e: Exception) {
            // Gracefully fall back to full drawable rendering if stream decoding fails (e.g. non-raster asset)
            Log.w("ComicPanelCropCache", "Failed to decode panel crop for resId=$resId", e)
            null
        }
    }

    private fun decodeRegion(
        resources: Resources,
        resId: Int,
        crop: PanelCrop
    ): ImageBitmap? {
        // First pass: read image dimensions without allocating pixel memory
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resources.openRawResource(resId).use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val width = boundsOpts.outWidth
        val height = boundsOpts.outHeight
        if (width <= 0 || height <= 0) return null

        val regionRect = computeRegionRect(width, height, crop)

        // Second pass: decode only the bounded comic panel rectangle via BitmapRegionDecoder
        val regionBitmap = resources.openRawResource(resId).use { stream ->
            val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(stream)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(stream, false)
            }
            try {
                decoder?.decodeRegion(regionRect, BitmapFactory.Options())
            } finally {
                decoder?.recycle()
            }
        }
        if (regionBitmap != null) {
            return regionBitmap.asImageBitmap()
        }

        // Secondary fallback when platform/stream decoder returns null: crop from full decoded bitmap
        val fullBitmap = resources.openRawResource(resId).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null
        val safeRect = computeRegionRect(fullBitmap.width, fullBitmap.height, crop)
        val cropped = android.graphics.Bitmap.createBitmap(
            fullBitmap,
            safeRect.left,
            safeRect.top,
            safeRect.width(),
            safeRect.height()
        )
        if (cropped !== fullBitmap) {
            fullBitmap.recycle()
        }
        return cropped.asImageBitmap()
    }

    internal fun computeRegionRect(width: Int, height: Int, crop: PanelCrop): Rect {
        val leftPx = (crop.left.coerceIn(0f, 1f) * width).roundToInt().coerceIn(0, width - 1)
        val topPx = (crop.top.coerceIn(0f, 1f) * height).roundToInt().coerceIn(0, height - 1)
        val rightPx = (crop.right.coerceIn(0f, 1f) * width).roundToInt().coerceIn(leftPx + 1, width)
        val bottomPx = (crop.bottom.coerceIn(0f, 1f) * height).roundToInt().coerceIn(topPx + 1, height)
        return Rect(leftPx, topPx, rightPx, bottomPx)
    }
}

/**
 * Unified comic artwork renderer used by the fullscreen reader, thumbnail filmstrip, and sequence sheet.
 * Automatically renders cropped single-panel regions when [ComicPage.panelCrop] is provided.
 */
@Composable
fun ComicPageArtwork(
    page: ComicPage,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val resId = page.drawableResId
    val crop = page.panelCrop

    if (resId != null && crop != null) {
        val context = LocalContext.current
        val croppedBitmap = remember(resId, crop) {
            ComicPanelCropCache.getOrDecodePanel(context.resources, resId, crop)
        }
        if (croppedBitmap != null) {
            Image(
                bitmap = croppedBitmap,
                contentDescription = page.title,
                modifier = modifier,
                contentScale = contentScale
            )
            return
        }
    }

    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = page.title,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (page.uriString != null) {
        AsyncImage(
            model = page.uriString,
            contentDescription = page.title,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
