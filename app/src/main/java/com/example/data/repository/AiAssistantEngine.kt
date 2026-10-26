package com.example.data.repository

import com.example.data.ai.AiChatGateway
import com.example.data.ai.AiTask
import com.example.data.ai.AssistantMemory
import com.example.data.ai.AssistantPrompt
import com.example.data.ai.CitationGuard
import com.example.data.model.NetworkMode
import com.example.data.model.RagDocument
import kotlinx.coroutines.CancellationException

/**
 * Full-AI assistant with a local floor.
 *
 * Flow: retrieve evidence -> if a model is configured, ask it to answer strictly
 * from that evidence -> validate every `[zdroj: …]` tag against the retrieved
 * chunks -> only then return the cloud answer. Missing key, network failure,
 * missing citations or a fabricated source all degrade to the deterministic,
 * cited local extraction instead of a plausible-sounding hallucination.
 */
class AiAssistantEngine(
    private val gateway: AiChatGateway,
    private val memory: AssistantMemory,
    private val ragEngine: RagEngine = RagEngine()
) {

    suspend fun ask(
        query: String,
        documents: List<RagDocument>,
        networkMode: NetworkMode = NetworkMode.AUTO_HYBRID
    ): RagSearchResult {
        val local = ragEngine.queryLocalRag(query, documents)
        val chunks = ragEngine.retrieveChunks(query, documents)
        val settings = gateway.currentSettings()

        if (chunks.isEmpty()) {
            remember(query, local.answer)
            return local
        }

        if (networkMode == NetworkMode.LOCAL_AIRGAPPED) {
            remember(query, local.answer)
            return local.copy(guardNote = "Režim LOCAL_ONLY: odpověď vytvořena lokální extrakcí s citacemi.")
        }

        if (!settings.isReady) {
            remember(query, local.answer)
            return local.copy(
                guardNote = "Chybí API klíč (${settings.statusLabel()}). Odpovídá lokální RAG – plné AI se aktivuje v Nastavení AI."
            )
        }

        val history = memory.window()
        val reply = try {
            gateway.chat(
                systemPrompt = AssistantPrompt.SYSTEM,
                userPrompt = AssistantPrompt.user(query, chunks, history),
                task = AiTask.CHAT
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            remember(query, local.answer)
            return local.copy(
                guardNote = "AI nedostupné: ${error.localizedMessage ?: "neznámá chyba"} – vrácena lokální extrakce s citacemi."
            )
        }

        val check = CitationGuard.check(reply.text, chunks)
        if (!check.isValid) {
            val reason = when {
                check.rejectedTags.isNotEmpty() -> "AI citovala zdroj, který v RAG vaultu není"
                else -> "AI nevrátila povinnou citaci [zdroj: soubor, část N]"
            }
            remember(query, local.answer)
            return local.copy(
                aiModel = reply.model,
                guardNote = "Citation guard: $reason. Odpověď odmítnuta, vrácena lokální extrakce.",
                hackerCoreTrace = local.hackerCoreTrace +
                    "\n[GUARD] Cloud answer rejected: $reason."
            )
        }

        remember(query, reply.text)
        val answer = CitationGuard.canonicalize(reply.text, chunks)
        val cloudTrace = buildString {
            appendLine("<HACKER-CORE>")
            appendLine("1. Engine: ${reply.provider.name} · ${reply.model}")
            appendLine("2. Retrieval: ${chunks.size} evidence chunks (${settings.effectiveChatModel})")
            appendLine("3. Latency: ${reply.latencyMs} ms · tokens: ${reply.totalTokens}")
            appendLine("4. Citation guard: ${check.citations.size} valid tags, 0 fabricated")
            appendLine("5. Hermes memory: ${history.size} previous turns used as context only")
            appendLine("6. Swarm: ${local.swarm.expertsWithEvidence}/${local.swarm.perspectives.size} perspectives grounded")
            append("</HACKER-CORE>")
        }

        return local.copy(
            answer = answer,
            citations = check.citations,
            engine = AnswerEngine.CLOUD_AI,
            aiModel = reply.model,
            guardNote = "",
            hackerCoreTrace = cloudTrace
        )
    }

    suspend fun clearMemory() = memory.clear()

    private suspend fun remember(query: String, answer: String) {
        memory.append("user", query)
        memory.append("assistant", answer)
    }
}
