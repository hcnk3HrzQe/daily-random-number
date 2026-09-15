package com.daily.random

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.data.PrefManager
import com.daily.random.update.UpdateManager
import com.daily.random.worker.FetchRandomWorker
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var tvRandom: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvOddEven: TextView
    private lateinit var tvUpdateStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvRandom = findViewById(R.id.tvRandom)
        tvDate = findViewById(R.id.tvDate)
        tvOddEven = findViewById(R.id.tvOddEven)
        tvUpdateStatus = findViewById(R.id.tvUpdateStatus)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnUpdate = findViewById<Button>(R.id.btnUpdate)

        // 直接显示缓存
        refreshDisplay()

        // 后台拉取（如果不匹配今天日期会自动拉取）
        fetchInBackground()

        // 刷新
        btnRefresh.setOnClickListener {
            tvRandom.text = "..."
            tvOddEven.text = ""
            val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(this).enqueue(work)
            tvRandom.postDelayed({ refreshDisplay() }, 5000)
            Toast.makeText(this, "正在刷新...", Toast.LENGTH_SHORT).show()
        }

        // 检查更新
        btnUpdate.setOnClickListener {
            btnUpdate.isEnabled = false
            tvUpdateStatus.text = "检查中..."
            UpdateManager(this).checkAndUpdate { msg ->
                runOnUiThread {
                    tvUpdateStatus.text = msg
                    btnUpdate.isEnabled = true
                }
            }
        }

        // 清除缓存
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("清除本地随机数缓存")
                .setPositiveButton("确定") { _, _ ->
                    PrefManager(this).clear()
                    tvRandom.text = "--"
                    tvDate.text = "暂无"
                    tvOddEven.text = ""
                    tvUpdateStatus.text = ""
                    tvRandom.setTextColor(Color.parseColor("#999999"))
                    tvOddEven.setTextColor(Color.parseColor("#CCCCCC"))
                    Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun fetchInBackground() {
        val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
        WorkManager.getInstance(this).enqueue(work)
        // 5秒后刷新显示（等Worker完成）
        tvRandom.postDelayed({ refreshDisplay() }, 5000)
    }

    private fun refreshDisplay() {
        val prefs = PrefManager(this)
        val num = prefs.getRandomNumber()
        val date = prefs.getDate()

        if (num >= 0) {
            tvRandom.text = num.toString()
            tvDate.text = if (date.isNotEmpty()) date else "暂无"

            if (num % 2 == 0) {
                tvRandom.setTextColor(Color.parseColor("#4CAF50"))
                tvOddEven.text = "双"
                tvOddEven.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                tvRandom.setTextColor(Color.parseColor("#F44336"))
                tvOddEven.text = "单"
                tvOddEven.setTextColor(Color.parseColor("#F44336"))
            }
        } else {
            tvRandom.text = "--"
            tvRandom.setTextColor(Color.parseColor("#999999"))
            tvDate.text = "暂无"
            tvOddEven.text = ""
        }
    }
}
