package com.example.data.repository

import com.example.data.local.ScrapMasterDao
import com.example.data.local.SeedData
import com.example.data.model.PinoutItem
import com.example.data.model.RagDocument
import com.example.data.model.RepurposeMatrixItem
import com.example.data.model.SalvageItem
import com.example.data.model.SalvageProject
import com.example.data.model.SystemErrorLog
import kotlinx.coroutines.flow.Flow

class ScrapMasterRepository(
    private val dao: ScrapMasterDao
) {
    val allItems: Flow<List<SalvageItem>> = dao.getAllSalvageItems()
    val allPinouts: Flow<List<PinoutItem>> = dao.getAllPinouts()
    val allProjects: Flow<List<SalvageProject>> = dao.getAllProjects()
    val allRagDocs: Flow<List<RagDocument>> = dao.getAllRagDocuments()
    val errorLogs: Flow<List<SystemErrorLog>> = dao.getAllErrorLogs()

    fun searchItems(query: String): Flow<List<SalvageItem>> =
        if (query.isBlank()) dao.getAllSalvageItems() else dao.searchSalvageItems(query)

    fun searchPinouts(query: String): Flow<List<PinoutItem>> =
        if (query.isBlank()) dao.getAllPinouts() else dao.searchPinouts(query)

    suspend fun getPinoutById(id: String): PinoutItem? = dao.getPinoutById(id)

    suspend fun updatePinoutDatasheet(id: String, url: String, note: String = "Datasheet link revised") =
        dao.updatePinoutDatasheetUrl(id = id, url = url, note = note)

    /**
     * Validates a user supplied JSON schema and persists only the components
     * that passed every check. Rejected rows are returned with their reason.
     */
    suspend fun importPinouts(json: String): PinoutImportReport {
        val report = PinoutJsonImporter.parse(json)
        if (report.accepted.isNotEmpty()) {
            dao.insertPinouts(report.accepted)
        }
        return report
    }

    suspend fun addItem(item: SalvageItem): Long = dao.insertSalvageItem(item)

    suspend fun updateItem(item: SalvageItem) = dao.updateSalvageItem(item)

    suspend fun deleteItem(id: Long) = dao.deleteSalvageItem(id)

    suspend fun adjustQuantity(id: Long, delta: Int) = dao.adjustQuantity(id, delta)

    suspend fun addProject(project: SalvageProject): Long = dao.insertProject(project)

    suspend fun updateProjectStatus(id: Long, status: String) = dao.updateProjectStatus(id, status)

    suspend fun deleteProject(id: Long) = dao.deleteProject(id)

    suspend fun addRagDocument(doc: RagDocument): Long = dao.insertRagDocument(doc)

    suspend fun logError(code: String, message: String, component: String) =
        dao.insertErrorLog(SystemErrorLog(errorCode = code, message = message, component = component))

    fun getRepurposeMatrix(): List<RepurposeMatrixItem> = SeedData.REPURPOSE_MATRIX

    fun findRepurposeForChip(chipQuery: String): List<RepurposeMatrixItem> {
        val q = chipQuery.lowercase()
        return SeedData.REPURPOSE_MATRIX.filter {
            it.keySalvagedChips.lowercase().contains(q) ||
            it.title.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }
    }
}
