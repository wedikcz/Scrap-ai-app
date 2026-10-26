package com.example.data.ai

import com.example.data.repository.EvidenceChunk

/**
 * Result of auditing a model answer against the evidence that was actually
 * retrieved. An answer with a fabricated source is rejected as a whole: keeping
 * the prose but dropping the fake tag would still present unverified hardware
 * facts as trustworthy.
 */
data class CitationCheck(
    val answer: String,
    val citations: List<String>,
    val rejectedTags: List<String>
) {
    val isValid: Boolean get() = citations.isNotEmpty() && rejectedTags.isEmpty()
}

/**
 * Enforces the mandatory `[zdroj: soubor, část N]` contract. The deterministic
 * RAG engine already cites correctly; this guards the non-deterministic LLM.
 */
object CitationGuard {
    private val TAG_REGEX = Regex(
        """\[\s*zdroj:\s*(.+?)\s*,\s*[cč][aá]st\s*(\d+)\s*]""",
        RegexOption.IGNORE_CASE
    )

    fun check(answer: String, available: List<EvidenceChunk>): CitationCheck {
        val known = available.associateBy { key(it.docTitle, it.partIndex) }
        val citations = LinkedHashSet<String>()
        val rejected = mutableListOf<String>()

        TAG_REGEX.findAll(answer).forEach { match ->
            val title = match.groupValues[1].trim()
            val part = match.groupValues[2].toIntOrNull() ?: -1
            val chunk = known[key(title, part)]
            if (chunk != null) {
                citations.add(chunk.citation)
            } else {
                rejected.add(match.value)
            }
        }

        return CitationCheck(
            answer = answer.trim(),
            citations = citations.toList(),
            rejectedTags = rejected
        )
    }

    /** Replaces tolerated tag spellings with the canonical `[zdroj: title, část N]`. */
    fun canonicalize(answer: String, available: List<EvidenceChunk>): String {
        val known = available.associateBy { key(it.docTitle, it.partIndex) }
        return TAG_REGEX.replace(answer) { match ->
            val title = match.groupValues[1].trim()
            val part = match.groupValues[2].toIntOrNull() ?: -1
            known[key(title, part)]?.citation ?: match.value
        }
    }

    private fun key(title: String, part: Int): String =
        title.trim().lowercase().replace(Regex("\\s+"), " ") + "#" + part
}
