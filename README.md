# ScrapMaster AI — Android app

Production Android app for an e-waste reverse-engineering workshop: AI component
scanner (Gemini / Qwen vision), salvage inventory with low-stock alerts, project
kanban, verified component pinout catalog with **JSON schema import**, and a RAG
assistant that answers **full AI** only from documents indexed on the device —
every claim carries a `[zdroj: soubor, část N]` citation that is verified against
the retrieved chunks.

There is **no demo mode**: the app never replays a hard-coded component sheet and
never pretends a cloud call succeeded. Without a provider key the scanner stays
disabled and the assistant uses the deterministic, cited local extraction.

## Stack

- Kotlin + Jetpack Compose (Material 3), single-activity
- Room (SQLite, on-device persistence, seeded sample data, migration 2 → 3)
- CameraX + runtime camera permission flow
- OkHttp gateway with two protocols: Google Generative Language and
  OpenAI-compatible `/chat/completions`
- Android Keystore (AES-256-GCM) for the API key
- R8 minification + resource shrinking on release builds

## Full AI — how to enable it

The installed APK can be switched to full AI **without rebuilding**:

1. Open the gear icon in the top bar (**Nastavení AI**, route `/ai`).
2. Pick a provider preset.
3. Paste the API key, press **Uložit**, then **Otestovat**.
4. The status changes from `AI NEAKTIVNÍ · chybí API klíč` to `FULL AI · <model>`.

The scanner button stays disabled until step 4 succeeds, and the telemetry screen
has the same real connection test. Nothing is pre-answered in the meantime.

| Preset | Protocol | Default models (Oct 2026) | Key |
|---|---|---|---|
| Google Gemini | `generativelanguage.googleapis.com` | `gemini-3.8-flash` (vision + chat + code) | `AIza…` from aistudio.google.com/apikey |
| OpenRouter | OpenAI-compatible | vision `qwen/qwen3-vl-235b-a22b-instruct`, chat/code `qwen/qwen3-coder` | `sk-or-v1-…` from openrouter.ai/keys |
| Ollama in the phone (Termux) | OpenAI-compatible | vision `llava:7b`, chat/code `qwen2.5-coder:7b` | none |
| Custom LiteLLM proxy | OpenAI-compatible | user-defined | proxy master key |

Every model id and Base URL is editable, so a self-hosted LiteLLM / vLLM / Groq
endpoint works through the same gateway. Remote endpoints must be HTTPS; plain
HTTP is allowed only for loopback (`127.0.0.1`, `10.0.2.2`), enforced both in the
settings validator and in `network_security_config.xml`.

**Key handling.** The key is encrypted with a hardware-backed Android Keystore
key (AES-256-GCM) before it touches `SharedPreferences`, and **per provider
preset** — switching from Gemini to OpenRouter selects a different sealed key
(or none), so one provider's key is never sent to another. It is excluded from
cloud backup and device transfer. The APK itself contains no key; the optional
`.env` → `BuildConfig.GEMINI_API_KEY` path is only a development convenience and
is explicitly not recommended for public builds. A missing key, an air-gapped
mode and an exhausted retry budget are all surfaced as errors — there is no
fabricated analysis to fall back to.

**Routing.** `ModelRouter` sends images to the vision model, firmware / register /
flashing questions to the code model, and everything else to the chat model.
Retries happen only for transient failures (HTTP 408, 429, 5xx, I/O), with
800 ms → 1.6 s → 3.2 s backoff (capped at 8 s). 401/403/400 fail immediately with
an actionable message instead of being retried.

**Self-repair.** If the model answers the component scan with prose, markdown or
an object without `componentName`, the gateway sends that broken answer back
with the JSON contract and asks for a repair (at most two passes). A successful
repair is counted in `healedRequests` and in the audit log; when it still fails,
the scan returns a readable error. This is the honest part of the Panus/Hermes
"self-healing" loop: a schema repair, not code execution.

