package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.VanGoghSolarYellow
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

@Composable
internal fun LibraryTopHeader(
    currentLanguage: AppLanguage,
    onOpenLanguageMenu: () -> Unit,
    onOpenSplash: () -> Unit,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit
) {
    Surface(
        color = CyberSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(VanGoghStarGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF241500),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.appTitle(currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = VanGoghStarGold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = AppStrings.appSubtitle(currentLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberNeonCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberMidnight,
                    border = BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { onOpenLanguageMenu() }
                        .padding(end = 4.dp)
                        .testTag("library_lang_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = VanGoghStarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLanguage.name.take(2),
                            fontWeight = FontWeight.Bold,
                            color = VanGoghStarGold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSplash,
                    modifier = Modifier.testTag("header_splash_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = AppStrings.splashTooltip(currentLanguage),
                        tint = VanGoghSolarYellow
                    )
                }

                IconButton(
                    onClick = onPickFolder,
                    modifier = Modifier.testTag("header_add_folder")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = AppStrings.loadFolderTitle(currentLanguage),
                        tint = VanGoghStarGold
                    )
                }
                IconButton(
                    onClick = onPickFiles,
                    modifier = Modifier.testTag("header_add_files")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = AppStrings.selectFilesTitle(currentLanguage),
                        tint = CyberNeonCyan
                    )
                }
            }
        }
    }
}

@Composable
internal fun PremiumPromoBanner(
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    unlockedCount: Int,
    totalBuiltInCount: Int,
    currentLanguage: AppLanguage,
    onOpenPaywall: () -> Unit
) {
    val isFullyAdFree = isPremiumUnlocked || seriesUnlocked
    val hasAdUnlockedStories = unlockedCount > 10 && !isFullyAdFree

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { if (!isPremiumUnlocked) onOpenPaywall() }
            .testTag("premium_promo_banner"),
        shape = RoundedCornerShape(14.dp),
        color = if (isFullyAdFree) CyberSurfaceDark else Color(0xFF261D0A),
        border = BorderStroke(
            1.5.dp,
            if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isFullyAdFree) CyberNeonCyan.copy(alpha = 0.2f) else VanGoghStarGold.copy(alpha = 0.25f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = when {
                            isPremiumUnlocked -> AppStrings.unlockedStatusBanner(currentLanguage)
                            seriesUnlocked -> AppStrings.seriesUnlockedStatusBanner(currentLanguage)
                            else -> AppStrings.unlockFullVaultBanner(currentLanguage)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isFullyAdFree) CyberNeonCyan else VanGoghStarGold
                    )
                    Text(
                        text = if (isFullyAdFree) {
                            AppStrings.unlockedStatusSubtext(currentLanguage, totalBuiltInCount)
                        } else {
                            AppStrings.hybridBannerSubtext(
                                lang = currentLanguage,
                                unlockedCount = unlockedCount,
                                totalCount = totalBuiltInCount,
                                isAdSupportedUnlocked = hasAdUnlockedStories
                            )
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            if (!isPremiumUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VanGoghStarGold,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = AppStrings.paywallSalePrice(),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2B1700),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ImportActionsRow(
    currentLanguage: AppLanguage,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPickFolder() }
                .testTag("import_folder_card"),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberNeonCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.loadFolderTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = AppStrings.loadFolderSub(currentLanguage),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPickFiles() }
                .testTag("import_files_card"),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(VanGoghStarGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = VanGoghStarGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.selectFilesTitle(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = AppStrings.selectFilesSub(currentLanguage),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
internal fun WisdomCard(currentLanguage: AppLanguage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberMidnight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyberSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = AppStrings.wisdomCardTitle(currentLanguage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = VanGoghStarGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AppStrings.wisdomCardText(currentLanguage),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
