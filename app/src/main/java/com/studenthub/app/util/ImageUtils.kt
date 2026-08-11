package com.studenthub.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Native equivalent of the website's client-side canvas compression (profile photos and
 * chat photos are stored as Base64 strings directly in Firestore, no Firebase Storage —
 * see NATIVE_BUILD_PROMPT.md "Architecture" notes). Downscales to maxDimension on the
 * longest side and JPEG-compresses so messages/profile docs stay well under Firestore's
 * 1 MiB document limit even with a few photos attached.
 */
object ImageUtils {

    fun uriToCompressedBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 70
    ): String? {
        val bitmap = decodeSampledBitmap(context, uri, maxDimension) ?: return null
        val scaled = scaleDown(bitmap, maxDimension)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver

        // First pass: read bounds only, to compute an inSampleSize (avoids OOM on big photos).
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        var sample = 1
        var (w, h) = boundsOptions.outWidth to boundsOptions.outHeight
        while (w / (sample * 2) >= maxDimension || h / (sample * 2) >= maxDimension) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longest
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
