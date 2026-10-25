package com.example.data.repository

import com.example.data.model.PinoutItem
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * One rejected or downgraded component inside an import batch.
 */
data class PinoutImportIssue(
    val componentRef: String,
    val message: String
)

/**
 * Outcome of parsing a user supplied pinout JSON schema.
 *
 * Nothing is ever silently dropped: every component that cannot be validated
 * ends up in [errors] with the reason, so the importer can report exactly what
 * happened instead of pretending the whole batch succeeded.
 */
data class PinoutImportReport(
    val declared: Int,
    val accepted: List<PinoutItem>,
    val errors: List<PinoutImportIssue>,
    val warnings: List<PinoutImportIssue>
) {
    val rejectedCount: Int get() = errors.size
    val isFullyValid: Boolean get() = errors.isEmpty() && accepted.isNotEmpty()
}

/**
 * Parses the ScrapMaster pinout JSON schema into validated [PinoutItem] rows.
 *
 * Schema (schemaVersion 1):
 * ```json
 * {
 *   "schemaVersion": 1,
 *   "components": [
 *     {
 *       "id": "my-chip-01",
 *       "name": "My Chip",
 *       "chipCode": "MYCHIP",
 *       "packageType": "SOP-8",
 *       "pinCount": 2,
 *       "voltageRange": "3.0V - 3.6V",
 *       "description": "What the chip does",
 *       "pins": [
 *         { "number": 1, "name": "VCC", "type": "power" },
 *         { "number": 2, "name": "GND", "type": "ground" }
 *       ],
 *       "hackerTip": "Salvage advice",
 *       "datasheetUrl": "https://example.com/datasheet.pdf"
 *     }
 *   ]
 * }
 * ```
 *
 * A bare JSON array of components is accepted as well. Data that the JSON does
 * not provide is never invented: missing optional fields stay empty and a
 * component that claims `VERIFIED` without any datasheet link is downgraded to
 * `PROBABLE` with a warning. Imported rows default to `UNKNOWN` verification.
 */
object PinoutJsonImporter {

    const val SCHEMA_VERSION = 1

    /** Example schema shipped in the import dialog and documented in README. */
    val SAMPLE_JSON: String = """
{
  "schemaVersion": 1,
  "components": [
    {
      "id": "lm7805-to220",
      "name": "LM7805 Linear Regulator",
      "chipCode": "LM7805",
      "packageType": "TO-220",
      "pinCount": 3,
      "voltageRange": "7.5V - 35V input / 5V output",
      "description": "Fixed 5V linear regulator, 1.5A with heatsink.",
      "pins": [
        { "number": 1, "name": "IN", "type": "power" },
        { "number": 2, "name": "GND", "type": "ground" },
        { "number": 3, "name": "OUT", "type": "power" }
      ],
      "hackerTip": "Salvaged from router PSUs; test dropout under load before reuse.",
      "datasheetUrl": "https://www.ti.com/lit/ds/symlink/lm7805.pdf"
    }
  ]
}
    """.trimIndent()

    private const val MAX_COMPONENTS = 250
    private const val MAX_JSON_LENGTH = 4_000_000

    private val ALLOWED_STATUS = setOf("VERIFIED", "PROBABLE", "UNKNOWN")

