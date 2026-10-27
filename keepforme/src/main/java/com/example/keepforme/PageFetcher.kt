package com.example.keepforme

import android.text.Html
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 抓取網頁的 Open Graph 資訊(標題、全文、圖片、影片),並下載媒體檔。 */
object PageFetcher {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    private const val MAX_HTML = 3 * 1024 * 1024
    private const val MAX_MEDIA = 60L * 1024 * 1024

    class Page(
        val title: String,
        val description: String,
        val images: List<String>,
        val videos: List<String>,
    )

    private val metaTag = Regex("<meta\\s[^>]*>", RegexOption.IGNORE_CASE)
    private val attr = Regex("([a-zA-Z:_-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
    private val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun fetch(url: String): Page {
        val html = openConnection(url).let { conn ->
            try {
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                conn.inputStream.use { it.readNBytes(MAX_HTML).toString(Charsets.UTF_8) }
            } finally {
                conn.disconnect()
            }
        }
        return parse(html)
    }

    fun parse(html: String): Page {
        val metas = mutableMapOf<String, MutableList<String>>()
        metaTag.findAll(html).forEach { m ->
            val attrs = attr.findAll(m.value).associate {
                it.groupValues[1].lowercase() to (it.groupValues[3].ifEmpty { it.groupValues[4] })
            }
            val key = (attrs["property"] ?: attrs["name"])?.lowercase() ?: return@forEach
            val content = attrs["content"] ?: return@forEach
            metas.getOrPut(key) { mutableListOf() }.add(decode(content))
        }
        fun first(vararg keys: String) = keys.firstNotNullOfOrNull { metas[it]?.firstOrNull { v -> v.isNotBlank() } }.orEmpty()
        fun all(vararg keys: String) = keys.flatMap { metas[it].orEmpty() }.filter { it.isNotBlank() }.distinct()

        val title = first("og:title", "twitter:title")
            .ifBlank { titleTag.find(html)?.groupValues?.get(1)?.let(::decode)?.trim().orEmpty() }
        val description = first("og:description", "twitter:description", "description")
        return Page(
            title = title,
            description = description,
            images = all("og:image", "og:image:url", "twitter:image"),
            videos = all("og:video", "og:video:url", "og:video:secure_url"),
        )
    }

    /** 下載媒體到 dest;成功回傳 true。 */
    fun download(url: String, dest: File): Boolean = runCatching {
        val conn = openConnection(url)
        try {
            if (conn.responseCode !in 200..299) return false
            if (conn.contentLengthLong > MAX_MEDIA) return false
            var total = 0L
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_MEDIA) { dest.delete(); return false }
                        out.write(buf, 0, n)
                    }
                }
            }
            true
        } finally {
            conn.disconnect()
        }
    }.getOrElse { dest.delete(); false }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8")
        }

    private fun decode(s: String): String =
        Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
}
