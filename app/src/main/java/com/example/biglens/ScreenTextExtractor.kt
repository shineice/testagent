package com.example.biglens

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 從目前畫面的無障礙節點樹收集文字,依「由上到下、由左到右」排序。
 * 節點的 text 是完整字串,即使畫面上因為字太大被截斷(…)也讀得到全文。
 */
object ScreenTextExtractor {

    private class Item(val top: Int, val left: Int, val text: String)

    fun extract(root: AccessibilityNodeInfo?, ownPackage: String): List<String> {
        if (root == null || root.packageName?.toString() == ownPackage) return emptyList()
        val items = ArrayList<Item>()
        walk(root, items)
        items.sortWith(compareBy({ it.top / ROW_TOLERANCE }, { it.left }))
        val result = ArrayList<String>()
        for (item in items) {
            if (result.lastOrNull() != item.text) result += item.text
        }
        return result
    }

    private fun walk(node: AccessibilityNodeInfo, out: MutableList<Item>) {
        if (!node.isVisibleToUser) return
        val text = node.text?.toString()?.trim().orEmpty()
        val label = if (text.isEmpty() && node.childCount == 0) {
            node.contentDescription?.toString()?.trim().orEmpty()
        } else text
        if (label.isNotEmpty() && !node.isPassword) {
            val r = Rect().also { node.getBoundsInScreen(it) }
            out += Item(r.top, r.left, label)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { walk(it, out) }
        }
    }

    // 同一行(高度相差不到這個值)的元素視為同列,再比左右位置
    private const val ROW_TOLERANCE = 40
}
