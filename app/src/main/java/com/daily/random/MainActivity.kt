package com.daily.random

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.daily.random.data.PrefManager

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefManager = PrefManager(this)

        val tvRandom = findViewById<TextView>(R.id.tvRandom)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        tvRandom.text = prefManager.getRandomNumber().toString()
        tvDate.text = "日期: ${prefManager.getLastFetchDate()}"

        btnRefresh.setOnClickListener {
            tvRandom.text = prefManager.getRandomNumber().toString()
            tvDate.text = "日期: ${prefManager.getLastFetchDate()}"
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
        }
    }
}
