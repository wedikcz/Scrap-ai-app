# ScrapMaster AI — Android app

Production Android app for an e-waste reverse-engineering workshop: AI component
scanner (Gemini multimodal + offline DEMO fallback), salvage inventory with
low-stock alerts, project kanban, verified component pinout catalog, and a local
RAG assistant that answers **only** with citations from documents indexed in the
on-device Room database (anti-hallucination guard).

## Stack

- Kotlin + Jetpack Compose (Material 3), single-activity
- Room (SQLite, on-device persistence, seeded demo data)
- CameraX + runtime camera permission flow
- OkHttp against `generativelanguage.googleapis.com` (model `gemini-3.8-flash`)
- R8 minification + resource shrinking on release builds

## Build

Requirements: JDK 21, Android SDK (platform 36 + 36.1, build-tools 36.0.0).

```bash
./gradlew assembleDebug          # debug build
./gradlew testDebugUnitTest      # unit tests (incl. Robolectric)
./gradlew lintDebug              # Android lint
./gradlew assembleRelease        # signed, minified release APK
```

### Signing

Release signing reads `keystore.properties` at the repo root (gitignored):

```properties
storeFile=my-upload-key.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

CI (GitHub Actions) can instead set `KEYSTORE_PATH`, `STORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD` env vars or decode the keystore from the
`KEYSTORE_BASE64` secret. The release build in CI only runs when the repo
variable `RELEASE_SIGNING=enabled` is set.

### API key

The Gemini key is injected at build time from `.env` (`GEMINI_API_KEY`) via the
Secrets Gradle plugin; `.env.example` documents the format. The key is compiled
into `BuildConfig` and never rendered in UI or logs.

## Verification status

Verified in this workspace (JDK 21 + Android SDK 36.1):

| Check | Result |
|---|---|
| `assembleDebug` | ✅ BUILD SUCCESSFUL |
| `testDebugUnitTest` (11 tests incl. Robolectric) | ✅ all pass |
| `lintDebug` | ✅ 0 errors / 50 warnings (40× dependency-update suggestions, 5× launcher-icon shape, tooling-version notices; all informational) |
| `assembleRelease` (R8 + shrinkResources + signed) | ✅ BUILD SUCCESSFUL, APK 4.3 MB, `apksigner verify` OK |

**Not verifiable in this sandbox (no Android device/emulator):** runtime camera
scanning, real Gemini network calls, on-device DB migrations under UI. The
offline DEMO scanner fallback and the RAG anti-hallucination guard are covered
by unit tests instead.

## Master-prompt fit report

The supplied master prompt targets a **TanStack Start + Convex web stack**
(PWA, Convex actions, WebLLM in the browser). This repository is the **Android**
variant of the same product, so the prompt's stack constraints do not apply
literally; its functional and truthfulness criteria were mapped 1:1 instead:

| Master-prompt concept | Android implementation |
|---|---|
| Convex persistence + validated mutations | Room DAOs; negative-quantity guard in SQL; input validated at repository boundary |
| Privileged AI calls in server actions | Gemini calls in `LiteLlmGateway` / `AiScannerEngine` with key from `BuildConfig`, never in UI layer |
| Two-phase scan + self-reflection | `AiScannerEngine` two-pass analysis with cross-verification |
| Local RAG + mandatory citations `[zdroj: soubor, část N]` | `RagEngine` BM25-style chunk scoring over Room `rag_documents`; refusal path when 0 matches |
| Offline DEMO fallback badge | `isOfflineFallback` / `DEMO-FALLBACK` provider badge |
| "Never claim 100% without proof" | table above; untestable-in-sandbox items listed explicitly |
