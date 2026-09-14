package com.daily.random.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.DailyRandomApp
import com.daily.random.worker.FetchRandomWorker

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(DailyRandomApp.TAG, "屏幕事件: ${intent.action}")

        // 解锁屏幕时触发一次数据拉取
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
            Log.d(DailyRandomApp.TAG, "已触发数据拉取")
        }
    }
}
