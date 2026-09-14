package com.daily.random.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.DailyRandomApp
import com.daily.random.worker.FetchRandomWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(DailyRandomApp.TAG, "开机事件: ${intent.action}")

        val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
        WorkManager.getInstance(context).enqueue(work)
    }
}
