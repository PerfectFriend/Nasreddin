# Documentation — Ходжа / Hodja

> **Repository Hygiene Rule:** Root `README.md` stays minimal as the GitHub landing page. All technical specs, architecture notes, and audit logs live strictly inside `docs/`. Do not add standalone report files to the repository root.

---

## 1. Product Overview

**Hodja (`Ходжа`)** is an Android Kotlin & Jetpack Compose interactive comic book and folklore parable reader built around curated visual storytelling, compact asset memory management, strict multi-language support, and a **Hybrid Monetization** model.

- **Application ID:** `com.aistudio.nasreddin.vdqz` (`namespace = "com.example"`)
- **Version:** `1.0.0` (`versionCode = 1`)
- **Platform:** Android (`minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`)
- **UI Stack:** Kotlin + Jetpack Compose + Material Design 3 + Coil
- **Monetization & Ads:** Google Play Billing (`billing-ktx 9.1.0`) + Google Mobile Ads (`play-services-ads 25.5.0`) + AES-256-GCM `EntitlementStore`
- **Supported Languages:** Russian (`RU`), Spanish (`ES`), English (`EN`) with OS locale auto-detection (`EN` fallback)

---

## 2. Core Engineering Rules & Invariants

1. **Minimum 4 Pages per Story (`pages.size >= 4`, 5 Pages on Episodes #2–#50):**
   - **Episode #1:** Dedicated ornate Mosque cover (`img_cover_mosque`) with localized cartouche title plus 3 full-page comic scenes (`4` pages total).
   - **Episodes #2–#10:** Localized Eastern cover (`img_cover_*`) prepended as Page 1 before **4 individual fullscreen panel pages** cropped via `PanelCrop` (`BitmapRegionDecoder` + `LruCache(32)`), giving `5` pages per story.
   - **Episodes #11–#50 (100% Unique Per-Story Artwork):** Language-agnostic bespoke text-free 2×2 comic sheets (`img_story_011..050.webp` resolved via `BESPOKE_STORY_DRAWABLES` map in `ComicRepository`), each illustrating that story's own 4-panel plot beats with eastern lapis-turquoise-gold framing, paired with dynamic localized `SpeechBubble` overlays (`StoryBubbleComposer` + `ComicBubbleOverlay` with adaptive typography) and localized `RU / ES / EN` morals stored in `nasreddin_anecdotes_vault.json`.
   - **Volume 2 Backlog (`docs/nasreddin_volume2_backlog.json`):** Curated texts `#51–#100` are preserved outside the production asset bundle for post-release expansion once Volume 1 validates demand.
   - **8 Curated Eastern Covers & Dynamic Cartouche Titles:** 8 `3:4` ornate Eastern covers (`mosque` for #1 plus `bazaar`, `teahouse`, `night`, `court`, `caravan`, `lake`, `garden`) framed in lapis-turquoise-gold ornaments. `LocalizedComicCover` pairs compact calligraphic **El Messiri** (with static **Philosopher Bold** fallback and crown `№ X` medallions) inside a widened illuminated parchment scroll (`78%` width, `maxLines = 4`, auto-fit shrink guard) so every story's full localized title wraps cleanly in `RU / ES / EN` without clipping.
   - **Parchment Story Viewer (`ParchmentStoryDialog`):** Accessible from the Library cards, featured banner, and Reader HUD (`reader_parchment_button`), rendering the complete localized narrative with drop caps and moral plaque on an Eastern parchment scroll using locally bundled full multi-script Google Fonts (`Playfair Display`, `Cormorant Garamond`, `Lora`, `El Messiri`, `Philosopher`).
2. **Strict Single-Language Catalog Filtering:**
   - `ComicRepository.loadAllComics()` exposes only built-in stories matching the active `AppLanguage` (plus user-imported SAF folders whose labels also localize via `AppStrings`). Switching languages reloads localized titles, cover cartouches, parchment stories, descriptions, drawables, bubbles, and reader strings immediately.
3. **Hybrid Monetization (Plan B — Play Billing + AdMob + Encrypted Local State):**
   - **Stories 1–10:** 100% Free, ad-free.
   - **Stories 11–50:**
     - **Rewarded Ad Path (`AdMobAdsManager`):** Unlocks episodes dynamically in batches of 5 (`11–15` through `46–50`, capping cleanly at `50`) strictly inside Google Mobile Ads `OnUserEarnedRewardListener`, with a periodic Interstitial break every `4` ad-supported story switches (`AD_BREAK_STORY_INTERVAL = 4`) when preloaded.
     - **Series Pack (`hodja_series_volume1`, `$0.99`):** Non-consumable Play Billing SKU unlocking the full built-in Volume 1 series ad-free; hides redundant Series/Ad buttons once owned.
     - **Lifetime PRO (`hodja_pro_lifetime`, `$4.99`):** Non-consumable Play Billing SKU unlocking full catalog ad-free + dynamic store pricing + zero sponsor breaks.
   - **Purchase Security & Restore:** `PlayBillingManager` acknowledges `PURCHASED` orders via `acknowledgePurchase`, reconciles `ITEM_ALREADY_OWNED` and `onStart` / **Restore Purchase** via `queryPurchasesAsync(ProductType.INAPP)`, and stores entitlements in **Android Keystore AES-256-GCM** encrypted prefs (`EntitlementStore`) that scrub legacy plaintext unlock flags and fail closed on tampering. No unverified click-to-unlock bypasses exist in production flows.
   - **AdMob Secrets Configuration:** `ADMOB_APP_ID`, `ADMOB_REWARDED_UNIT_ID`, and `ADMOB_INTERSTITIAL_UNIT_ID` default to Google's official sample test IDs in `.env.example` and are injected into `BuildConfig` + `AndroidManifest.xml` via `secrets-gradle-plugin`. Before uploading to Google Play Console, configure your live AdMob IDs in AI Studio **Secrets** (or `.env`) and create `hodja_pro_lifetime` and `hodja_series_volume1` in Play Console.
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
├── MainActivity.kt                     # Edge-to-edge Compose host, lifecycle observer, Activity lookup & router
├── audio/
│   └── SoundtrackPlayer.kt             # Closed-loop 6-track OGG Vorbis player, AudioFocus & ducking
├── monetization/
│   ├── MonetizationContracts.kt        # SkuCatalog, BillingGateway & AdsGateway interfaces/events
│   ├── PlayBillingManager.kt           # Play Billing 9.1.0 client, acknowledgePurchase & startup restore
│   └── AdMobAdsManager.kt              # Google Mobile Ads Rewarded & Interstitial preloader/presenter
├── security/
│   └── EntitlementStore.kt             # AndroidKeyStore AES-256-GCM encrypted entitlement persistence
├── model/
│   └── ComicModels.kt                  # Immutable domain models, AppLanguage, PanelCrop, UI enums
├── data/
│   └── ComicRepository.kt              # Single Source of Truth (vault JSON, encrypted unlocks, progress)
├── ui/
│   ├── ComicViewModel.kt               # UDF state holder (StateFlow<UiState>), Billing/Ad & reader flows
│   ├── components/
│   │   └── LocalizedComicCover.kt      # 3:4 Eastern cover renderer, cartouche typography & filigree trim
│   ├── parchment/
│   │   └── ParchmentStoryDialog.kt     # Illuminated parchment scroll reader with drop cap & moral card
│   ├── splash/SplashScreen.kt          # Ornate minaret launch screen & instant RU/ES/EN toggle
│   ├── library/
│   │   ├── ComicLibraryScreen.kt       # 50-story catalog host, LazyColumn layout & language scrim
│   │   ├── LibraryHeaderAndBanners.kt  # Top bar, Hybrid Monetization banner, SAF import cards, wisdom card
│   │   ├── LibrarySearchAndFilters.kt  # Search bar, tier/atmosphere FilterChips & empty-state reset card
│   │   ├── LibraryCatalogCards.kt      # Hero featured card & 50-card list items with Coil downsampling
│   │   └── LibraryFilter.kt            # Tier/atmosphere filter model & pure search predicate
│   ├── reader/
│   │   ├── ComicReaderScreen.kt        # Dual-mode reader, direct story switcher, overlays, BackHandler
│   │   ├── ComicPageArtwork.kt         # BitmapRegionDecoder panel cropper, cartouche titles & LruCache(32)
│   │   ├── ComicBubbleOverlay.kt       # Dynamic localized speech/narration balloons fitted to artwork rect
│   │   ├── ZoomableComicImage.kt       # Page-keyed pinch-to-zoom, double-tap & pan gestures
│   │   ├── ThumbnailFilmstripDrawer.kt # Auto-synced bottom thumbnail strip
│   │   └── ComicLoreSheet.kt           # Delegates reader story view to ParchmentStoryDialog
│   ├── sequence/SequenceManagerSheet.kt# Page reordering, natural/reverse sort & pruning
│   ├── monetization/PaywallDialog.kt   # Hybrid Paywall (Store prices, error states) & AdLoadingOverlayDialog
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

### APK Size, Release R8 & Media Separation
- Android bundled assets live in `app/src/main/res/drawable/` (`86` WebP images + `2` adaptive icon XML layers, `23.86 MB` total: ornate Eastern splash art, **8** themed story covers, and 100% unique illustrated sheets for every Volume 1 story **#1–#50** with universal fallback sheets removed), `app/src/main/res/raw/` (`soundtrack_tashkent_01..06.ogg`: **6** silence-trimmed, cross-faded 32 kHz stereo Vorbis loops compressed from `18.64 MB` down to **`5.71 MB`**, ~17.9 min closed cycle), `app/src/main/res/font/` (`el_messiri.ttf`, `philosopher.ttf`, `playfair_display.ttf`, `cormorant_garamond.ttf`, `lora.ttf` — `1.87 MB`, all 5 actively wired in `Type.kt`), and `app/src/main/assets/`.
- `release` buildType configures `isMinifyEnabled = true` and `isShrinkResources = true` with `proguard-rules.pro` (`-keep class com.example.model.**`) for lean Play Console AAB/APK packaging.
- Library thumbnails and featured covers load through Coil `ImageRequest` downsampling (`240×320` and `480×640` px) with `LocalizedComicCover` overlaying the active language title cleanly inside the blank cartouche.
- GitHub documentation media (`banner_top.jpg`, `banner_bottom.jpg`) lives exclusively in `docs/images/` so documentation artwork never inflates the compiled APK.

---

## 5. Planned Release Roadmap (Summary)

| Milestone | Scope | Technical Approach |
| :--- | :--- | :--- |
| **v1.0 (Baseline)** | 25 anecdotes (`>= 4` pages each), `RU/ES/EN`, SAF importer, Hybrid Paywall UX | Bundled vault JSON + 2×2 panel region cropping + local state |
| **v1.0.0 (Plan B Hybrid Store RC)** | **50 curated unique stories (`#1–#50`)**, 8 Eastern covers, Search/Filter, Parchment Scroll, **6-track OGG loop**, **Live Play Billing + AdMob + AES-256-GCM EntitlementStore** | Map-backed bespoke 2×2 artwork (`#11–#50`), `SoundtrackPlayer`, `PlayBillingManager` (`hodja_pro_lifetime`, `hodja_series_volume1`), `AdMobAdsManager`, `EntitlementStore`, R8 release minification |
| **v1.2 (Volume 2)** | Stories `#51–#100` (`docs/nasreddin_volume2_backlog.json`) + Top-10 global languages | Same bespoke 2×2 pipeline + Play Asset Packs / Cloud delivery |
| **v2.0** | 500–1,200+ episodes via seasonal episodic drops | Cloud CDN / Play Feature/Asset packs + automated 20s cliffhanger video teaser funnel |
