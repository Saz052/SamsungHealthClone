package com.samsunghealthclone

import android.app.Application
import com.samsunghealthclone.data.HealthDatabase
import com.samsunghealthclone.data.StepRepository

class HealthApplication : Application() {

    private var repository: StepRepository? = null

    override fun onCreate() {
        super.onCreate()
        val database = HealthDatabase.getDatabase(this)
        repository = StepRepository(database.stepDao())
    }

    fun getRepository(): StepRepository {
        return repository!!
    }
}