package com.example.data.ai

import android.content.Context
import com.example.BuildConfig

/**
 * Persists the AI configuration. The API key itself is never written in plain
 * text: [AiKeyVault] seals it with an Android Keystore AES-GCM key.
 *
 * Keys are stored **per preset**. That is a security boundary, not a convenience:
 * a Gemini key must never be sent to OpenRouter, and an OpenRouter key must never
 * be sent to a self-hosted LiteLLM proxy just because the user switched presets.
 * Switching a preset therefore selects a different sealed key (or none).
 *
 * A key baked into the APK at build time (`.env` -> BuildConfig) is used as a
 * fallback for Gemini development builds. Runtime keys entered in the app always
 * win, which is what makes a distributed APK usable without rebuilding and
 * without shipping a secret in the binary.
 */
class AiSettingsStore(
    context: Context,
    private val vault: AiKeyVault = AiKeyVault()
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Decrypted keys by preset id. Empty string means "no stored key". */
    private val keyCache = mutableMapOf<String, String>()

    @Volatile
    private var cachedBuildKey: String? = null

    fun load(): AiSettings {
        val preset = AiPreset.fromId(prefs.getString(KEY_PRESET, AiPreset.GEMINI.id))
        val storedKey = storedKey(preset)
        val buildKey = if (preset.provider == AiProvider.GEMINI) buildConfigKey() else ""
        val effectiveKey = storedKey.ifBlank { buildKey }
        val source = when {
            storedKey.isNotBlank() -> AiKeySource.APP_STORAGE
            buildKey.isNotBlank() -> AiKeySource.BUILD_CONFIG
            else -> AiKeySource.NONE
        }

        return AiSettings(
            preset = preset,
            apiKey = effectiveKey,
            keySource = source,
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            visionModel = prefs.getString(KEY_VISION_MODEL, "") ?: "",
            chatModel = prefs.getString(KEY_CHAT_MODEL, "") ?: "",
            codeModel = prefs.getString(KEY_CODE_MODEL, "") ?: ""
        )
    }

    /**
     * Persists the non-secret fields and, when [newKey] is non-null, replaces the
     * sealed key for [AiSettings.preset] only. Pass `null` to keep every stored key
     * untouched.
     *
     * @return false when the Keystore refused to encrypt the key; nothing is saved
     *         in that case so the UI can report the failure instead of pretending.
     */
    @Synchronized
    fun save(settings: AiSettings, newKey: String? = null): Boolean {
        val editor = prefs.edit()
            .putString(KEY_PRESET, settings.preset.id)
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_VISION_MODEL, settings.visionModel.trim())
            .putString(KEY_CHAT_MODEL, settings.chatModel.trim())
            .putString(KEY_CODE_MODEL, settings.codeModel.trim())

        if (newKey != null) {
            val prefKey = keyPrefName(settings.preset)
            if (newKey.isBlank()) {
                editor.remove(prefKey)
                keyCache[settings.preset.id] = ""
            } else {
                val sealed = vault.encrypt(newKey.trim()) ?: return false
                editor.putString(prefKey, sealed)
                keyCache[settings.preset.id] = newKey.trim()
            }
        }

        return editor.commit()
    }

    /** Removes the sealed key for exactly one preset; other providers keep theirs. */
    @Synchronized
    fun clearKey(preset: AiPreset): Boolean {
        keyCache[preset.id] = ""
        val editor = prefs.edit().remove(keyPrefName(preset))
        if (preset == AiPreset.GEMINI) {
            // The pre-1.3 single-key format belonged to the default Gemini preset.
            editor.remove(KEY_LEGACY_ENCRYPTED_KEY)
        }
        return editor.commit()
    }

    fun hasStoredKey(preset: AiPreset): Boolean = storedKey(preset).isNotBlank()

    @Synchronized
    private fun storedKey(preset: AiPreset): String {
        keyCache[preset.id]?.let { return it }

        var payload = prefs.getString(keyPrefName(preset), null)
        if (payload == null) {
            payload = migrateLegacyKey(preset)
        }

        val value = vault.decrypt(payload) ?: ""
        keyCache[preset.id] = value
        return value
    }

    /**
     * Version 1.2 kept one sealed key for every preset. We cannot prove which
     * provider it was issued for, so it is assigned to Gemini (the default and
     * the only preset that shipped a build-time key fallback). It is never
     * replayed to a different provider.
     */
    private fun migrateLegacyKey(preset: AiPreset): String? {
        if (preset != AiPreset.GEMINI) return null
        val legacy = prefs.getString(KEY_LEGACY_ENCRYPTED_KEY, null) ?: return null
        prefs.edit()
            .putString(keyPrefName(AiPreset.GEMINI), legacy)
            .remove(KEY_LEGACY_ENCRYPTED_KEY)
            .apply()
        return legacy
    }

    private fun buildConfigKey(): String {
        cachedBuildKey?.let { return it }
        // Direct reference (not reflection) so R8 keeps and inlines the field in
        // the minified release build.
        val value = BuildConfig.GEMINI_API_KEY
        val usable = if (AiSettings.looksLikePlaceholder(value)) "" else value.trim()
        cachedBuildKey = usable
        return usable
    }

    private fun keyPrefName(preset: AiPreset): String = KEY_ENCRYPTED_KEY_PREFIX + preset.id

    companion object {
        private const val PREFS_NAME = "scrapmaster_ai"
        private const val KEY_PRESET = "preset"
        private const val KEY_ENCRYPTED_KEY_PREFIX = "encrypted_api_key_"
        private const val KEY_LEGACY_ENCRYPTED_KEY = "encrypted_api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val KEY_CHAT_MODEL = "chat_model"
        private const val KEY_CODE_MODEL = "code_model"
    }
}
