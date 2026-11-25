package com.samsunghealthclone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samsunghealthclone.HealthApplication
import com.samsunghealthclone.data.StepEntry
import com.samsunghealthclone.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

class SecretMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StepRepository = (application as HealthApplication).getRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _selectedDate = MutableLiveData<Long>(StepEntry.getDayStart())
    val selectedDate: LiveData<Long> = _selectedDate

    private val _currentSteps = MutableLiveData<Int>(0)
    val currentSteps: LiveData<Int> = _currentSteps

    private val _targetSteps = MutableLiveData<Int>(0)
    val targetSteps: LiveData<Int> = _targetSteps

    private val _hoursWalked = MutableLiveData<Float>(0f)
    val hoursWalked: LiveData<Float> = _hoursWalked

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>("")
    val message: LiveData<String> = _message

    init {
        loadCurrentData()
    }

    fun loadCurrentData() {
        _isLoading.postValue(true)
        scope.launch {
            val date = _selectedDate.value ?: StepEntry.getDayStart()
            val entry = repository.getEntryForDateSync(date)
            val steps = entry?.steps ?: 0
            _currentSteps.postValue(steps)
            _targetSteps.postValue(steps)
            _hoursWalked.postValue(steps / 6000f)
            _isLoading.postValue(false)
        }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.postValue(date)
        loadCurrentData()
    }

    fun setTargetSteps(steps: Int) {
        _targetSteps.postValue(steps)
        _hoursWalked.postValue(steps / 6000f)
    }

    fun setHoursWalked(hours: Float) {
        _hoursWalked.postValue(hours)
        val steps = (hours * 6000).toInt()
        _targetSteps.postValue(steps)
    }

    fun applyChanges() {
        _isLoading.postValue(true)
        scope.launch {
            val date = _selectedDate.value ?: StepEntry.getDayStart()
            val target = _targetSteps.value ?: 0
            repository.setStepsForDate(date, target)
            _currentSteps.postValue(target)
            _message.postValue("✅ Steps updated to $target for ${formatDate(date)}")
            _isLoading.postValue(false)
        }
    }

    fun addRandomSteps(min: Int = 1000, max: Int = 5000) {
        _isLoading.postValue(true)
        scope.launch {
            val date = _selectedDate.value ?: StepEntry.getDayStart()
            val current = _currentSteps.value ?: 0
            val random = (Math.random() * (max - min + 1) + min).toInt()
            val newTotal = current + random
            repository.setStepsForDate(date, newTotal)
            _currentSteps.postValue(newTotal)
            _targetSteps.postValue(newTotal)
            _hoursWalked.postValue(newTotal / 6000f)
            _message.postValue("🎲 Added $random random steps! Total: $newTotal")
            _isLoading.postValue(false)
        }
    }

    fun setMaxSteps() {
        _isLoading.postValue(true)
        scope.launch {
            val date = _selectedDate.value ?: StepEntry.getDayStart()
            val maxSteps = 50000
            repository.setStepsForDate(date, maxSteps)
            _currentSteps.postValue(maxSteps)
            _targetSteps.postValue(maxSteps)
            _hoursWalked.postValue(maxSteps / 6000f)
            _message.postValue("🚀 Set to MAX steps (50,000)!")
            _isLoading.postValue(false)
        }
    }

    fun resetToZero() {
        _isLoading.postValue(true)
        scope.launch {
            val date = _selectedDate.value ?: StepEntry.getDayStart()
            repository.setStepsForDate(date, 0)
            _currentSteps.postValue(0)
            _targetSteps.postValue(0)
            _hoursWalked.postValue(0f)
            _message.postValue("🔄 Reset to 0 steps")
            _isLoading.postValue(false)
        }
    }

    fun copyYesterdaySteps() {
        _isLoading.postValue(true)
        scope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = _selectedDate.value ?: StepEntry.getDayStart()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = calendar.timeInMillis
            val yesterdayEntry = repository.getEntryForDateSync(yesterday)
            val yesterdaySteps = yesterdayEntry?.steps ?: 0
            val today = _selectedDate.value ?: StepEntry.getDayStart()
            repository.setStepsForDate(today, yesterdaySteps)
            _currentSteps.postValue(yesterdaySteps)
            _targetSteps.postValue(yesterdaySteps)
            _hoursWalked.postValue(yesterdaySteps / 6000f)
            _message.postValue("📋 Copied yesterday's steps: $yesterdaySteps")
            _isLoading.postValue(false)
        }
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return android.text.format.DateFormat.format("dd MMM yyyy", calendar).toString()
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}