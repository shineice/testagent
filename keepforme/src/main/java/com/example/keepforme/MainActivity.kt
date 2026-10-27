package com.example.keepforme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var store: EntryStore
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private val adapter = EntryAdapter()
    private var all: List<Entry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EntryStore(this)

        search = EditText(this).apply {
            hint = "搜尋標題、內文、連結"
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = refilter()
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            })
        }
        empty = TextView(this).apply {
            text = "還沒有收藏。\n在 Threads、IG 或任何 App 按「分享」,選「留我自己看」。"
            textSize = 16f
            setPadding(dp(24), dp(48), dp(24), 0)
        }
        val list = ListView(this).apply {
            this.adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, pos, _ ->
                startActivity(
                    Intent(this@MainActivity, DetailActivity::class.java)
                        .putExtra(DetailActivity.EXTRA_ID, this@MainActivity.adapter.getItem(pos).id)
                )
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(32), dp(12), 0)
            addView(search)
            addView(empty)
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        })
    }

    override fun onResume() {
        super.onResume()
        all = store.list()
        refilter()
    }

    private fun refilter() {
        adapter.items = all.filter { it.matches(search.text.toString().trim()) }
        empty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }

    private inner class EntryAdapter : BaseAdapter() {
        var items: List<Entry> = emptyList()
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val e = getItem(position)
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(12), dp(8), dp(12))
                addView(TextView(context).apply { textSize = 17f; maxLines = 2 })
                addView(TextView(context).apply { textSize = 13f; alpha = 0.6f })
            }
            val status = when (e.status) {
                Entry.STATUS_PENDING -> " · 抓取中…"
                Entry.STATUS_FAILED -> " · 沒抓到全文"
                else -> ""
            }
            (row.getChildAt(0) as TextView).text = e.displayTitle()
            (row.getChildAt(1) as TextView).text = formatTime(e.createdAt) + status
            return row
        }
    }
}
