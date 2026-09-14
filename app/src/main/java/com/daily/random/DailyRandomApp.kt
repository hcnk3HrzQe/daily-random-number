package com.daily.random

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.worker.FetchRandomWorker
import java.util.concurrent.TimeUnit

class DailyRandomApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleFetchWork()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日随机数",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持后台服务运行"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleFetchWork() {
        val fetchWork = PeriodicWorkRequestBuilder<FetchRandomWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fetch_random",
            ExistingPeriodicWorkPolicy.KEEP,
            fetchWork
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_random_channel"
    }
}
