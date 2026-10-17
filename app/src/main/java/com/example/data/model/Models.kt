package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String, val label: String) {
    CZ("cs", "CZ"),
    EN("en", "EN")
}

enum class NetworkMode(val labelCz: String, val labelEn: String, val badge: String) {
    AUTO_HYBRID("Auto (Hybridní)", "Auto (Hybrid)", "HYBRID"),
    CLOUD_ONLY("Cloud Only (AI Gateway)", "Cloud Only (AI Gateway)", "CLOUD"),
    LOCAL_AIRGAPPED("Local Only (Air-Gapped)", "Local Only (Air-Gapped)", "OFFLINE")
}

enum class LiteLlmProvider(val label: String, val badge: String) {
    CLOUD_GATEWAY("Cloud AI Gateway (Convex/Gemini)", "CLOUD-GW"),
    LOCAL_EDGE_NEURAL("Local Edge Neural Engine (aarch64)", "EDGE-NN"),
    AIR_GAPPED_FALLBACK("Heuristic Rule Base (DEMO)", "DEMO-FALLBACK")
}

data class GatewayTelemetry(
    val totalRequests: Int = 16,
    val successfulRequests: Int = 15,
    val failedRequests: Int = 1,
    val healedRequests: Int = 1,
    val totalTokens: Long = 6240L,
    val lastLatencyMs: Long = 180L,
    val activeProvider: LiteLlmProvider = LiteLlmProvider.CLOUD_GATEWAY,
    val isAutoHealingActive: Boolean = true
)

data class SelfHealingTraceLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val errorDetails: String,
    val healingStrategy: String,
    val isResolved: Boolean = true
)

@Entity(tableName = "system_error_logs")
data class SystemErrorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val errorCode: String,
    val message: String,
    val component: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = true
)

@Entity(tableName = "salvage_items")
data class SalvageItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int = 1,
    val minQuantityAlert: Int = 2,
    val sourceDevice: String = "Broken Appliance",
    val condition: String = "Tested Working",
    val estimatedValueUsd: Double = 2.50,
    val specs: String = "",
    val pinCount: Int = 8,
    val testedWorking: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "pinout_items")
data class PinoutItem(
    @PrimaryKey val id: String,
    val name: String,
    val chipCode: String,
    val packageType: String,
    val voltageRange: String,
    val description: String,
    val pinDiagram: String,
    val pinDetailsJson: String,
    val hackerTip: String,
    val antiHallucinationStatus: String = "VERIFIED", // VERIFIED, PROBABLE, UNKNOWN
    val googleDork: String = "",
    val datasheetUrl: String = "",
    val suggestedDatasheetUrl: String = "",
    val version: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis(),
    val revisionNotes: String = "Initial verified salvage baseline"
)

@Entity(tableName = "salvage_projects")
data class SalvageProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val status: String, // IDEA, IN_PROGRESS, TESTING, DONE
    val category: String,
    val description: String,
    val requiredParts: String,
    val difficulty: String,
    val stepByStep: String,
    val matrixIndex: Int = 0
)

@Entity(tableName = "rag_documents")
data class RagDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceName: String,
    val content: String,
    val category: String,
    val dateAdded: Long = System.currentTimeMillis()
)

data class RepurposeMatrixItem(
    val index: Int,
    val title: String,
    val category: String,
    val salvageSources: String,
    val keySalvagedChips: String,
    val difficulty: String,
    val description: String,
    val pinoutHint: String
)

data class HackerCoreAnalysis(
    val componentName: String,
    val packageType: String,
    val estimatedOrigin: String,
    val conditionAssessment: String,
    val hardwareAnalysis: String,
    val rfAnalysis: String,
    val secOpsAnalysis: String,
    val firmwareAnalysis: String,
    val selfCritiqueReflections: List<String>,
    val osintGoogleDorks: List<String>,
    val topEdcMatches: List<String>,
    val isDemoFallback: Boolean = false,
    val verificationConfidence: Float = 0.98f,
    val gatewayProviderUsed: LiteLlmProvider = LiteLlmProvider.CLOUD_GATEWAY,
    val tokenUsageEstimate: Int = 380,
    val latencyMs: Long = 210L,
    val reflectionNotes: String = "Pass 1 & Pass 2 merged with cross-verification"
)