**Telemetry.** Request counts, success/failure, healed incidents, tokens and
latency are real values from `usageMetadata` / `usage.total_tokens`, no seeded
numbers. The gateway screen computes availability from actual requests.

## Panus-model.py → Hermes integration

The attached `Panus-model.py` is **not a model**: it contains no weights and no
inference engine. It is a 6.0 "Hermes-Titan" Python script whose `process_turn`
returns a fixed string; its `Qwen/Qwen2.5-Coder-7B` name is only a label. Shipping
it as-is into an Android APK would add a dead Python runtime and fake "AI".

Its architecture was still useful, so it was ported where it has real value:

| Panus / Hermes concept | Android implementation | Why |
|---|---|---|
| `HermesMemoryManager` (SQLite history + last N messages) | `HermesMemory` + Room table `ai_chat_messages`, `MemoryWindow` with a 10-message / 4000-char sliding budget | Genuine benefit: the assistant keeps context across restarts without unbounded prompt growth. Newest question is never dropped. |
| Exponential retry / "self-healing" | `RetryPolicy` + gateway retry loop with capped backoff, real 429/5xx classification and audit log | Useful and testable; unlike the original it does not pretend a failed call succeeded. |
| `Qwen2.5-Coder-7B` model label | Code-specialist route: `qwen/qwen3-coder` via OpenRouter or `qwen2.5-coder:7b` via Ollama | Turns a hard-coded string into an actually selectable model that matches the task. |
| Dual CLI / FastAPI interface | In-app terminal (`gateway`, `ai`, `rag`, `scan`) + MCP/Termux screen | Keeps the operator workflow without adding a Python server to the phone. |
| `SelfHealingEngine` retry/patch loop | `RetryPolicy` for transport plus a **model-driven JSON contract repair** (`callAnalysisWithRepair`) | Keeps the self-repair idea where it is safe and useful, without executing generated code. |
| `subprocess` + `_patch_code` "self-healing execution" | **Rejected** | Executing model-generated code on a phone is remote code execution by design; Android also has no Python interpreter. |
| `TitaniumObfuscator` (base85) | **Rejected**, replaced by Keystore AES-GCM | Base85 is encoding, not encryption; it would give a false sense of security. |

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

CI can instead set `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD` env vars or decode the keystore from the `KEYSTORE_BASE64` secret.
The release job only runs when the repo variable `RELEASE_SIGNING=enabled` is set.
CI needs no AI key.

### CI

`.github/workflows/android.yml` runs lint, unit tests and debug assemble on every
push/PR to `main`, then uploads the debug APK; the signed release step is optional
(repo variable `RELEASE_SIGNING=enabled` plus keystore secrets). Actions are on
their current majors (`checkout@v7`, `setup-java@v6`, `setup-android@v4`,
`setup-gradle@v6`, `upload-artifact@v7`), and the workflow installs
`platforms;android-36.1` + `build-tools;36.0.0` explicitly so a runner-image
update cannot silently change the toolchain. Runs use
`--no-configuration-cache` for reproducible CI builds; fork PRs skip the signing
steps because they do not receive repository secrets.

## Verification status

Verified in this workspace (JDK 21 + Android SDK 36.1):

| Check | Result |
|---|---|
| `compileDebugKotlin` | ✅ BUILD SUCCESSFUL |
| `testDebugUnitTest` (57 tests incl. Robolectric) | ✅ 0 failures |
| `lintDebug` | ✅ 0 errors / 56 informational warnings |
| `assembleDebug` | ✅ BUILD SUCCESSFUL |
| `assembleRelease` (R8 + shrinkResources + signed) | ✅ APK 4.2 MB, `versionCode 4`, `versionName 1.3`, `apksigner verify` OK |
| Gateway integration (loopback HTTP: 200, 429 retry, 401 no-retry, JSON analysis, no-key refusal, air-gapped refusal, JSON repair) | ✅ 7 tests |

