package com.samsunghealthclone.data

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.Executors

class StepRepository(private val dao: StepDao) {

    private val ioExecutor = Executors.newSingleThreadExecutor()

    fun getTodayEntry(): Flow<StepEntry?> {
        val today = StepEntry.getDayStart()
        return dao.getEntryForDateFlow(today)
    }

    fun getEntryForDate(date: Long): Flow<StepEntry?> {
        return dao.getEntryForDateFlow(date)
    }

    suspend fun getTodayEntrySync(): StepEntry? {
        val today = StepEntry.getDayStart()
        return dao.getEntryForDate(today)
    }

    suspend fun getEntryForDateSync(date: Long): StepEntry? {
        return dao.getEntryForDate(date)
    }

    suspend fun updateSteps(date: Long, steps: Int) {
        val entry = dao.getEntryForDate(date) ?: StepEntry(date = date)
        entry.steps = steps
        entry.lastUpdated = System.currentTimeMillis()
        updateCalculatedFields(entry)
        dao.insert(entry)
    }

    suspend fun addSteps(date: Long, additionalSteps: Int) {
        val entry = dao.getEntryForDate(date) ?: StepEntry(date = date)
        entry.steps += additionalSteps
        entry.lastUpdated = System.currentTimeMillis()
        updateCalculatedFields(entry)
        dao.insert(entry)
    }

    suspend fun setStepsForDate(date: Long, steps: Int) {
        val entry = dao.getEntryForDate(date) ?: StepEntry(date = date)
        entry.steps = steps
        entry.lastUpdated = System.currentTimeMillis()
        updateCalculatedFields(entry)
        dao.insert(entry)
    }

    suspend fun incrementSteps(additionalSteps: Int = 1) {
        val today = StepEntry.getDayStart()
        val entry = dao.getEntryForDate(today) ?: StepEntry(date = today)
        entry.steps += additionalSteps
        entry.lastUpdated = System.currentTimeMillis()
        updateCalculatedFields(entry)
        dao.insert(entry)
    }

    private fun updateCalculatedFields(entry: StepEntry) {
        // ~0.75m per step average
        entry.distanceMeters = entry.steps * 0.75f
        // ~0.04 kcal per step average
        entry.caloriesBurned = entry.steps * 0.04f
        // Active minutes: 100 steps per minute = 1 active minute
        entry.activeMinutes = entry.steps / 100
    }

    suspend fun getWeeklyData(): List<StepEntry> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getEntriesInRange(startDate, endDate)
    }

    suspend fun getMonthlyData(): List<StepEntry> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getEntriesInRange(startDate, endDate)
    }

    suspend fun getTotalStepsThisWeek(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getTotalStepsInRange(startDate, endDate) ?: 0
    }

    suspend fun getAverageStepsThisWeek(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getAverageStepsInRange(startDate, endDate) ?: 0.0
    }

    suspend fun getTotalStepsThisMonth(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getTotalStepsInRange(startDate, endDate) ?: 0
    }

    suspend fun getAverageStepsThisMonth(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        return dao.getAverageStepsInRange(startDate, endDate) ?: 0.0
    }

    fun getAllEntriesLiveData(): LiveData<List<StepEntry>> {
        return dao.getRecentEntries(30).asLiveData()
    }
}