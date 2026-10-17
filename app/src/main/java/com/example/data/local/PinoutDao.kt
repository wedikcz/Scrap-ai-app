package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PinoutItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing electronic component pinouts in the Room database,
 * ensuring fast and reactive offline access to the component catalog.
 */
@Dao
interface PinoutDao {

    @Query("SELECT * FROM pinout_items ORDER BY name ASC")
    fun getAllPinouts(): Flow<List<PinoutItem>>

    @Query("SELECT * FROM pinout_items WHERE id = :id OR LOWER(chipCode) = LOWER(:id)")
    suspend fun getPinoutById(id: String): PinoutItem?

    @Query("SELECT * FROM pinout_items WHERE name LIKE '%' || :query || '%' OR chipCode LIKE '%' || :query || '%' OR packageType LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPinouts(query: String): Flow<List<PinoutItem>>

    @Query("SELECT * FROM pinout_items WHERE LOWER(chipCode) = LOWER(:chipCode) OR LOWER(name) LIKE '%' || LOWER(:chipCode) || '%' LIMIT 1")
    suspend fun findPinoutByChipCode(chipCode: String): PinoutItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinout(pinout: PinoutItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinouts(pinouts: List<PinoutItem>)

    @Update
    suspend fun updatePinout(pinout: PinoutItem)

    @Query("DELETE FROM pinout_items WHERE id = :id")
    suspend fun deletePinout(id: String)

    @Query("UPDATE pinout_items SET datasheetUrl = :url, version = version + 1, lastUpdated = :timestamp, revisionNotes = :note WHERE id = :id")
    suspend fun updatePinoutDatasheetUrl(
        id: String,
        url: String,
        timestamp: Long = System.currentTimeMillis(),
        note: String = "Datasheet link revised"
    )
}
