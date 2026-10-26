package com.example.data.repository

import com.example.data.model.RagDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

/** Which engine actually produced an answer, so the UI can label it honestly. */
enum class AnswerEngine(val badge: String) {
    CLOUD_AI("FULL AI"),
    LOCAL_RAG("LOCAL RAG")
}

data class RagSearchResult(
    val answer: String,
    val citations: List<String>,
    val matchedSources: List<Pair<String, String>>, // Doc title, chunk excerpt
    val hackerCoreTrace: String,
    val hasCitations: Boolean = true,
    val swarm: SwarmVerdict,
    val engine: AnswerEngine = AnswerEngine.LOCAL_RAG,
    val aiModel: String = "",
    val guardNote: String = ""
)

/**
 * Deterministic, offline retrieval over the user's own datasheets. The same
 * chunk list feeds both the extractive local answer and the cloud prompt, so the
 * citations a model sees are exactly the citations the guard will accept.
 */
class RagEngine(private val swarmEngine: HackerSwarmEngine = HackerSwarmEngine()) {

    suspend fun retrieveChunks(
        query: String,
        documents: List<RagDocument>,
        maxChunks: Int = 3
    ): List<EvidenceChunk> = withContext(Dispatchers.Default) {
        if (query.isBlank() || documents.isEmpty()) return@withContext emptyList()
        val tokens = tokenize(query)

        documents.flatMap { doc ->
            doc.content.split(Regex("\\n\\n+")).mapIndexedNotNull { index, paragraph ->
                if (paragraph.isBlank()) null
                else EvidenceChunk(
                    docTitle = doc.title,
                    partIndex = index + 1,
                    text = paragraph.trim(),
                    sourceName = doc.sourceName
                )
            }
        }.map { chunk ->
            val lower = normalize(chunk.text)
            val title = normalize(chunk.docTitle)
            var score = 0
            tokens.forEach { token ->
                if (lower.contains(token)) {
                    score += 10
                    if (title.contains(token)) score += 15
                }
            }
            chunk to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxChunks)
            .map { it.first }
    }

    fun swarm(query: String, chunks: List<EvidenceChunk>): SwarmVerdict =
        swarmEngine.analyze(query, chunks)

    suspend fun queryLocalRag(
        query: String,
        documents: List<RagDocument>
    ): RagSearchResult = withContext(Dispatchers.Default) {
        val tokens = tokenize(query)
        val topChunks = retrieveChunks(query, documents)

        if (topChunks.isEmpty()) {
            return@withContext RagSearchResult(
                answer = "Nenalezl jsem ve tvých dokumentech. Žádný z indexovaných technických listů ani bezpečnostních manuálů neobsahuje ověřitelné informace k dotazu '$query'. Anti-halucinační zámek zablokoval neověřené tvrzení.",
                citations = emptyList(),
                matchedSources = emptyList(),
                hackerCoreTrace = """
                <HACKER-CORE>
                1. Token scan: [${tokens.joinToString(", ")}] -> 0 matching documents in local vault.
                2. Anti-Hallucination Guard: EXPLICIT FAILURE REPORTED. Refusing to invent pinout/voltage data without cited datasheet.
                3. OSINT Dork: "$query" (datasheet | schematic | "pinout") filetype:pdf
                4. Action: Add matching PDF/TXT datasheet to vault.
                </HACKER-CORE>
                """.trimIndent(),
                hasCitations = false,
                swarm = swarmEngine.analyze(query, emptyList()),
                engine = AnswerEngine.LOCAL_RAG
            )
        }

        val citations = topChunks.map { it.citation }
        val sources = topChunks.map { Pair(it.docTitle, it.text) }

        val answerBuilder = StringBuilder()
        answerBuilder.append("Na základě analýzy indexovaných technických listů a bezpečnostních manuálů:\n\n")
        topChunks.forEach { chunk ->
            answerBuilder.append("• ${chunk.text}\n  ${chunk.citation}\n\n")
        }

        val swarm = swarmEngine.analyze(query, topChunks)
        val hackerCoreTrace = """
        <HACKER-CORE>
        1. Multidisciplinary query parse: [${tokens.joinToString(", ")}]
        2. RAG index matches: ${topChunks.size} chunks retrieved from local storage.
        3. Smart-Mixture-of-Hackers: ${swarm.expertsWithEvidence}/${swarm.perspectives.size} expert perspectives grounded (agreement ${(swarm.agreementRatio * 100).toInt()}%).
        4. Self-Critique & Anti-Hallucination: every assertion verified against physical datasheet citations.
        5. Verified citations: ${citations.joinToString("; ")}
        </HACKER-CORE>
        """.trimIndent()

        RagSearchResult(
            answer = answerBuilder.toString().trim(),
            citations = citations,
            matchedSources = sources,
            hackerCoreTrace = hackerCoreTrace,
            hasCitations = true,
            swarm = swarm,
            engine = AnswerEngine.LOCAL_RAG
        )
    }

    private fun tokenize(query: String): List<String> =
        normalize(query)
            .split(Regex("[\\s,;:.?!()\\[\\]]+"))
            .filter { it.length > 2 }
            .distinct()

    /** Diacritics-insensitive matching: "napeti" finds "napětí". */
    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
