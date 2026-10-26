package com.example.data.ai

/**
 * Transport family of the upstream AI endpoint. Both families speak JSON over
 * HTTPS; they differ only in the request/response envelope.
 */
enum class AiProvider {
    /** Google Generative Language API (generativelanguage.googleapis.com). */
    GEMINI,

    /** Any OpenAI-compatible `/chat/completions` endpoint: OpenRouter, LiteLLM proxy, Ollama, vLLM. */
    OPENAI_COMPATIBLE
}

/**
 * Ready-made endpoint + model combinations. The user can edit every field after
 * picking a preset, so a self-hosted LiteLLM proxy is just [CUSTOM] with a base URL.
 */
enum class AiPreset(
    val id: String,
    val label: String,
    val provider: AiProvider,
    val defaultBaseUrl: String,
    val defaultVisionModel: String,
    val defaultChatModel: String,
    val defaultCodeModel: String,
    val keyHint: String
) {
    GEMINI(
        id = "gemini",
        label = "Google Gemini",
        provider = AiProvider.GEMINI,
        defaultBaseUrl = "",
        defaultVisionModel = ModelCatalog.GEMINI_FLASH,
        defaultChatModel = ModelCatalog.GEMINI_FLASH,
        defaultCodeModel = ModelCatalog.GEMINI_FLASH,
        keyHint = "AIza… klíč z aistudio.google.com/apikey"
    ),
    OPENROUTER(
        id = "openrouter",
        label = "OpenRouter (Qwen)",
        provider = AiProvider.OPENAI_COMPATIBLE,
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultVisionModel = ModelCatalog.QWEN_VL,
        defaultChatModel = ModelCatalog.QWEN_CODER,
        defaultCodeModel = ModelCatalog.QWEN_CODER,
        keyHint = "sk-or-v1-… klíč z openrouter.ai/keys"
    ),
    OLLAMA(
        id = "ollama",
        label = "Ollama (v zařízení)",
        provider = AiProvider.OPENAI_COMPATIBLE,
        defaultBaseUrl = "http://127.0.0.1:11434/v1",
        defaultVisionModel = ModelCatalog.OLLAMA_VISION,
        defaultChatModel = ModelCatalog.OLLAMA_CODER,
        defaultCodeModel = ModelCatalog.OLLAMA_CODER,
        keyHint = "Ollama klíč nevyžaduje – nech pole prázdné"
    ),
    CUSTOM(
        id = "custom",
        label = "Vlastní LiteLLM",
        provider = AiProvider.OPENAI_COMPATIBLE,
        defaultBaseUrl = "https://",
        defaultVisionModel = ModelCatalog.QWEN_VL,
        defaultChatModel = ModelCatalog.QWEN_CODER,
        defaultCodeModel = ModelCatalog.QWEN_CODER,
        keyHint = "Master key tvého LiteLLM proxy"
    );

    companion object {
        fun fromId(id: String?): AiPreset = entries.firstOrNull { it.id == id } ?: GEMINI
    }
}

/** Model ids verified against the live provider catalogues (October 2026). */
object ModelCatalog {
    const val GEMINI_FLASH = "gemini-3.8-flash"
    const val GEMINI_LITE = "gemini-3.5-flash-lite"
    const val GEMINI_ARGON = "gemini-4-argon"
    const val QWEN_CODER = "qwen/qwen3-coder"
    const val QWEN_CODER_NEXT = "qwen/qwen3-coder-next"
    const val QWEN_VL = "qwen/qwen3-vl-235b-a22b-instruct"
    const val OLLAMA_CODER = "qwen2.5-coder:7b"
    const val OLLAMA_VISION = "llava:7b"

    val geminiSuggestions = listOf(GEMINI_FLASH, GEMINI_LITE, GEMINI_ARGON)
    val openAiSuggestions = listOf(QWEN_CODER, QWEN_CODER_NEXT, QWEN_VL)
    val ollamaSuggestions = listOf(OLLAMA_CODER, OLLAMA_VISION, "qwen3:8b")

    fun suggestionsFor(preset: AiPreset): List<String> = when (preset) {
        AiPreset.GEMINI -> geminiSuggestions
        AiPreset.OPENROUTER -> openAiSuggestions
        AiPreset.OLLAMA -> ollamaSuggestions
        AiPreset.CUSTOM -> openAiSuggestions
    }
}

/** Where the currently effective key came from, so the UI never lies about it. */
enum class AiKeySource { NONE, APP_STORAGE, BUILD_CONFIG }

/**
 * Complete runtime AI configuration. Empty model/base-url fields fall back to the
 * preset default, so a preset change is one tap and advanced users can still
 * override any single value.
 */
