package com.example.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppLanguage
import com.example.model.ComicBook
import com.example.model.ReaderBackground
import com.example.model.ReadingMode
import com.example.ui.theme.CyberMidnight
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.VanGoghStarGold
import com.example.util.AppStrings
import kotlinx.coroutines.launch

/**
 * Fullscreen comic reader supporting both horizontal swipe paging ([ReadingMode.HORIZONTAL_PAGER])
 * and continuous vertical strip scrolling ([ReadingMode.VERTICAL_SCROLL]).
 *
 * Reviewer notes:
 * - [BackHandler] resolves nested overlays in strict priority order:
 *   Story Picker -> Background Theme Menu -> Thumbnail Filmstrip -> Exit Reader.
 * - Background Menu and Story Picker use in-window [AnimatedVisibility] boxes rather than native
 *   Material `DropdownMenu`/`ModalBottomSheet` popup windows to avoid `BLASTBufferQueue` surface
 *   teardown glitches when toggling quickly during fullscreen reading.
 * - Switching stories directly keeps the reader open; caller resets pager/zoom state via
 *   `key(activeComic.id)` in `MainActivity`.
 */
@Composable
fun ComicReaderScreen(
    comic: ComicBook,
    allComics: List<ComicBook>,
    isPremiumUnlocked: Boolean,
    seriesUnlocked: Boolean,
    adUnlockedMaxStory: Int,
    initialPageIndex: Int,
    currentLanguage: AppLanguage,
    readingMode: ReadingMode,
    readerBackground: ReaderBackground,
    controlsVisible: Boolean,
    isThumbnailDrawerOpen: Boolean,
    showSpeechBubbles: Boolean,
    onPageChanged: (Int) -> Unit,
    onToggleControls: () -> Unit,
    onToggleThumbnails: (Boolean) -> Unit,
    onToggleSpeechBubbles: () -> Unit,
    onOpenSequenceManager: () -> Unit,
    onOpenLore: () -> Unit,
    onChangeReadingMode: (ReadingMode) -> Unit,
    onChangeBackground: (ReaderBackground) -> Unit,
    onSelectComic: (ComicBook) -> Unit,
    onPreviousComic: () -> Unit,
    onNextComic: () -> Unit,
    onClose: () -> Unit
) {
    var bgMenuExpanded by remember { mutableStateOf(false) }
    var storyPickerOpen by remember { mutableStateOf(false) }

    val currentStoryIdx = allComics.indexOfFirst { it.id == comic.id }
    val hasPreviousStory = currentStoryIdx > 0
    val nextStoryBook = if (currentStoryIdx in 0 until allComics.lastIndex) {
        allComics[currentStoryIdx + 1]
    } else null
    val hasNextStory = nextStoryBook != null
    val isNextStoryLocked = nextStoryBook != null &&
        !nextStoryBook.isFree &&
        !isPremiumUnlocked &&
        !seriesUnlocked &&
        nextStoryBook.storyNumber > adUnlockedMaxStory

    BackHandler {
        when {
            storyPickerOpen -> storyPickerOpen = false
            bgMenuExpanded -> bgMenuExpanded = false
            isThumbnailDrawerOpen -> onToggleThumbnails(false)
            else -> onClose()
        }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            bgMenuExpanded = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val totalPages = comic.pages.size
    val safePageIndex = initialPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))

    val bgColor = when (readerBackground) {
        ReaderBackground.CYBER_DARK -> CyberMidnight
        ReaderBackground.PURE_BLACK -> Color.Black
        ReaderBackground.SEPIA_PARCHMENT -> Color(0xFF191410)
    }

    val pagerState = rememberPagerState(
        initialPage = safePageIndex,
        pageCount = { totalPages }
    )

    val verticalListState = rememberLazyListState(initialFirstVisibleItemIndex = safePageIndex)

    // Keep ViewModel reading progress and Lore sheet synced across BOTH horizontal pager and vertical scroll modes
    LaunchedEffect(readingMode, pagerState, verticalListState) {
        snapshotFlow {
            if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                pagerState.currentPage
            } else {
                verticalListState.firstVisibleItemIndex
            }
        }.collect { page ->
            onPageChanged(page)
        }
    }

    LaunchedEffect(initialPageIndex) {
        if (readingMode == ReadingMode.HORIZONTAL_PAGER && pagerState.currentPage != initialPageIndex) {
            pagerState.scrollToPage(initialPageIndex)
        } else if (readingMode == ReadingMode.VERTICAL_SCROLL && verticalListState.firstVisibleItemIndex != initialPageIndex) {
            verticalListState.scrollToItem(initialPageIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (totalPages == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onToggleControls() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.emptyComicNotice(currentLanguage),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            when (readingMode) {
                ReadingMode.HORIZONTAL_PAGER -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { pageIdx ->
                        val page = comic.pages.getOrNull(pageIdx)
                        if (page != null) {
                            ZoomableComicImage(
                                page = page,
                                showBubbles = showSpeechBubbles,
                                onTap = onToggleControls
                            )
                        }
                    }
                }
                ReadingMode.VERTICAL_SCROLL -> {
                    LazyColumn(
                        state = verticalListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(comic.pages, key = { _, page -> page.id }) { _, page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(580.dp)
                            ) {
                                ZoomableComicImage(
                                    page = page,
                                    showBubbles = showSpeechBubbles,
                                    onTap = onToggleControls
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top App Bar Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                title = comic.title,
                currentPage = if (readingMode == ReadingMode.HORIZONTAL_PAGER) pagerState.currentPage + 1 else verticalListState.firstVisibleItemIndex + 1,
                totalPages = totalPages,
                currentLanguage = currentLanguage,
                readingMode = readingMode,
                hasDynamicBubbles = comic.pages.any { it.bubbles.isNotEmpty() },
                showSpeechBubbles = showSpeechBubbles,
                onBack = onClose,
                onToggleSpeechBubbles = onToggleSpeechBubbles,
                onOpenLore = onOpenLore,
                onOpenSequence = onOpenSequenceManager,
                onToggleReadingMode = {
                    val nextMode = if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                        ReadingMode.VERTICAL_SCROLL
                    } else {
                        ReadingMode.HORIZONTAL_PAGER
                    }
                    onChangeReadingMode(nextMode)
                },
                onOpenBackgroundMenu = { bgMenuExpanded = !bgMenuExpanded }
            )
        }

        // Bottom Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val currentPageIdx = if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                pagerState.currentPage
            } else {
                verticalListState.firstVisibleItemIndex
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                ThumbnailFilmstripDrawer(
                    visible = isThumbnailDrawerOpen,
                    comic = comic,
                    currentPageIndex = currentPageIdx,
                    currentLanguage = currentLanguage,
                    onSelectPage = { selectedIdx ->
                        coroutineScope.launch {
                            if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                                pagerState.animateScrollToPage(selectedIdx)
                            } else {
                                verticalListState.animateScrollToItem(selectedIdx)
                            }
                        }
                        onToggleThumbnails(false)
                    },
                    onClose = { onToggleThumbnails(false) }
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyberSurfaceDark.copy(alpha = 0.95f),
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (totalPages > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${currentPageIdx + 1}",
                                            color = VanGoghStarGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        Slider(
                                            value = currentPageIdx.toFloat(),
                                            onValueChange = { newVal ->
                                                val target = newVal.toInt()
                                                coroutineScope.launch {
                                                    if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                                                        pagerState.scrollToPage(target)
                                                    } else {
                                                        verticalListState.scrollToItem(target)
                                                    }
                                                }
                                            },
                                            valueRange = 0f..(totalPages - 1).toFloat(),
                                            steps = if (totalPages > 2) totalPages - 2 else 0,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("reader_page_slider"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = VanGoghStarGold,
                                                activeTrackColor = VanGoghStarGold,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )

                                        Text(
                                            text = "$totalPages",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (currentPageIdx > 0) {
                                                    coroutineScope.launch {
                                                        if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                                                            pagerState.animateScrollToPage(currentPageIdx - 1)
                                                        } else {
                                                            verticalListState.animateScrollToItem(currentPageIdx - 1)
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = currentPageIdx > 0,
                                            modifier = Modifier.testTag("reader_prev_page")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = AppStrings.prevPage(currentLanguage),
                                                tint = if (currentPageIdx > 0) VanGoghStarGold else Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CyberMidnight.copy(alpha = 0.8f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, VanGoghStarGold.copy(alpha = 0.4f)),
                                            modifier = Modifier.clickable { onToggleThumbnails(!isThumbnailDrawerOpen) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GridView,
                                                    contentDescription = null,
                                                    tint = VanGoghStarGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = AppStrings.pageCounter(currentLanguage, currentPageIdx + 1, totalPages),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (currentPageIdx < totalPages - 1) {
                                                    coroutineScope.launch {
                                                        if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                                                            pagerState.animateScrollToPage(currentPageIdx + 1)
                                                        } else {
                                                            verticalListState.animateScrollToItem(currentPageIdx + 1)
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = currentPageIdx < totalPages - 1,
                                            modifier = Modifier.testTag("reader_next_page")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = AppStrings.nextPage(currentLanguage),
                                                tint = if (currentPageIdx < totalPages - 1) VanGoghStarGold else Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    )
                                }

                                // Direct Story-to-Story Switcher Row (available on all stories)
                                if (allComics.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Previous Story
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (hasPreviousStory) CyberSurfaceElevated else Color.Transparent,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = hasPreviousStory) { onPreviousComic() }
                                                .testTag("reader_prev_story")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronLeft,
                                                    contentDescription = AppStrings.previousStory(currentLanguage),
                                                    tint = if (hasPreviousStory) CyberNeonCyan else Color.Gray.copy(alpha = 0.35f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = AppStrings.previousStory(currentLanguage),
                                                    color = if (hasPreviousStory) Color.White else Color.Gray.copy(alpha = 0.35f),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Center Story Selector Pill
                                        val displayIndex = if (currentStoryIdx >= 0) currentStoryIdx + 1 else 1
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CyberMidnight,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { storyPickerOpen = true }
                                                .testTag("reader_story_picker_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = CyberNeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = AppStrings.storyCounter(currentLanguage, displayIndex, allComics.size),
                                                    color = VanGoghStarGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ExpandMore,
                                                    contentDescription = AppStrings.chooseStoryTitle(currentLanguage),
                                                    tint = CyberNeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Next Story
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (hasNextStory) CyberSurfaceElevated else Color.Transparent,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = hasNextStory) { onNextComic() }
                                                .testTag("reader_next_story")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isNextStoryLocked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = VanGoghStarGold,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = AppStrings.nextStory(currentLanguage),
                                                    color = if (hasNextStory) Color.White else Color.Gray.copy(alpha = 0.35f),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = AppStrings.nextStory(currentLanguage),
                                                    tint = if (hasNextStory) CyberNeonCyan else Color.Gray.copy(alpha = 0.35f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

        // In-window Background Theme Overlay (avoids native PopupWindow BLASTBufferQueue teardown warnings)
        AnimatedVisibility(
            visible = bgMenuExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { bgMenuExpanded = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 60.dp, end = 12.dp)
                        .widthIn(min = 210.dp, max = 270.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    shape = RoundedCornerShape(14.dp),
                    color = CyberSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        listOf(
                            ReaderBackground.CYBER_DARK to AppStrings.bgCyberMidnight(currentLanguage),
                            ReaderBackground.PURE_BLACK to AppStrings.bgPureBlack(currentLanguage),
                            ReaderBackground.SEPIA_PARCHMENT to AppStrings.bgSepia(currentLanguage)
                        ).forEach { (bgOption, label) ->
                            val isSelected = readerBackground == bgOption
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        bgMenuExpanded = false
                                        onChangeBackground(bgOption)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) VanGoghStarGold else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = VanGoghStarGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // In-window Story Switcher Overlay (jump directly to any story without leaving the reader)
        AnimatedVisibility(
            visible = storyPickerOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val pickerListState = rememberLazyListState(
                initialFirstVisibleItemIndex = currentStoryIdx.coerceAtLeast(0)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { storyPickerOpen = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .fillMaxHeight(0.72f)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    shape = RoundedCornerShape(22.dp),
                    color = CyberSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, VanGoghStarGold.copy(alpha = 0.55f)),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = VanGoghStarGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.chooseStoryTitle(currentLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VanGoghStarGold
                                )
                            }
                            IconButton(
                                onClick = { storyPickerOpen = false },
                                modifier = Modifier.testTag("close_story_picker")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = AppStrings.close(currentLanguage),
                                    tint = Color.White
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        LazyColumn(
                            state = pickerListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allComics, key = { it.id }) { itemComic ->
                                val isCurrent = itemComic.id == comic.id
                                val isLocked = !itemComic.isFree &&
                                    !isPremiumUnlocked &&
                                    !seriesUnlocked &&
                                    itemComic.storyNumber > adUnlockedMaxStory

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isCurrent) CyberMidnight else CyberSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) VanGoghStarGold else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            storyPickerOpen = false
                                            if (!isCurrent) {
                                                onSelectComic(itemComic)
                                            }
                                        }
                                        .testTag("story_picker_item_${itemComic.storyNumber}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp, 54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (itemComic.coverDrawableResId != null) {
                                                Image(
                                                    painter = painterResource(id = itemComic.coverDrawableResId),
                                                    contentDescription = itemComic.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else if (itemComic.coverUriString != null) {
                                                AsyncImage(
                                                    model = itemComic.coverUriString,
                                                    contentDescription = itemComic.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            if (isLocked) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.55f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = VanGoghStarGold,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = itemComic.title,
                                                color = if (isCurrent) VanGoghStarGold else Color.White,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = itemComic.description,
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(VanGoghStarGold, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF281600),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = VanGoghStarGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = CyberNeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    currentLanguage: AppLanguage,
    readingMode: ReadingMode,
    hasDynamicBubbles: Boolean,
    showSpeechBubbles: Boolean,
    onBack: () -> Unit,
    onToggleSpeechBubbles: () -> Unit,
    onOpenLore: () -> Unit,
    onOpenSequence: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onOpenBackgroundMenu: () -> Unit
) {

    Surface(
        color = CyberSurfaceDark.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reader_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = AppStrings.backToLibrary(currentLanguage),
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = AppStrings.pageCounter(currentLanguage, currentPage, totalPages),
                    color = CyberNeonCyan,
                    fontSize = 11.sp
                )
            }

            // Dynamic Speech Bubble Toggle (shown when story uses universal dynamic bubble templates)
            if (hasDynamicBubbles) {
                IconButton(
                    onClick = onToggleSpeechBubbles,
                    modifier = Modifier.testTag("reader_bubbles_toggle")
                ) {
                    Icon(
                        imageVector = if (showSpeechBubbles) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                        contentDescription = AppStrings.speechBubblesTooltip(currentLanguage),
                        tint = if (showSpeechBubbles) CyberNeonCyan else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Lore / Sermon Text Button
            IconButton(
                onClick = onOpenLore,
                modifier = Modifier.testTag("reader_lore_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = AppStrings.sermonTextTooltip(currentLanguage),
                    tint = VanGoghStarGold
                )
            }

            // Sequence Manager Button
            IconButton(
                onClick = onOpenSequence,
                modifier = Modifier.testTag("reader_sequence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = AppStrings.pageSequenceTooltip(currentLanguage),
                    tint = CyberNeonCyan
                )
            }

            // Reading mode toggle (Pager vs Scroll)
            IconButton(
                onClick = onToggleReadingMode,
                modifier = Modifier.testTag("reader_mode_button")
            ) {
                Icon(
                    imageVector = if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                        Icons.Default.ViewStream
                    } else {
                        Icons.Default.ViewCarousel
                    },
                    contentDescription = if (readingMode == ReadingMode.HORIZONTAL_PAGER) {
                        AppStrings.switchToScrollTooltip(currentLanguage)
                    } else {
                        AppStrings.switchToPagerTooltip(currentLanguage)
                    },
                    tint = Color.White
                )
            }

            // Background Theme Menu
            IconButton(
                onClick = onOpenBackgroundMenu,
                modifier = Modifier.testTag("reader_bg_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = AppStrings.readerBackgroundTooltip(currentLanguage),
                    tint = Color.White
                )
            }
        }
    }
}
