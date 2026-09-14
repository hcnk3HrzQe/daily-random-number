package com.daily.random.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.worker.FetchRandomWorker

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT ||
            intent.action == Intent.ACTION_SCREEN_ON
        ) {
            val workRequest = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
