package com.example

import com.example.data.ai.AiNotConfiguredException
import com.example.data.ai.AiPreset
import com.example.data.ai.AiProvider
import com.example.data.ai.AiSettings
import com.example.data.gateway.LiteLlmGateway
import com.example.data.model.NetworkMode
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * Exercises the real HTTP plumbing of the gateway against a loopback server:
 * request envelope, response parsing, retry classification and telemetry.
 * No provider key or internet access is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GatewayIntegrationTest {

    private lateinit var server: HttpServer
    private val requestCount = AtomicInteger()
    private val receivedBodies = mutableListOf<String>()

    @Volatile
    private var responses: MutableList<Pair<Int, String>> = mutableListOf()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            requestCount.incrementAndGet()
            receivedBodies += exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val (code, body) = synchronized(responses) {
                if (responses.size > 1) responses.removeAt(0) else responses.first()
            }
            respond(exchange, code, body)
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `openai compatible chat returns parsed text and real telemetry`() = runTest {
        responses = mutableListOf(
            200 to """{"choices":[{"message":{"content":"OK"}}],"usage":{"total_tokens":33}}"""
        )
        val gateway = gateway()

        val reply = gateway.chat("system prompt", "user prompt")

        assertEquals("OK", reply.text)
        assertEquals(AiProvider.OPENAI_COMPATIBLE, reply.provider)
        assertEquals("qwen/qwen3-coder", reply.model)
        assertEquals(33, reply.totalTokens)
        assertEquals(1, requestCount.get())

        val telemetry = gateway.telemetry.value
        assertEquals(1, telemetry.totalRequests)
        assertEquals(1, telemetry.successfulRequests)
        assertEquals(33L, telemetry.totalTokens)
        val sent = org.json.JSONObject(receivedBodies.first())
        assertEquals("qwen/qwen3-coder", sent.getString("model"))
        assertEquals(2, sent.getJSONArray("messages").length())
    }

    @Test
    fun `429 is retried and then succeeds`() = runTest {
        responses = mutableListOf(
            429 to """{"error":{"message":"rate limited"}}""",
            200 to """{"choices":[{"message":{"content":"recovered"}}],"usage":{"total_tokens":5}}"""
        )
        val gateway = gateway()

        val reply = gateway.chat("system", "user")

        assertEquals("recovered", reply.text)
        assertEquals(2, requestCount.get())
        assertEquals(1, gateway.telemetry.value.successfulRequests)
    }

    @Test
    fun `401 is not retried`() = runTest {
        responses = mutableListOf(401 to """{"error":{"message":"invalid api key"}}""")
        val gateway = gateway()

        val error = runCatching { gateway.chat("system", "user") }.exceptionOrNull()

        assertTrue(error is IOException)
        assertTrue(error!!.message!!.contains("klíč"))
        assertEquals(1, requestCount.get())
        assertEquals(1, gateway.telemetry.value.failedRequests)
    }

    @Test
    fun `component analysis parses the JSON contract`() = runTest {
        val analysisJson = """{"componentName":"TP4056","packageType":"SOP-8","hardwareAnalysis":"Vin 4.5-5.5V","selfCritiqueReflections":["clone check"],"osintGoogleDorks":["dork"],"topEdcMatches":["power bank"],"confidence":0.93}"""
        val envelope = org.json.JSONObject()
            .put(
                "choices",
                org.json.JSONArray().put(
                    org.json.JSONObject().put(
                        "message",
                        org.json.JSONObject().put("content", analysisJson)
                    )
                )
            )
            .put("usage", org.json.JSONObject().put("total_tokens", 120))
            .toString()
        responses = mutableListOf(200 to envelope)
        val gateway = gateway()

        val analysis = gateway.routeMultimodalAnalysis("TP4056", null, NetworkMode.CLOUD_ONLY)

        assertEquals("TP4056", analysis.componentName)
        assertEquals("SOP-8", analysis.packageType)
        assertEquals(0.93f, analysis.verificationConfidence, 0.001f)
        assertEquals(com.example.data.model.LiteLlmProvider.OPENAI_COMPATIBLE, analysis.gatewayProviderUsed)
        assertEquals(120, analysis.tokenUsageEstimate)
    }

    @Test
    fun `scan without a key fails instead of inventing an analysis`() = runTest {
        val gateway = LiteLlmGateway(
            client = OkHttpClient(),
            settingsProvider = { AiSettings(preset = AiPreset.GEMINI) }
        )

        val error = runCatching {
            gateway.routeMultimodalAnalysis("TP4056", null, NetworkMode.AUTO_HYBRID)
        }.exceptionOrNull()

        assertTrue(error is AiNotConfiguredException)
        assertTrue(error!!.message!!.contains("Nastavení AI"))
        assertEquals(0, requestCount.get())
        assertEquals(0, gateway.telemetry.value.totalRequests)
    }

    @Test
    fun `air gapped mode refuses to scan`() = runTest {
        val error = runCatching {
            gateway().routeMultimodalAnalysis("TP4056", null, NetworkMode.LOCAL_AIRGAPPED)
        }.exceptionOrNull()

        assertTrue(error is AiNotConfiguredException)
        assertEquals(0, requestCount.get())
    }

    @Test
    fun `invalid json is repaired by a second model call`() = runTest {
        responses = mutableListOf(
            200 to """{"choices":[{"message":{"content":"I cannot answer in JSON"}}],"usage":{"total_tokens":9}}""",
            200 to """{"choices":[{"message":{"content":"{\"componentName\":\"TP4056\",\"packageType\":\"SOP-8\"}"}}],"usage":{"total_tokens":7}}"""
        )
        val gateway = gateway()

        val analysis = gateway.routeMultimodalAnalysis("TP4056", null, NetworkMode.CLOUD_ONLY)

        assertEquals("TP4056", analysis.componentName)
        assertEquals(2, requestCount.get())
        assertEquals(1, gateway.telemetry.value.healedRequests)
        assertEquals(1, gateway.telemetry.value.successfulRequests)
        assertEquals(16L, gateway.telemetry.value.totalTokens)
        assertEquals(1, gateway.healingLogs.value.size)
        assertTrue(gateway.healingLogs.value.first().actionName.contains("Self-repair"))
    }

    private fun gateway(): LiteLlmGateway {
        val settings = AiSettings(
            preset = AiPreset.CUSTOM,
            apiKey = "sk-test-key",
            baseUrl = "http://127.0.0.1:${server.address.port}/v1",
            chatModel = "qwen/qwen3-coder",
            visionModel = "qwen/qwen3-vl-235b-a22b-instruct",
            codeModel = "qwen/qwen3-coder"
        )
        return LiteLlmGateway(
            client = OkHttpClient(),
            settingsProvider = { settings }
        )
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
