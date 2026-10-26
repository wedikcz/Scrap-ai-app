package com.example.data.ai

import java.io.IOException

/**
 * Self-healing policy for upstream calls. This is the honest replacement for the
 * Python engine's `SelfHealingEngine`: retry only what is actually transient,
 * back off with a cap, and never hide a permanent failure behind a fake success.
 */
object RetryPolicy {
    const val MAX_ATTEMPTS = 3

    fun isRetryable(error: Throwable): Boolean = when (error) {
        is AiHttpException -> error.statusCode == 408 || error.statusCode == 429 ||
            error.statusCode in 500..599
        is AiProtocolException -> false
        is IOException -> true
        else -> false
    }

    /** 800 ms, 1600 ms, 3200 ms ... capped so a phone never waits half a minute. */
    fun backoffMs(attempt: Int): Long = (800L shl attempt.coerceIn(0, 3)).coerceAtMost(8_000L)

    fun userMessage(error: Throwable): String = when (error) {
        is AiHttpException -> when (error.statusCode) {
            400 -> "Poskytovatel odmítl požadavek (HTTP 400). Zkontroluj název modelu a formát."
            401, 403 -> "API klíč byl odmítnut (HTTP ${error.statusCode}). Vlož platný klíč v nastavení AI."
            404 -> "Model nebo endpoint nebyl nalezen (HTTP 404). Zkontroluj název modelu a Base URL."
            429 -> "Limit požadavků poskytovatele (HTTP 429). Zkus to za chvíli nebo jiný model."
            in 500..599 -> "Poskytovatel je dočasně nedostupný (HTTP ${error.statusCode})."
            else -> "AI brána vrátila HTTP ${error.statusCode}: ${error.message}"
        }
        is AiProtocolException -> "Neočekávaná odpověď AI: ${error.message}"
        is AiNotConfiguredException -> error.message ?: "AI není nakonfigurováno."
        is IOException -> "Síťová chyba při volání AI: ${error.localizedMessage ?: "timeout"}"
        else -> "AI volání selhalo: ${error.localizedMessage ?: error.javaClass.simpleName}"
    }
}
