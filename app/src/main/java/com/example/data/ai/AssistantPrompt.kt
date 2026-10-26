package com.example.data.ai

import com.example.data.model.AiChatMessage
import com.example.data.repository.EvidenceChunk

/**
 * Prompt contract for the assistant. The rules are deliberately strict: the
 * model may only restate retrieved datasheet evidence and must copy citation
 * tags verbatim. Everything else is rejected by [CitationGuard].
 */
object AssistantPrompt {

    val SYSTEM: String = """
        You are ScrapMaster AI, a hardware reverse-engineering assistant for an
        e-waste workshop. Answer in Czech, concise and technically precise.

        ABSOLUTE RULES:
        1. Use ONLY the evidence inside the <EVIDENCE> block. Do not add facts
           from memory, guesses, generic datasheets or "typical" pinouts.
        2. Every factual sentence must end with a citation tag copied exactly
           from the evidence header, in the form [zdroj: <file>, část <N>].
        3. Never invent, translate, renumber or merge citation tags. A tag must
           name a file and part number that literally appears in the evidence.
        4. If the evidence does not answer the question, say so explicitly and
           list what is missing. Do not fill the gap with speculation.
        5. Pin counts, voltages, currents and pin numbers must appear exactly as
           written in the evidence.
        6. Never output JSON unless the user explicitly asks for JSON.
    """.trimIndent()

    fun user(
        query: String,
        chunks: List<EvidenceChunk>,
        history: List<AiChatMessage> = emptyList()
    ): String = buildString {
        if (history.isNotEmpty()) {
            appendLine("<HISTORY>")
            history.forEach { message ->
                appendLine("${message.role.uppercase()}: ${message.content.trim()}")
            }
            appendLine("</HISTORY>")
            appendLine()
            appendLine("History is context only. It is NOT evidence and must never be cited.")
            appendLine()
        }

        appendLine("<EVIDENCE>")
        chunks.forEach { chunk ->
            appendLine("### ${chunk.citation}")
            // Bound the prompt even when the user indexed a very long paragraph.
            appendLine(chunk.text.trim().take(MAX_EVIDENCE_CHARS_PER_CHUNK))
            appendLine()
        }
        appendLine("</EVIDENCE>")
        appendLine()
        appendLine("<QUESTION>")
        appendLine(query.trim())
        appendLine("</QUESTION>")
        appendLine()
        append("Answer in Czech with mandatory citation tags.")
    }

    private const val MAX_EVIDENCE_CHARS_PER_CHUNK = 1_500
}
