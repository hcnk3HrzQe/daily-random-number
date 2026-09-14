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
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // 直接读 raw 文件，不用 GitHub API，无限流
    companion object {
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/hcnk3HrzQe/daily-random-number/main/version.json"
        private const val RELEASE_BASE =
            "https://github.com/hcnk3HrzQe/daily-random-number/releases/download/"
    }

    fun checkAndUpdate(onResult: (String) -> Unit) {
        Thread {
            try {
                // 每天只检查一次
                val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val lastCheck = prefs.getString("last_check_date", "")

                if (lastCheck == today) {
                    onResult("今天已检查过")
                    return@Thread
                }

                onResult("正在检查...")

                Log.d(DailyRandomApp.TAG, "检查更新: $VERSION_URL")

                val req = Request.Builder()
                    .url(VERSION_URL)
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val resp = client.newCall(req).execute()
                val code = resp.code
                Log.d(DailyRandomApp.TAG, "HTTP $code")

                if (code != 200) {
                    onResult("检查失败: HTTP $code")
                    return@Thread
                }

                val body = resp.body?.string()
                if (body.isNullOrBlank()) {
                    onResult("响应为空")
                    return@Thread
                }

                Log.d(DailyRandomApp.TAG, "version.json: $body")

                val json = JSONObject(body)
                val remoteVersion = json.optString("version", "")
                val remoteVC = json.optInt("versionCode", 0)
                val apkName = json.optString("apk", "")

                val localVersion = getAppVersion()
                val localVC = getAppVersionCode()

                Log.d(DailyRandomApp.TAG, "本地: v$localVersion ($localVC) 远程: v$remoteVersion ($remoteVC)")

                // 记录今天已检查
                prefs.edit().putString("last_check_date", today).apply()

                if (remoteVC <= localVC) {
                    onResult("已是最新 v$localVersion")
                    return@Thread
                }

                // 拼接下载链接
                val tag = "v${apkName.substringAfter("build").substringBefore("-").let { "build$it" }}"
                // 直接用 apkName 找 release
                val apkUrl = RELEASE_BASE + "v" + apkName.substringAfter("build").replace("-.*".toRegex(), "") + "/" + apkName

                // 更简单：直接用 GitHub 页面重定向
                val downloadUrl = "https://github.com/hcnk3HrzQe/daily-random-number/releases/download/v" +
                    apkName.substringAfter("build-").substringBefore("-") + "/" + apkName

                Log.d(DailyRandomApp.TAG, "下载: $downloadUrl")

                onResult("发现 v$remoteVersion，下载中...")
                downloadAndInstall(downloadUrl, apkName, onResult)

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

    private fun getAppVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) { 0 }
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
