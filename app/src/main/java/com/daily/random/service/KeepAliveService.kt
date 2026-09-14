package com.daily.random.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.daily.random.DailyRandomApp
import com.daily.random.R

class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d(DailyRandomApp.TAG, "保活服务启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, DailyRandomApp.CHANNEL_ID)
            .setContentTitle("每日随机数")
            .setContentText("后台运行中")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(DailyRandomApp.TAG, "保活服务停止")
    }
}
