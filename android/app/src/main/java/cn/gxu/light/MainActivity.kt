package cn.gxu.light

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    /** 网页 save() 时把整份 store JSON 同步过来，供桌面小组件使用 */
    inner class Bridge {
        @JavascriptInterface
        fun sync(json: String) {
            getSharedPreferences("gxu", MODE_PRIVATE)
                .edit().putString("params", json).apply()
            LightWidgetProvider.refreshAll(this@MainActivity)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true          // localStorage 持久化
        web.addJavascriptInterface(Bridge(), "GxuAndroid")

        // 在线优先：有网时加载线上最新版，断网/加载失败回退到内嵌副本
        web.webViewClient = object : WebViewClient() {
            private var fellBack = false
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                if (!fellBack && request?.isForMainFrame == true) {
                    fellBack = true
                    view?.loadUrl("file:///android_asset/index.html")
                }
            }
        }
        web.loadUrl("https://gxu-south-gate-light.app.workbuddy.host/")

        Updater.check(this)   // 应用内自动更新检查
    }
}
