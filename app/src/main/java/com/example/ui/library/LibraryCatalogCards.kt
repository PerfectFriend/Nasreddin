package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.ui.components.LocalizedComicCover
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.GaudiCoralRed
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Featured hero story card at the top of the library when no search/filter is active.
 * Displays the complete uncropped `3:4` Eastern cover with its dynamically localized cartouche
 * title, plus actions to read the comic, open the full parchment text, or manage page order.
 */
@Composable
internal fun FeaturedComicBanner(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    onRead: () -> Unit,
    onOpenParchment: () -> Unit,
    onManageSequence: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("featured_comic_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        border = BorderStroke(1.5.dp, VanGoghStarGold.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(252.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF071128),
                                Color(0xFF142B56),
                                Color(0xFF071128)
                            )
                        )
                    )
            ) {
                LocalizedComicCover(
                    title = comic.title,
                    subtitle = comic.subtitle,
                    drawableResId = comic.coverDrawableResId,
                    uriString = comic.coverUriString,
                    targetWidthPx = 480,
                    targetHeightPx = 640,
                    showCartoucheTitle = true,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(10.dp))
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, CyberNeonCyan),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${AppStrings.freeBadge(currentLanguage)} • ${AppStrings.officialEdition(currentLanguage, comic.pages.size)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = VanGoghStarGold
                )
                Text(
                    text = comic.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberNeonCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = comic.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRead,
                        modifier = Modifier
                            .weight(1.25f)
                            .heightIn(min = 48.dp)
                            .testTag("featured_read_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanGoghStarGold,
                            contentColor = Color(0xFF2E1700)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (comic.lastReadPageIndex > 0) {
                                AppStrings.continueReading(currentLanguage, comic.lastReadPageIndex + 1)
                            } else {
                                AppStrings.readButton(currentLanguage)
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenParchment,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("featured_parchment_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF3DFB2),
                            contentColor = Color(0xFF281406)
                        ),
                        border = BorderStroke(1.dp, VanGoghStarGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = AppStrings.parchmentOpenTooltip(currentLanguage),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.parchmentShortLabel(currentLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberMidnight,
                        border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f))
                    ) {
                        IconButton(
                            onClick = onManageSequence,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("featured_sequence_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = AppStrings.pageSequence(currentLanguage, comic.pages.size),
                                tint = CyberNeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual story card inside the 100-story library list.
 * Downsamples `3:4` ornate Eastern covers to `240x320` px via Coil while dynamically stamping
 * the story's localized title into the top cartouche, plus quick Comic, Parchment Text, and Sequence actions.
 */
@Composable
internal fun ComicListItemCard(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    onRead: () -> Unit,
    onOpenParchment: () -> Unit,
    onManageSequence: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnlockedViaAd = !comic.isFree && !isPremiumUnlocked && !seriesUnlocked && comic.storyNumber <= adUnlockedMaxStory
    val isLocked = !comic.isFree && !isPremiumUnlocked && !seriesUnlocked && comic.storyNumber > adUnlockedMaxStory

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onRead() }
            .testTag("comic_item_${comic.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isLocked) VanGoghStarGold.copy(alpha = 0.5f) else CyberSurfaceElevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp, 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A142D))
            ) {
                LocalizedComicCover(
                    title = comic.title,
                    drawableResId = comic.coverDrawableResId,
                    uriString = comic.coverUriString,
                    targetWidthPx = 240,
                    targetHeightPx = 320,
                    showCartoucheTitle = true,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.52f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = AppStrings.unlockCtaChip(currentLanguage),
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comic.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    when {
                        comic.isFree -> {
                            Text(
                                text = AppStrings.freeBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        isPremiumUnlocked || seriesUnlocked -> {
                            Text(
                                text = AppStrings.proUnlockedBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        isUnlockedViaAd -> {
                            Text(
                                text = AppStrings.adUnlockedBadge(currentLanguage),
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text(
                                text = AppStrings.premiumBadge(currentLanguage),
                                color = VanGoghStarGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = " • ${AppStrings.pagesCountSuffix(currentLanguage, comic.pages.size)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                if (comic.lastReadPageIndex > 0) {
                    Text(
                        text = AppStrings.stoppedAtPage(currentLanguage, comic.lastReadPageIndex + 1),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onRead,
                        modifier = Modifier.heightIn(min = 38.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isLocked) VanGoghStarGold else VanGoghStarGold.copy(alpha = 0.2f),
                            contentColor = if (isLocked) Color(0xFF281500) else VanGoghStarGold
                        )
                    ) {
                        if (isLocked) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (isLocked) AppStrings.unlockCtaChip(currentLanguage) else AppStrings.readButton(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenParchment,
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("comic_parchment_btn_${comic.id}"),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFF3DFB2),
                            contentColor = Color(0xFF281406)
                        ),
                        border = BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = AppStrings.parchmentOpenTooltip(currentLanguage),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.parchmentShortLabel(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onManageSequence,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = AppStrings.pageOrderButton(currentLanguage),
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!comic.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = AppStrings.deleteComic(currentLanguage),
                        tint = GaudiCoralRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
