package com.daily.random.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.DailyRandomApp
import com.daily.random.data.PrefManager
import com.daily.random.worker.FetchRandomWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            // 防抖：检查今天是否已经拉取过
            val prefs = PrefManager(context)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (prefs.getDate() == today && prefs.getRandomNumber() != -1) {
                Log.d(DailyRandomApp.TAG, "今天已有数据，跳过触发")
                return
            }

            // 24小时内已拉取过也跳过
            val lastTs = prefs.getTimestamp()
            if (lastTs > 0 && System.currentTimeMillis() - lastTs < 24 * 60 * 60 * 1000) {
                Log.d(DailyRandomApp.TAG, "24小时内已拉取，跳过触发")
                return
            }

            Log.d(DailyRandomApp.TAG, "解锁触发拉取")
            val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
