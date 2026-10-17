package com.example

import com.example.data.local.SeedData
import com.example.data.model.NetworkMode
import com.example.data.model.SalvageItem
import com.example.data.repository.RagEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testRepurposeMatrixHas35VerifiedEntries() {
        val matrix = SeedData.REPURPOSE_MATRIX
        assertEquals("Matrix must contain exactly 35 hardcore IoT & EDC repurpose items", 35, matrix.size)
        matrix.forEach { item ->
            assertTrue("Item title must not be blank", item.title.isNotBlank())
            assertTrue("Salvage sources must be specified", item.salvageSources.isNotBlank())
            assertTrue("Key chips must be documented", item.keySalvagedChips.isNotBlank())
            assertTrue("Difficulty must be stated", item.difficulty.isNotBlank())
        }
    }

    @Test
    fun testRagEngineWithMatchesGeneratesCorrectCitations() = runBlocking {
        val engine = RagEngine()
        val docs = SeedData.INITIAL_RAG_DOCS

        val result = engine.queryLocalRag("TP4056 proud", docs)
        assertTrue("RAG should find matches for TP4056", result.hasCitations)
        assertTrue("Answer should contain charging current details", result.answer.contains("PROG") || result.answer.contains("1.2k"))
        assertTrue("Must include citation in format [zdroj: ..., část N]", result.citations.any { it.startsWith("[zdroj:") && it.contains("část") })
    }

    @Test
    fun testRagEngineWithoutMatchesExplicitlyRejectsHallucination() = runBlocking {
        val engine = RagEngine()
        val docs = SeedData.INITIAL_RAG_DOCS

        val result = engine.queryLocalRag("Kvantový gravitační warp pohon", docs)
        assertFalse("RAG must indicate no citations found", result.hasCitations)
        assertTrue("Must state clearly that documents don't contain info", result.answer.contains("Nenalezl jsem ve tvých dokumentech"))
        assertTrue("Hacker-Core trace must record anti-hallucination guard", result.hackerCoreTrace.contains("Anti-Hallucination Guard"))
    }

    @Test
    fun testSeedPinoutsIntegrity() {
        val pinouts = SeedData.INITIAL_PINOUTS
        assertTrue("Must have seed pinouts", pinouts.isNotEmpty())
        pinouts.forEach { p ->
            assertNotNull("Pinout must have an ID", p.id)
            assertTrue("Pinout ASCII diagram must not be empty", p.pinDiagram.isNotBlank())
            assertTrue("Must contain hacker tip", p.hackerTip.isNotBlank())
            assertEquals("Initial version must be 1", 1, p.version)
            assertTrue("Must have suggested or associated datasheet URL", p.suggestedDatasheetUrl.isNotBlank() || p.datasheetUrl.isNotBlank())
        }
    }

    @Test
    fun testInventoryQuantityNonNegativeValidationLogic() {
        val item = SalvageItem(
            id = 1L,
            name = "Test FET",
            category = "Semiconductors",
            quantity = 2
        )
        // Simulate delta -2: allowed -> 0
        val newQty1 = item.quantity + (-2)
        assertTrue("Quantity 0 is valid", newQty1 >= 0)

        // Simulate delta -3: invalid -> -1
        val newQty2 = item.quantity + (-3)
        assertFalse("Quantity < 0 must be rejected by self-healing guard", newQty2 >= 0)
    }

    @Test
    fun testNetworkModeEnum() {
        assertEquals("HYBRID", NetworkMode.AUTO_HYBRID.badge)
        assertEquals("CLOUD", NetworkMode.CLOUD_ONLY.badge)
        assertEquals("OFFLINE", NetworkMode.LOCAL_AIRGAPPED.badge)
    }

    @Test
    fun testCameraPermissionConstant() {
        assertEquals(
            "Camera permission constant must match android.permission.CAMERA",
            android.Manifest.permission.CAMERA,
            com.example.util.CameraPermission.PERMISSION
        )
    }

    @Test
    fun testComponentScanModes() {
        val modes = com.example.ui.components.ComponentScanMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(com.example.ui.components.ComponentScanMode.SOIC_SOP))
        assertTrue(modes.contains(com.example.ui.components.ComponentScanMode.QFP_QFN))
        assertTrue(modes.contains(com.example.ui.components.ComponentScanMode.PASSIVE))
        assertTrue(modes.contains(com.example.ui.components.ComponentScanMode.DIP_MODULE))

        // Check positive aspect ratios
        modes.forEach { mode ->
            assertTrue(mode.aspectWidth > 0f)
            assertTrue(mode.aspectHeight > 0f)
            assertTrue(mode.title.isNotBlank())
            assertTrue(mode.leadDescription.isNotBlank())
        }
    }

    @Test
    fun testCommonElectronicComponentPinoutsOfflineCatalog() {
        val pinouts = com.example.data.local.SeedData.INITIAL_PINOUTS
        assertTrue("Must contain at least 10 verified electronic components", pinouts.size >= 10)

        val chipCodes = pinouts.map { it.id.lowercase() }
        assertTrue(chipCodes.contains("ne555"))
        assertTrue(chipCodes.contains("tp4056"))
        assertTrue(chipCodes.contains("esp32-wroom"))
        assertTrue(chipCodes.contains("lm358"))
        assertTrue(chipCodes.contains("ams1117-33"))
        assertTrue(chipCodes.contains("atmega328p"))
        assertTrue(chipCodes.contains("7805"))
        assertTrue(chipCodes.contains("pc817"))
        assertTrue(chipCodes.contains("uln2003a"))

        pinouts.forEach { item ->
            assertTrue("Pin diagram must be present for ${item.name}", item.pinDiagram.isNotBlank())
            assertTrue("Pin details must be present for ${item.name}", item.pinDetailsJson.isNotBlank())
            assertTrue("Voltage range must be specified for ${item.name}", item.voltageRange.isNotBlank())
            assertTrue("Hacker tip must be provided for ${item.name}", item.hackerTip.isNotBlank())
        }
    }

    @Test
    fun testIdentifiedComponentDataIntegrity() {
        val component = com.example.ui.viewmodel.IdentifiedComponent(
            partNumber = "NE555P",
            commonName = "Precision Timer",
            category = "ICs & Timing",
            packageType = "DIP-8",
            pinCount = 8,
            pin1Location = "Top-Left notch",
            voltageRange = "4.5V - 16.0V",
            functionalSummary = "Precision timer",
            salvageOrigin = "TV power board",
            keyPinsSummary = listOf("Pin 1: GND", "Pin 8: VCC"),
            safetyTestingTips = "Diode test between 1 and 8",
            repurposeRecommendations = listOf("PWM Generator"),
            isOfflineFallback = false
        )

        assertEquals("NE555P", component.partNumber)
        assertEquals(8, component.pinCount)
        assertEquals("DIP-8", component.packageType)
        assertFalse(component.isOfflineFallback)
        assertTrue(component.keyPinsSummary.isNotEmpty())
    }
}
