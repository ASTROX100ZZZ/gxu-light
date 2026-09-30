package cn.gxu.light

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import org.json.JSONObject
import kotlin.math.ceil

/** 每秒刷新一次桌面小组件的灯态与剩余秒数 */
class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateWidgets()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val chId = "gxu_widget"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(chId, "红绿灯小组件", NotificationManager.IMPORTANCE_MIN)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, chId)
            .setContentTitle("南门红绿灯小组件运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .build()
    }

    private fun updateWidgets() {
        val json = getSharedPreferences("gxu", MODE_PRIVATE).getString("params", null) ?: return
        val store = try { JSONObject(json) } catch (e: Exception) { return }
        val p = store.getJSONObject("params")
        val redSec = p.getDouble("R")
        val greenSec = p.getDouble("G")
        val anchor = p.getDouble("anchor")
        val Cms = (redSec + greenSec) * 1000.0
        val es = (((System.currentTimeMillis() - anchor) % Cms) + Cms) % Cms / 1000.0
        val isRed = es < redSec
        val sec = ceil(if (isRed) redSec - es else Cms / 1000.0 - es).toInt()

        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, LightWidgetProvider::class.java))
        for (id in ids) {
            val views = RemoteViews(packageName, R.layout.widget_light)
            views.setTextViewText(R.id.w_state, if (isRed) "红灯" else "绿灯")
            views.setTextViewText(R.id.w_sec, sec.toString())
            views.setInt(
                R.id.w_root, "setBackgroundColor",
                if (isRed) 0xFFE5484D.toInt() else 0xFF30A46C.toInt()
            )
            mgr.updateAppWidget(id, views)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, WidgetUpdateService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, WidgetUpdateService::class.java))
        }
    }
}
