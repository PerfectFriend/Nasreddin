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

1. **Minimum 4 Pages per Story (`pages.size >= 4`):**
   - **Episode #1:** 4 dedicated full-page illustrations per language.
   - **Episodes #2–#10:** Localized 2×2 comic sheets dynamically cropped into **4 individual fullscreen panel pages** (`PanelCrop` fractional rects) using `BitmapRegionDecoder` + two-pass dimension decoding + thread-safe `LruCache(32)`.
   - **Episodes #11–#25:** Powered by the v1.1 universal text-free template (`img_universal_hodja_template`) paired with dynamic localized `SpeechBubble` overlays (`StoryBubbleComposer` + `ComicBubbleOverlay` with adaptive typography for narrow screens) and localized `RU / ES / EN` morals stored in `nasreddin_anecdotes_vault.json`.
2. **Strict Single-Language Catalog Filtering:**
   - `ComicRepository.loadAllComics()` exposes only built-in stories matching the active `AppLanguage` (plus user-imported SAF folders whose labels also localize via `AppStrings`). Switching languages reloads localized titles, descriptions, drawables, bubbles, and reader strings immediately.
3. **Hybrid Monetization Model:**
   - **Stories 1–10:** 100% Free, ad-free.
   - **Stories 11–25:**
     - **Rewarded Ad Path:** Unlocks episodes in batches of 5 (`11–15`, `16–20`, `21–25`) with a periodic non-rewarded sponsor break every `4` ad-supported story switches (`AD_BREAK_STORY_INTERVAL = 4`).
     - **Series Pack (`$0.99`):** Unlocks the series ad-free; hides redundant Series/Ad buttons in `PaywallDialog` once owned.
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
│   ├── ComicViewModel.kt               # UDF state holder (StateFlow<UiState>) & ad-frequency logic
│   ├── splash/SplashScreen.kt          # Hero minaret launch screen & instant RU/ES/EN toggle
│   ├── library/ComicLibraryScreen.kt   # Story catalog, SAF importers, hybrid status banner
│   ├── reader/
│   │   ├── ComicReaderScreen.kt        # Dual-mode reader, direct story switcher, overlays, BackHandler
│   │   ├── ComicPageArtwork.kt         # BitmapRegionDecoder panel cropper & bounded LruCache(32)
│   │   ├── ComicBubbleOverlay.kt       # Dynamic localized speech/narration balloons fitted to artwork rect
│   │   ├── ZoomableComicImage.kt       # Page-keyed pinch-to-zoom, double-tap & pan gestures
│   │   ├── ThumbnailFilmstripDrawer.kt # Auto-synced bottom thumbnail strip
│   │   └── ComicLoreSheet.kt           # ModalBottomSheet for narrative, dialogue & wisdom card
│   ├── sequence/SequenceManagerSheet.kt# Page reordering, natural/reverse sort & pruning
│   ├── monetization/PaywallDialog.kt   # Hybrid Paywall & Rewarded/Interstitial Ad slot overlays
│   └── theme/                          # Material 3 high-contrast Midnight Gold & Cyan palette
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
- Android bundled assets live in `app/src/main/res/drawable/` and `app/src/main/assets/`.
- GitHub documentation media (`banner_top.jpg`, `banner_bottom.jpg`) lives exclusively in `docs/images/` so documentation artwork never inflates the compiled APK.

---

## 5. Planned Release Roadmap (Summary)

| Milestone | Scope | Technical Approach |
| :--- | :--- | :--- |
| **v1.0 (Current)** | 25 anecdotes (`>= 4` pages each), `RU/ES/EN`, SAF importer, Hybrid Paywall UX | Bundled vault JSON + 2×2 panel region cropping + local state |
| **v1.1** | 100 stories + 1h Eastern soundtrack (Suno Pro / ACE-Step) | Universal blank speech-bubble panels + on-canvas dynamic localized text overlay (1× art set instead of N× languages); WebP compression / Play Asset Delivery |
| **v1.2** | Top-10 global languages + Live Billing & Ads SDKs | Google Play Billing / RuStore BillingClient + AdMob / Yandex Mobile Ads wiring at `PaywallDialog` / `ComicViewModel` seams |
| **v2.0** | 500–1,200+ episodes via seasonal episodic drops | Cloud CDN / Play Feature/Asset packs + automated 20s cliffhanger video teaser funnel |
