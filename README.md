# ScrapMaster AI — Android app

Production Android app for an e-waste reverse-engineering workshop: AI component
scanner (Gemini multimodal + offline DEMO fallback), salvage inventory with
low-stock alerts, project kanban, verified component pinout catalog with **JSON
schema import**, and a local RAG assistant with a **Smart-Mixture-of-Hackers**
layer (four expert perspectives) that answers **only** with citations from
documents indexed in the on-device Room database (anti-hallucination guard).

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
| `testDebugUnitTest` (21 tests incl. Robolectric) | ✅ all pass |
| `lintDebug` | ✅ 0 errors / 32 warnings (dependency-update suggestions, launcher-icon shape, tooling-version notices; all informational) |
| `assembleRelease` (R8 + shrinkResources + signed) | ✅ BUILD SUCCESSFUL, APK 4.2 MB (`versionCode 2`, `versionName 1.1`), `apksigner verify` OK |

**Not verifiable in this sandbox (no Android device/emulator):** runtime camera
scanning, real Gemini network calls, on-device DB migrations under UI. The
offline DEMO scanner fallback, the JSON importer and the RAG anti-hallucination
guard are covered by unit tests instead.

Known limitations:

- Light mode switches the Material palette, top bar and system bars; the screen
  canvases stay dark because the industrial dark theme is part of the product
  design. This has not been visually reviewed on a device.
- The distributed APK is built without a Gemini key (DEMO scanner fallback).
- `mapping.txt` from R8 is not uploaded by CI yet, so release crash reports are
  not deobfuscatable.

## Prebuilt APK

`artifacts/ScrapMaster-AI-v1.1-release.apk` — signed, minified, `versionCode 2`,
`minSdk 24`, `targetSdk 36`, ABIs `arm64-v8a` (native for Infinix Hot 50 Pro+
X6880), `armeabi-v7a`, `x86`, `x86_64`.

## Pinout JSON import

The pinout catalog accepts bulk component definitions from a JSON schema. Open
**Pinouty → Import JSON**, paste the schema or load a `.json` file, and press
**Importovat**. Every component is validated on its own; invalid rows are
rejected with a reason instead of being silently dropped.

```json
{
  "schemaVersion": 1,
  "components": [
    {
      "id": "lm7805-to220",
      "name": "LM7805 Linear Regulator",
      "chipCode": "LM7805",
      "packageType": "TO-220",
      "pinCount": 3,
      "voltageRange": "7.5V - 35V input / 5V output",
      "description": "Fixed 5V linear regulator, 1.5A with heatsink.",
      "pins": [
        { "number": 1, "name": "IN", "type": "power" },
        { "number": 2, "name": "GND", "type": "ground" },
        { "number": 3, "name": "OUT", "type": "power" }
      ],
      "hackerTip": "Salvaged from router PSUs; test dropout under load before reuse.",
      "datasheetUrl": "https://www.ti.com/lit/ds/symlink/lm7805.pdf"
    }
  ]
}
```

Rules enforced by `PinoutJsonImporter`: required `name`, `chipCode`,
`packageType`, `pinCount` (1–256) and `pins` with unique numbers in that range;
`pins` must contain exactly `pinCount` entries; duplicate ids inside one batch are
rejected; imported rows default to `UNKNOWN` verification and a `VERIFIED` claim
without any datasheet link is downgraded to `PROBABLE` (anti-hallucination rule).
The ASCII pin map and pin function list are generated from `pins`.

## Smart-Mixture-of-Hackers

`HackerSwarmEngine` runs four specialists over the retrieved RAG chunks:
Silicon Analyst (pins, packages, voltages), Circuit Hacker (passives, currents),
Firmware Engineer (UART/I2C/SPI, boot straps, flashing) and Cost/Scrap Optimizer
(safety, yield, value). Each perspective quotes only the passages it was given,
carries their `[zdroj: soubor, část N]` citations, and reports `0 %` with an
explicit refusal when its domain has no documentary support. The consensus line
states how many of the four perspectives are grounded.

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
| `convex/schema.ts` + validated mutations + JSON import | Room entities with DAO guards; `PinoutJsonImporter` validates per-component JSON schemas before insert |
| Smart-Mixture-of-Hackers (4 agents) | `HackerSwarmEngine`: four grounded perspectives, per-expert confidence, consensus and gap reporting |
| Dark/light theme toggle with persistence | Toggle in the top bar; theme + language persist in `SharedPreferences` (`scrapmaster_prefs`). System bars, top bar and Material surfaces follow the theme; the workshop canvas keeps its dark industrial styling by design (see limitations) |
| Offline DEMO fallback badge | `isOfflineFallback` / `DEMO-FALLBACK` provider badge |
| "Never claim 100% without proof" | table above; untestable-in-sandbox items listed explicitly |
