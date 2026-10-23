package com.example.biglens

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 浮動「字」按鈕 + 大字閱讀面板。
 * 使用 TYPE_ACCESSIBILITY_OVERLAY,不需要額外的「顯示在其他應用程式上層」權限。
 */
class ReaderService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var fab: View? = null
    private var panel: View? = null

    override fun onServiceConnected() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFab()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        fab?.let { wm.removeView(it) }
        closePanel()
        super.onDestroy()
    }

    // ---------- 浮動按鈕 ----------

    private fun showFab() {
        val size = dp(72)
        val button = TextView(this).apply {
            text = "字"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE60D47A1.toInt())
            }
            contentDescription = "放大閱讀"
        }
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - dp(8)
            y = resources.displayMetrics.heightPixels / 2
        }
        button.setOnTouchListener(DragOrClick(lp) { openPanel() })
        wm.addView(button, lp)
        fab = button
    }

    /** 拖曳移動按鈕;位移很小就視為點擊。 */
    private inner class DragOrClick(
        private val lp: WindowManager.LayoutParams,
        private val onClick: () -> Unit
    ) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true
                    if (moved) {
                        lp.x = startX + dx
                        lp.y = startY + dy
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> if (!moved) onClick()
            }
            return true
        }
    }

    // ---------- 閱讀面板 ----------

    private fun openPanel() {
        if (panel != null) return
        // 必須在顯示面板之前先抓取畫面文字
        val lines = ScreenTextExtractor.extract(rootInActiveWindow, packageName)
        val prefs = getSharedPreferences("biglens", MODE_PRIVATE)
        var textSp = prefs.getFloat("textSp", 32f)

        val body = TextView(this).apply {
            textSize = textSp
            setTextColor(Color.BLACK)
            setLineSpacing(0f, 1.3f)
            setPadding(dp(20), dp(12), dp(20), dp(24))
            text = if (lines.isEmpty()) "這個畫面沒有讀到文字。"
            else lines.joinToString("\n\n")
        }

        fun resize(delta: Float) {
            textSp = (textSp + delta).coerceIn(18f, 72f)
            body.textSize = textSp
            prefs.edit().putFloat("textSp", textSp).apply()
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF0D47A1.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(barButton("字 −") { resize(-4f) }, weight())
            addView(barButton("字 ＋") { resize(+4f) }, weight())
            addView(barButton("關閉") { closePanel() }, weight())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(bar)
            addView(ScrollView(this@ReaderService).apply { addView(body) },
                LinearLayout.LayoutParams(-1, 0, 1f))
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.OPAQUE
        )
        wm.addView(root, lp)
        panel = root
    }

    private fun closePanel() {
        panel?.let { wm.removeView(it) }
        panel = null
    }

    private fun barButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 22f
        setOnClickListener { onClick() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, dp(64), 1f).apply {
        marginStart = dp(4); marginEnd = dp(4)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
