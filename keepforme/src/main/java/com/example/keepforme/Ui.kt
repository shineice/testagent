package com.example.keepforme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import java.io.File
import java.text.DateFormat
import java.util.Date

fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

fun formatTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

/** 依寬度縮小解碼,避免大圖爆記憶體。 */
fun decodeScaled(file: File, maxWidth: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
    return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
}
