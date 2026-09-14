package com.daily.random.data

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveRandomNumber(number: Int) {
        prefs.edit().putInt(KEY_RANDOM_NUMBER, number).apply()
    }

    fun getRandomNumber(): Int {
        return prefs.getInt(KEY_RANDOM_NUMBER, -1)
    }

    fun saveLastFetchDate(date: String) {
        prefs.edit().putString(KEY_LAST_FETCH_DATE, date).apply()
    }

    fun getLastFetchDate(): String {
        return prefs.getString(KEY_LAST_FETCH_DATE, "暂无数据") ?: "暂无数据"
    }

    fun saveLastFetchTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_FETCH_TIMESTAMP, timestamp).apply()
    }

    fun getLastFetchTimestamp(): Long {
        return prefs.getLong(KEY_LAST_FETCH_TIMESTAMP, 0L)
    }

    companion object {
        private const val PREFS_NAME = "daily_random_prefs"
        private const val KEY_RANDOM_NUMBER = "random_number"
        private const val KEY_LAST_FETCH_DATE = "last_fetch_date"
        private const val KEY_LAST_FETCH_TIMESTAMP = "last_fetch_timestamp"
    }
}
