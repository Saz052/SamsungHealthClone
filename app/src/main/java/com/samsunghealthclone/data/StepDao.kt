package com.samsunghealthclone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StepEntry)

    @Update
    suspend fun update(entry: StepEntry)

    @Query("SELECT * FROM step_entries WHERE date = :date")
    suspend fun getEntryForDate(date: Long): StepEntry?

    @Query("SELECT * FROM step_entries WHERE date = :date")
    fun getEntryForDateFlow(date: Long): Flow<StepEntry?>

    @Query("SELECT * FROM step_entries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int): List<StepEntry>

    @Query("SELECT * FROM step_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getEntriesInRange(startDate: Long, endDate: Long): List<StepEntry>

    @Query("SELECT SUM(steps) FROM step_entries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalStepsInRange(startDate: Long, endDate: Long): Int?

    @Query("SELECT AVG(steps) FROM step_entries WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageStepsInRange(startDate: Long, endDate: Long): Double?

    @Query("DELETE FROM step_entries WHERE date < :beforeDate")
    suspend fun deleteOldEntries(beforeDate: Long): Int
}