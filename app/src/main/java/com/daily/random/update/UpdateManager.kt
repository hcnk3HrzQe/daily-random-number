package com.daily.random.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.daily.random.DailyRandomApp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: android.content.Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val VERSION_URL =
            "https://raw.githubusercontent.com/hcnk3HrzQe/daily-random-number/main/version.json"
    }

    fun checkAndUpdate(onResult: (String) -> Unit) {
        Thread {
            try {
                onResult("检查中...")

                val req = Request.Builder()
                    .url(VERSION_URL)
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val resp = client.newCall(req).execute()
                val code = resp.code

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
                val remoteVersion = json.optString("version", "")
                val remoteVC = json.optInt("versionCode", 0)
                val apkName = json.optString("apk", "")

                val localVC = getAppVersionCode()

                if (remoteVC <= localVC) {
                    onResult("已是最新")
                    return@Thread
                }

                val tag = apkName.substringAfter("build-").substringBefore("-")
                val downloadUrl = "https://github.com/hcnk3HrzQe/daily-random-number/releases/download/v$tag/$apkName"

                onResult("发现 v$remoteVersion，下载中...")
                downloadApk(downloadUrl, apkName, onResult)

            } catch (e: Exception) {
                Log.e(DailyRandomApp.TAG, "检查更新异常", e)
                onResult("检查失败: ${e.message}")
            }
        }.start()
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

    private fun downloadApk(url: String, filename: String, onResult: (String) -> Unit) {
        try {
            val downloadDir = File(context.filesDir, "updates")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val file = File(downloadDir, filename)
            if (file.exists()) file.delete()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.code != 200) {
                onResult("下载失败: HTTP ${response.code}")
                return
            }

            val body = response.body ?: run {
                onResult("下载失败: 响应为空")
                return
            }

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            val totalSize = body.contentLength()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalSize > 0) {
                    val progress = (totalRead * 100 / totalSize).toInt()
                    onResult("下载中... $progress%")
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onResult("下载完成，安装中...")
            installApk(file, onResult)

        } catch (e: Exception) {
            Log.e(DailyRandomApp.TAG, "下载异常", e)
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
