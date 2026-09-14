package com.daily.random

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.daily.random.data.PrefManager
import com.daily.random.worker.FetchRandomWorker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvRandom = findViewById<TextView>(R.id.tvRandom)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            // 手动触发拉取
            val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
            WorkManager.getInstance(this).enqueue(work)
            tvRandom.text = "加载中..."
            tvDate.text = ""

            // 延迟刷新显示
            tvRandom.postDelayed({
                val prefs = PrefManager(this)
                val num = prefs.getRandomNumber()
                val date = prefs.getDate()
                tvRandom.text = if (num >= 0) num.toString() else "无数据"
                tvDate.text = if (date.isNotEmpty()) date else "暂无"
            }, 2000)
        }

        // 显示已有数据
        val prefs = PrefManager(this)
        val num = prefs.getRandomNumber()
        val date = prefs.getDate()
        tvRandom.text = if (num >= 0) num.toString() else "无数据"
        tvDate.text = if (date.isNotEmpty()) date else "暂无"
    }
}
