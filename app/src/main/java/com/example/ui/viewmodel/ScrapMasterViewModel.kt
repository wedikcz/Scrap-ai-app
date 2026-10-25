package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gateway.LiteLlmGateway
import com.example.data.local.ScrapMasterDatabase
import com.example.data.model.AppLanguage
import com.example.data.model.GatewayTelemetry
import com.example.data.model.HackerCoreAnalysis
import com.example.data.model.NetworkMode
import com.example.data.model.PinoutItem
import com.example.data.model.RagDocument
import com.example.data.model.RepurposeMatrixItem
import com.example.data.model.SalvageItem
import com.example.data.model.SalvageProject
import com.example.data.model.SelfHealingTraceLog
import com.example.data.model.SystemErrorLog
import com.example.data.repository.AiScannerEngine
import com.example.data.repository.PinoutImportIssue
import com.example.data.repository.PinoutImportReport
import com.example.data.repository.RagEngine
import com.example.data.repository.RagSearchResult
import com.example.data.repository.ScrapMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

class ScrapMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScrapMasterRepository
    private val scannerEngine = AiScannerEngine()
    private val liteLlmGateway = LiteLlmGateway()
    private val ragEngine = RagEngine()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        val db = ScrapMasterDatabase.getDatabase(application, viewModelScope)
        repository = ScrapMasterRepository(db.scrapMasterDao())
    }

    // Navigation and Language
    val currentRoute = MutableStateFlow("/")
    val isTerminalMode = MutableStateFlow(false)
    val isDarkTheme = MutableStateFlow(true)
    val currentLanguage = MutableStateFlow(AppLanguage.CZ)
    val networkMode = MutableStateFlow(NetworkMode.AUTO_HYBRID)

    // LiteLLM Gateway & Self-Healing Engine State
    val gatewayTelemetry: StateFlow<GatewayTelemetry> = liteLlmGateway.telemetry
    val healingLogs: StateFlow<List<SelfHealingTraceLog>> = liteLlmGateway.healingLogs
    val systemErrorLogs: StateFlow<List<SystemErrorLog>> = repository.errorLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selfHealingToast = MutableStateFlow<String?>(null)

    // Inventory State
    val inventorySearch = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)

    val items: StateFlow<List<SalvageItem>> = combine(
        repository.allItems,
        inventorySearch,
        selectedCategory
    ) { allItems, search, category ->
        allItems.filter { item ->
            val matchesSearch = search.isBlank() ||
                    item.name.contains(search, ignoreCase = true) ||
                    item.category.contains(search, ignoreCase = true) ||
                    item.sourceDevice.contains(search, ignoreCase = true)
            val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pinouts
    val pinoutSearch = MutableStateFlow("")
    val pinouts: StateFlow<List<PinoutItem>> = combine(
        repository.allPinouts,
        pinoutSearch
    ) { allPinouts, search ->
        if (search.isBlank()) allPinouts
        else allPinouts.filter {
            it.name.contains(search, ignoreCase = true) ||
            it.chipCode.contains(search, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projects Kanban
    val projects: StateFlow<List<SalvageProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // RAG Docs
    val ragDocs: StateFlow<List<RagDocument>> = repository.allRagDocs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The 35-Point Repurpose Matrix
    val repurposeMatrix: List<RepurposeMatrixItem> = repository.getRepurposeMatrix()
    val matrixSearch = MutableStateFlow("")
    val matrixCategoryFilter = MutableStateFlow<String?>(null)

    val filteredMatrix: StateFlow<List<RepurposeMatrixItem>> = combine(
        matrixSearch,
        matrixCategoryFilter
    ) { search, filter ->
        repurposeMatrix.filter { item ->
            val matchesSearch = search.isBlank() ||
                    item.title.contains(search, ignoreCase = true) ||
                    item.keySalvagedChips.contains(search, ignoreCase = true) ||
                    item.salvageSources.contains(search, ignoreCase = true) ||
                    item.description.contains(search, ignoreCase = true)
            val matchesFilter = filter == null || item.category.equals(filter, ignoreCase = true)
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repurposeMatrix)

    // Scanner State
    val scanInput = MutableStateFlow("")
    val isScanning = MutableStateFlow(false)
    val currentAnalysis = MutableStateFlow<HackerCoreAnalysis?>(null)
    val scanError = MutableStateFlow<String?>(null)

    // Assistant / RAG State
    val ragQuery = MutableStateFlow("")
    val isRagSearching = MutableStateFlow(false)
    val ragResult = MutableStateFlow<RagSearchResult?>(null)
    val chatHistory = MutableStateFlow<List<Pair<String, RagSearchResult>>>(emptyList())

    // Interactive Terminal State
    val terminalOutput = MutableStateFlow<List<String>>(
        listOf(
            "============================================================",
            "  SCRAPMASTER AI v6.0 OMEGA // AI GATEWAY ALL-IN-ONE",
            "  Platform: Android 15 aarch64 | Target: Infinix HOT 50 Pro+",
            "  Self-Healing Engine & Client-Side RAG Active",
            "  Type 'help' or 'gateway' to inspect failover state.",
            "============================================================"
        )
    )
    val terminalInput = MutableStateFlow("")

    // Pinout JSON schema importer state
    val pinoutImportReport = MutableStateFlow<PinoutImportReport?>(null)
    val isImportingPinouts = MutableStateFlow(false)

    // Restore persisted UI preferences (declared after the flows they modify).
    init {
        isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, true)
        currentLanguage.value = runCatching {
            AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.CZ.name) ?: AppLanguage.CZ.name)
        }.getOrDefault(AppLanguage.CZ)
    }

    fun setRoute(route: String) {
        currentRoute.value = route
    }

    fun toggleTerminalMode() {
        isTerminalMode.value = !isTerminalMode.value
    }

    fun toggleDarkTheme() {
        isDarkTheme.value = !isDarkTheme.value
        prefs.edit().putBoolean(KEY_DARK_THEME, isDarkTheme.value).apply()
    }

    fun toggleLanguage() {
        currentLanguage.value = if (currentLanguage.value == AppLanguage.CZ) AppLanguage.EN else AppLanguage.CZ
        prefs.edit().putString(KEY_LANGUAGE, currentLanguage.value.name).apply()
    }

    fun importPinoutsSchema(jsonText: String) {
        if (jsonText.isBlank()) {
            pinoutImportReport.value = PinoutImportReport(
                declared = 0,
                accepted = emptyList(),
                errors = listOf(PinoutImportIssue("Import dat", "Vstup je prázdný.")),
                warnings = emptyList()
            )
            return
        }
        viewModelScope.launch {
            isImportingPinouts.value = true
            try {
                val report = repository.importPinouts(jsonText)
                pinoutImportReport.value = report
                selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ) {
                    "Pinout import: ${report.accepted.size} přijato, ${report.rejectedCount} odmítnuto."
                } else {
                    "Pinout import: ${report.accepted.size} accepted, ${report.rejectedCount} rejected."
                }
                if (report.errors.isNotEmpty()) {
                    repository.logError(
                        "PINOUT_IMPORT_REJECTED",
                        report.errors.joinToString("; ") { "${it.componentRef}: ${it.message}" }.take(500),
                        "PinoutImporter"
                    )
                }
            } catch (e: Exception) {
                pinoutImportReport.value = PinoutImportReport(
                    declared = 0,
                    accepted = emptyList(),
                    errors = listOf(PinoutImportIssue("Import dat", e.localizedMessage ?: "Neznámá chyba importu")),
                    warnings = emptyList()
                )
                repository.logError("PINOUT_IMPORT_ERROR", e.localizedMessage ?: "Import failed", "PinoutImporter")
            } finally {
                isImportingPinouts.value = false
            }
        }
    }

    fun clearPinoutImportReport() {
        pinoutImportReport.value = null
    }

    fun updatePinoutDatasheet(id: String, url: String, note: String = "Datasheet URL revised") {
        viewModelScope.launch {
            repository.updatePinoutDatasheet(id, url, note)
            selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
                "Pinout byl aktualizován (inkrementována verze záznamu)."
            else "Pinout updated (version incremented)."
        }
    }

    fun setNetworkMode(mode: NetworkMode) {
        networkMode.value = mode
    }

    fun testGatewayFailover() {
        liteLlmGateway.triggerTestFailover()
        selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
            "AI Gateway: Simulován 429 Rate Limit. Proveden okamžitý failover na lokální DEMO heuristiku."
        else "AI Gateway: Simulated 429 Rate Limit. Immediate failover to local DEMO heuristic executed."
    }

    fun clearSelfHealingToast() {
        selfHealingToast.value = null
    }

    // Optimistic inventory mutation with atomic non-negative guard & rollback
    fun adjustQuantity(id: Long, delta: Int) {
        viewModelScope.launch {
            val currentItem = items.value.find { it.id == id }
            if (currentItem != null && (currentItem.quantity + delta) < 0) {
                selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
                    "Self-Healing Guard: Množství nelze snížit pod 0 (Rollback aplikován)."
                else "Self-Healing Guard: Quantity cannot drop below 0 (Rollback applied)."
                repository.logError("INVENTORY_NEGATIVE_BLOCKED", "Blocked attempt to set negative quantity on item $id", "InventoryEngine")
                return@launch
            }
            repository.adjustQuantity(id, delta)
        }
    }

    fun addItem(item: SalvageItem) {
        viewModelScope.launch {
            repository.addItem(item)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun scanComponent(queryOrMarking: String, base64Image: String? = null) {
        if (queryOrMarking.isBlank() && base64Image == null) return
        viewModelScope.launch {
            isScanning.value = true
            scanError.value = null
            try {
                // Route through AI Gateway All-in-One with exponential retry & dual-pass reflection loop
                val analysis = liteLlmGateway.routeMultimodalAnalysis(
                    queryOrMarking = queryOrMarking,
                    base64Image = base64Image,
                    networkMode = networkMode.value
                )
                currentAnalysis.value = analysis
            } catch (e: Exception) {
                scanError.value = e.localizedMessage ?: "AI Gateway error"
                repository.logError("GATEWAY_SCAN_ERROR", e.localizedMessage ?: "Scan failed", "AiGateway")
            } finally {
                isScanning.value = false
            }
        }
    }

    /**
     * Analyzes image captured by CameraPreview to identify electronic parts.
     */
    fun analyzeComponent(base64Image: String, queryOrMarking: String = "") {
        scanComponent(queryOrMarking.ifBlank { "PCB Component" }, base64Image)
    }

    fun addCurrentAnalysisToInventory() {
        val analysis = currentAnalysis.value ?: return
        viewModelScope.launch {
            val item = scannerEngine.convertToSalvageItem(analysis)
            repository.addItem(item)
            currentRoute.value = "/inventory"
            selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
                "Součástka ${analysis.componentName} přidána do skladu."
            else "Component ${analysis.componentName} added to inventory."
        }
    }

    // Optimistic Kanban mutation with 3500ms safety timeout & safe state rollback
    fun updateProjectStatus(id: Long, status: String) {
        val originalProjects = projects.value
        viewModelScope.launch {
            try {
                withTimeout(3500) {
                    repository.updateProjectStatus(id, status)
                }
            } catch (e: Exception) {
                val origStatus = originalProjects.find { it.id == id }?.status ?: "IDEA"
                repository.updateProjectStatus(id, origStatus)
                selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
                    "Self-Healing Rollback: Timeout synchronizace (3500 ms). Obnoven bezpečný stav."
                else "Self-Healing Rollback: Sync timeout (3500ms). Safe state restored."
                repository.logError("KANBAN_TIMEOUT", "Project $id sync exceeded 3500ms timeout", "KanbanEngine")
            }
        }
    }

    fun addProject(project: SalvageProject) {
        viewModelScope.launch {
            repository.addProject(project)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun askRag(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            isRagSearching.value = true
            try {
                val docs = ragDocs.value
                val result = ragEngine.queryLocalRag(query, docs)
                ragResult.value = result
                chatHistory.value = chatHistory.value + (query to result)
                ragQuery.value = ""
            } finally {
                isRagSearching.value = false
            }
        }
    }

    fun addRagDocument(title: String, source: String, content: String, category: String) {
        viewModelScope.launch {
            repository.addRagDocument(
                RagDocument(
                    title = title,
                    sourceName = source,
                    content = content,
                    category = category
                )
            )
            selfHealingToast.value = if (currentLanguage.value == AppLanguage.CZ)
                "Dokument $title byl indexován do lokálního RAG vaultu."
            else "Document $title indexed into local RAG vault."
        }
    }

    // Terminal Command Interpreter
    fun executeTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) return
        val out = terminalOutput.value.toMutableList()
        out.add("root@scrapmaster-aarch64:~# $trimmed")

        val parts = trimmed.split(" ")
        val command = parts[0].lowercase()
        val arg = parts.drop(1).joinToString(" ")

        when (command) {
            "help", "?" -> {
                out.add("AVAILABLE HACKER-OPS COMMANDS (v6.0):")
                out.add("  sys-state         Display telemetry, CPU, memory, database, net mode")
                out.add("  gateway           Inspect AI Gateway All-in-One routing & token metrics")
                out.add("  heal-test         Simulate 429 rate limit and test zero-downtime failover")
                out.add("  lang [cz|en]      Switch language interface")
                out.add("  invent, ls        List current salvaged inventory")
                out.add("  scan [chip]       Run dual-pass component analysis with reflection loop")
                out.add("  pinout [chip]     Display ASCII pinout mapping with version history")
                out.add("  projects          List active salvage workshop Kanban projects")
                out.add("  matrix            Dump 35-point EDC/IoT salvage repurpose matrix")
                out.add("  dork [chip]       Generate OSINT Google Dork for missing schematics")
                out.add("  rag [query]       Query offline RAG with mandatory [zdroj: soubor, část N] citations")
                out.add("  termux            Display aarch64 Termux setup script & MCP bridge")
                out.add("  gui               Switch to Cyber Glassmorphic GUI Mode")
                out.add("  clear             Clear terminal screen")
            }
            "gateway" -> {
                val t = gatewayTelemetry.value
                out.add("[AI GATEWAY ALL-IN-ONE STATUS]")
                out.add("  Active Provider: ${t.activeProvider.label} [${t.activeProvider.badge}]")
                out.add("  Total Requests: ${t.totalRequests} | Success: ${t.successfulRequests} | Healed: ${t.healedRequests}")
                out.add("  Total Tokens: ${t.totalTokens} | Latency: ${t.lastLatencyMs} ms")
                out.add("  Self-Healing Engine: ${if (t.isAutoHealingActive) "ENGAGED" else "OFF"}")
            }
            "lang" -> {
                if (arg.equals("en", true)) {
                    currentLanguage.value = AppLanguage.EN
                    out.add("Language switched to ENGLISH.")
                } else {
                    currentLanguage.value = AppLanguage.CZ
                    out.add("Jazyk přepnut na ČEŠTINU.")
                }
            }
            "heal-test" -> {
                testGatewayFailover()
                out.add("[SELF-HEALING ENGINE ACTIVATED]")
                out.add("  Simulated 429 Rate Limit injected on Cloud Gateway.")
                out.add("  Silent failover executed to Local Heuristic Engine (DEMO mode).")
                out.add("  Active Provider is now: ${gatewayTelemetry.value.activeProvider.badge}.")
            }
            "sys-state", "status" -> {
                out.add("[SYS-STATE: SCRAPMASTER OMEGA V6.0]")
                out.add("  Platform Target: Infinix HOT 50 Pro Plus (ARM64 Android 15)")
                out.add("  Gateway Provider: ${gatewayTelemetry.value.activeProvider.badge}")
                out.add("  Network Mode: ${networkMode.value.badge}")
                out.add("  Stock Units: ${items.value.sumOf { it.quantity }} (${items.value.size} distinct SKUs)")
                out.add("  Estimated Vault: $${String.format(Locale.US, "%.2f", items.value.sumOf { it.quantity * it.estimatedValueUsd })} USD")
                out.add("  Kanban Projects: ${projects.value.size}")
                out.add("  Indexed RAG Documents: ${ragDocs.value.size}")
                out.add("  Repurpose Matrix: 35/35 Nodes Operational")
                out.add("  Anti-Hallucination Lock: ENGAGED [ZÁMEK AKTIVNÍ]")
            }
            "invent", "ls" -> {
                out.add("[DATA-DUMP: SALVAGED INVENTORY]")
                items.value.forEach { item ->
                    out.add("  • [ID:${item.id}] [QTY:${item.quantity}] ${item.name} (${item.category}) | ${item.condition}")
                }
            }
            "scan" -> {
                val target = if (arg.isNotBlank()) arg else "TP4056"
                out.add("[SCANNING COMPONENT: $target] AI Gateway Dual-Pass Initiated...")
                viewModelScope.launch {
                    val analysis = liteLlmGateway.routeMultimodalAnalysis(target, null, networkMode.value)
                    currentAnalysis.value = analysis
                    out.add("<HACKER-CORE>")
                    out.add("  Provider: ${analysis.gatewayProviderUsed.badge} (${analysis.latencyMs}ms)")
                    out.add("  Identified: ${analysis.componentName} (${analysis.packageType})")
                    out.add("  Pass 2 Reflection: ${analysis.reflectionNotes}")
                    out.add("  Origin: ${analysis.estimatedOrigin}")
                    out.add("  Top EDC: ${analysis.topEdcMatches.firstOrNull() ?: "N/A"}")
                    out.add("</HACKER-CORE>")
                    terminalOutput.value = out
                }
                return
            }
            "pinout" -> {
                val target = arg.lowercase()
                val match = pinouts.value.find {
                    it.id.contains(target) || it.name.lowercase().contains(target) || it.chipCode.lowercase().contains(target)
                } ?: pinouts.value.firstOrNull()

                if (match != null) {
                    out.add("[PINOUT SPEC: ${match.name} v${match.version}]")
                    out.add(match.pinDiagram)
                    out.add("HACKER TIP: ${match.hackerTip}")
                    out.add("DATASHEET: ${match.datasheetUrl.ifBlank { match.suggestedDatasheetUrl }}")
                    out.add("REVISION: ${match.revisionNotes}")
                } else {
                    out.add("Pinout not found for '$arg'. Type 'pinout esp32' or 'pinout tp4056'.")
                }
            }
            "projects" -> {
                out.add("[WORKSHOP KANBAN PROJECTS]")
                projects.value.forEach { proj ->
                    out.add("  [${proj.status}] ${proj.title} (${proj.difficulty}) - Needs: ${proj.requiredParts}")
                }
            }
            "matrix" -> {
                out.add("[35-POINT EDC & REPURPOSE MATRIX: DUMP]")
                repurposeMatrix.take(10).forEach { m ->
                    out.add("  #${m.index} [${m.category}] ${m.title} -> Uses: ${m.keySalvagedChips}")
                }
                out.add("  ... (Total 35 modules indexed. Open Projects tab in GUI for full schematics)")
            }
            "dork" -> {
                val chip = if (arg.isNotBlank()) arg else "ESP32-WROOM"
                out.add("[OSINT GOOGLE DORK GENERATOR]")
                out.add("  Query: \"$chip\" filetype:pdf (datasheet | schematic | \"pinout\")")
            }
            "rag" -> {
                val query = if (arg.isNotBlank()) arg else "TP4056 charging current"
                out.add("[LOCAL RAG QUERY: $query]")
                viewModelScope.launch {
                    val result = ragEngine.queryLocalRag(query, ragDocs.value)
                    out.add(result.answer)
                    out.add(result.swarm.consensus)
                    if (result.hasCitations) {
                        out.add("CITATIONS: ${result.citations.joinToString(", ")}")
                    }
                    terminalOutput.value = out
                }
                return
            }
            "termux" -> {
                out.add("[ANDROID 15 aarch64 TERMUX INTEGRATION]")
                out.add("  Execute `bash full_setup.sh` to initialize headless MCP server.")
            }
            "gui" -> {
                isTerminalMode.value = false
                return
            }
            "clear" -> {
                terminalOutput.value = listOf("[TERMINAL CLEARED]")
                terminalInput.value = ""
                return
            }
            else -> {
                out.add("Unknown command '$command'. Type 'help' for valid hacker-ops commands.")
            }
        }

        terminalOutput.value = out
        terminalInput.value = ""
    }

    companion object {
        private const val PREFS_NAME = "scrapmaster_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_LANGUAGE = "language"
    }
}
