package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object ImageWebpUtils {

    fun uriToWebpBase64(context: Context, uri: Uri, maxDimension: Int = 256, quality: Int = 65): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth.coerceAtLeast(1)
            val origH = boundsOpts.outHeight.coerceAtLeast(1)
            var sampleSize = 1
            while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val ratio = minOf(
                maxDimension.toFloat() / decoded.width.coerceAtLeast(1),
                maxDimension.toFloat() / decoded.height.coerceAtLeast(1),
                1f
            )
            val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (targetW != decoded.width || targetH != decoded.height) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            scaled.compress(format, quality, baos)
            scaled.recycle()

            val bytes = baos.toByteArray()
            if (bytes.size > 105_000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        return decodeBase64ToBitmap(base64)?.asImageBitmap()
    }
}
