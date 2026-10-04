package club.matix.mathclub.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Shrinks picked images before storing them in the existing Realtime Database fields. */
object ImageCodec {
    private const val MAX_DIMENSION = 768
    private const val MAX_BYTES = 320_000

    fun encodeJpegDataUri(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "That image could not be read." }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_DIMENSION * 2) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("That image could not be read.")
        val ratio = minOf(1f, MAX_DIMENSION.toFloat() / max(bitmap.width, bitmap.height))
        var scaled: Bitmap = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1), (bitmap.height * ratio).roundToInt().coerceAtLeast(1), true) else bitmap
        if (scaled !== bitmap) bitmap.recycle()
        var quality = 84
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            if (bytes.size <= MAX_BYTES) break
            if (quality > 55) quality -= 10
            else {
                val smaller = Bitmap.createScaledBitmap(scaled, (scaled.width * .78f).roundToInt().coerceAtLeast(1),
                    (scaled.height * .78f).roundToInt().coerceAtLeast(1), true)
                if (smaller !== scaled) scaled.recycle()
                scaled = smaller
                quality = 74
            }
        } while (scaled.width > 1 && scaled.height > 1)
        scaled.recycle()
        require(bytes.size <= MAX_BYTES) { "Please choose a smaller image." }
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeDataUri(data: String): Bitmap? {
        return try {
            val comma = data.indexOf(',')
            if (comma < 0 || !data.startsWith("data:image/", ignoreCase = true)) null
            else {
                val bytes = Base64.decode(data.substring(comma + 1), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Exception) { null }
    }
}
