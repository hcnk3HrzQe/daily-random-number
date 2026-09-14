package com.daily.random.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daily.random.data.PrefManager
import com.daily.random.widget.RandomWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FetchRandomWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val prefManager = PrefManager(applicationContext)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val lastFetchDate = prefManager.getLastFetchDate()

                if (lastFetchDate == today) {
                    return@withContext Result.success()
                }

                val randomNumber = fetchRandomFromGitHub()

                if (randomNumber != null) {
                    prefManager.saveRandomNumber(randomNumber)
                    prefManager.saveLastFetchDate(today)
                    prefManager.saveLastFetchTimestamp(System.currentTimeMillis())

                    withContext(Dispatchers.Main) {
                        RandomWidget.updateAllWidgets(applicationContext)
                    }

                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun fetchRandomFromGitHub(): Int? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(RAW_URL)
                .addHeader("Cache-Control", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val date = json.getString("date")
            val random = json.getInt("random")

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (date == today) {
                random
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val RAW_URL =
            "https://raw.githubusercontent.com/hcnk3HrzQe/daily-random-number/main/random.json"
    }
}
