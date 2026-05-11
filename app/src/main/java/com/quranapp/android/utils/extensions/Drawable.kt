package com.quranapp.android.utils.extensions

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable


fun Drawable.rotate(ctx: Context, angle: Float): Drawable {
    val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)

    val canvas = Canvas(bitmap).apply {
        save()
        rotate(angle, (bitmap.width shr 1).toFloat(), (bitmap.height shr 1).toFloat())
    }

    mutate().apply {
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }

    canvas.restore()

    return bitmap.toDrawable(ctx.resources)
}
