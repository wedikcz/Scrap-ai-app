package com.example.data.repository

import com.example.data.model.HackerCoreAnalysis
import com.example.data.model.SalvageItem

/**
 * Maps a completed [HackerCoreAnalysis] into an inventory row. The AI call itself
 * lives only in [com.example.data.gateway.LiteLlmGateway], so there is a single
 * place that knows about providers, keys and retries.
 */
class AiScannerEngine {

    fun convertToSalvageItem(analysis: HackerCoreAnalysis): SalvageItem {
        val category = when {
            analysis.componentName.contains("ESP", true) ||
                analysis.componentName.contains("MCU", true) -> "Microcontroller"
            analysis.componentName.contains("TP4056", true) ||
                analysis.componentName.contains("18650", true) ||
                analysis.componentName.contains("Charger", true) -> "Power/BMS"
            analysis.componentName.contains("RF", true) ||
                analysis.componentName.contains("Wi-Fi", true) -> "RF/Wireless"
            analysis.componentName.contains("OLED", true) ||
                analysis.componentName.contains("Display", true) -> "Displays"
            else -> "ICs"
        }

        return SalvageItem(
            name = analysis.componentName,
            category = category,
            quantity = 1,
            sourceDevice = analysis.estimatedOrigin,
            condition = "Requires Testing",
            estimatedValueUsd = 2.00,
            specs = "${analysis.packageType} | ${analysis.hardwareAnalysis.take(120)}...",
            notes = buildString {
                append("Salvaged via ScrapMaster AI. Origin: ${analysis.estimatedOrigin}. ")
                append("Testing: ${analysis.conditionAssessment} ")
                append("[AI confidence: ${(analysis.verificationConfidence * 100).toInt()}% – verify against a datasheet before reuse.]")
            }
        )
    }
}
