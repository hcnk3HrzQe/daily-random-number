package com.daily.random

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
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
        Log.d("DailyRandom", "App 启动完成")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日随机数",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持后台服务运行"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun scheduleFetchWork() {
        val work = PeriodicWorkRequestBuilder<FetchRandomWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "fetch_random",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_random_channel"
        const val TAG = "DailyRandom"
    }
}
