package com.daily.random.data

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "daily_random_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_RANDOM_NUMBER = "random_number"
        private const val KEY_DATE = "date"
        private const val KEY_TIMESTAMP = "timestamp"
    }

    fun saveRandomNumber(number: Int) {
        prefs.edit().putInt(KEY_RANDOM_NUMBER, number).apply()
    }

    fun getRandomNumber(): Int {
        return prefs.getInt(KEY_RANDOM_NUMBER, -1)
    }

    fun saveDate(date: String) {
        prefs.edit().putString(KEY_DATE, date).apply()
    }

    fun getDate(): String {
        return prefs.getString(KEY_DATE, "") ?: ""
    }

    fun saveTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_TIMESTAMP, timestamp).apply()
    }

    fun getTimestamp(): Long {
        return prefs.getLong(KEY_TIMESTAMP, 0)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
