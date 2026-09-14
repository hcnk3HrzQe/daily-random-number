package com.daily.random.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.daily.random.DailyRandomApp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun checkAndUpdate(onResult: (String) -> Unit) {
        Thread {
            try {
                // 缓存：每天只检查一次
                val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val lastCheck = prefs.getString("last_check_date", "")

                if (lastCheck == today) {
                    onResult("今天已检查过，明天再来")
                    return@Thread
                }

                onResult("正在检查...")

                val url = "https://api.github.com/repos/hcnk3HrzQe/daily-random-number/releases/latest"
                Log.d(DailyRandomApp.TAG, "检查更新: $url")

                val req = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                val resp = client.newCall(req).execute()
                val code = resp.code
                Log.d(DailyRandomApp.TAG, "HTTP $code")

                if (code == 403) {
                    onResult("请求太频繁，请稍后再试")
                    return@Thread
                }

                if (code != 200) {
                    onResult("检查失败: HTTP $code")
                    return@Thread
                }

                val body = resp.body?.string()
                if (body.isNullOrBlank()) {
                    onResult("响应为空")
                    return@Thread
                }

                val json = JSONObject(body)
                val assets = json.getJSONArray("assets")

                if (assets.length() == 0) {
                    onResult("没有 APK")
                    return@Thread
                }

                val apkUrl = assets.getJSONObject(0).getString("browser_download_url")
                val apkName = assets.getJSONObject(0).getString("name")

                val localVersion = getAppVersion()
                val remoteVersion = extractVersion(apkName)
                Log.d(DailyRandomApp.TAG, "本地: v$localVersion 远程: v$remoteVersion")

                // 记录今天已检查
                prefs.edit().putString("last_check_date", today).apply()

                if (remoteVersion.isNotEmpty() && remoteVersion <= localVersion) {
                    onResult("已是最新 v$localVersion")
                    return@Thread
                }

                onResult("发现 v$remoteVersion，下载中...")
                downloadAndInstall(apkUrl, apkName, onResult)

            } catch (e: Exception) {
                Log.e(DailyRandomApp.TAG, "检查更新异常", e)
                onResult("检查失败: ${e.message}")
            }
        }.start()
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
    }

    private fun extractVersion(filename: String): String {
        val regex = Regex("""v([\d.]+)""")
        return regex.find(filename)?.groupValues?.get(1) ?: ""
    }

    private fun downloadAndInstall(url: String, filename: String, onResult: (String) -> Unit) {
        try {
            val downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            } else {
                @Suppress("DEPRECATION")
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val file = File(downloadDir, filename)
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("下载更新")
                .setDescription(filename)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try { context.unregisterReceiver(this) } catch (_: Exception) {}
                        installApk(file, onResult)
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        } catch (e: Exception) {
            onResult("下载失败: ${e.message}")
        }
    }

    private fun installApk(file: File, onResult: (String) -> Unit) {
        if (!file.exists()) {
            onResult("下载失败")
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onResult("正在安装...")
        } catch (e: Exception) {
            onResult("安装失败: ${e.message}")
        }
    }
}
