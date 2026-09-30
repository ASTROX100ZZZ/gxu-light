package cn.gxu.light

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** 应用内自动更新：对比线上 version.json，弹窗提示，一键下载并调起安装 */
object Updater {

    private const val VERSION_URL =
        "https://gxu-south-gate-light.app.workbuddy.host/version.json"

    fun check(activity: Activity) {
        thread {
            try {
                val myCode = activity.packageManager
                    .getPackageInfo(activity.packageName, 0).longVersionCode.toInt()
                val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val o = JSONObject(text)
                if (o.getInt("versionCode") > myCode) {
                    val name = o.optString("versionName", "")
                    val notes = o.optString("notes", "发现新版本")
                    val url = o.getString("url")
                    activity.runOnUiThread { showDialog(activity, name, notes, url) }
                }
            } catch (e: Exception) {
                // 网络失败静默，不影响使用
            }
        }
    }

    private fun showDialog(activity: Activity, name: String, notes: String, url: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("发现新版本 $name")
            .setMessage(notes)
            .setPositiveButton("立即更新") { _, _ -> downloadAndInstall(activity, url) }
            .setNegativeButton("下次再说", null)
            .show()
    }

    private fun downloadAndInstall(context: Context, url: String) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("南门红绿灯 · 正在下载更新")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, "gxu-update.apk"
            )
        val id = dm.enqueue(req)

        thread {
            val query = DownloadManager.Query().setFilterById(id)
            while (true) {
                val c = dm.query(query)
                var done = false
                if (c.moveToFirst()) {
                    when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val file = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                "gxu-update.apk"
                            )
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            context.startActivity(intent)
                            done = true
                        }
                        DownloadManager.STATUS_FAILED -> done = true
                    }
                }
                c.close()
                if (done) break
                Thread.sleep(1000)
            }
        }
    }
}
