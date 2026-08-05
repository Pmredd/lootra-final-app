package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB limit

    /**
     * Validates if the selected image URI matches allowed formats: JPG, JPEG, PNG, WEBP.
     */
    fun isValidImageFormat(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null) {
            val lowerMime = mimeType.lowercase()
            if (lowerMime.contains("jpeg") || lowerMime.contains("jpg") ||
                lowerMime.contains("png") || lowerMime.contains("webp")) {
                return true
            }
        }
        
        val path = uri.path?.lowercase() ?: ""
        return path.endsWith(".jpg") || path.endsWith(".jpeg") ||
               path.endsWith(".png") || path.endsWith(".webp") ||
               mimeType?.startsWith("image/") == true
    }

    /**
     * Retrieves the file size in bytes from Uri.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Validates and prepares the image Uri.
     * Compresses oversized images, maintains aspect ratio, preserves visual quality,
     * and outputs a cache file Uri ready for upload.
     */
    fun prepareAndCompressImage(context: Context, uri: Uri): Result<Uri> {
        return try {
            if (!isValidImageFormat(context, uri)) {
                return Result.failure(Exception("Invalid image format. Allowed formats: JPG, JPEG, PNG, WEBP."))
            }

            val fileSize = getFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                // Large image (> 10MB) - will be compressed below
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Unable to read selected image file."))

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth <= 0 || origHeight <= 0) {
                return Result.failure(Exception("Invalid or corrupted image file."))
            }

            // Target maximum dimension of 2048px to maintain sharp visual detail while optimizing upload size
            val maxDimension = 2048
            var sampleSize = 1
            while ((origWidth / sampleSize) > maxDimension || (origHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Unable to open image stream for decoding."))

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()

            if (bitmap == null) {
                return Result.failure(Exception("Failed to decode image bitmap."))
            }

            // Rotate based on EXIF metadata if needed
            bitmap = rotateBitmapIfRequired(context, uri, bitmap)

            // Compress to JPEG file in cache directory
            val cacheFile = File(context.cacheDir, "product_upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(cacheFile)

            // 85% JPEG compression keeps crisp details while ensuring optimal payload size
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            if (cacheFile.length() > MAX_FILE_SIZE_BYTES) {
                return Result.failure(Exception("Image file exceeds the maximum size limit of 10 MB even after compression."))
            }

            Result.success(Uri.fromFile(cacheFile))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to process image file."))
        }
    }

    private fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
