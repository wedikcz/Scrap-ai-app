package com.example

import com.example.data.ai.AiChatGateway
import com.example.data.ai.AiChatReply
import com.example.data.ai.AiHttpException
import com.example.data.ai.AiPreset
import com.example.data.ai.AiProbeResult
import com.example.data.ai.AiProtocolException
import com.example.data.ai.AiProvider
import com.example.data.ai.AiSettings
import com.example.data.ai.AiTask
import com.example.data.ai.AiWire
import com.example.data.ai.AssistantMemory
import com.example.data.ai.CitationGuard
import com.example.data.ai.MemoryWindow
import com.example.data.ai.ModelCatalog
import com.example.data.ai.ModelRouter
import com.example.data.ai.RetryPolicy
import com.example.data.model.AiChatMessage
import com.example.data.model.RagDocument
import com.example.data.repository.AiAssistantEngine
import com.example.data.repository.AnswerEngine
import com.example.data.repository.EvidenceChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiCoreTest {

    // ---------- Model routing ----------

    @Test
    fun `routes images to vision model`() {
        val settings = AiSettings.forPreset(AiPreset.GEMINI, "test-key-123456")
        assertEquals(AiTask.VISION, ModelRouter.taskFor("PCB close-up", hasImage = true))
        assertEquals(ModelCatalog.GEMINI_FLASH, ModelRouter.modelFor(settings, AiTask.VISION))
    }

    @Test
    fun `routes firmware questions to the code specialist`() {
        assertEquals(AiTask.CODE, ModelRouter.taskFor("Jak přeflashovat firmware přes esptool?", false))
        val openRouter = AiSettings.forPreset(AiPreset.OPENROUTER, "sk-or-test-123456")
        assertEquals(ModelCatalog.QWEN_CODER, ModelRouter.modelFor(openRouter, AiTask.CODE))
    }

    @Test
    fun `generic question uses chat model`() {
        assertEquals(AiTask.CHAT, ModelRouter.taskFor("Z čeho pochází TP4056?", false))
    }

    @Test
    fun `model overrides win over preset defaults`() {
        val settings = AiSettings(
            preset = AiPreset.GEMINI,
            apiKey = "test-key-123456",
            codeModel = "custom-code-model"
        )
        assertEquals("custom-code-model", ModelRouter.modelFor(settings, AiTask.CODE))
    }

    // ---------- Settings / validation ----------

    @Test
    fun `placeholder keys never count as configured`() {
        assertTrue(AiSettings.looksLikePlaceholder("MY_GEMINI_API_KEY"))
        assertTrue(AiSettings.looksLikePlaceholder("YOUR_KEY_HERE"))
        assertTrue(AiSettings.looksLikePlaceholder(""))
        assertFalse(AiSettings.looksLikePlaceholder("AIzaSyRealLookingKey123"))
        val settings = AiSettings(preset = AiPreset.GEMINI, apiKey = "MY_GEMINI_API_KEY")
        assertFalse(settings.hasKey)
        assertFalse(settings.isConfigured)
        assertEquals("AI NEAKTIVNÍ · chybí API klíč", settings.statusLabel())
    }

    @Test
    fun `remote endpoints require https while loopback may use http`() {
        assertTrue(AiSettings.isEndpointUsable("https://openrouter.ai/api/v1"))
        assertTrue(AiSettings.isEndpointUsable("http://127.0.0.1:11434/v1"))
        assertTrue(AiSettings.isEndpointUsable("http://10.0.2.2:11434/v1"))
        assertFalse(AiSettings.isEndpointUsable("http://192.168.1.50:4000/v1"))
        assertFalse(AiSettings.isEndpointUsable("ftp://example.com"))
        assertFalse(AiSettings.isEndpointUsable(""))
    }

    @Test
    fun `ollama needs no key but does need a usable endpoint`() {
        val ollama = AiSettings.forPreset(AiPreset.OLLAMA)
        assertTrue(ollama.isReady)
        assertTrue(ollama.statusLabel().contains("bez klíče"))
    }

    @Test
    fun `loopback proxy works without a key but remote does not`() {
        val localProxy = AiSettings(preset = AiPreset.CUSTOM, baseUrl = "http://127.0.0.1:4000/v1")
        assertTrue(localProxy.isReady)

        val remote = AiSettings(preset = AiPreset.OPENROUTER, baseUrl = "https://openrouter.ai/api/v1")
        assertFalse(remote.isReady)
        assertEquals("AI NEAKTIVNÍ · chybí API klíč", remote.statusLabel())
    }

    @Test
    fun `masked key never exposes the middle`() {
        val settings = AiSettings(preset = AiPreset.GEMINI, apiKey = "AIzaSyABCDEFGHIJKLMNOP")
        val masked = settings.maskedKey()
        assertFalse(masked.contains("ABCDEFGHIJ"))
        assertTrue(masked.startsWith("AIza"))
    }

    // ---------- Memory window ----------

    @Test
    fun `memory window keeps chronological order and message cap`() {
        val messages = (1..20).map { AiChatMessage(role = "user", content = "msg$it") }
        val window = MemoryWindow.trim(messages, maxMessages = 5, maxChars = 10_000)
        assertEquals(5, window.size)
        assertEquals("msg16", window.first().content)
        assertEquals("msg20", window.last().content)
    }

    @Test
    fun `memory window respects character budget but keeps the newest turn`() {
        val messages = listOf(
            AiChatMessage(role = "user", content = "a".repeat(300)),
            AiChatMessage(role = "assistant", content = "b".repeat(300)),
            AiChatMessage(role = "user", content = "current question")
        )
        val window = MemoryWindow.trim(messages, maxMessages = 10, maxChars = 320)
        assertEquals(1, window.size)
        assertEquals("current question", window.last().content)
    }

    @Test
    fun `single oversized message is never dropped`() {
        val huge = AiChatMessage(role = "user", content = "x".repeat(9_000))
        val window = MemoryWindow.trim(listOf(huge), maxMessages = 10, maxChars = 1_000)
        assertEquals(1, window.size)
    }

    // ---------- Citation guard ----------

    @Test
    fun `valid citation is accepted and canonicalized`() {
        val chunks = evidence()
        val check = CitationGuard.check(
            "Proud nastavuje Rprog. [zdroj: TP4056.txt, cast 1]",
            chunks
        )
        assertTrue(check.isValid)
        assertEquals(listOf("[zdroj: TP4056.txt, část 1]"), check.citations)
        val canonical = CitationGuard.canonicalize(check.answer, chunks)
        assertTrue(canonical.contains("[zdroj: TP4056.txt, část 1]"))
    }

    @Test
    fun `fabricated source invalidates the whole answer`() {
        val check = CitationGuard.check(
            "Něco. [zdroj: FakeDatasheet.pdf, část 1]",
            evidence()
        )
        assertFalse(check.isValid)
        assertTrue(check.rejectedTags.isNotEmpty())
    }

    @Test
    fun `missing citation is rejected`() {
        val check = CitationGuard.check("Proud je 1 A.", evidence())
        assertFalse(check.isValid)
        assertTrue(check.citations.isEmpty())
    }

    @Test
    fun `wrong part number is rejected`() {
        val check = CitationGuard.check("Něco. [zdroj: TP4056.txt, část 9]", evidence())
        assertFalse(check.isValid)
    }

    // ---------- Wire format ----------

    @Test
    fun `gemini text parts are concatenated and tokens read`() {
        val root = JSONObject(
            """{"candidates":[{"content":{"parts":[{"text":"A"},{"text":"B"}]},"finishReason":"STOP"}],
               "usageMetadata":{"totalTokenCount":42}}"""
        )
        assertEquals("A\nB", AiWire.extractTextFromGemini(root))
        assertEquals(42, AiWire.totalTokensFromGemini(root))
    }

    @Test
    fun `gemini blocked response throws protocol error`() {
        val root = JSONObject("""{"promptFeedback":{"blockReason":"SAFETY"},"candidates":[]}""")
        val error = runCatching { AiWire.extractTextFromGemini(root) }.exceptionOrNull()
        assertTrue(error is AiProtocolException)
        assertTrue(error!!.message!!.contains("SAFETY"))
    }

    @Test
    fun `openai content array and usage are parsed`() {
        val root = JSONObject(
            """{"choices":[{"message":{"content":[{"type":"text","text":"hello"},{"type":"text","text":"world"}]}}],
               "usage":{"total_tokens":17}}"""
        )
        assertEquals("hello\nworld", AiWire.extractTextFromOpenAi(root))
        assertEquals(17, AiWire.totalTokensFromOpenAi(root))
    }

    @Test
    fun `json payload is extracted from markdown fences`() {
        val json = AiWire.extractJsonObject("```json\n{\"componentName\":\"NE555\"}\n```")
        assertNotNull(json)
        assertEquals("NE555", json!!.optString("componentName"))
    }

    @Test
    fun `request bodies carry the right envelopes`() {
        val gemini = AiWire.buildGeminiBody("probe", systemPrompt = "sys", imageBase64 = "AAA")
        assertEquals("sys", gemini.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals(2, gemini.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").length())

        val openAi = AiWire.buildOpenAiBody("qwen/qwen3-coder", "probe", systemPrompt = "sys")
        assertEquals("qwen/qwen3-coder", openAi.getString("model"))
        assertEquals(2, openAi.getJSONArray("messages").length())
    }

    // ---------- Retry policy ----------

    @Test
    fun `retryable statuses are limited to transient failures`() {
        assertTrue(RetryPolicy.isRetryable(AiHttpException(429, "rate limit")))
        assertTrue(RetryPolicy.isRetryable(AiHttpException(503, "unavailable")))
        assertTrue(RetryPolicy.isRetryable(java.io.IOException("timeout")))
        assertFalse(RetryPolicy.isRetryable(AiHttpException(401, "unauthorized")))
        assertFalse(RetryPolicy.isRetryable(AiHttpException(400, "bad request")))
        assertFalse(RetryPolicy.isRetryable(AiProtocolException("bad json")))
    }

    @Test
    fun `backoff grows and is capped`() {
        assertEquals(800L, RetryPolicy.backoffMs(0))
        assertEquals(1_600L, RetryPolicy.backoffMs(1))
        assertEquals(3_200L, RetryPolicy.backoffMs(2))
        assertEquals(6_400L, RetryPolicy.backoffMs(3))
        assertEquals(6_400L, RetryPolicy.backoffMs(9))
        assertTrue(RetryPolicy.userMessage(AiHttpException(401, "x")).contains("klíč"))
    }

    // ---------- Grounded assistant ----------

    @Test
    fun `cloud answer is used only with a valid citation`() = runTest {
        val gateway = FakeGateway(
            answer = "Nabíjecí proud nastavuje Rprog na pinu 2. [zdroj: TP4056.txt, část 1]"
        )
        val memory = FakeMemory()
        val result = AiAssistantEngine(gateway, memory).ask("TP4056 proud", documents())

        assertEquals(AnswerEngine.CLOUD_AI, result.engine)
        assertEquals(listOf("[zdroj: TP4056.txt, část 1]"), result.citations)
        assertEquals("test-model", result.aiModel)
        assertTrue(result.answer.contains("Rprog"))
        assertEquals(2, memory.stored.size)
        assertEquals(1, gateway.calls)
    }

    @Test
    fun `fabricated citation is rejected and local extraction returned`() = runTest {
        val gateway = FakeGateway(answer = "Něco. [zdroj: Vymyšlený.pdf, část 4]")
        val result = AiAssistantEngine(gateway, FakeMemory()).ask("TP4056 proud", documents())

        assertEquals(AnswerEngine.LOCAL_RAG, result.engine)
        assertTrue(result.guardNote.contains("Citation guard"))
        assertTrue(result.citations.all { it.contains("TP4056.txt") })
        assertFalse(result.answer.contains("Vymyšlený"))
    }

    @Test
    fun `assistant does not call the gateway without a key`() = runTest {
        val gateway = FakeGateway(answer = "unused", settings = AiSettings())
        val result = AiAssistantEngine(gateway, FakeMemory()).ask("TP4056 proud", documents())

        assertEquals(0, gateway.calls)
        assertEquals(AnswerEngine.LOCAL_RAG, result.engine)
        assertTrue(result.guardNote.contains("Chybí API klíč"))
    }

    @Test
    fun `assistant refuses to invent an answer when the vault has no match`() = runTest {
        val gateway = FakeGateway(answer = "irrelevant")
        val result = AiAssistantEngine(gateway, FakeMemory()).ask("kvantový warp pohon", documents())

        assertEquals(0, gateway.calls)
        assertFalse(result.hasCitations)
        assertTrue(result.answer.contains("Nenalezl jsem"))
    }

    // ---------- helpers ----------

    private fun evidence() = listOf(
        EvidenceChunk(
            docTitle = "TP4056.txt",
            partIndex = 1,
            text = "Nabíjecí proud se nastavuje odporem Rprog na pinu 2. 1.2kΩ = 1000 mA.",
            sourceName = "unit-test"
        )
    )

    private fun documents() = listOf(
        RagDocument(
            id = 1,
            title = "TP4056.txt",
            sourceName = "unit-test",
            content = "Nabíjecí proud se nastavuje odporem Rprog na pinu 2. 1.2kΩ = 1000 mA.\n\n" +
                "Tepelný pad na spodní straně pouzdra musí být připájen k mědi.",
            category = "Power"
        )
    )

    private class FakeGateway(
        private val answer: String,
        private val settings: AiSettings = AiSettings.forPreset(AiPreset.GEMINI, "test-key-123456")
    ) : AiChatGateway {
        var calls = 0

        override fun currentSettings(): AiSettings = settings

        override suspend fun chat(systemPrompt: String, userPrompt: String, task: AiTask): AiChatReply {
            calls++
            return AiChatReply(
                text = answer,
                model = "test-model",
                provider = AiProvider.GEMINI,
                latencyMs = 12,
                totalTokens = 100
            )
        }

        override suspend fun probe(): AiProbeResult = AiProbeResult(true, "ok")
    }

    private class FakeMemory : AssistantMemory {
        val stored = mutableListOf<AiChatMessage>()

        override fun messages(): Flow<List<AiChatMessage>> = flowOf(stored.toList())

        override suspend fun append(role: String, content: String) {
            stored += AiChatMessage(role = role, content = content)
        }

        override suspend fun window(maxMessages: Int, maxChars: Int): List<AiChatMessage> =
            stored.takeLast(maxMessages)

        override suspend fun clear() {
            stored.clear()
        }
    }
}
