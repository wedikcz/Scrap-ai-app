package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiKeySource
import com.example.data.ai.AiKeyVault
import com.example.data.ai.AiPreset
import com.example.data.ai.AiSettings
import com.example.data.ai.AiSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sealed key store is a security boundary: switching a provider must never
 * replay another provider's key. A fake vault keeps the test off Android Keystore
 * while exercising the real preference layout.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiSettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("scrapmaster_ai", Context.MODE_PRIVATE)

    private class FakeVault : AiKeyVault() {
        override fun encrypt(plaintext: String): String = "sealed:$plaintext"
        override fun decrypt(payload: String?): String? =
            payload?.takeIf { it.startsWith("sealed:") }?.removePrefix("sealed:")
    }

    @Before
    fun resetPrefs() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `keys are isolated per provider preset`() {
        val store = AiSettingsStore(context, FakeVault())

        assertTrue(store.save(AiSettings(preset = AiPreset.GEMINI), "AIza-gemini-key"))
        store.save(store.load().copy(preset = AiPreset.OPENROUTER, baseUrl = "https://openrouter.ai/api/v1"), null)

        val openRouter = store.load()
        assertEquals(AiPreset.OPENROUTER, openRouter.preset)
        assertFalse(openRouter.hasKey)
        assertEquals(AiKeySource.NONE, openRouter.keySource)
        assertFalse(openRouter.isReady)

        assertTrue(store.save(openRouter, "sk-or-openrouter-key"))
        assertEquals("sk-or-openrouter-key", store.load().apiKey)

        store.save(store.load().copy(preset = AiPreset.GEMINI), null)
        assertEquals("AIza-gemini-key", store.load().apiKey)
    }

    @Test
    fun `clearing one preset keeps the other provider key`() {
        val store = AiSettingsStore(context, FakeVault())
        store.save(AiSettings(preset = AiPreset.GEMINI), "AIza-gemini-key")
        store.save(store.load().copy(preset = AiPreset.OPENROUTER, baseUrl = "https://openrouter.ai/api/v1"), "sk-or-key")

        store.clearKey(AiPreset.OPENROUTER)

        store.save(store.load().copy(preset = AiPreset.GEMINI), null)
        assertEquals("AIza-gemini-key", store.load().apiKey)
        store.save(store.load().copy(preset = AiPreset.OPENROUTER), null)
        assertFalse(store.load().hasKey)
    }

    @Test
    fun `legacy single key migrates only to gemini`() {
        prefs.edit().putString("encrypted_api_key", "sealed:legacy-gemini-key").commit()
        val store = AiSettingsStore(context, FakeVault())

        // Default preset is Gemini; the legacy key becomes usable there.
        assertEquals("legacy-gemini-key", store.load().apiKey)
        assertFalse(prefs.contains("encrypted_api_key"))

        // It must not leak to a different provider.
        store.save(store.load().copy(preset = AiPreset.OPENROUTER, baseUrl = "https://openrouter.ai/api/v1"), null)
        assertFalse(store.load().hasKey)
    }
}
