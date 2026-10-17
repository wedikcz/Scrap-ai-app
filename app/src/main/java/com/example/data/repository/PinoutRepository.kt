package com.example.data.repository

import com.example.data.local.PinoutDao
import com.example.data.model.PinoutItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for electronic component pinouts,
 * abstracting data access from ViewModel and ensuring offline availability.
 */
class PinoutRepository(
    private val pinoutDao: PinoutDao
) {
    val allPinouts: Flow<List<PinoutItem>> = pinoutDao.getAllPinouts()

    fun searchPinouts(query: String): Flow<List<PinoutItem>> =
        if (query.isBlank()) pinoutDao.getAllPinouts() else pinoutDao.searchPinouts(query)

    suspend fun getPinoutById(id: String): PinoutItem? = pinoutDao.getPinoutById(id)

    suspend fun findPinoutForComponent(chipCodeOrName: String): PinoutItem? {
        val direct = pinoutDao.findPinoutByChipCode(chipCodeOrName)
        if (direct != null) return direct

        // Also sanitize common prefix/suffixes (e.g. "NE555P" -> "NE555", "AMS1117-3.3" -> "AMS1117")
        val cleanCode = chipCodeOrName.trim().uppercase()
            .replace(Regex("[-_ ]"), "")
        return pinoutDao.findPinoutByChipCode(cleanCode)
    }

    suspend fun savePinout(pinout: PinoutItem): Long = pinoutDao.insertPinout(pinout)

    suspend fun savePinouts(pinouts: List<PinoutItem>) = pinoutDao.insertPinouts(pinouts)

    suspend fun updatePinout(pinout: PinoutItem) = pinoutDao.updatePinout(pinout)

    suspend fun updateDatasheet(id: String, url: String, note: String = "Datasheet link revised") =
        pinoutDao.updatePinoutDatasheetUrl(id = id, url = url, note = note)

    suspend fun deletePinout(id: String) = pinoutDao.deletePinout(id)
}
