package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Upstream answered with a body that does not match the documented envelope. */
class AiProtocolException(message: String) : IOException(message)

/** Upstream rejected the request with a non-2xx status. */
class AiHttpException(val statusCode: Int, message: String) : IOException(message)

/**
 * All wire-format knowledge in one place. Kept free of Android APIs so the
 * envelope handling can be unit tested on the JVM.
 */
object AiWire {

    fun buildGeminiBody(
        prompt: String,
        systemPrompt: String? = null,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        jsonMode: Boolean = false,
        temperature: Double = 0.2
    ): JSONObject {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (!imageBase64.isNullOrBlank()) {
            parts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject().put("mimeType", mimeType).put("data", imageBase64)
                )
            )
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            if (!systemPrompt.isNullOrBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                )
            }
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", temperature)
                    if (jsonMode) put("responseMimeType", "application/json")
                }
            )
        }
    }

    fun buildOpenAiBody(
        model: String,
        userPrompt: String,
        systemPrompt: String? = null,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        temperature: Double = 0.2
    ): JSONObject {
        val messages = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }

        val userContent: Any = if (imageBase64.isNullOrBlank()) {
            userPrompt
        } else {
            JSONArray()
                .put(JSONObject().put("type", "text").put("text", userPrompt))
                .put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:$mimeType;base64,$imageBase64")
                    )
                )
        }
        messages.put(JSONObject().put("role", "user").put("content", userContent))

        return JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", temperature)
            // Deliberately no `response_format`: several self-hosted proxies reject
            // it, and the prompt already demands one JSON object. The parser strips
            // markdown fences, so this stays compatible across Ollama/LiteLLM/vLLM.
        }
    }

    fun extractTextFromGemini(root: JSONObject): String {
        val candidates = root.optJSONArray("candidates")
            ?: throw AiProtocolException(geminiErrorMessage(root) ?: "Gemini response has no candidates")
        if (candidates.length() == 0) {
            throw AiProtocolException(geminiErrorMessage(root) ?: "Gemini returned zero candidates")
        }

        val candidate = candidates.optJSONObject(0) ?: throw AiProtocolException("Invalid Gemini candidate")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val builder = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (text.isNotBlank()) {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append(text)
                }
            }
        }

        if (builder.isBlank()) {
            val finish = candidate.optString("finishReason")
            val block = geminiErrorMessage(root)
            throw AiProtocolException(
                block ?: "Gemini returned no text (finishReason=${finish.ifBlank { "unknown" }})"
            )
        }
        return builder.toString()
    }

    fun extractTextFromOpenAi(root: JSONObject): String {
        val choices = root.optJSONArray("choices")
            ?: throw AiProtocolException(openAiErrorMessage(root) ?: "Response has no choices")
        if (choices.length() == 0) {
            throw AiProtocolException(openAiErrorMessage(root) ?: "Provider returned zero choices")
        }
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw AiProtocolException("Provider choice has no message")

        val content = message.opt("content")
        val text = when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.optJSONObject(i) ?: continue
                    val chunk = part.optString("text")
                    if (chunk.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(chunk)
                    }
                }
            }
            else -> ""
        }

        if (text.isBlank()) {
            throw AiProtocolException(openAiErrorMessage(root) ?: "Provider returned an empty message")
        }
        return text
    }

    fun totalTokensFromGemini(root: JSONObject): Int =
        root.optJSONObject("usageMetadata")?.optInt("totalTokenCount", 0) ?: 0

    fun totalTokensFromOpenAi(root: JSONObject): Int =
        root.optJSONObject("usage")?.optInt("total_tokens", 0) ?: 0

    /** Extracts the first JSON object from a model answer, tolerating ``` fences. */
    fun extractJsonObject(raw: String): JSONObject? {
        val cleaned = raw.replace("\r", "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(cleaned.substring(start, end + 1)) }.getOrNull()
    }

    fun stringList(json: JSONObject, key: String): List<String> {
        val array = json.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun geminiErrorMessage(root: JSONObject): String? {
        val block = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (block.isNotBlank()) return "Gemini zablokoval požadavek (blockReason=$block)"
        val error = root.optJSONObject("error") ?: return null
        val message = error.optString("message")
        return message.ifBlank { "Gemini API error" }
    }

    private fun openAiErrorMessage(root: JSONObject): String? {
        val error = root.optJSONObject("error") ?: return null
        val message = error.optString("message")
        return message.ifBlank { "Provider API error" }
    }
}
