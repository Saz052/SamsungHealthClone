package com.samsunghealthclone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.samsunghealthclone.data.HealthDatabase
import com.samsunghealthclone.data.StepEntry
import com.samsunghealthclone.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StepRepository = (application as HealthApplication).getRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _todayEntry = MutableLiveData<StepEntry?>()
    val todayEntry: LiveData<StepEntry?> = _todayEntry

    private val _weeklyData = MutableLiveData<List<StepEntry>>()
    val weeklyData: LiveData<List<StepEntry>> = _weeklyData

    private val _monthlyData = MutableLiveData<List<StepEntry>>()
    val monthlyData: LiveData<List<StepEntry>> = _monthlyData

    private val _weeklyTotal = MutableLiveData<Int>()
    val weeklyTotal: LiveData<Int> = _weeklyTotal

    private val _weeklyAverage = MutableLiveData<Double>()
    val weeklyAverage: LiveData<Double> = _weeklyAverage

    private val _monthlyTotal = MutableLiveData<Int>()
    val monthlyTotal: LiveData<Int> = _monthlyTotal

    private val _monthlyAverage = MutableLiveData<Double>()
    val monthlyAverage: LiveData<Double> = _monthlyAverage

    private val _selectedDate = MutableLiveData<Long>(StepEntry.getDayStart())
    val selectedDate: LiveData<Long> = _selectedDate

    init {
        loadTodayData()
        loadWeeklyData()
        loadMonthlyData()
        observeTodayEntry()
    }

    private fun observeTodayEntry() {
        repository.getTodayEntry()
            .map { entry ->
                entry?.let { _todayEntry.postValue(it) }
            }
            .asLiveData()
            .observeForever { }
    }

    fun loadTodayData() {
        scope.launch {
            val entry = repository.getTodayEntrySync()
            _todayEntry.postValue(entry)
        }
    }

    fun loadWeeklyData() {
        scope.launch {
            val data = repository.getWeeklyData()
            _weeklyData.postValue(data)
            val total = repository.getTotalStepsThisWeek()
            _weeklyTotal.postValue(total)
            val avg = repository.getAverageStepsThisWeek()
            _weeklyAverage.postValue(avg)
        }
    }

    fun loadMonthlyData() {
        scope.launch {
            val data = repository.getMonthlyData()
            _monthlyData.postValue(data)
            val total = repository.getTotalStepsThisMonth()
            _monthlyTotal.postValue(total)
            val avg = repository.getAverageStepsThisMonth()
            _monthlyAverage.postValue(avg)
        }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.postValue(date)
        scope.launch {
            val entry = repository.getEntryForDateSync(date)
            _todayEntry.postValue(entry)
        }
    }

    fun addSteps(steps: Int) {
        val date = _selectedDate.value ?: StepEntry.getDayStart()
        scope.launch {
            repository.addSteps(date, steps)
            loadTodayData()
            loadWeeklyData()
            loadMonthlyData()
        }
    }

    fun setSteps(steps: Int) {
        val date = _selectedDate.value ?: StepEntry.getDayStart()
        scope.launch {
            repository.setStepsForDate(date, steps)
            loadTodayData()
            loadWeeklyData()
            loadMonthlyData()
        }
    }

    fun getEntryForDate(date: Long): StepEntry? {
        return repository.getEntryForDateSync(date)
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return android.text.format.DateFormat.format("dd MMM", calendar).toString()
    }

    fun formatFullDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return android.text.format.DateFormat.format("EEEE, dd MMMM yyyy", calendar).toString()
    }

    fun getHoursWalked(steps: Int): Float {
        // Average walking speed: ~100 steps per minute = ~5 km/h
        // So steps / 100 = minutes, / 60 = hours
        return steps / 6000f
    }

    fun getDistanceKm(steps: Int): Float {
        return steps * 0.75f / 1000f
    }

    fun getCalories(steps: Int): Float {
        return steps * 0.04f
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}