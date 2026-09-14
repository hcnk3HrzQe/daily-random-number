package com.daily.random.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.daily.random.R
import com.daily.random.data.PrefManager

class RandomWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    companion object {

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefManager = PrefManager(context)
            val views = RemoteViews(context.packageName, R.layout.widget_random)

            val randomNumber = prefManager.getRandomNumber()
            val date = prefManager.getLastFetchDate()

            if (randomNumber != -1) {
                views.setTextViewText(R.id.tvWidgetRandom, randomNumber.toString())
                views.setTextViewText(R.id.tvWidgetDate, date)
            } else {
                views.setTextViewText(R.id.tvWidgetRandom, "---")
                views.setTextViewText(R.id.tvWidgetDate, "等待首次更新")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, RandomWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
