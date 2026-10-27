package com.example.keepforme

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import kotlin.concurrent.thread

/** 分享選單的「留我自己看」:立刻存下分享內容,再交給背景服務抓整篇。 */
class ShareReceiverActivity : Activity() {
    private val urlRegex = Regex("https?://[^\\s]+")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val uris = streamUris()
        val url = urlRegex.find(text)?.value?.trimEnd('.', ',', ')', '」')

        if (text.isBlank() && uris.isEmpty()) {
            finish()
            return
        }

        val store = EntryStore(this)
        val entry = Entry(
            id = store.newId(),
            createdAt = System.currentTimeMillis(),
            sourceUrl = url,
            sharedText = text,
            title = subject,
            status = if (url != null) Entry.STATUS_PENDING else Entry.STATUS_DONE,
        )
        store.save(entry)
        Toast.makeText(this, "已存進「留我自己看」", Toast.LENGTH_SHORT).show()

        // URI 的讀取權限只在本 Activity 存活時有效,所以複製完才 finish()
        thread {
            val dir = store.dir(entry.id)
            uris.forEachIndexed { i, uri ->
                val name = "share_$i.${extensionFor(uri)}"
                runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        File(dir, name).outputStream().use { input.copyTo(it) }
                    }
                    entry.media.add(name)
                }
            }
            if (uris.isNotEmpty()) store.save(entry)
            if (url != null) SaveService.start(applicationContext, entry.id)
            runOnUiThread { finish() }
        }
    }

    @Suppress("DEPRECATION")
    private fun streamUris(): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        )
        Intent.ACTION_SEND_MULTIPLE ->
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        else -> emptyList()
    }

    private fun extensionFor(uri: Uri): String {
        val mime = contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
    }
}
