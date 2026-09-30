package cn.gxu.light.wear

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.ceil

/**
 * 手表端：大色块 + 大秒数，抬腕即看。
 * 点按「变红 / 变绿」在灯刚变色瞬间校准相位；
 * 长按秒数进入配时微调（R/G ±1s）。
 * 数据只存手表本地（SharedPreferences），断网可用。
 */
class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("gxu_wear", Context.MODE_PRIVATE) }
    private var redDur = 101.0
    private var greenDur = 40.0
    private var anchor = 0.0   // 红灯开始时刻（ms）

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var stateView: TextView
    private lateinit var secView: TextView
    private lateinit var adjPanel: LinearLayout

    private val tick = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        stateView = findViewById(R.id.stateView)
        secView = findViewById(R.id.secView)
        adjPanel = findViewById(R.id.adjPanel)

        redDur = java.lang.Double.longBitsToDouble(prefs.getLong("R", java.lang.Double.doubleToRawLongBits(101.0)))
        greenDur = java.lang.Double.longBitsToDouble(prefs.getLong("G", java.lang.Double.doubleToRawLongBits(40.0)))
        anchor = java.lang.Double.longBitsToDouble(
            prefs.getLong("anchor", java.lang.Double.doubleToRawLongBits(System.currentTimeMillis().toDouble()))
        )

        // 校准：灯刚变红 / 刚变绿的瞬间点按
        findViewById<Button>(R.id.btnRed).setOnClickListener {
            anchor = System.currentTimeMillis().toDouble(); save(); render()
        }
        findViewById<Button>(R.id.btnGreen).setOnClickListener {
            anchor = System.currentTimeMillis() - redDur * 1000; save(); render()
        }

        // 长按秒数 → 配时微调面板
        secView.setOnLongClickListener {
            adjPanel.visibility = if (adjPanel.visibility == View.GONE) View.VISIBLE else View.GONE
            true
        }
        findViewById<Button>(R.id.rMinus).setOnClickListener { redDur = (redDur - 1).coerceAtLeast(5.0); save() }
        findViewById<Button>(R.id.rPlus).setOnClickListener { redDur = (redDur + 1).coerceAtMost(600.0); save() }
        findViewById<Button>(R.id.gMinus).setOnClickListener { greenDur = (greenDur - 1).coerceAtLeast(5.0); save() }
        findViewById<Button>(R.id.gPlus).setOnClickListener { greenDur = (greenDur + 1).coerceAtMost(600.0); save() }
        findViewById<Button>(R.id.adjDone).setOnClickListener { adjPanel.visibility = View.GONE }
    }

    override fun onResume() { super.onResume(); handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    private fun save() {
        prefs.edit()
            .putLong("R", java.lang.Double.doubleToRawLongBits(redDur))
            .putLong("G", java.lang.Double.doubleToRawLongBits(greenDur))
            .putLong("anchor", java.lang.Double.doubleToRawLongBits(anchor))
            .apply()
    }

    private fun render() {
        val C = (redDur + greenDur) * 1000
        val e = (((System.currentTimeMillis() - anchor) % C) + C) % C
        val es = e / 1000
        val isRed = es < redDur
        val sec = ceil(if (isRed) redDur - es else C / 1000 - es).toInt()

        stateView.text = if (isRed) "红灯" else "绿灯"
        secView.text = sec.toString()
        root.setBackgroundColor(if (isRed) 0xFFE5484D.toInt() else 0xFF30A46C.toInt())
    }
}
