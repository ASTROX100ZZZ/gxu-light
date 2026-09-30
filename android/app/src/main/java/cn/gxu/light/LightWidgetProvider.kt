package cn.gxu.light

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class LightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdateService.start(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdateService.start(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdateService.stop(context)
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, LightWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) WidgetUpdateService.start(context)
        }
    }
}
