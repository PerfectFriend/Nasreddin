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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.GaudiCoralRed
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Featured hero story card at the top of the library when no search/filter is active.
 * Downsamples cover artwork to `900x660` px via Coil [ImageRequest].
 */
@Composable
internal fun FeaturedComicBanner(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    onRead: () -> Unit,
    onManageSequence: () -> Unit
) {
    val context = LocalContext.current
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
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                val coverData = comic.coverDrawableResId ?: comic.coverUriString
                if (coverData != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverData)
                            .size(900, 660)
                            .crossfade(false)
                            .build(),
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberSurfaceElevated.copy(alpha = 0.8f),
                                    CyberSurfaceElevated
                                )
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRead,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                            .testTag("featured_read_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VanGoghStarGold,
                            contentColor = Color(0xFF2E1700)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (comic.lastReadPageIndex > 0) {
                                AppStrings.continueReading(currentLanguage, comic.lastReadPageIndex + 1)
                            } else {
                                AppStrings.readButton(currentLanguage)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onManageSequence,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("featured_sequence_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberMidnight,
                            contentColor = CyberNeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.pageSequence(currentLanguage, comic.pages.size),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual story card inside the 100-story library list.
 * Downsamples cover artwork to thumbnail bounds (`240x320` px) via Coil [ImageRequest] so scrolling
 * across 100 cards keeps bitmap heap allocations minimal.
 */
@Composable
internal fun ComicListItemCard(
    comic: ComicBook,
    currentLanguage: AppLanguage,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    onRead: () -> Unit,
    onManageSequence: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
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
                    .size(68.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                val thumbData = comic.coverDrawableResId ?: comic.coverUriString
                if (thumbData != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbData)
                            .size(240, 320)
                            .crossfade(false)
                            .build(),
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onRead,
                        modifier = Modifier.height(32.dp),
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
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onManageSequence,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberSurfaceElevated,
                            contentColor = CyberNeonCyan
                        )
                    ) {
                        Text(AppStrings.pageOrderButton(currentLanguage), fontSize = 11.sp)
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
