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
import com.daily.random.update.UpdateManager
import com.daily.random.worker.FetchRandomWorker

class MainActivity : AppCompatActivity() {

    private lateinit var tvRandom: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvOddEven: TextView
    private lateinit var tvUpdateStatus: TextView
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRandom = findViewById(R.id.tvRandom)
        tvDate = findViewById(R.id.tvDate)
        tvOddEven = findViewById(R.id.tvOddEven)
        tvUpdateStatus = findViewById(R.id.tvUpdateStatus)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnUpdate = findViewById<Button>(R.id.btnUpdate)

        autoFetch()

        btnRefresh.setOnClickListener { autoFetch() }

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

        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("清除本地随机数缓存")
                .setPositiveButton("确定") { _, _ ->
                    PrefManager(this).clear()
                    tvRandom.text = "无数据"
                    tvDate.text = "已清除"
                    tvOddEven.text = ""
                    tvUpdateStatus.text = ""
                    Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun autoFetch() {
        refreshDisplay()
        val work = OneTimeWorkRequestBuilder<FetchRandomWorker>().build()
        WorkManager.getInstance(this).enqueue(work)

        if (!polling) {
            polling = true
            tvRandom.text = "加载中..."
            tvDate.text = ""
            tvOddEven.text = ""
            var count = 0
            val checker = object : Runnable {
                override fun run() {
                    val prefs = PrefManager(this@MainActivity)
                    val num = prefs.getRandomNumber()
                    val ts = prefs.getTimestamp()
                    if (num >= 0 && ts > 0) {
                        refreshDisplay()
                        polling = false
                        return
                    }
                    count++
                    if (count < 10) {
                        tvRandom.postDelayed(this, 500)
                    } else {
                        refreshDisplay()
                        polling = false
                    }
                }
            }
            tvRandom.postDelayed(checker, 500)
        }
    }

    private fun refreshDisplay() {
        val prefs = PrefManager(this)
        val num = prefs.getRandomNumber()
        val date = prefs.getDate()
        tvRandom.text = if (num >= 0) num.toString() else "无数据"
        tvDate.text = if (date.isNotEmpty()) date else "暂无"
        tvOddEven.text = if (num >= 0) {
            if (num % 2 == 0) "双数 偶数" else "单数 奇数"
        } else ""
    }
}
