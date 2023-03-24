package com.quranapp.android.utils.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable


fun Drawable.rotate(ctx: Context, angle: Float): Drawable {
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap).apply {
        save()
        rotate(angle, (bitmap.width shr 1).toFloat(), (bitmap.height shr 1).toFloat())
    }
    mutate().apply {
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }
    canvas.restore()
    return BitmapDrawable(ctx.resources, bitmap)
}
