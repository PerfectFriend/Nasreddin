# Documentation — Ходжа / Hodja

> **Repository Hygiene Rule:** Root `README.md` stays minimal as the GitHub landing page. All technical specs, architecture notes, and audit logs live strictly inside `docs/`. Do not add standalone report files to the repository root.

---

## 1. Product Overview

**Hodja (`Ходжа`)** is an Android Kotlin & Jetpack Compose interactive comic book and folklore parable reader built around curated visual storytelling, compact asset memory management, strict multi-language support, and a **Hybrid Monetization** model.

- **Platform:** Android (`minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`)
- **UI Stack:** Kotlin + Jetpack Compose + Material Design 3 + Coil
- **Supported Languages:** Russian (`RU`), Spanish (`ES`), English (`EN`) with OS locale auto-detection (`EN` fallback)

---

## 2. Core Engineering Rules & Invariants

1. **Minimum 4 Pages per Story (`pages.size >= 4`, 5 Pages on Episodes #2–#100):**
   - **Episode #1:** Dedicated ornate Mosque cover (`img_cover_mosque`) with localized cartouche title plus 3 full-page comic scenes (`4` pages total).
   - **Episodes #2–#10:** Localized Eastern cover (`img_cover_*`) prepended as Page 1 before **4 individual fullscreen panel pages** cropped via `PanelCrop` (`BitmapRegionDecoder` + `LruCache(32)`), giving `5` pages per story.
   - **Episodes #11–#50 (Unique Per-Story Artwork):** Language-agnostic bespoke text-free 2×2 comic sheets (`img_story_011..050.webp`), each illustrating that story's own 4-panel plot beats with eastern lapis-turquoise-gold framing, paired with dynamic localized `SpeechBubble` overlays (`StoryBubbleComposer` + `ComicBubbleOverlay` with adaptive typography) and localized `RU / ES / EN` morals stored in `nasreddin_anecdotes_vault.json`.
   - **Episodes #51–#100 (Universal Thematic Fallback):** Theme-matched text-free 2×2 sheets (`bazaar`, `teahouse`, `night`) paired with the same localized bubble & moral engine (`StoryBubbleComposer`) while remaining episodes await future art drops.
   - **8 Curated Eastern Covers & Dynamic Cartouche Titles:** 8 `3:4` ornate Eastern covers (`mosque` for #1 plus `bazaar`, `teahouse`, `night`, `court`, `caravan`, `lake`, `garden`) framed in lapis-turquoise-gold ornaments. `LocalizedComicCover` pairs compact calligraphic **El Messiri** (with static **Philosopher Bold** fallback and crown `№ X` medallions) inside a widened illuminated parchment scroll (`78%` width, `maxLines = 4`, auto-fit shrink guard) so every story's full localized title wraps cleanly in `RU / ES / EN` without clipping.
   - **Parchment Story Viewer (`ParchmentStoryDialog`):** Accessible from the Library cards, featured banner, and Reader HUD (`reader_parchment_button`), rendering the complete localized narrative with drop caps and moral plaque on an Eastern parchment scroll using locally bundled full multi-script Google Fonts (`Playfair Display`, `Cormorant Garamond`, `Lora`, `El Messiri`, `Philosopher`).
2. **Strict Single-Language Catalog Filtering:**
   - `ComicRepository.loadAllComics()` exposes only built-in stories matching the active `AppLanguage` (plus user-imported SAF folders whose labels also localize via `AppStrings`). Switching languages reloads localized titles, cover cartouches, parchment stories, descriptions, drawables, bubbles, and reader strings immediately.
3. **Hybrid Monetization Model:**
   - **Stories 1–10:** 100% Free, ad-free.
   - **Stories 11–100:**
     - **Rewarded Ad Path:** Unlocks episodes dynamically in batches of 5 (`11–15` through `96–100`) with a periodic non-rewarded sponsor break every `4` ad-supported story switches (`AD_BREAK_STORY_INTERVAL = 4`).
     - **Series Pack (`$0.99`):** Unlocks the full built-in series ad-free; hides redundant Series/Ad buttons in `PaywallDialog` once owned.
     - **Lifetime PRO (`$4.99`):** Full ad-free unlock across all volumes and customization tools.
4. **Compose Stability & Window Safety:**
   - `ComicReaderScreen` is wrapped in `key(activeComic.id)` in `MainActivity` so switching stories resets pager/scroll/zoom states cleanly.
   - Zoom & pan gestures in `ZoomableComicImage` use `remember(page.id)` to prevent zoom leakage across pages.
   - Both `HORIZONTAL_PAGER` and `VERTICAL_SCROLL` reading modes synchronize page progress (`pagerState.currentPage` / `verticalListState.firstVisibleItemIndex`).
   - Dropdown/picker overlays use in-window `AnimatedVisibility` instead of native `PopupWindow` dropdowns to eliminate `BLASTBufferQueue` surface teardown warnings.
   - `ModalBottomSheet` dismiss handlers await `sheetState.hide()` before updating host visibility state.
5. **Diagnostics Hygiene:**
   - Zero silent `catch` blocks across production code; JSON, SAF URI, and bitmap decode fallbacks log structured warnings/errors via `android.util.Log`.

---

## 3. Architecture & Module Map

```text
app/src/main/java/com/example/
├── MainActivity.kt                     # Edge-to-edge Compose host & root dialog/router scope
├── model/
│   └── ComicModels.kt                  # Immutable domain models, AppLanguage, PanelCrop, UI enums
├── data/
│   └── ComicRepository.kt              # Single Source of Truth (vault JSON, prefs, unlocks, progress)
├── ui/
│   ├── ComicViewModel.kt               # UDF state holder (StateFlow<UiState>), parchment & unlock flows
│   ├── components/
│   │   └── LocalizedComicCover.kt      # 3:4 Eastern cover renderer, cartouche typography & filigree trim
│   ├── parchment/
│   │   └── ParchmentStoryDialog.kt     # Illuminated parchment scroll reader with drop cap & moral card
│   ├── splash/SplashScreen.kt          # Ornate minaret launch screen & instant RU/ES/EN toggle
│   ├── library/
│   │   ├── ComicLibraryScreen.kt       # 100-story catalog host, LazyColumn layout & language scrim
│   │   ├── LibraryHeaderAndBanners.kt  # Top bar, Hybrid Monetization banner, SAF import cards, wisdom card
│   │   ├── LibrarySearchAndFilters.kt  # Search bar, tier/atmosphere FilterChips & empty-state reset card
│   │   ├── LibraryCatalogCards.kt      # Hero featured card & 100-card list items with Coil downsampling
│   │   └── LibraryFilter.kt            # Tier/atmosphere filter model & pure search predicate
│   ├── reader/
│   │   ├── ComicReaderScreen.kt        # Dual-mode reader, direct story switcher, overlays, BackHandler
│   │   ├── ComicPageArtwork.kt         # BitmapRegionDecoder panel cropper, cartouche titles & LruCache(32)
│   │   ├── ComicBubbleOverlay.kt       # Dynamic localized speech/narration balloons fitted to artwork rect
│   │   ├── ZoomableComicImage.kt       # Page-keyed pinch-to-zoom, double-tap & pan gestures
│   │   ├── ThumbnailFilmstripDrawer.kt # Auto-synced bottom thumbnail strip
│   │   └── ComicLoreSheet.kt           # Delegates reader story view to ParchmentStoryDialog
│   ├── sequence/SequenceManagerSheet.kt# Page reordering, natural/reverse sort & pruning
│   ├── monetization/PaywallDialog.kt   # Hybrid Paywall & Rewarded/Interstitial Ad slot overlays
│   └── theme/                          # Material 3 palette + bundled TTF serif Typography
└── util/
    ├── AppStrings.kt                   # Centralized RU / ES / EN localization dictionary
    ├── StoryBubbleComposer.kt          # Splits parables into 4 dynamic panel speech/narration beats
    ├── FolderImporter.kt               # SAF folder/multi-image importer with persistable URI access
    └── AlphanumericComparator.kt       # Overflow-safe natural numeric string comparator
```

---

## 4. Build, Test & Asset Separation

### Gradle Commands (Workspace Root)

```bash
# Build debug APK
gradle :app:assembleDebug

# Run JVM + Robolectric unit tests (no emulator required)
gradle :app:testDebugUnitTest
```

> **Note:** Always invoke `gradle` directly (`./gradlew` wrapper is not used in this container).

### APK Size vs Repo Media Separation
- Android bundled assets live in `app/src/main/res/drawable/` (`89` WebP images + `2` adaptive icon XML layers, `24.71 MB` total: ornate Eastern splash art, **8** themed story covers, dedicated plates for **#1–#50**, and 3 universal sheets for `#51–#100`), `app/src/main/res/font/` (`el_messiri.ttf`, `philosopher.ttf`, `playfair_display.ttf`, `cormorant_garamond.ttf`, `lora.ttf`), and `app/src/main/assets/`.
- Library thumbnails and featured covers load through Coil `ImageRequest` downsampling (`240×320` and `480×640` px) with `LocalizedComicCover` overlaying the active language title cleanly inside the blank cartouche.
- GitHub documentation media (`banner_top.jpg`, `banner_bottom.jpg`) lives exclusively in `docs/images/` so documentation artwork never inflates the compiled APK.

---

## 5. Planned Release Roadmap (Summary)

| Milestone | Scope | Technical Approach |
| :--- | :--- | :--- |
| **v1.0 (Baseline)** | 25 anecdotes (`>= 4` pages each), `RU/ES/EN`, SAF importer, Hybrid Paywall UX | Bundled vault JSON + 2×2 panel region cropping + local state |
| **v1.1 (Stories #1–#50 Bespoke Art — Audio Next)** | 100 stories + Unique 2×2 art for **#1–#50** + 8 Eastern covers + Search/Filter + Parchment Scroll | 40 bespoke text-free 2×2 sheets (`#11–#50`) + 3 universal templates (`#51–#100`) + 8 ornate covers + dynamic localized cartouches & bubbles + `.webp` assets (`24.71 MB`); next: ACE-Step / Suno `4×3 min` Eastern soundtrack (`AudioFocus`-safe) + remaining bespoke art `#51–#100` |
| **v1.2** | Top-10 global languages + Live Billing & Ads SDKs | Google Play Billing / RuStore BillingClient + AdMob / Yandex Mobile Ads wiring at `PaywallDialog` / `ComicViewModel` seams |
| **v2.0** | 500–1,200+ episodes via seasonal episodic drops | Cloud CDN / Play Feature/Asset packs + automated 20s cliffhanger video teaser funnel |
