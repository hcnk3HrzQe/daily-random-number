package com.daily.random.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.daily.random.DailyRandomApp
import com.daily.random.R
import com.daily.random.data.PrefManager

class RandomWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    companion object {

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val name = ComponentName(context, RandomWidget::class.java)
            val ids = mgr.getAppWidgetIds(name)
            for (id in ids) {
                updateOne(context, mgr, id)
            }
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = PrefManager(context)
            val views = RemoteViews(context.packageName, R.layout.widget_random)
            val num = prefs.getRandomNumber()
            val date = prefs.getDate()

            if (num >= 0) {
                views.setTextViewText(R.id.tvWidgetRandom, num.toString())
                views.setTextViewText(R.id.tvWidgetDate, date)

                if (num % 2 == 0) {
                    views.setTextColor(R.id.tvWidgetRandom, Color.parseColor("#4CAF50"))
                    views.setTextViewText(R.id.tvWidgetOddEven, "双")
                    views.setTextColor(R.id.tvWidgetOddEven, Color.parseColor("#4CAF50"))
                } else {
                    views.setTextColor(R.id.tvWidgetRandom, Color.parseColor("#F44336"))
                    views.setTextViewText(R.id.tvWidgetOddEven, "单")
                    views.setTextColor(R.id.tvWidgetOddEven, Color.parseColor("#F44336"))
                }
            } else {
                views.setTextViewText(R.id.tvWidgetRandom, "?")
                views.setTextViewText(R.id.tvWidgetDate, "等待更新")
                views.setTextViewText(R.id.tvWidgetOddEven, "")
            }

            mgr.updateAppWidget(id, views)
        }
    }
}
