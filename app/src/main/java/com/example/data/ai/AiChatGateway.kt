package com.example.data.ai

import java.io.IOException

/** One successful upstream completion. */
data class AiChatReply(
    val text: String,
    val model: String,
    val provider: AiProvider,
    val latencyMs: Long,
    val totalTokens: Int
)

/** Result of the explicit "test connection" button. */
data class AiProbeResult(
    val ok: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
    val model: String = ""
)

/**
 * Raised when no cloud call is possible because the app has no usable key or
 * endpoint. The UI turns this into a "configure full AI" gate; it must never be
 * converted into a fabricated answer.
 */
class AiNotConfiguredException(message: String) : IOException(message)

/**
 * Narrow surface the assistant depends on. Keeps [com.example.data.repository.AiAssistantEngine]
 * testable without HTTP, and keeps provider details inside the gateway.
 */
interface AiChatGateway {
    fun currentSettings(): AiSettings
    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        task: AiTask = AiTask.CHAT
    ): AiChatReply
    suspend fun probe(): AiProbeResult
}
