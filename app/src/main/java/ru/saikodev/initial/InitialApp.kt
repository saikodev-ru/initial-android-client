package ru.saikodev.initial

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.saikodev.initial.util.NotificationHelper

@HiltAndroidApp
class InitialApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