    fun parse(rawJson: String): PinoutImportReport {
        val text = rawJson.trim()
        if (text.isEmpty()) {
            return failure("Import dat", "JSON je prázdný – vlož text schématu nebo vyber soubor.")
        }
        if (text.length > MAX_JSON_LENGTH) {
            return failure("Import dat", "JSON je příliš velký (${text.length} znaků, limit $MAX_JSON_LENGTH).")
        }

        val rootObject = runCatching { JSONObject(text) }.getOrNull()
        if (rootObject != null && rootObject.has("schemaVersion")) {
            val declaredVersion = rootObject.optInt("schemaVersion", -1)
            if (declaredVersion != SCHEMA_VERSION) {
                return failure(
                    "Import dat",
                    "Nepodporovaná verze schématu '$declaredVersion' – tento build čte schemaVersion $SCHEMA_VERSION."
                )
            }
        }

        val array = extractComponentArray(text, rootObject)
            ?: return failure("Import dat", "JSON neobsahuje pole 'components' (ani pole 'pinouts'), ani není polem komponent.")

        if (array.length() == 0) {
            return failure("Import dat", "Pole komponent je prázdné – není co importovat.")
        }
        if (array.length() > MAX_COMPONENTS) {
            return failure("Import dat", "Příliš mnoho komponent (${array.length()}, limit $MAX_COMPONENTS na jeden import.")
        }

        val accepted = mutableListOf<PinoutItem>()
        val errors = mutableListOf<PinoutImportIssue>()
        val warnings = mutableListOf<PinoutImportIssue>()
        val seenIds = mutableMapOf<String, Int>()

        for (index in 0 until array.length()) {
            val ref = "komponenta #${index + 1}"
            val element = array.optJSONObject(index)
            if (element == null) {
                errors.add(PinoutImportIssue(ref, "Není JSON objekt."))
                continue
            }

            val label = element.optString("name").takeIf { it.isNotBlank() }
                ?: element.optString("chipCode").takeIf { it.isNotBlank() }
                ?: ref

            try {
                val parsed = parseComponent(element, label) { message ->
                    errors.add(PinoutImportIssue(label, message))
                } ?: continue

                val (item, componentWarnings) = parsed

                val previous = seenIds[item.id]
                if (previous != null) {
                    errors.add(
                        PinoutImportIssue(label, "Duplicitní id '${item.id}' – poprvé použito komponentou #$previous.")
                    )
                    continue
                }

                componentWarnings.forEach { warnings.add(PinoutImportIssue(label, it)) }
                seenIds[item.id] = index + 1
                accepted.add(item)
            } catch (e: JSONException) {
                errors.add(PinoutImportIssue(label, "Neplatná struktura JSON: ${e.message}"))
            }
        }

        return PinoutImportReport(
            declared = array.length(),
            accepted = accepted,
            errors = errors,
            warnings = warnings
        )
    }

    private fun failure(ref: String, message: String) = PinoutImportReport(
        declared = 0,
        accepted = emptyList(),
        errors = listOf(PinoutImportIssue(ref, message)),
        warnings = emptyList()
    )

    private fun extractComponentArray(text: String, root: JSONObject?): JSONArray? {
        // Accept a top-level array first, then an object that wraps the array.
        runCatching { return JSONArray(text) }
        return root?.optJSONArray("components") ?: root?.optJSONArray("pinouts")
    }

    /**
     * Returns the item plus the list of non-fatal warnings, or null when the
     * component is invalid (the caller receives the reason through [onError]).
     */
    private fun parseComponent(
        element: JSONObject,
        label: String,
        onError: (String) -> Unit
    ): Pair<PinoutItem, List<String>>? {
        val name = element.optString("name").trim()
        if (name.isBlank()) {
            onError("Chybí povinné pole 'name'.")
            return null
        }

        val chipCode = element.optString("chipCode").ifBlank { element.optString("code") }.trim()
        if (chipCode.isBlank()) {
            onError("Chybí povinné pole 'chipCode'.")
            return null
        }

        val packageType = element.optString("packageType").ifBlank { element.optString("package") }.trim()
        if (packageType.isBlank()) {
            onError("Chybí povinné pole 'packageType'.")
            return null
        }

        val pinCount = element.optInt("pinCount", -1)
        if (pinCount !in 1..256) {
            onError("Pole 'pinCount' musí být celé číslo 1–256 (nalezeno: ${element.opt("pinCount")}).")
            return null
        }

        val pins = parsePins(element.optJSONArray("pins"), pinCount, onError) ?: return null

        val id = element.optString("id").takeIf { it.isNotBlank() }?.trim() ?: slug(chipCode)
        if (id.isBlank()) {
            onError("Nelze odvodit 'id' z 'chipCode'.")
            return null
        }

        val warnings = mutableListOf<String>()

        val voltageRange = element.optString("voltageRange").trim()
        if (voltageRange.isBlank()) warnings.add("Chybí 'voltageRange' – v katalogu zůstane prázdné.")

        val description = element.optString("description").trim()
        if (description.isBlank()) warnings.add("Chybí 'description' – karta součástky bude bez popisu.")

        val hackerTip = element.optString("hackerTip").trim()
        if (hackerTip.isBlank()) warnings.add("Chybí 'hackerTip' – tip pro demontáž nebyl dodán.")

        val datasheetUrl = element.optString("datasheetUrl").trim()
        val suggestedUrl = element.optString("suggestedDatasheetUrl").trim()
        if (datasheetUrl.isNotBlank() && !datasheetUrl.startsWith("https://") && !datasheetUrl.startsWith("http://")) {
            onError("'datasheetUrl' musí začínat na http:// nebo https://.")
            return null
        }

        val claimedStatus = element.optString("antiHallucinationStatus").trim().uppercase()
        var status = claimedStatus.ifBlank { "UNKNOWN" }
        if (status !in ALLOWED_STATUS) {
            warnings.add("Neznámý 'antiHallucinationStatus' '$status' – nastaveno UNKNOWN.")
            status = "UNKNOWN"
        }
        if (status == "VERIFIED" && datasheetUrl.isBlank() && suggestedUrl.isBlank()) {
            status = "PROBABLE"
            warnings.add("Stav VERIFIED bez odkazu na datasheet byl snížen na PROBABLE (anti-halucinační pravidlo).")
        }

        val googleDork = element.optString("googleDork").ifBlank {
            "\"$chipCode\" (datasheet | schematic | \"pinout\") filetype:pdf"
        }

        val item = PinoutItem(
            id = id,
            name = name,
            chipCode = chipCode,
            packageType = packageType,
            voltageRange = voltageRange,
            description = description,
            pinDiagram = buildPinDiagram(name, packageType, pins),
            pinDetailsJson = buildPinDetails(pins),
            hackerTip = hackerTip,
            antiHallucinationStatus = status,
            googleDork = googleDork,
            datasheetUrl = datasheetUrl,
            suggestedDatasheetUrl = suggestedUrl,
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            revisionNotes = "Imported from user JSON schema"
        )

        return item to warnings
    }

