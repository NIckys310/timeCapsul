package com.timecapsule.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

object MediaUtil {
    const val MAX_VIDEO_BYTES = 50L * 1024 * 1024   // límite del plan gratis de Supabase

    fun mime(ctx: Context, uri: Uri): String = ctx.contentResolver.getType(uri) ?: "image/jpeg"
    fun kindOf(ctx: Context, uri: Uri): String = if (mime(ctx, uri).startsWith("video")) "video" else "photo"

    fun sizeOf(ctx: Context, uri: Uri): Long =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L

    /** Reduce la foto a máx. 1920 px, corrige la rotación y la guarda como JPG. */
    fun compressPhoto(ctx: Context, uri: Uri, maxSide: Int = 1920): ByteArray {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer una de las fotos")

        val orientation = runCatching {
            cr.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val scale = min(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
        val m = Matrix().apply { postScale(scale, scale); postRotate(degrees) }
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        return ByteArrayOutputStream().use { s ->
            out.compress(Bitmap.CompressFormat.JPEG, 85, s)
            s.toByteArray()
        }
    }

    fun readVideo(ctx: Context, uri: Uri): ByteArray {
        val size = sizeOf(ctx, uri)
        if (size > MAX_VIDEO_BYTES) error("Un video pesa más de 50 MB. Elige uno más corto.")
        return ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo leer un video")
    }
}
