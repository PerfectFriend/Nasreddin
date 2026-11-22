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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Hybrid Monetization paywall offering three Play/AdMob-verified paths to read locked episodes:
 * 1. Lifetime PRO Ad-Free (`hodja_pro_lifetime`) — unlocks all stories + removes all sponsor breaks.
 * 2. Ad-Free Series Pack (`hodja_series_volume1`) — unlocks the built-in Volume 1 series ad-free.
 * 3. Watch AdMob Rewarded Ad (`+5 stories`) — unlocks episodes in batches of 5 via verified reward callback.
 */
@Composable
fun PaywallDialog(
    currentLanguage: AppLanguage,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    totalStoriesCount: Int,
    proPriceLabel: String? = null,
    seriesPriceLabel: String? = null,
    isBusy: Boolean = false,
    statusMessage: String? = null,
    onBuyPro: () -> Unit,
    onBuySeries: () -> Unit,
    onWatchAd: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
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
                            if (proPriceLabel == null) {
                                Text(
                                    text = AppStrings.paywallOriginalPrice(),
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    textDecoration = TextDecoration.LineThrough,
                                    modifier = Modifier.padding(bottom = 4.dp, end = 10.dp)
                                )
                            }
                            Text(
                                text = proPriceLabel ?: AppStrings.paywallSalePrice(),
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

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paywall_status_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = GaudiCoralRed.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GaudiCoralRed.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = statusMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Primary CTA: PRO Ad-Free Lifetime (Google Play Billing)
                Button(
                    onClick = onBuyPro,
                    enabled = !isBusy,
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
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF281500)
                        )
                    } else {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.buyButtonText(currentLanguage, proPriceLabel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. Series Pack CTA (Google Play Billing) — hidden if already purchased
                if (!seriesUnlocked) {
                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onBuySeries,
                        enabled = !isBusy,
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
                            text = AppStrings.buySeriesButtonText(currentLanguage, totalStoriesCount, seriesPriceLabel),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3. Free Ad-Supported Option (+5 stories via AdMob RewardedAd)
                if (canUnlockMoreViaAd) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onWatchAd,
                        enabled = !isBusy,
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

                // 4. Restore Purchase via Google Play Billing queryPurchasesAsync
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isBusy,
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
 * Lightweight scrim displayed only while Google Mobile Ads SDK is actively loading/handing off
 * to the native ad activity. Contains zero unlock/bypass actions.
 */
@Composable
fun AdLoadingOverlayDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .padding(vertical = 24.dp)
                .testTag("ad_loading_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = CyberMidnight,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberNeonCyan),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = VanGoghStarGold,
                    trackColor = CyberSurfaceElevated
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = AppStrings.adLoadingTitle(currentLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VanGoghStarGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.adLoadingBody(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ad_loading_cancel_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.85f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = AppStrings.cancel(currentLanguage), fontSize = 13.sp)
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
