package com.example.keepforme

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ScrollView
import android.widget.TextView
import android.widget.VideoView
import java.io.File

class DetailActivity : Activity() {
    private lateinit var store: EntryStore
    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EntryStore(this)
        id = intent.getStringExtra(EXTRA_ID) ?: run { finish(); return }
    }

    // 回到畫面時重畫,這樣背景抓完全文後可以看到更新
    override fun onResume() {
        super.onResume()
        if (!::id.isInitialized) return
        val entry = store.load(id) ?: run { finish(); return }
        render(entry)
    }

    private fun render(entry: Entry) {
        val dir = store.dir(entry.id)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(32))
        }
        fun text(s: String, size: Float, alpha: Float = 1f, selectable: Boolean = false) =
            TextView(this).apply {
                text = s; textSize = size; this.alpha = alpha
                setTextIsSelectable(selectable)
                setPadding(0, dp(6), 0, dp(6))
            }.also { col.addView(it) }

        text(entry.displayTitle(), 22f, selectable = true)
        text(formatTime(entry.createdAt), 13f, 0.6f)
        if (entry.status == Entry.STATUS_PENDING) text("正在抓取全文…(稍後回到這頁會自動更新)", 14f, 0.7f)
        if (entry.status == Entry.STATUS_FAILED) text("沒能抓到全文,下方是分享時的原始內容。", 14f, 0.7f)
        if (entry.body.isNotBlank()) text(entry.body, 17f, selectable = true)
        else if (entry.sharedText.isNotBlank()) text(entry.sharedText, 17f, selectable = true)

        val width = resources.displayMetrics.widthPixels - dp(32)
        entry.media.map { File(dir, it) }.filter { it.exists() }.forEach { f ->
            if (f.extension.lowercase() in VIDEO_EXT) {
                col.addView(VideoView(this).apply {
                    setVideoURI(Uri.fromFile(f))
                    setMediaController(MediaController(this@DetailActivity).also { it.setAnchorView(this) })
                    seekTo(1)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)).apply { topMargin = dp(8) })
            } else {
                decodeScaled(f, width)?.let { bmp ->
                    col.addView(ImageView(this).apply {
                        setImageBitmap(bmp); adjustViewBounds = true
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = dp(8) })
                }
            }
        }

        entry.sourceUrl?.let { url ->
            col.addView(Button(this).apply {
                text = "開啟原文"
                setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            })
            col.addView(Button(this).apply {
                text = "重新抓取全文"
                setOnClickListener {
                    entry.status = Entry.STATUS_PENDING
                    store.save(entry)
                    SaveService.start(applicationContext, entry.id)
                    recreate()
                }
            })
        }
        col.addView(Button(this).apply {
            text = "刪除"
            setOnClickListener {
                AlertDialog.Builder(this@DetailActivity)
                    .setMessage("確定刪除這篇收藏?")
                    .setPositiveButton("刪除") { _, _ -> store.delete(entry.id); finish() }
                    .setNegativeButton("取消", null)
                    .show()
            }
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    companion object {
        const val EXTRA_ID = "id"
        private val VIDEO_EXT = setOf("mp4", "mov", "webm", "3gp", "mkv")
    }
}