data class AiSettings(
    val preset: AiPreset = AiPreset.GEMINI,
    val apiKey: String = "",
    val keySource: AiKeySource = AiKeySource.NONE,
    val baseUrl: String = "",
    val visionModel: String = "",
    val chatModel: String = "",
    val codeModel: String = ""
) {
    val provider: AiProvider get() = preset.provider

    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { preset.defaultBaseUrl }.trimEnd('/')

    val effectiveVisionModel: String
        get() = visionModel.ifBlank { preset.defaultVisionModel }

    val effectiveChatModel: String
        get() = chatModel.ifBlank { preset.defaultChatModel }

    val effectiveCodeModel: String
        get() = codeModel.ifBlank { effectiveChatModel }

    val hasKey: Boolean get() = apiKey.isNotBlank() && !looksLikePlaceholder(apiKey)

    /** Full AI is available only when a real key and a reachable endpoint are configured. */
    val isConfigured: Boolean
        get() = hasKey && (provider == AiProvider.GEMINI || isEndpointUsable(effectiveBaseUrl))

    /** Ollama on the phone itself needs no key; everything else does. Treat it as configured. */
    val isLocalEndpoint: Boolean get() = isLoopbackUrl(effectiveBaseUrl)

    val isReady: Boolean
        get() = when (provider) {
            // Gemini always talks to Google; a key is the only prerequisite.
            AiProvider.GEMINI -> hasKey
            // A local LiteLLM/Ollama proxy commonly runs without auth; remote
            // OpenAI-compatible endpoints still require a key.
            AiProvider.OPENAI_COMPATIBLE -> isEndpointUsable(effectiveBaseUrl) && (hasKey || isLocalEndpoint)
        }

    fun modelFor(task: AiTask): String = when (task) {
        AiTask.VISION -> effectiveVisionModel
        AiTask.CHAT -> effectiveChatModel
        AiTask.CODE -> effectiveCodeModel
    }

    fun maskedKey(): String {
        if (!hasKey) return ""
        val visible = 4
        return if (apiKey.length <= visible * 2) "•".repeat(apiKey.length)
        else apiKey.take(visible) + "…" + apiKey.takeLast(visible)
    }

    fun statusLabel(): String = when {
        isReady && keySource == AiKeySource.NONE -> "FULL AI · ${effectiveChatModel} (bez klíče)"
        isReady -> "FULL AI · ${effectiveChatModel}"
        hasKey -> "AI · neúplná konfigurace"
        else -> "AI NEAKTIVNÍ · chybí API klíč"
    }

    /** Human readable validation, or null when the settings can be used. */
    fun validationError(): String? {
        if (provider == AiProvider.OPENAI_COMPATIBLE && !isEndpointUsable(effectiveBaseUrl)) {
            return "Base URL musí začínat https:// (nebo http://127.0.0.1 pro Ollama)."
        }
        if (!isReady && !hasKey) {
            return "Vlož API klíč poskytovatele. Bez klíče běží pouze offline RAG asistent, skener zůstane vypnutý."
        }
        if (effectiveChatModel.isBlank()) return "Model pro chat nesmí být prázdný."
        if (effectiveVisionModel.isBlank()) return "Vision model nesmí být prázdný."
        return null
    }

    companion object {
        private val LOOPBACK_HOSTS = setOf("127.0.0.1", "localhost", "::1", "10.0.2.2")

        fun looksLikePlaceholder(value: String): Boolean {
            val v = value.trim().uppercase()
            return v.isBlank() ||
                v == "MY_GEMINI_API_KEY" ||
                v.contains("PLACEHOLDER") ||
                v.startsWith("YOUR_") ||
                v.startsWith("<") ||
                v == "NULL"
        }

        fun isLoopbackUrl(url: String): Boolean {
            val host = runCatching { java.net.URI(url.trim()).host }.getOrNull() ?: return false
            return host.lowercase() in LOOPBACK_HOSTS
        }

        /**
         * HTTPS is required for remote endpoints: an API key must never travel in
         * cleartext. Plain HTTP is tolerated only on loopback / emulator host.
         */
        fun isEndpointUsable(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return false
            if (trimmed.startsWith("https://")) {
                return runCatching { java.net.URI(trimmed).host }.getOrNull()?.isNotBlank() == true
            }
            if (trimmed.startsWith("http://")) return isLoopbackUrl(trimmed)
            return false
        }

        fun forPreset(preset: AiPreset, apiKey: String = ""): AiSettings = AiSettings(
            preset = preset,
            apiKey = apiKey,
            keySource = if (apiKey.isBlank()) AiKeySource.NONE else AiKeySource.APP_STORAGE
        )
    }
}

/** What the gateway is being asked to do; selects the model route. */
enum class AiTask { VISION, CHAT, CODE }
