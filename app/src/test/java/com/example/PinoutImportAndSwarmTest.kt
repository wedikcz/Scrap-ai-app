package com.example

import com.example.data.local.SeedData
import com.example.data.repository.EvidenceChunk
import com.example.data.repository.HackerExpert
import com.example.data.repository.HackerSwarmEngine
import com.example.data.repository.PinoutJsonImporter
import com.example.data.repository.RagEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the pinout JSON schema importer and the Smart-Mixture-of-Hackers layer.
 * Robolectric is used because the importer parses with the real org.json
 * implementation shipped in the Android runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinoutImportAndSwarmTest {

    @Test
    fun sampleSchemaImportsAndBuildsPinMap() {
        val report = PinoutJsonImporter.parse(PinoutJsonImporter.SAMPLE_JSON)

        assertEquals(1, report.declared)
        assertEquals(0, report.rejectedCount)
        assertTrue(report.isFullyValid)

        val item = report.accepted.single()
        assertEquals("lm7805-to220", item.id)
        assertEquals("LM7805", item.chipCode)
        assertEquals("TO-220", item.packageType)
        assertEquals("UNKNOWN", item.antiHallucinationStatus)
        assertTrue(item.pinDiagram.contains("PIN MAP"))
        assertTrue(item.pinDiagram.contains("LM7805"))
        assertTrue(item.pinDetailsJson.contains("Pin 1: IN"))
        assertTrue(item.pinDetailsJson.contains("Pin 3: OUT"))
        assertEquals(1, item.version)
    }

    @Test
    fun rejectsPinCountMismatchWithReason() {
        val json = """
        {
          "schemaVersion": 1,
          "components": [
            {
              "name": "Broken Chip",
              "chipCode": "BRK1",
              "packageType": "SOP-8",
              "pinCount": 8,
              "pins": [
                { "number": 1, "name": "VCC" },
                { "number": 2, "name": "GND" }
              ]
            }
          ]
        }
        """.trimIndent()

        val report = PinoutJsonImporter.parse(json)
        assertEquals(0, report.accepted.size)
        assertEquals(1, report.rejectedCount)
        assertTrue(report.errors.single().message.contains("pinCount"))
    }

    @Test
    fun rejectsComponentWithoutPins() {
        val json = """
        {
          "components": [
            { "name": "No Pins", "chipCode": "NOPIN", "packageType": "QFN", "pinCount": 4 }
          ]
        }
        """.trimIndent()

        val report = PinoutJsonImporter.parse(json)
        assertEquals(0, report.accepted.size)
        assertTrue(report.errors.single().message.contains("pins"))
    }

    @Test
    fun rejectsDuplicateIdsInsideOneBatch() {
        val component = """
            {
              "name": "Duplicate $1",
              "chipCode": "DUP1",
              "packageType": "SOT-23",
              "pinCount": 1,
              "pins": [ { "number": 1, "name": "A" } ]
            }
        """.trimIndent()
        val json = """{ "components": [ $component, $component ] }"""

        val report = PinoutJsonImporter.parse(json)
        assertEquals(1, report.accepted.size)
        assertEquals(1, report.rejectedCount)
        assertTrue(report.errors.single().message.contains("Duplicitní"))
    }

    @Test
    fun downgradesVerifiedClaimWithoutDatasheet() {
        val json = """
        {
          "components": [
            {
              "name": "Claimed Verified",
              "chipCode": "CLV1",
              "packageType": "DIP-8",
              "pinCount": 1,
              "antiHallucinationStatus": "VERIFIED",
              "pins": [ { "number": 1, "name": "VCC" } ]
            }
          ]
        }
        """.trimIndent()

        val report = PinoutJsonImporter.parse(json)
        val item = report.accepted.single()
        assertEquals("PROBABLE", item.antiHallucinationStatus)
        assertTrue(report.warnings.any { it.message.contains("VERIFIED") })
    }

    @Test
    fun rejectsJsonWithoutComponentsArray() {
        val report = PinoutJsonImporter.parse("""{ "foo": 1 }""")
        assertEquals(0, report.accepted.size)
        assertEquals(1, report.rejectedCount)
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val report = PinoutJsonImporter.parse("""{ "schemaVersion": 99, "components": [] }""")
        assertEquals(0, report.accepted.size)
        assertTrue(report.errors.single().message.contains("schemaVersion"))
    }

    @Test
    fun swarmAlwaysProducesFourGroundedPerspectives() = runBlocking {
        val result = RagEngine().queryLocalRag("TP4056 proud", SeedData.INITIAL_RAG_DOCS)

        assertEquals(4, result.swarm.perspectives.size)
        assertEquals(HackerExpert.entries.size, result.swarm.perspectives.size)
        assertTrue(result.swarm.isGrounded)

        result.swarm.perspectives.filter { it.hasEvidence }.forEach { perspective ->
            assertTrue("Confidence must be positive with evidence", perspective.confidence > 0)
            assertTrue("Perspective must cite a chunk", perspective.citations.isNotEmpty())
            perspective.citations.forEach { citation ->
                assertTrue("Citation format must be [zdroj: ..., část N]", citation.startsWith("[zdroj:"))
                assertTrue("Citation must be part of the RAG citations", result.citations.contains(citation))
            }
        }
    }

    @Test
    fun swarmRefusesWithoutDocumentaryEvidence() = runBlocking {
        val result = RagEngine().queryLocalRag("Kvantový warp pohon", SeedData.INITIAL_RAG_DOCS)

        assertFalse(result.hasCitations)
        assertEquals(4, result.swarm.perspectives.size)
        assertTrue(result.swarm.perspectives.none { it.hasEvidence })
        assertEquals(0f, result.swarm.agreementRatio, 0.001f)
        assertTrue(result.swarm.consensus.contains("odmítá odpovědět"))
        assertEquals(4, result.swarm.gaps.size)
    }

    @Test
    fun swarmMatchesSpecialistDomainsOnly() {
        val chunk = EvidenceChunk(
            docTitle = "ESP32_Boot.txt",
            partIndex = 2,
            text = "GPIO0 must be low during boot to enter the UART flash mode via esptool."
        )

        val verdict = HackerSwarmEngine().analyze("boot", listOf(chunk))

        val firmware = verdict.perspectives.first { it.expert == HackerExpert.FIRMWARE_ENGINEER }
        assertTrue(firmware.hasEvidence)
        assertTrue(firmware.verdict.contains("esptool"))
        assertEquals("[zdroj: ESP32_Boot.txt, část 2]", firmware.citations.single())

        val optimizer = verdict.perspectives.first { it.expert == HackerExpert.SCRAP_OPTIMIZER }
        assertFalse("Unrelated domain must stay ungrounded", optimizer.hasEvidence)
        assertTrue(verdict.gaps.contains(HackerExpert.SCRAP_OPTIMIZER.label))
    }
}
