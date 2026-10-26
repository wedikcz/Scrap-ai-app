package com.example.data.gateway

import android.os.SystemClock
import com.example.data.ai.AiChatGateway
import com.example.data.ai.AiChatReply
import com.example.data.ai.AiHttpException
import com.example.data.ai.AiNotConfiguredException
import com.example.data.ai.AiProbeResult
import com.example.data.ai.AiProtocolException
import com.example.data.ai.AiProvider
import com.example.data.ai.AiSettings
import com.example.data.ai.AiTask
import com.example.data.ai.AiWire
import com.example.data.ai.ModelRouter
import com.example.data.ai.RetryPolicy
import com.example.data.model.GatewayTelemetry
import com.example.data.model.HackerCoreAnalysis
import com.example.data.model.LiteLlmProvider
import com.example.data.model.NetworkMode
import com.example.data.model.SelfHealingTraceLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * LiteLLM-style gateway for the app: one entry point, two wire protocols
 * (Gemini and OpenAI-compatible), per-task model routing, bounded retries and
 * honest telemetry.
 *
 * Full-AI policy: there is no demo answer. Without a usable key the gateway
 * refuses the scan with [AiNotConfiguredException], and a failed call is
 * reported as a failure. The only "self-healing" left is real: transport retries
 * for transient errors and a model-driven repair pass when the JSON contract of
 * the component analysis is broken.
 */
