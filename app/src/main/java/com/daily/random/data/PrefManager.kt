package com.daily.random.data

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveRandomNumber(number: Int) {
        prefs.edit().putInt(KEY_RANDOM, number).apply()
    }

    fun getRandomNumber(): Int {
        return prefs.getInt(KEY_RANDOM, -1)
    }

    fun saveDate(date: String) {
        prefs.edit().putString(KEY_DATE, date).apply()
    }

    fun getDate(): String {
        return prefs.getString(KEY_DATE, "") ?: ""
    }

    fun saveTimestamp(ts: Long) {
        prefs.edit().putLong(KEY_TS, ts).apply()
    }

    fun getTimestamp(): Long {
        return prefs.getLong(KEY_TS, 0L)
    }

    companion object {
        private const val PREFS_NAME = "daily_random_prefs"
        private const val KEY_RANDOM = "random_number"
        private const val KEY_DATE = "fetch_date"
        private const val KEY_TS = "fetch_timestamp"
    }
}