    private fun parsePins(
        rawPins: JSONArray?,
        pinCount: Int,
        onError: (String) -> Unit
    ): List<PinDefinition>? {
        if (rawPins == null) {
            // A pinout without pins is not a pinout: refuse instead of inventing one.
            onError("Chybí pole 'pins' – bez definice vývodů nelze pinout importovat.")
            return null
        }
        if (rawPins.length() != pinCount) {
            onError("Pole 'pins' má ${rawPins.length()} položek, ale 'pinCount' je $pinCount.")
            return null
        }

        val pins = mutableListOf<PinDefinition>()
        val seenNumbers = mutableSetOf<Int>()

        for (i in 0 until rawPins.length()) {
            val pin = rawPins.optJSONObject(i)
            if (pin == null) {
                onError("Pin #${i + 1} není JSON objekt.")
                return null
            }
            val number = pin.optInt("number", -1)
            if (number !in 1..pinCount) {
                onError("Pin #${i + 1} má neplatné číslo '$number' (musí být 1–$pinCount).")
                return null
            }
            if (!seenNumbers.add(number)) {
                onError("Pin číslo $number je v seznamu vícekrát.")
                return null
            }
            val pinName = pin.optString("name").trim()
            if (pinName.isBlank()) {
                onError("Pin $number nemá 'name'.")
                return null
            }
            val type = pin.optString("type").trim()
            pins.add(PinDefinition(number, pinName, type))
        }

        return pins.sortedBy { it.number }
    }

    private fun buildPinDiagram(
        name: String,
        packageType: String,
        pins: List<PinDefinition>
    ): String {
        val header = "PIN MAP · $name ($packageType)"
        val rows = pins.map { pin ->
            val type = if (pin.type.isBlank()) "" else "  [${pin.type}]"
            "| " + pin.number.toString().padEnd(3) + pin.name.padEnd(12) + type.padEnd(12) + "|"
        }
        val width = (rows + header).maxOf { it.length } - 2
        val border = "+" + "-".repeat(width) + "+"
        return buildString {
            appendLine(border)
            appendLine("| " + header.padEnd(width - 1) + "|")
            appendLine(border)
            rows.forEach { appendLine(it) }
            append(border)
        }
    }

    private fun buildPinDetails(pins: List<PinDefinition>): String =
        pins.joinToString("\n") { pin ->
            val type = if (pin.type.isBlank()) "" else " – ${pin.type}"
            "Pin ${pin.number}: ${pin.name}$type"
        }

    private fun slug(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    private data class PinDefinition(
        val number: Int,
        val name: String,
        val type: String
    )
}
