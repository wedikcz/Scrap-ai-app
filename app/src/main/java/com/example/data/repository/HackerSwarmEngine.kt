package com.example.data.repository

import java.text.Normalizer
import java.util.Locale

/**
 * Evidence retrieved from the local document vault. Chunks are the only thing
 * the swarm is allowed to reason about - no chunk, no claim.
 */
data class EvidenceChunk(
    val docTitle: String,
    val partIndex: Int,
    val text: String,
    val sourceName: String = ""
) {
    val citation: String get() = "[zdroj: $docTitle, část $partIndex]"
}

/**
 * The four virtual specialists of the Smart-Mixture-of-Hackers layer.
 * Each one only reports domains covered by its own keyword set.
 */
enum class HackerExpert(
    val label: String,
    val roleCz: String,
    val keywords: List<String>
) {
    SILICON_ANALYST(
        label = "Silicon Analyst",
        roleCz = "Pouzdra, piny, napájení",
        keywords = listOf("pin", "piny", "pouzdro", "package", "dip", "sop", "qfn", "sot", "napětí", "napeti", "vcc", "gnd", "teplota", "watt", "ma")
    ),
    CIRCUIT_HACKER(
        label = "Circuit Hacker",
        roleCz = "Zapojení, proudové limity",
        keywords = listOf("odpor", "rezistor", "kondenzátor", "kondenzator", "kapacita", "zapojení", "zapojeni", "obvod", "proud", "prog", "1.2k", "ohm", "ω", "pull-up", "dělič", "delic")
    ),
    FIRMWARE_ENGINEER(
        label = "Firmware Engineer",
        roleCz = "Protokoly, flash, debug",
        keywords = listOf("uart", "i2c", "spi", "flash", "boot", "firmware", "baud", "gpio", "jtag", "swd", "strap", "esptool", "at command", "registr")
    ),
    SCRAP_OPTIMIZER(
        label = "Cost/Scrap Optimizer",
        roleCz = "Výtěžnost, bezpečná demontáž",
        keywords = listOf("bezpeč", "bezpec", "demontáž", "demontaz", "sklad", "cena", "usd", "hodnota", "recykl", "baterie", "článek", "clanek", "teplo", "poškození", "poskozeni")
    )
}

/**
 * One specialist's answer. [hasEvidence] is false when the vault contained
 * nothing for that domain - the perspective then explicitly refuses to guess.
 */
data class ExpertPerspective(
    val expert: HackerExpert,
    val verdict: String,
    val citations: List<String>,
    val confidence: Int,
    val hasEvidence: Boolean
)

/**
 * Consolidated swarm output. [agreementRatio] is the share of the four
 * specialists that found direct documentary support.
 */
data class SwarmVerdict(
    val perspectives: List<ExpertPerspective>,
    val consensus: String,
    val gaps: List<String>,
    val agreementRatio: Float
) {
    val expertsWithEvidence: Int get() = perspectives.count { it.hasEvidence }
    val isGrounded: Boolean get() = expertsWithEvidence > 0
}

/**
 * Smart-Mixture-of-Hackers: deterministic, offline multi-expert synthesis over
 * retrieved evidence chunks.
 *
 * The engine never generates hardware facts of its own. It assigns retrieved
 * passages to the specialist whose domain they cover, quotes them with their
 * citation, and reports the coverage honestly. This keeps the same
 * anti-hallucination guarantee as the base RAG engine while giving the four
 * expert perspectives the master prompt asks for.
 */
class HackerSwarmEngine {

    fun analyze(query: String, chunks: List<EvidenceChunk>): SwarmVerdict {
        val perspectives = HackerExpert.entries.map { expert ->
            buildPerspective(expert, chunks)
        }

        val withEvidence = perspectives.filter { it.hasEvidence }
        val gaps = perspectives.filterNot { it.hasEvidence }.map { it.expert.label }

        val consensus = when {
            withEvidence.isEmpty() -> {
                "Smart-Mixture-of-Hackers: 0 ze 4 expertů našlo v indexovaných dokumentech oporu pro dotaz " +
                    "\"$query\". Swarm odmítá odpovědět bez důkazu a nechává dotaz neuzavřený."
            }
            else -> {
                val ratio = (withEvidence.size * 100) / HackerExpert.entries.size
                buildString {
                    append("Smart-Mixture-of-Hackers: ${withEvidence.size} ze ${HackerExpert.entries.size} ")
                    append("expertních pohledů našlo přímou oporu v dokumentech (shoda $ratio %). ")
                    append("Konsenzus se opírá výhradně o citované pasáže.")
                    if (gaps.isNotEmpty()) {
                        append(" Bez důkazu zůstává: ${gaps.joinToString(", ")}.")
                    }
                }
            }
        }

        return SwarmVerdict(
            perspectives = perspectives,
            consensus = consensus,
            gaps = gaps,
            agreementRatio = withEvidence.size.toFloat() / HackerExpert.entries.size
        )
    }

    private fun buildPerspective(expert: HackerExpert, chunks: List<EvidenceChunk>): ExpertPerspective {
        val ranked = chunks
            .map { chunk -> chunk to scoreChunk(expert, chunk) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(2)

        if (ranked.isEmpty()) {
            return ExpertPerspective(
                expert = expert,
                verdict = "Bez přímého důkazu v indexovaných dokumentech – ${expert.label} odmítá spekulovat " +
                    "a vyžaduje doplnění datasheetu.",
                citations = emptyList(),
                confidence = 0,
                hasEvidence = false
            )
        }

        val citations = ranked.map { it.first.citation }.distinct()
        val bestScore = ranked.first().second
        val confidence = minOf(95, 35 + bestScore * 12)

        val verdict = ranked.joinToString(" ") { (chunk, _) ->
            "„${excerpt(chunk.text)}“ (${chunk.citation})"
        }

        return ExpertPerspective(
            expert = expert,
            verdict = verdict,
            citations = citations,
            confidence = confidence,
            hasEvidence = true
        )
    }

    private fun scoreChunk(expert: HackerExpert, chunk: EvidenceChunk): Int {
        val text = normalize(chunk.text)
        val title = normalize(chunk.docTitle)
        var hits = 0
        expert.keywords.forEach { keyword ->
            val needle = normalize(keyword)
            if (text.contains(needle)) hits += 2
            if (title.contains(needle)) hits += 1
        }
        return hits
    }

    private fun excerpt(text: String): String {
        val singleLine = text.replace(Regex("\\s+"), " ").trim()
        return if (singleLine.length <= 180) singleLine else singleLine.take(177).trimEnd() + "…"
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
}
