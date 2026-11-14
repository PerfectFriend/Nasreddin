<p align="center">
  <img src="docs/images/banner_top.jpg" alt="Hodja Nasreddin — Interactive Graphic Novel Reader" width="100%" />
</p>

<h1 align="center">Ходжа / Hodja — Graphic Novel & Parable Reader</h1>

<p align="center">
  <strong>Interactive Android comic & parable reader set in an illuminated Neo-Akşehir mosaic universe (Kotlin · Jetpack Compose · Material 3 · RU/ES/EN).</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Min%20SDK-24%20%7C%20Target%20SDK-36-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK" />
  <img src="https://img.shields.io/badge/Languages-RU%20%7C%20ES%20%7C%20EN-F59E0B?style=for-the-badge" alt="Languages RU ES EN" />
</p>

---

## Documentation Hub (`docs/`)

All technical documentation, architectural invariants, and multi-AI audit reports are kept inside `docs/` to keep the repository root clean:

| Document | Purpose |
| :--- | :--- |
| [**Technical & Product Docs**](docs/DOCUMENTATION.md) | Architecture, module map, `>= 4` pages/story & `BitmapRegionDecoder` invariants, Hybrid Monetization, build & test commands, and roadmap. |
| [**Dev & Cross-AI Audit Log**](docs/DEV_AND_AUDIT_LOG.md) | Chronological development changelog, author self-audit log (Round 0), and live cross-model review tracker (Grok + Reviewer #2). |

---

## Quick Build & Verification

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

<p align="center">
  <img src="docs/images/banner_bottom.jpg" alt="Hodja Nasreddin — The Road Continues" width="100%" />
</p>
