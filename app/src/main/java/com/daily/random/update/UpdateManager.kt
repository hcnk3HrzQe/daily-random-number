package com.daily.random.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.daily.random.DailyRandomApp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkAndUpdate(onResult: (String) -> Unit) {
        Thread {
            try {
                onResult("正在检查更新...")

                // 获取最新 release
                val req = Request.Builder()
                    .url("https://api.github.com/repos/hcnk3HrzQe/daily-random-number/releases/latest")
                    .build()
                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: run {
                    onResult("获取失败")
                    return@Thread
                }

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val assets = json.getJSONArray("assets")

                if (assets.length() == 0) {
                    onResult("没有找到 APK 文件")
                    return@Thread
                }

                val apkUrl = assets.getJSONObject(0).getString("browser_download_url")
                val apkName = assets.getJSONObject(0).getString("name")

                // 提取版本号
                val localVersion = getAppVersion()
                val remoteVersion = extractVersion(apkName)
                Log.d(DailyRandomApp.TAG, "本地版本: $localVersion, 远程: $remoteVersion")

                if (remoteVersion.isNotEmpty() && remoteVersion <= localVersion) {
                    onResult("已是最新版本 v$localVersion")
                    return@Thread
                }

                onResult("发现新版本，正在下载...")
                downloadAndInstall(apkUrl, apkName, onResult)

            } catch (e: Exception) {
                Log.e(DailyRandomApp.TAG, "检查更新失败", e)
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
        // DailyRandom-v1.1-build68-20260914.apk -> 1.1
        val regex = Regex("v([\d.]+)")
        return regex.find(filename)?.groupValues?.get(1) ?: ""
    }

    private fun downloadAndInstall(url: String, filename: String, onResult: (String) -> Unit) {
        val downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        val file = File(downloadDir, filename)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("下载更新")
            .setDescription("正在下载 $filename")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // 监听下载完成
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(file, onResult)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(file: File, onResult: (String) -> Unit) {
        if (!file.exists()) {
            onResult("下载失败，文件不存在")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onResult("正在安装...")
        } catch (e: Exception) {
            Log.e(DailyRandomApp.TAG, "安装失败", e)
            onResult("安装失败: ${e.message}")
        }
    }
}
