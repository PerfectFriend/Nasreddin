package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Ornate literary display serif (full Cyrillic + Latin Extended support) used on
 * splash titles and illuminated parchment headings.
 */
val DisplayTitleFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Bold),
    Font(R.font.playfair_display, FontWeight.Black)
)

/**
 * Compact Eastern calligraphic display typeface (El Messiri on API 26+, static Philosopher Bold
 * fallback on API 24-25) tailored for wrapping full parable titles on story covers without clipping.
 */
@OptIn(ExperimentalTextApi::class)
val CoverTitleFontFamily: FontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            resId = R.font.el_messiri,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(650))
        ),
        Font(
            resId = R.font.el_messiri,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700))
        )
    )
} else {
    FontFamily(Font(R.font.philosopher, FontWeight.Bold))
}

/**
 * High-contrast calligraphic serif (Philosopher Bold, full Cyrillic + Latin Extended) used on
 * cover episode medallions and ornamental crown labels.
 */
val CoverEmblemFontFamily = FontFamily(
    Font(R.font.philosopher, FontWeight.Bold)
)

/**
 * Classical manuscript serif for illuminated drop caps, wisdom callouts, and episode subtitles.
 */
val ParchmentHeadingFontFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond, FontWeight.Bold)
)

/**
 * Warm bookish body serif optimized for sustained parchment reading in RU, ES, and EN.
 */
val ParchmentBodyFontFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.Medium),
    Font(R.font.lora, FontWeight.Bold)
)

val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = DisplayTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.2.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ParchmentHeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ParchmentBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
