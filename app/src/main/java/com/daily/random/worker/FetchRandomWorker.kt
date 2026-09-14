package com.daily.random.worker

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daily.random.DailyRandomApp
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
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = PrefManager(applicationContext)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                if (prefs.getDate() == today && prefs.getRandomNumber() != -1) {
                    Log.d(DailyRandomApp.TAG, "今天已拉取，跳过")
                    return@withContext Result.success()
                }

                Log.d(DailyRandomApp.TAG, "开始拉取云端数据...")

                val result = fetchFromGitHub()
                if (result != null) {
                    val (date, random) = result
                    prefs.saveRandomNumber(random)
                    prefs.saveDate(date)
                    prefs.saveTimestamp(System.currentTimeMillis())

                    Log.d(DailyRandomApp.TAG, "拉取成功: date=$date, random=$random")

                    withContext(Dispatchers.Main) {
                        RandomWidget.updateAll(applicationContext)
                    }

                    Result.success()
                } else {
                    Log.w(DailyRandomApp.TAG, "拉取失败，稍后重试")
                    Result.retry()
                }
            } catch (e: Exception) {
                Log.e(DailyRandomApp.TAG, "Worker异常", e)
                Result.retry()
            }
        }
    }

    private fun fetchFromGitHub(): Pair<String, Int>? {
        return try {
            val url = "https://raw.githubusercontent.com/hcnk3HrzQe/daily-random-number/main/random.json"
            Log.d(DailyRandomApp.TAG, "请求: $url")

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val code = response.code
            Log.d(DailyRandomApp.TAG, "HTTP $code")

            if (code != 200) return null

            val body = response.body?.string()
            if (body.isNullOrBlank()) return null

            Log.d(DailyRandomApp.TAG, "响应: $body")

            val json = JSONObject(body)
            val date = json.getString("date")
            val random = json.getInt("random")

            Pair(date, random)
        } catch (e: Exception) {
            Log.e(DailyRandomApp.TAG, "网络请求异常", e)
            null
        }
    }
}
