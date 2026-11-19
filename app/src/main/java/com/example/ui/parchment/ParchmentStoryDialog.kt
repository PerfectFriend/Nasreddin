package com.example.ui.parchment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.ui.theme.DisplayTitleFontFamily
import com.example.ui.theme.ParchmentBodyFontFamily
import com.example.ui.theme.ParchmentHeadingFontFamily
import com.example.util.AppStrings

private val LapisDeepInk = Color(0xFF0B1C3D)
private val AntiqueGold = Color(0xFFC58B20)
private val BrightGold = Color(0xFFECC663)
private val TurquoiseAccent = Color(0xFF17898B)
private val ParchmentInkDark = Color(0xFF251407)
private val ParchmentInkWarm = Color(0xFF6B3F12)

/**
 * Illuminated Eastern parchment scroll dialog displaying the complete localized parable text
 * and closing moral in traditional book serif typography ([DisplayTitleFontFamily],
 * [ParchmentHeadingFontFamily], and [ParchmentBodyFontFamily]).
 */
@Composable
fun ParchmentStoryDialog(
    comic: ComicBook,
    currentPage: ComicPage? = null,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val resolvedStoryText = comic.fullStoryText.ifBlank {
        comic.pages.mapNotNull { it.narrativeText }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n\n")
            .ifBlank { comic.description }
    }
    val paragraphs = resolvedStoryText
        .split("\n\n", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val resolvedMoral = comic.moralText.ifBlank {
        currentPage?.dialogueText?.trim().orEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.94f)
                .heightIn(min = 320.dp, max = 680.dp)
                .padding(vertical = 12.dp)
                .testTag("parchment_dialog_root"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Carved wooden/gold top scroll roller
            ParchmentScrollRoller(modifier = Modifier.fillMaxWidth())

            // Parchment body container nested inside scroll rollers
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFEAD19E),
                                Color(0xFFF8ECD0),
                                Color(0xFFF5E5C0),
                                Color(0xFFE7CA90)
                            )
                        )
                    )
            ) {
                // Aged paper horizontal fibers, celestial watermark & double lapis/gold Eastern frame
                Canvas(modifier = Modifier.matchParentSize()) {
                    val fiberStep = 22.dp.toPx()
                    var yPos = fiberStep
                    while (yPos < size.height) {
                        drawLine(
                            color = Color(0x0E7A4B16),
                            start = Offset(14.dp.toPx(), yPos),
                            end = Offset(size.width - 14.dp.toPx(), yPos),
                            strokeWidth = 1f
                        )
                        yPos += fiberStep
                    }

                    // Soft celestial eight-pointed star watermark behind text
                    val center = Offset(size.width / 2f, size.height * 0.48f)
                    val r = size.minDimension * 0.24f
                    val half = r * 0.72f
                    drawRect(
                        color = Color(0x0CC58B20),
                        topLeft = Offset(center.x - half, center.y - half),
                        size = Size(half * 2f, half * 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    val rotated = Path().apply {
                        moveTo(center.x, center.y - r)
                        lineTo(center.x + r, center.y)
                        lineTo(center.x, center.y + r)
                        lineTo(center.x - r, center.y)
                        close()
                    }
                    drawPath(rotated, color = Color(0x0CC58B20), style = Stroke(width = 2.dp.toPx()))

                    // Outer lapis border & antique gold fillet
                    val m1 = 6.dp.toPx()
                    val m2 = 12.dp.toPx()
                    drawRoundRect(
                        color = LapisDeepInk,
                        topLeft = Offset(m1, m1),
                        size = Size(size.width - m1 * 2f, size.height - m1 * 2f),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = AntiqueGold,
                        topLeft = Offset(m2, m2),
                        size = Size(size.width - m2 * 2f, size.height - m2 * 2f),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    // Four turquoise-and-gold corner rosettes
                    val cr = 5.dp.toPx()
                    val offset = m2 + 4.dp.toPx()
                    val cornerPts = listOf(
                        Offset(offset, offset),
                        Offset(size.width - offset, offset),
                        Offset(offset, size.height - offset),
                        Offset(size.width - offset, size.height - offset)
                    )
                    for (pt in cornerPts) {
                        drawCircle(color = TurquoiseAccent, radius = cr, center = pt)
                        drawCircle(color = BrightGold, radius = cr, center = pt, style = Stroke(width = 1.4.dp.toPx()))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    // Top bar inside parchment: episode medallion badge + accessible close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LapisDeepInk.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AntiqueGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = ParchmentInkWarm,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.parchmentEpisodeBadge(currentLanguage, comic.storyNumber),
                                    fontFamily = ParchmentHeadingFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.9.sp,
                                    color = ParchmentInkWarm
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_parchment_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = AppStrings.close(currentLanguage),
                                tint = ParchmentInkDark
                            )
                        }
                    }

                    // Scrollable illuminated tale text & moral
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = comic.title,
                            fontFamily = DisplayTitleFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp,
                            lineHeight = 30.sp,
                            color = ParchmentInkDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("parchment_story_title")
                        )
                        if (comic.subtitle.isNotBlank()) {
                            Text(
                                text = comic.subtitle,
                                fontFamily = ParchmentHeadingFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TurquoiseAccent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        EasternOrnamentDivider(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().testTag("parchment_story_body"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            paragraphs.forEachIndexed { index, paragraph ->
                                if (index == 0 && paragraph.length >= 2) {
                                    IlluminatedDropCapParagraph(text = paragraph)
                                } else {
                                    Text(
                                        text = paragraph,
                                        fontFamily = ParchmentBodyFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 17.sp,
                                        lineHeight = 27.sp,
                                        color = ParchmentInkDark
                                    )
                                }
                            }
                        }
                        if (resolvedMoral.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x28C58B20))
                                    .border(1.5.dp, AntiqueGold, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .testTag("parchment_moral_card")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = AppStrings.parchmentMoralHeader(currentLanguage),
                                        fontFamily = ParchmentHeadingFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp,
                                        color = ParchmentInkWarm
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = resolvedMoral,
                                        fontFamily = ParchmentHeadingFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 18.sp,
                                        lineHeight = 25.sp,
                                        color = LapisDeepInk,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        if (currentPage != null && !currentPage.dialogueText.isNullOrBlank() &&
                            currentPage.dialogueText.trim() != resolvedMoral
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LapisDeepInk.copy(alpha = 0.06f))
                                    .border(1.dp, LapisDeepInk.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = AppStrings.comicDialoguesLabel(currentLanguage),
                                        fontFamily = ParchmentHeadingFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = LapisDeepInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentPage.dialogueText,
                                        fontFamily = ParchmentBodyFontFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        color = ParchmentInkDark
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("parchment_done_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LapisDeepInk,
                            contentColor = BrightGold
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = AppStrings.close(currentLanguage),
                            fontFamily = ParchmentHeadingFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            // Carved wooden/gold bottom scroll roller
            ParchmentScrollRoller(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun IlluminatedDropCapParagraph(text: String) {
    val firstChar = text.first().uppercaseChar().toString()
    val remainder = text.drop(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp, end = 10.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LapisDeepInk)
                .border(1.5.dp, AntiqueGold, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstChar,
                fontFamily = DisplayTitleFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = BrightGold
            )
        }
        Text(
            text = remainder,
            fontFamily = ParchmentBodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 27.sp,
            color = ParchmentInkDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EasternOrnamentDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(14.dp)) {
        val midY = size.height / 2f
        val midX = size.width / 2f
        drawLine(
            color = AntiqueGold,
            start = Offset(12.dp.toPx(), midY),
            end = Offset(midX - 18.dp.toPx(), midY),
            strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
            color = AntiqueGold,
            start = Offset(midX + 18.dp.toPx(), midY),
            end = Offset(size.width - 12.dp.toPx(), midY),
            strokeWidth = 1.4.dp.toPx()
        )
        val r = 6.dp.toPx()
        val diamond = Path().apply {
            moveTo(midX, midY - r)
            lineTo(midX + r, midY)
            lineTo(midX, midY + r)
            lineTo(midX - r, midY)
            close()
        }
        drawPath(diamond, color = TurquoiseAccent, style = Fill)
        drawPath(diamond, color = AntiqueGold, style = Stroke(width = 1.3.dp.toPx()))
    }
}

@Composable
private fun ParchmentScrollRoller(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(18.dp)) {
        val finialWidth = 16.dp.toPx()
        val barTop = 3.dp.toPx()
        val barHeight = size.height - 6.dp.toPx()

        // Main carved wood scroll rod
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF7D5324), Color(0xFF472A0D), Color(0xFF2B1805))
            ),
            topLeft = Offset(finialWidth / 2f, barTop),
            size = Size(size.width - finialWidth, barHeight),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        // Left and right turned gold finials
        drawRoundRect(
            color = AntiqueGold,
            topLeft = Offset(0f, 1.dp.toPx()),
            size = Size(finialWidth, size.height - 2.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawRoundRect(
            color = AntiqueGold,
            topLeft = Offset(size.width - finialWidth, 1.dp.toPx()),
            size = Size(finialWidth, size.height - 2.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
    }
}
