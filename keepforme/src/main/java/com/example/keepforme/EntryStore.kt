package com.example.keepforme

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** 每篇收藏一個資料夾:entries/<id>/entry.json 加上下載或複製來的圖片、影片。 */
class EntryStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "entries").apply { mkdirs() }

    fun newId(): String = UUID.randomUUID().toString()

    fun dir(id: String): File = File(root, id).apply { mkdirs() }

    @Synchronized
    fun save(entry: Entry) {
        val tmp = File(dir(entry.id), "entry.json.tmp")
        tmp.writeText(entry.toJson().toString())
        tmp.renameTo(File(dir(entry.id), "entry.json"))
    }

    fun load(id: String): Entry? = runCatching {
        Entry.fromJson(JSONObject(File(dir(id), "entry.json").readText()))
    }.getOrNull()

    fun list(): List<Entry> =
        (root.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { load(it.name) }
            .sortedByDescending { it.createdAt }

    fun delete(id: String) {
        File(root, id).deleteRecursively()
    }
}
