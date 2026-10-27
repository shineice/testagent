package com.example.keepforme

import org.json.JSONArray
import org.json.JSONObject

data class Entry(
    val id: String,
    val createdAt: Long,
    val sourceUrl: String?,
    val sharedText: String,
    var title: String = "",
    var body: String = "",
    var media: MutableList<String> = mutableListOf(),
    var status: String = STATUS_PENDING,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("createdAt", createdAt)
        .put("sourceUrl", sourceUrl ?: JSONObject.NULL)
        .put("sharedText", sharedText)
        .put("title", title)
        .put("body", body)
        .put("media", JSONArray(media))
        .put("status", status)

    /** 列表上顯示的標題:優先用抓到的標題,否則用分享的文字。 */
    fun displayTitle(): String =
        title.ifBlank { sharedText.lineSequence().firstOrNull { it.isNotBlank() } ?: "(無標題)" }

    fun matches(query: String): Boolean =
        query.isBlank() || listOf(title, body, sharedText, sourceUrl.orEmpty())
            .any { it.contains(query, ignoreCase = true) }

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"

        fun fromJson(o: JSONObject): Entry {
            val media = mutableListOf<String>()
            o.optJSONArray("media")?.let { for (i in 0 until it.length()) media.add(it.getString(i)) }
            return Entry(
                id = o.getString("id"),
                createdAt = o.getLong("createdAt"),
                sourceUrl = if (o.isNull("sourceUrl")) null else o.getString("sourceUrl"),
                sharedText = o.optString("sharedText"),
                title = o.optString("title"),
                body = o.optString("body"),
                media = media,
                status = o.optString("status", STATUS_DONE),
            )
        }
    }
}
