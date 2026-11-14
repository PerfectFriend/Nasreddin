<p align="center">
  <img src="docs/images/banner_top.jpg" alt="Hodja Nasreddin — Interactive Graphic Novel Reader" width="100%" />
</p>

<h1 align="center">Ходжа / Hodja — Graphic Novel & Parable Reader</h1>

<p align="center">
  <strong>Interactive Android comic reader bringing the timeless wit of Hodja Nasreddin to life in an illuminated Eastern mosaic universe.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Min%20SDK-24%20%7C%20Target%20SDK-36-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK" />
  <img src="https://img.shields.io/badge/Languages-RU%20%7C%20ES%20%7C%20EN-F59E0B?style=for-the-badge" alt="Languages RU ES EN" />
</p>

---

## Overview

**Hodja (`Ходжа`)** is an Android Kotlin & Jetpack Compose comic book and folklore parable reader built around curated visual storytelling, low-memory panel decoding, multi-language localization, and a **Hybrid Monetization** engine.

> **О проекте:** Интерактивная полноэкранная читалка комиксов и притч о Ходже Насреддине на Kotlin + Jetpack Compose (M3). Коллекция включает 25 иллюстрированных историй (минимум 4 страницы в каждой), поддержку русского, испанского и английского языков, импорт пользовательских папок и гибридную модель доступа (бесплатные эпизоды, открытие партиями за просмотр рекламы, покупка серии без рекламы или пожизненный PRO-доступ).

---

## Key Features

- **Curated Parable Vault (`25+` Stories, `>= 4` Pages Each)**
  - **Episode #1:** 4 bespoke fullscreen comic illustrations localized per language.
  - **Episodes #2–#10:** Localized 2×2 comic sheets dynamically sliced into **4 individual fullscreen panel pages** via `BitmapRegionDecoder` + a bounded `LruCache(32)` to avoid 4× memory overhead and keep APK size compact.
  - **Episodes #11–#25:** Full 4-page sequences backed by bundled narrative & moral scripts (roadmap: expanding to **100+ stories** per release with universal speech-bubble templates and dynamic text rendering).
- **Strict Multi-Language Support (`RU` / `ES` / `EN`)**
  - Automatic OS locale detection on first launch with instant switching across splash, catalog cards, reader overlays, paywall dialogs, and system notifications.
- **Fullscreen Dual-Mode Reader**
  - Seamless switching between **Horizontal Pager** and **Vertical Webtoon Scroll**, with synchronized reading progress, page-keyed pinch-to-zoom/pan, collapsible **Thumbnail Filmstrip**, **Direct Story-to-Story Switcher**, and **Lore & Dialogue Sheet**.
- **Hybrid Monetization Funnel**
  - **Stories 1–10:** 100% Free.
  - **Stories 11+:** Choice between **Watch Rewarded Ad** (`+5` stories batch with periodic sponsor breaks every 4 ad-supported reads), **Ad-Free Series Pack (`$0.99`)**, or **Lifetime PRO (`$4.99`)**.
- **Custom Folder & Image Importer (SAF)**
  - Import local comic folders or multiple images with persistable URI permissions and **natural alphanumeric sorting** (`page2` before `page10`) plus an interactive **Page Sequence Manager**.

---

## Architecture & Module Map

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge Compose host & root dialog/router scope
├── model/
│   └── ComicModels.kt               # Immutable domain entities, AppLanguage, PanelCrop, UI enums
├── data/
│   └── ComicRepository.kt           # Single Source of Truth (vault JSON, prefs, unlocks, progress)
├── ui/
│   ├── ComicViewModel.kt            # UDF state holder (StateFlow<UiState>) & ad-frequency rules
│   ├── splash/SplashScreen.kt       # Minaret hero launch screen & instant language toggle
│   ├── library/ComicLibraryScreen.kt# Catalog, SAF importers, hybrid status banner, language overlay
│   ├── reader/
│   │   ├── ComicReaderScreen.kt     # Dual-mode reader, story picker, in-window overlays, BackHandler
│   │   ├── ComicPageArtwork.kt      # BitmapRegionDecoder panel cropper & bounded LruCache(32)
│   │   ├── ZoomableComicImage.kt    # Page-keyed pinch-to-zoom, double-tap & pan gestures
│   │   ├── ThumbnailFilmstripDrawer.kt # Auto-synced bottom thumbnail strip
│   │   └── ComicLoreSheet.kt        # ModalBottomSheet for narrative, dialogue & Eastern wisdom
│   ├── sequence/SequenceManagerSheet.kt # Page reordering, natural/reverse sort & pruning
│   ├── monetization/PaywallDialog.kt    # Hybrid Paywall & Rewarded/Interstitial Ad slot overlays
│   └── theme/                       # Material 3 Midnight Gold & Cyan color system & typography
└── util/
    ├── AppStrings.kt                # Centralized RU / ES / EN localization dictionary
    ├── FolderImporter.kt            # Storage Access Framework importer with persistable URI access
    └── AlphanumericComparator.kt    # Overflow-safe natural numeric string comparator
```

---

## Build & Verification

Standard Gradle commands from the workspace root:

```bash
# Compile debug APK
gradle :app:assembleDebug

# Run all JVM & Robolectric unit tests
gradle :app:testDebugUnitTest
```

### Automated Robolectric Test Coverage
- Bundled vault integrity (`25` stories: `10` free + `15` premium; every story strictly `>= 4` pages).
- Canonical Hodja reference asset & localized drawable consistency across `RU`, `ES`, and `EN`.
- Direct story-to-story reader navigation & Hybrid Ad (`+5` batch) -> Series Pack -> PRO unlock flow.
- Strict localized `Snackbar` verification across language switches.

---

## Engineering & Multi-AI Audit Workflow

This repository follows a structured **Author + Multi-Model Peer Audit** workflow:

1. **Single Author Implementation:** Core features, bug fixes, and architectural KDoc are authored and verified via local compilation and Robolectric test runs before every push.
2. **Cross-Model Code Audit:** Reviewer AI models inspect diffs for edge cases, Compose lifecycle bugs (e.g., `sheetState.hide()` teardown, in-window `AnimatedVisibility` vs `PopupWindow` `BLASTBufferQueue` stability), localization leaks, and memory safety.
3. **Code Hygiene Standards:**
   - Every public screen, cache, and state boundary includes concise KDoc explaining **why**, not just **what**.
   - Zero silent exception catches (`catch` blocks always log diagnostics or document explicit fallbacks).
   - Strict separation between APK drawable resources (`app/src/main/res/drawable/`) and repository documentation media (`docs/images/`).

---

<p align="center">
  <img src="docs/images/banner_bottom.jpg" alt="Hodja Nasreddin — The Road Continues" width="100%" />
</p>
