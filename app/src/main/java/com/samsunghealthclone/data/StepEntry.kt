package com.samsunghealthclone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "step_entries")
data class StepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Timestamp in milliseconds (start of day)
    var steps: Int = 0,
    var activeMinutes: Int = 0,
    var distanceMeters: Float = 0f,
    var caloriesBurned: Float = 0f,
    var lastUpdated: Long = System.currentTimeMillis()
) : Serializable {

    companion object {
        fun getDayStart(timestamp: Long = System.currentTimeMillis()): Long {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}