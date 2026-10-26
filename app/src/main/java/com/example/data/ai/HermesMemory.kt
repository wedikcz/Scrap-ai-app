package com.example.data.ai

import com.example.data.local.ScrapMasterDao
import com.example.data.model.AiChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Pure context-window arithmetic. The original Hermes memory manager took the
 * last N rows; a phone also needs a character budget, otherwise a handful of
 * long datasheet answers blow the model context (and the user's token budget).
 *
 * The newest message is always kept, even if it alone exceeds the budget: the
 * current question must never be silently dropped.
 */
object MemoryWindow {
    const val DEFAULT_MAX_MESSAGES = 10
    const val DEFAULT_MAX_CHARS = 4_000

    fun trim(
        messages: List<AiChatMessage>,
        maxMessages: Int = DEFAULT_MAX_MESSAGES,
        maxChars: Int = DEFAULT_MAX_CHARS
    ): List<AiChatMessage> {
        if (messages.isEmpty()) return emptyList()
        val bounded = messages.takeLast(maxMessages.coerceAtLeast(1))
        val selected = ArrayDeque<AiChatMessage>()
        var used = 0

        for (message in bounded.asReversed()) {
            val cost = message.role.length + message.content.length + 8
            if (selected.isNotEmpty() && used + cost > maxChars) break
            selected.addFirst(message)
            used += cost
        }
        return selected.toList()
    }

    fun renderForPrompt(messages: List<AiChatMessage>): String =
        messages.joinToString(separator = "\n") { "${it.role.uppercase()}: ${it.content.trim()}" }
}

/** Contract used by the assistant engine, so memory can be faked in tests. */
interface AssistantMemory {
    fun messages(): Flow<List<AiChatMessage>>
    suspend fun append(role: String, content: String)
    suspend fun window(
        maxMessages: Int = MemoryWindow.DEFAULT_MAX_MESSAGES,
        maxChars: Int = MemoryWindow.DEFAULT_MAX_CHARS
    ): List<AiChatMessage>
    suspend fun clear()
}

/**
 * Session-scoped assistant memory. Stores only role + text, prunes old rows and
 * feeds a bounded window into the next prompt, so the assistant keeps context
 * across app restarts without growing the database forever.
 */
class HermesMemory(
    private val dao: ScrapMasterDao,
    val sessionId: String = DEFAULT_SESSION
) : AssistantMemory {
    override fun messages(): Flow<List<AiChatMessage>> = dao.observeChatMessages(sessionId)

    override suspend fun append(role: String, content: String) {
        if (content.isBlank()) return
        dao.insertChatMessage(
            AiChatMessage(
                sessionId = sessionId,
                role = role,
                content = content.trim()
            )
        )
        dao.pruneChatSession(sessionId, MAX_STORED_MESSAGES)
    }

    override suspend fun window(
        maxMessages: Int,
        maxChars: Int
    ): List<AiChatMessage> {
        val recent = dao.getRecentChatMessages(sessionId, LOOKBACK_MESSAGES)
        return MemoryWindow.trim(recent.asReversed(), maxMessages, maxChars)
    }

    override suspend fun clear() = dao.clearChatSession(sessionId)

    companion object {
        const val DEFAULT_SESSION = "default"
        private const val LOOKBACK_MESSAGES = 40
        private const val MAX_STORED_MESSAGES = 200
    }
}