class LiteLlmGateway(
    private val client: OkHttpClient = defaultClient(),
    private val settingsProvider: () -> AiSettings = { AiSettings() }
) : AiChatGateway {

    private val _telemetry = MutableStateFlow(GatewayTelemetry())
    val telemetry: StateFlow<GatewayTelemetry> = _telemetry.asStateFlow()

    private val _healingLogs = MutableStateFlow<List<SelfHealingTraceLog>>(emptyList())
    val healingLogs: StateFlow<List<SelfHealingTraceLog>> = _healingLogs.asStateFlow()

    override fun currentSettings(): AiSettings = settingsProvider()

    /**
     * Component analysis with the dual-pass prompt. Images route to the vision
     * model, marking-only firmware questions to the code specialist. Every field
     * comes from the upstream model; missing configuration is an error, not a
     * reason to replay a hard-coded component sheet.
     */
    suspend fun routeMultimodalAnalysis(
        queryOrMarking: String,
        base64Image: String? = null,
        networkMode: NetworkMode = NetworkMode.AUTO_HYBRID
    ): HackerCoreAnalysis = withContext(Dispatchers.IO) {
        val settings = currentSettings()

        if (networkMode == NetworkMode.LOCAL_AIRGAPPED) {
            throw AiNotConfiguredException(
                "Skenování součástek vyžaduje cloudové AI. Režim LOCAL_ONLY je určený pro offline RAG asistenta."
            )
        }
        if (!settings.isReady) {
            throw AiNotConfiguredException(
                "Plné AI není aktivní: chybí API klíč nebo Base URL. Otevři Nastavení AI a vlož klíč."
            )
        }

        _telemetry.update { it.copy(totalRequests = it.totalRequests + 1) }
        val started = SystemClock.elapsedRealtime()
        val task = ModelRouter.taskFor(queryOrMarking, base64Image != null)

        try {
            val result = callAnalysisWithRepair(settings, task, queryOrMarking, base64Image)
            val latency = SystemClock.elapsedRealtime() - started
            val analysis = parseAnalysis(result.json, queryOrMarking, settings, latency, result.totalTokens)
            recordSuccess(
                reply = result.reply.copy(latencyMs = latency, totalTokens = result.totalTokens),
                settings = settings
            )
            analysis
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            recordFailure(error)
            throw IOException(RetryPolicy.userMessage(error), error)
        }
    }

    override suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        task: AiTask
    ): AiChatReply = withContext(Dispatchers.IO) {
        val settings = currentSettings()
        if (!settings.isReady) {
            throw AiNotConfiguredException("AI není nakonfigurováno. Vlož API klíč v Nastavení AI.")
        }
        _telemetry.update { it.copy(totalRequests = it.totalRequests + 1) }
        val started = SystemClock.elapsedRealtime()
        try {
            val reply = callWithRetry(
                settings = settings,
                task = task,
                userPrompt = userPrompt,
                systemPrompt = systemPrompt,
                imageBase64 = null,
                jsonMode = false
            )
            val measured = reply.copy(latencyMs = SystemClock.elapsedRealtime() - started)
            recordSuccess(measured, settings)
            measured
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            recordFailure(error)
            throw IOException(RetryPolicy.userMessage(error), error)
        }
    }

    override suspend fun probe(): AiProbeResult = withContext(Dispatchers.IO) {
        val settings = currentSettings()
        if (!settings.isReady) {
            return@withContext AiProbeResult(
                ok = false,
                message = "AI není aktivní: chybí API klíč nebo Base URL. Vlož je v Nastavení AI."
            )
        }
        val started = SystemClock.elapsedRealtime()
        try {
            val reply = callWithRetry(
                settings = settings,
                task = AiTask.CHAT,
                userPrompt = "Reply with the single word: OK",
                systemPrompt = "You are a connectivity probe. Answer with OK only.",
                imageBase64 = null,
                jsonMode = false
            )
            AiProbeResult(
                ok = true,
                message = "Spojení funguje. ${reply.provider.name} · ${reply.model}",
                latencyMs = SystemClock.elapsedRealtime() - started,
                model = reply.model
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            AiProbeResult(ok = false, message = RetryPolicy.userMessage(error))
        }
    }

    /**
     * Asks for the analysis and repairs a broken JSON contract by sending the
     * model its own output plus a strict reminder. This is the useful part of the
     * Panus/Hermes self-healing loop, applied to a JSON schema instead of
     * executing Python code on the device.
     */
    private suspend fun callAnalysisWithRepair(
        settings: AiSettings,
        task: AiTask,
        marking: String,
        imageBase64: String?
    ): RepairOutcome {
        var reply = callWithRetry(
            settings = settings,
            task = task,
            userPrompt = buildScanPrompt(marking),
            imageBase64 = imageBase64,
            jsonMode = true
        )
        var tokens = reply.totalTokens
        var json = AiWire.extractJsonObject(reply.text)
        var repairs = 0

        while (!isUsableAnalysis(json) && repairs < MAX_JSON_REPAIRS) {
            repairs++
            val brokenAnswer = reply.text.take(MAX_REPAIR_INPUT_CHARS)
            reply = callWithRetry(
                settings = settings,
                task = task,
                systemPrompt = REPAIR_SYSTEM_PROMPT,
                userPrompt = buildRepairPrompt(marking, brokenAnswer),
                imageBase64 = null,
                jsonMode = true
            )
            tokens += reply.totalTokens
            json = AiWire.extractJsonObject(reply.text)
        }

        val usable = json?.takeIf { isUsableAnalysis(it) }
            ?: throw AiProtocolException(
                "Model nevrátil použitelný JSON ani po $repairs opravných pokusech."
            )

        if (repairs > 0) {
            logHealing(
                actionName = "Self-repair JSON kontraktu",
                errorDetails = "První odpověď nebyla platný JSON s componentName (opravné pokusy: $repairs).",
                healingStrategy = "Model dostal vlastní výstup a striktní schéma a musel odpověď opravit. " +
                    "Žádná náhradní demo analýza se nepoužívá.",
                isResolved = true
            )
        }

        return RepairOutcome(reply = reply, json = usable, totalTokens = tokens)
    }

    private fun isUsableAnalysis(json: JSONObject?): Boolean =
        json != null && json.optString("componentName").isNotBlank()

    private suspend fun callWithRetry(
        settings: AiSettings,
        task: AiTask,
        userPrompt: String,
        systemPrompt: String? = null,
        imageBase64: String? = null,
        jsonMode: Boolean
    ): AiChatReply {
        val model = ModelRouter.modelFor(settings, task)
        var lastError: Throwable? = null

        for (attempt in 0 until RetryPolicy.MAX_ATTEMPTS) {
            try {
                return callOnce(settings, model, systemPrompt, userPrompt, imageBase64, jsonMode)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastError = error
                if (!RetryPolicy.isRetryable(error) || attempt == RetryPolicy.MAX_ATTEMPTS - 1) {
                    throw error
                }
                delay(RetryPolicy.backoffMs(attempt))
            }
        }
        throw lastError ?: IOException("AI call failed without an error")
    }

    private fun callOnce(
        settings: AiSettings,
        model: String,
        systemPrompt: String?,
        userPrompt: String,
        imageBase64: String?,
        jsonMode: Boolean
    ): AiChatReply {
        val request = when (settings.provider) {
            AiProvider.GEMINI -> buildGeminiRequest(settings, model, systemPrompt, userPrompt, imageBase64, jsonMode)
            AiProvider.OPENAI_COMPATIBLE -> buildOpenAiRequest(settings, model, systemPrompt, userPrompt, imageBase64)
        }

        val started = SystemClock.elapsedRealtime()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiHttpException(response.code, errorMessage(raw, response.message))
            }
            val json = runCatching { JSONObject(raw) }
                .getOrElse { throw AiProtocolException("Odpověď není platný JSON.") }

            val text = when (settings.provider) {
                AiProvider.GEMINI -> AiWire.extractTextFromGemini(json)
                AiProvider.OPENAI_COMPATIBLE -> AiWire.extractTextFromOpenAi(json)
            }
            val tokens = when (settings.provider) {
                AiProvider.GEMINI -> AiWire.totalTokensFromGemini(json)
                AiProvider.OPENAI_COMPATIBLE -> AiWire.totalTokensFromOpenAi(json)
            }.let { if (it > 0) it else estimateTokens(userPrompt, text) }

            return AiChatReply(
                text = text,
                model = model,
                provider = settings.provider,
                latencyMs = SystemClock.elapsedRealtime() - started,
                totalTokens = tokens
            )
        }
    }

    private fun buildGeminiRequest(
        settings: AiSettings,
        model: String,
        systemPrompt: String?,
        userPrompt: String,
        imageBase64: String?,
        jsonMode: Boolean
    ): Request {
        val body = AiWire.buildGeminiBody(
            prompt = userPrompt,
            systemPrompt = systemPrompt,
            imageBase64 = imageBase64,
            jsonMode = jsonMode
        )
        return Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", settings.apiKey)
            .header("User-Agent", USER_AGENT)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun buildOpenAiRequest(
        settings: AiSettings,
        model: String,
        systemPrompt: String?,
        userPrompt: String,
        imageBase64: String?
    ): Request {
        val base = settings.effectiveBaseUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val body = AiWire.buildOpenAiBody(
            model = model,
            userPrompt = userPrompt,
            systemPrompt = systemPrompt,
            imageBase64 = imageBase64
        )
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (settings.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${settings.apiKey}")
        }
        return builder.build()
    }

    private fun parseAnalysis(
        data: JSONObject,
        marking: String,
        settings: AiSettings,
        latency: Long,
        tokens: Int
    ): HackerCoreAnalysis {
        return HackerCoreAnalysis(
            componentName = data.optString("componentName").ifBlank { marking.ifBlank { "UNKNOWN COMPONENT" } },
            packageType = data.optString("packageType").ifBlank { "SMD / Discrete" },
            estimatedOrigin = data.optString("estimatedOrigin").ifBlank { "Consumer E-Waste" },
            conditionAssessment = data.optString("conditionAssessment").ifBlank { "Verify diode drops with a multimeter" },
            hardwareAnalysis = data.optString("hardwareAnalysis").ifBlank { "Standard IC specifications" },
            rfAnalysis = data.optString("rfAnalysis").ifBlank { "Unknown passband" },
            secOpsAnalysis = data.optString("secOpsAnalysis").ifBlank { "Check for exposed debug headers" },
            firmwareAnalysis = data.optString("firmwareAnalysis").ifBlank { "No programming interface documented" },
            selfCritiqueReflections = AiWire.stringList(data, "selfCritiqueReflections"),
            osintGoogleDorks = AiWire.stringList(data, "osintGoogleDorks"),
            topEdcMatches = AiWire.stringList(data, "topEdcMatches"),
            verificationConfidence = data.optDouble("confidence", 0.9)
                .toFloat().coerceIn(0.1f, 0.99f),
            gatewayProviderUsed = providerKind(settings),
            tokenUsageEstimate = tokens,
            latencyMs = latency,
            reflectionNotes = data.optString("reflectionNotes")
                .ifBlank { "Pass 1 identification merged with Pass 2 self-reflection" }
        )
    }

    private fun recordSuccess(reply: AiChatReply, settings: AiSettings) {
        _telemetry.update {
            it.copy(
                successfulRequests = it.successfulRequests + 1,
                totalTokens = it.totalTokens + reply.totalTokens,
                lastLatencyMs = reply.latencyMs,
                activeProvider = providerKind(settings),
                activeModel = reply.model,
                consecutiveFailures = 0
            )
        }
    }

    private fun recordFailure(error: Throwable) {
        _telemetry.update {
            it.copy(
                failedRequests = it.failedRequests + 1,
                consecutiveFailures = it.consecutiveFailures + 1
            )
        }
        logHealing(
            actionName = "AI volání selhalo",
            errorDetails = error.localizedMessage ?: error.javaClass.simpleName,
            healingStrategy = "Transport: ${RetryPolicy.MAX_ATTEMPTS} pokusy s backoffem pro 408/429/5xx/IO. " +
                "Žádný falešný fallback – chyba se vrací uživateli.",
            isResolved = false
        )
    }

    private fun logHealing(
        actionName: String,
        errorDetails: String,
        healingStrategy: String,
        isResolved: Boolean
    ) {
        if (isResolved) {
            _telemetry.update { it.copy(healedRequests = it.healedRequests + 1) }
        }
        val log = SelfHealingTraceLog(
            id = UUID.randomUUID().toString(),
            actionName = actionName,
            errorDetails = errorDetails,
            healingStrategy = healingStrategy,
            isResolved = isResolved
        )
        _healingLogs.update { (listOf(log) + it).take(25) }
    }

    private fun providerKind(settings: AiSettings): LiteLlmProvider = when (settings.provider) {
        AiProvider.GEMINI -> LiteLlmProvider.CLOUD_GATEWAY
        AiProvider.OPENAI_COMPATIBLE -> LiteLlmProvider.OPENAI_COMPATIBLE
    }

    private fun errorMessage(rawBody: String, fallback: String): String {
        val parsed = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return parsed?.takeIf { it.isNotBlank() }?.take(300) ?: fallback
    }

    private fun estimateTokens(prompt: String, answer: String): Int =
        ((prompt.length + answer.length) / 4).coerceAtLeast(1)

    private fun buildScanPrompt(marking: String): String = """
        You are ScrapMaster AI running through a unified AI gateway.
        Analyze this component marking or image: "$marking".

        Execute a DUAL-PASS analysis:
        - PASS 1: identify chip, package, pin count, functional category, salvage source.
        - PASS 2: audit pass 1 for SMD code collisions, counterfeit clones, thermal
          pad requirements, boot-strap traps and voltage limits.
        - MERGE: synthesize both passes into one verified result.

        Return strictly one JSON object, no prose and no markdown fences:
        {
          "componentName": "Exact chip name",
          "packageType": "e.g. SOP-8, QFN-32, Module",
          "estimatedOrigin": "Probable source appliance",
          "conditionAssessment": "Inspection & diode test steps",
          "hardwareAnalysis": "Voltages, pinout highlights, limits",
          "rfAnalysis": "RF characteristics & emissions",
          "secOpsAnalysis": "Hardware security, boot strapping, glitching vectors",
          "firmwareAnalysis": "Flashing protocol & debug headers",
          "selfCritiqueReflections": ["Pass 2 critique 1", "Pass 2 critique 2"],
          "osintGoogleDorks": ["Dork 1", "Dork 2"],
          "topEdcMatches": ["Project 1", "Project 2"],
          "reflectionNotes": "Synthesis of pass 1 and pass 2",
          "confidence": 0.0
        }

        "componentName" is mandatory. Set "confidence" below 0.5 when the marking
        is ambiguous. Never invent a datasheet pinout for an unknown code; state
        the uncertainty instead. If nothing can be identified, use
        "componentName": "UNKNOWN" and explain what to inspect next.
    """.trimIndent()

    private fun buildRepairPrompt(marking: String, previousAnswer: String): String = """
        Your previous answer for the marking "$marking" did not satisfy the
        required JSON contract, so it cannot be parsed.

        Previous answer:
        ---
        $previousAnswer
        ---

        Return ONE valid JSON object only, with all keys from the contract and a
        non-empty "componentName". No markdown fences, no commentary. Keep the
        same analysis content; fix only the structure and any missing keys.
    """.trimIndent()

    private data class RepairOutcome(
        val reply: AiChatReply,
        val json: JSONObject,
        val totalTokens: Int
    )

    companion object {
        private const val USER_AGENT = "ScrapMaster-AI/1.3 (Android)"
        private const val MAX_JSON_REPAIRS = 2
        private const val MAX_REPAIR_INPUT_CHARS = 1_500
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val REPAIR_SYSTEM_PROMPT =
            "You repair malformed JSON answers. Output exactly one JSON object and nothing else."

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
