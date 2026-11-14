package com.example.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.VanGoghSolarYellow
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings

/**
 * Launch splash screen displaying the Hodja minaret hero artwork, detected/selectable
 * language toggles (RU / ES / EN), and direct entry actions to either Episode #1 or the Library.
 */
@Composable
fun SplashScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onStartReading: () -> Unit,
    onEnterLibrary: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberMidnight)
            .clickable { onStartReading() }
            .testTag("splash_screen_root")
    ) {
        // Fullscreen Hero Artwork: Nasreddin preaching on top of minaret
        Image(
            painter = painterResource(id = R.drawable.img_splash_minaret),
            contentDescription = AppStrings.splashSermonTitle(currentLanguage),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Scrim overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyberMidnight.copy(alpha = 0.85f),
                            Color.Transparent,
                            CyberMidnight.copy(alpha = 0.5f),
                            CyberMidnight.copy(alpha = 0.95f),
                            CyberMidnight
                        ),
                        startY = 0f,
                        endY = 2200f
                    )
                )
        )

        // Top Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = AppStrings.appTitle(currentLanguage),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = VanGoghStarGold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AppStrings.splashSermonTitle(currentLanguage),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }

        // Bottom Content & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language detection badge with 3 language choices
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = AppStrings.systemLangDetected(currentLanguage),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = currentLanguage.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VanGoghStarGold
                            )
                        }
                    }

                    // 3 Language toggles: RU / ES / EN
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LanguageChip(
                            label = "RU",
                            selected = currentLanguage == AppLanguage.RUSSIAN,
                            onClick = { onSelectLanguage(AppLanguage.RUSSIAN) }
                        )
                        LanguageChip(
                            label = "ES",
                            selected = currentLanguage == AppLanguage.SPANISH,
                            onClick = { onSelectLanguage(AppLanguage.SPANISH) }
                        )
                        LanguageChip(
                            label = "EN",
                            selected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = { onSelectLanguage(AppLanguage.ENGLISH) }
                        )
                    }
                }
            }

            // CTAs
            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("splash_start_reading_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VanGoghStarGold,
                    contentColor = Color(0xFF281600)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.readComicAction(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onEnterLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("splash_enter_library_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = AppStrings.goToLibraryAction(currentLanguage),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (selected) VanGoghStarGold else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (selected) VanGoghStarGold else Color.White.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) Color(0xFF281600) else Color.White
        )
    }
}
