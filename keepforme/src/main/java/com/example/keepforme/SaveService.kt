package com.example.keepforme

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import java.io.File
import kotlin.concurrent.thread

/** 在背景抓取分享連結指向的整篇內容並存進對應的 Entry。 */
class SaveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID)
        if (id == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        thread {
            try {
                fetchInto(EntryStore(this), id)
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun fetchInto(store: EntryStore, id: String) {
        val entry = store.load(id) ?: return
        val url = entry.sourceUrl
        if (url == null) {
            entry.status = Entry.STATUS_DONE
            store.save(entry)
            return
        }
        try {
            val page = PageFetcher.fetch(url)
            if (page.title.isNotBlank()) entry.title = page.title
            entry.body = page.description
            val dir = store.dir(id)
            // 重新抓取時先清掉舊的下載檔(使用者分享進來的媒體以 share_ 開頭,保留)
            entry.media.filter { !it.startsWith("share_") }.forEach { File(dir, it).delete() }
            entry.media = entry.media.filter { it.startsWith("share_") }.toMutableList()
            page.images.forEachIndexed { i, u ->
                val name = "img_$i.jpg"
                if (PageFetcher.download(u, File(dir, name))) entry.media.add(name)
            }
            page.videos.take(1).forEach { u ->
                val name = "video_0.mp4"
                if (PageFetcher.download(u, File(dir, name))) entry.media.add(name)
            }
            entry.status = if (entry.body.isBlank() && entry.media.isEmpty()) Entry.STATUS_FAILED else Entry.STATUS_DONE
        } catch (e: Exception) {
            entry.status = Entry.STATUS_FAILED
        }
        store.save(entry)
    }

    companion object {
        private const val EXTRA_ID = "id"

        fun start(context: Context, id: String) {
            context.startService(Intent(context, SaveService::class.java).putExtra(EXTRA_ID, id))
        }
    }
}
