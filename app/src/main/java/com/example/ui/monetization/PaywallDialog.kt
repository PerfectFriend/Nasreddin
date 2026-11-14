package com.example.ui.monetization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppLanguage
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.GaudiCoralRed
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings
import kotlinx.coroutines.delay

/**
 * Hybrid Monetization paywall offering three clear paths to continue reading locked episodes:
 * 1. Lifetime PRO Ad-Free (`$4.99`) — unlocks all stories + removes all sponsor breaks + custom tools.
 * 2. Ad-Free Series Pack (`$0.99`) — unlocks the built-in series ad-free (hidden once [seriesUnlocked] is true).
 * 3. Watch Rewarded Ad (`+5 stories`) — unlocks episodes in batches of 5 while keeping sponsor breaks active.
 *
 * Production integration seam: wire [onBuyPro], [onBuySeries], and [onRestore] to Google Play
 * Billing / RuStore BillingClient, and route [onWatchAd] to AdMob / Yandex RewardedAd callbacks.
 */
@Composable
fun PaywallDialog(
    currentLanguage: AppLanguage,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    totalStoriesCount: Int,
    onBuyPro: () -> Unit,
    onBuySeries: () -> Unit,
    onWatchAd: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    // Once the user owns the Series Pack, only Lifetime PRO upgrade remains relevant
    val canUnlockMoreViaAd = !seriesUnlocked && adUnlockedMaxStory < totalStoriesCount
    val nextBatchStart = (adUnlockedMaxStory + 1).coerceAtMost(totalStoriesCount)
    val nextBatchEnd = (adUnlockedMaxStory + 5).coerceAtMost(totalStoriesCount)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = CyberSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, VanGoghStarGold),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("paywall_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = AppStrings.close(currentLanguage),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(listOf(VanGoghStarGold, GaudiCoralRed)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFF281600),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = AppStrings.paywallTitle(currentLanguage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = VanGoghStarGold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.paywallSubtitle(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PRO Card ($4.99)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberMidnight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GaudiCoralRed.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GaudiCoralRed)
                        ) {
                            Text(
                                text = AppStrings.paywallDiscountBadge(currentLanguage),
                                color = GaudiCoralRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = AppStrings.paywallOriginalPrice(),
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(bottom = 4.dp, end = 10.dp)
                            )
                            Text(
                                text = AppStrings.paywallSalePrice(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = VanGoghStarGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BenefitRow(AppStrings.paywallBenefit1(currentLanguage, totalStoriesCount))
                    BenefitRow(AppStrings.paywallBenefit2(currentLanguage))
                    BenefitRow(AppStrings.paywallBenefit3(currentLanguage))
                    BenefitRow(AppStrings.paywallBenefit4(currentLanguage))
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Primary CTA: PRO Ad-Free Lifetime ($4.99)
                Button(
                    onClick = onBuyPro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("paywall_buy_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanGoghStarGold,
                        contentColor = Color(0xFF281500)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.buyButtonText(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // 2. Series Pack CTA ($0.99 Ad-Free Series) — hidden if already purchased
                if (!seriesUnlocked) {
                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onBuySeries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("paywall_buy_series_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberSurfaceElevated,
                            contentColor = CyberNeonCyan
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.buySeriesButtonText(currentLanguage, totalStoriesCount),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3. Free Ad-Supported Option (+5 stories via Ad)
                if (canUnlockMoreViaAd) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onWatchAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("paywall_watch_ad_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VanGoghStarGold),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, VanGoghStarGold.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.watchAdButtonText(currentLanguage, nextBatchStart, nextBatchEnd),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Restore Purchase
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("paywall_restore_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = AppStrings.restoreButtonText(currentLanguage),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Modal overlay handling both Rewarded Ad unlocks (`isRewarded == true`) and periodic
 * Sponsor Interstitial breaks (`isRewarded == false`, every 4 ad-supported story switches).
 *
 * Enforces a non-skippable countdown in rewarded mode before enabling [onComplete] so
 * reward verification matches standard ad-network completion contracts.
 */
@Composable
fun AdSlotOverlayDialog(
    currentLanguage: AppLanguage,
    isRewarded: Boolean,
    onComplete: () -> Unit,
    onUpgradePro: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalSeconds = 4
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    val progress = 1f - (secondsLeft.toFloat() / totalSeconds.toFloat())

    Dialog(
        onDismissRequest = { if (secondsLeft == 0 || !isRewarded) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isRewarded || secondsLeft == 0,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = CyberMidnight,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberNeonCyan),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurfaceElevated
                    ) {
                        Text(
                            text = "AD • SPONSOR",
                            color = CyberNeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (secondsLeft == 0 || !isRewarded) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp).testTag("ad_slot_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = AppStrings.close(currentLanguage),
                                tint = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(CyberSurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = VanGoghStarGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = AppStrings.adSlotTitle(currentLanguage, isRewarded),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VanGoghStarGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.adSlotBody(currentLanguage, isRewarded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = VanGoghStarGold,
                    trackColor = CyberSurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (secondsLeft > 0) {
                        AppStrings.adSlotCountdown(currentLanguage, secondsLeft)
                    } else {
                        "✓"
                    },
                    color = CyberNeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onComplete,
                    enabled = secondsLeft == 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ad_slot_claim_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VanGoghStarGold,
                        contentColor = Color(0xFF281500),
                        disabledContainerColor = CyberSurfaceElevated,
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = AppStrings.adSlotContinueAction(currentLanguage, isRewarded),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onUpgradePro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ad_slot_upgrade_pro_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberNeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = AppStrings.buyButtonText(currentLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CyberNeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
