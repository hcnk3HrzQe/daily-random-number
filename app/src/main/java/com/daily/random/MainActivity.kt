package com.daily.random

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.data.PrefManager
import com.daily.random.worker.FetchRandomWorker

class MainActivity : AppCompatActivity() {

    private lateinit var tvRandom: TextView
    private lateinit var tvDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRandom = findViewById(R.id.tvRandom)
        tvDate = findViewById(R.id.tvDate)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val btnClear = findViewById<Button>(R.id.btnClear)

        btnRefresh.setOnClickListener {
            val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(this).enqueue(work)
            tvRandom.text = "加载中..."
            tvDate.text = ""
            tvRandom.postDelayed({ refreshDisplay() }, 2000)
        }

        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("将清除本地缓存的随机数，下次开屏会重新拉取")
                .setPositiveButton("确定") { _, _ ->
                    PrefManager(this).clear()
                    tvRandom.text = "无数据"
                    tvDate.text = "已清除"
                    Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        refreshDisplay()
    }

    private fun refreshDisplay() {
        val prefs = PrefManager(this)
        val num = prefs.getRandomNumber()
        val date = prefs.getDate()
        tvRandom.text = if (num >= 0) num.toString() else "无数据"
        tvDate.text = if (date.isNotEmpty()) date else "暂无"
    }
}