**Not verifiable in this sandbox (no Android device/emulator):** runtime camera
scanning, real Gemini/OpenRouter/Ollama network calls, Keystore encryption on
real hardware, the Room 2 → 3 migration under the UI, and visual theme review.
The migration DDL is byte-identical to the Room-generated schema, the gateway
envelopes, routing, memory window, citation guard and retry policy are covered by
unit tests.

Known limitations:

- Light mode switches the Material palette, top bar and system bars; the screen
  canvases stay dark because the industrial dark theme is part of the product
  design. This has not been visually reviewed on a device.
- Full AI requires a provider key or a local Ollama instance. Without one the
  scanner stays disabled (no demo data) and the assistant uses offline RAG
  extraction with citations.
- `mapping.txt` from R8 is not uploaded by CI yet, so release crash reports are
  not deobfuscatable.

## Prebuilt APK

`artifacts/ScrapMaster-AI-v1.3-release.apk` — signed (APK Signature Scheme v2),
minified, `versionCode 4`, `versionName 1.3`, `minSdk 24`, `targetSdk 36`, native
ABIs `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (packaged AndroidX native
helpers). It contains no API key: the first launch is full-AI-ready but inactive
until a key is entered (or Ollama is reachable on loopback).

SHA-256: `c670c0bb635f05e8c3a6d8030d1b0a4ea5f246eca85a8ee119ab7b6504da077a`

Signing certificate SHA-256: `1eba095ea9fea6f511c499e835f5fb85093b49621a60cb46de12ff6175d00f5a`
(same key as 1.0/1.1, so it installs over the previous version).

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

For cloud answers the same chunks are placed in `<EVIDENCE>` and the model is
forbidden to use anything else. `CitationGuard` parses every returned tag; a
single fabricated file name or part number rejects the complete answer and the
deterministic local extraction is returned instead.

## Master-prompt fit report

The supplied master prompt targets a **TanStack Start + Convex web stack**
(PWA, Convex actions, WebLLM in the browser). This repository is the **Android**
variant of the same product, so the prompt's stack constraints do not apply
literally; its functional and truthfulness criteria were mapped 1:1 instead:

| Master-prompt concept | Android implementation |
|---|---|
| Convex persistence + validated mutations | Room DAOs; negative-quantity guard in SQL; input validated at repository boundary |
| Privileged AI calls in server actions | All model calls go through `LiteLlmGateway`; the key is Keystore-sealed and never rendered in UI or logs |
| LiteLLM Gateway All-in-One | Two wire protocols, four presets, per-task model routing, retry/backoff, token and latency telemetry |
| Two-phase scan + self-reflection | Dual-pass prompt in `LiteLlmGateway`; JSON contract repair on a broken answer; no canned fallback |
| Local RAG + mandatory citations `[zdroj: soubor, část N]` | `RagEngine` BM25-style scoring over Room `rag_documents`, then `CitationGuard` validates model citations |
| `convex/schema.ts` + validated mutations + JSON import | Room entities with DAO guards; `PinoutJsonImporter` validates per-component JSON schemas before insert |
| Smart-Mixture-of-Hackers (4 agents) | `HackerSwarmEngine`: four grounded perspectives, per-expert confidence, consensus and gap reporting |
| Dark/light theme toggle with persistence | Toggle in the top bar; theme + language persist in `SharedPreferences`. The workshop canvas keeps its dark industrial styling by design (see limitations) |
| No demo mode | Missing key, air-gapped mode and exhausted retries are errors; the scanner never shows a fabricated result |
| Hermes memory / self-healing | Ported as `HermesMemory`, `RetryPolicy` and JSON self-repair; unsafe Python execution and base85 "obfuscation" intentionally rejected |
| "Never claim 100% without proof" | table above; untestable-in-sandbox items listed explicitly |
