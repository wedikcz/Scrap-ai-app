package com.example.data.ai

/**
 * Routes each request to the specialist model configured for its task.
 *
 * This is where the "Qwen Coder" idea from the Panus/Hermes engine becomes
 * useful: firmware and scripting questions are sent to the configured code
 * model, while images go to the vision model and ordinary salvage questions to
 * the chat model. Nothing is hard-wired to a vendor.
 */
object ModelRouter {

    private val CODE_HINTS = listOf(
        "firmware", "bootloader", "esptool", "flash", "uart", "jtag", "swd", "spi", "i2c",
        "register", "registr", "arduino", "avr", "stm32", "esp-idf", "skript", "script",
        "python", "c++", "c#", "kotlin", "java", "compile", "kompil", "kód", "code",
        "terminal", "shell", "bash", "at command", "modbus", "can bus"
    )

    fun taskFor(query: String, hasImage: Boolean): AiTask {
        if (hasImage) return AiTask.VISION
        val lower = query.lowercase()
        return if (CODE_HINTS.any { lower.contains(it) }) AiTask.CODE else AiTask.CHAT
    }

    fun modelFor(settings: AiSettings, task: AiTask): String {
        val configured = settings.modelFor(task).trim()
        return when {
            configured.isNotBlank() -> configured
            settings.effectiveChatModel.isNotBlank() -> settings.effectiveChatModel
            else -> settings.preset.defaultChatModel
        }
    }
}
