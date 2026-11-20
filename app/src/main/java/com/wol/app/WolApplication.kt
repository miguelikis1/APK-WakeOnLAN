package com.wol.app

import android.app.Application
import com.wol.app.data.SettingsRepository

class WolApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
    }
}
