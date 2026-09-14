package com.daily.random.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 已禁用 - 不再每次解锁触发拉取
    }
}
