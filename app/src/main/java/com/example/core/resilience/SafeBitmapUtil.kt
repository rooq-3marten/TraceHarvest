package com.example.core.resilience

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Memory-safe Bitmap loader and compressor designed specifically to prevent
 * OutOfMemoryError (OOM) crashes on 1GB–2GB RAM budget phones (itel, Tecno, Infinix).
 */
object SafeBitmapUtil {

    private const val TAG = "SafeBitmapUtil"

    /**
     * Decodes a local image file with strict subsampling, ensuring memory allocation
     * never exceeds small thumbnail dimensions.
     */
    fun decodeSampledBitmap(
        file: File,
        targetWidth: Int = 300,
        targetHeight: Int = 300,
        isLowRamDevice: Boolean = false
    ): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null

        return try {
            // Step 1: Decode bounds only (zero memory allocated for pixels)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight

            if (rawWidth <= 0 || rawHeight <= 0) return null

            // Step 2: Calculate power-of-two inSampleSize
            var inSampleSize = 1
            if (rawHeight > targetHeight || rawWidth > targetWidth) {
                val halfHeight = rawHeight / 2
                val halfWidth = rawWidth / 2
                while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                    inSampleSize *= 2
                }
            }

            // Step 3: Decode with subsampling and memory-efficient pixel config
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = max(1, inSampleSize)
                this.inJustDecodeBounds = false
                // RGB_565 uses 2 bytes per pixel instead of 4 (ARGB_8888), saving 50% RAM!
                this.inPreferredConfig = if (isLowRamDevice) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            }

            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError intercepted while decoding ${file.name}. Triggering GC.", oom)
            System.gc()
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error decoding bitmap: ${e.message}")
            null
        }
    }

    /**
     * Compresses and saves an image Uri from the camera or photo picker to an app-private
     * lightweight JPEG file (max 640px, ~60KB-120KB) to prevent storage bloat.
     */
    fun compressAndSavePhoto(
        context: Context,
        sourceUri: Uri,
        destFileName: String,
        maxDimensionPx: Int = 640,
        quality: Int = 75
    ): File? {
        return try {
            var input: InputStream? = context.contentResolver.openInputStream(sourceUri) ?: return null

            // Read bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val rawW = options.outWidth
            val rawH = options.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            var longest = max(rawW, rawH)
            while (longest / 2 >= maxDimensionPx) {
                longest /= 2
                sampleSize *= 2
            }

            // Decode scaled bitmap
            input = context.contentResolver.openInputStream(sourceUri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = sampleSize
                this.inPreferredConfig = Bitmap.Config.RGB_565
            }
            val original = BitmapFactory.decodeStream(input, null, decodeOptions)
            input.close()

            if (original == null) return null

            // Scale to exact bounding box if still larger
            val finalBitmap = if (original.width > maxDimensionPx || original.height > maxDimensionPx) {
                val ratio = minOf(maxDimensionPx.toFloat() / original.width, maxDimensionPx.toFloat() / original.height)
                val newW = (original.width * ratio).toInt()
                val newH = (original.height * ratio).toInt()
                Bitmap.createScaledBitmap(original, newW, newH, true).also {
                    if (it != original) original.recycle()
                }
            } else {
                original
            }

            // Save to internal storage
            val photosDir = File(context.filesDir, "farmer_photos").apply { mkdirs() }
            val destFile = File(photosDir, destFileName)
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            finalBitmap.recycle()

            destFile
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM during photo compression", oom)
            System.gc()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress photo", e)
            null
        }
    }
}
