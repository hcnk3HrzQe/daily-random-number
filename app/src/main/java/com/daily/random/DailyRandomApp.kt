package com.daily.random

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.worker.FetchRandomWorker
import java.util.concurrent.TimeUnit

class DailyRandomApp : Application() {

    companion object {
        const val TAG = "DailyRandom"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")

        // 启动定时任务（每 1 小时检查一次，内部判断是否今天已拉取）
        val periodicWork = PeriodicWorkRequestBuilder<FetchRandomWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_random",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_random",
                "每日随机数",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持后台运行"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
