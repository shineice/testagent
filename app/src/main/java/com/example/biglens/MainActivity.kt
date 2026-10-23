package com.example.biglens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** 只有一個畫面:說明用法,並一鍵跳到系統的無障礙設定。 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val steps = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.BLACK)
            setLineSpacing(0f, 1.3f)
            text = "使用方法\n\n" +
                "1. 按下面的按鈕\n" +
                "2. 找到「大字閱讀鏡」並打開\n" +
                "3. 之後在任何 App,點畫面上的藍色「字」按鈕,就會出現大字閱讀畫面\n\n" +
                "按鈕可以用手指拖到不擋字的位置。"
        }
        val open = Button(this).apply {
            text = "前往開啟"
            textSize = 26f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad, pad, pad)
            addView(steps)
            addView(open)
        })
    }
}
