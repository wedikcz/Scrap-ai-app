package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PinoutItem
import com.example.data.model.RagDocument
import com.example.data.model.SalvageItem
import com.example.data.model.SalvageProject
import com.example.data.model.SystemErrorLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapMasterDao {

    // Salvage Items
    @Query("SELECT * FROM salvage_items ORDER BY id DESC")
    fun getAllSalvageItems(): Flow<List<SalvageItem>>

    @Query("SELECT * FROM salvage_items WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR sourceDevice LIKE '%' || :query || '%'")
    fun searchSalvageItems(query: String): Flow<List<SalvageItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalvageItem(item: SalvageItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalvageItems(items: List<SalvageItem>)

    @Update
    suspend fun updateSalvageItem(item: SalvageItem)

    @Query("DELETE FROM salvage_items WHERE id = :id")
    suspend fun deleteSalvageItem(id: Long)

    @Query("UPDATE salvage_items SET quantity = quantity + :delta WHERE id = :id AND (quantity + :delta) >= 0")
    suspend fun adjustQuantity(id: Long, delta: Int)

    // Pinouts with Versioning
    @Query("SELECT * FROM pinout_items ORDER BY name ASC")
    fun getAllPinouts(): Flow<List<PinoutItem>>

    @Query("SELECT * FROM pinout_items WHERE id = :id")
    suspend fun getPinoutById(id: String): PinoutItem?

    @Query("SELECT * FROM pinout_items WHERE name LIKE '%' || :query || '%' OR chipCode LIKE '%' || :query || '%'")
    fun searchPinouts(query: String): Flow<List<PinoutItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinouts(items: List<PinoutItem>)

    @Query("UPDATE pinout_items SET datasheetUrl = :url, version = version + 1, lastUpdated = :timestamp, revisionNotes = :note WHERE id = :id")
    suspend fun updatePinoutDatasheetUrl(id: String, url: String, timestamp: Long = System.currentTimeMillis(), note: String = "Datasheet link revised")

    @Update
    suspend fun updatePinout(item: PinoutItem)

    // Projects
    @Query("SELECT * FROM salvage_projects ORDER BY id ASC")
    fun getAllProjects(): Flow<List<SalvageProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SalvageProject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<SalvageProject>)

    @Update
    suspend fun updateProject(project: SalvageProject)

    @Query("UPDATE salvage_projects SET status = :status WHERE id = :id")
    suspend fun updateProjectStatus(id: Long, status: String)

    @Query("DELETE FROM salvage_projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    // RAG Documents
    @Query("SELECT * FROM rag_documents ORDER BY id DESC")
    fun getAllRagDocuments(): Flow<List<RagDocument>>

    @Query("SELECT * FROM rag_documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchRagDocuments(query: String): Flow<List<RagDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRagDocument(doc: RagDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRagDocuments(docs: List<RagDocument>)

    // System Error Logs (Convex errorLogs equivalent)
    @Query("SELECT * FROM system_error_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllErrorLogs(): Flow<List<SystemErrorLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorLog(log: SystemErrorLog): Long
}
