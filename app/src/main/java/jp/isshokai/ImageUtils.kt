package jp.isshokai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageUtils {
    fun decodeSampled(path: String, maxWidth: Int = 900, maxHeight: Int = 900): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /**
     * Product photos do not need 50 MP resolution. Keep enough detail for package text/JAN checks,
     * while preventing list screens from running out of memory and reducing storage use.
     */
    fun normalizePhoto(file: File, maxSide: Int = 1800, quality: Int = 88): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            val longest = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest / sample > maxSide * 2) sample *= 2

            val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
            }) ?: return false

            val scale = if (max(decoded.width, decoded.height) > maxSide) {
                maxSide.toFloat() / max(decoded.width, decoded.height).toFloat()
            } else 1f

            val output = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else decoded

            val temp = File(file.parentFile, file.nameWithoutExtension + "_small.jpg")
            FileOutputStream(temp).use { stream ->
                output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }
            if (output !== decoded) output.recycle()
            decoded.recycle()

            if (temp.length() > 0L) {
                // Replace within the same directory without deleting the original first.
                if (temp.renameTo(file)) true else {
                    temp.delete()
                    false
                }
            } else {
                temp.delete()
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
