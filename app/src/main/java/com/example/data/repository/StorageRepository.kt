package com.example.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CloudinaryUploadResponse(
    val secureUrl: String,
    val publicId: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val uploadedAt: Long = System.currentTimeMillis()
)

interface StorageRepository {
    suspend fun uploadProfileImage(uid: String, fileUri: Uri): Result<String>
    suspend fun uploadReelVideo(reelId: String, fileUri: Uri): Result<String>
    suspend fun uploadReelThumbnail(reelId: String, fileUri: Uri): Result<String>
    suspend fun uploadProductImage(
        fileUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<CloudinaryUploadResponse>
}

class StorageRepositoryImpl(
    private val firebaseStorage: FirebaseStorage? = null
) : StorageRepository {

    private val storage: FirebaseStorage
        get() = firebaseStorage ?: FirebaseStorage.getInstance()

    override suspend fun uploadProfileImage(uid: String, fileUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference
                .child("profile_images")
                .child("$uid.jpg")
            
            // Upload file
            val uploadTask = storageRef.putFile(fileUri).await()
            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            // Under JVM test environments or if storage fails, we can handle it or fall back gracefully
            Result.failure(e)
        }
    }

    private suspend fun uploadToCloudinary(fileUri: Uri, preset: String, resourceType: String): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val requestId = com.cloudinary.android.MediaManager.get().upload(fileUri)
                    .unsigned(preset)
                    .option("resource_type", resourceType)
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {}
                        
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as? String ?: resultData["url"] as? String
                            if (url != null) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(url))
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Cloudinary upload succeeded but returned no URL")))
                                }
                            }
                        }
                        
                        override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception(error.description ?: "Cloudinary upload failed")))
                            }
                        }
                        
                        override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Cloudinary upload rescheduled: ${error.description}")))
                            }
                        }
                    })
                    .dispatch()
                    
                continuation.invokeOnCancellation {
                    try {
                        com.cloudinary.android.MediaManager.get().cancelRequest(requestId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    private suspend fun uploadToCloudinaryWithDetails(
        fileUri: Uri,
        preset: String,
        resourceType: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<CloudinaryUploadResponse> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val requestId = com.cloudinary.android.MediaManager.get().upload(fileUri)
                    .unsigned(preset)
                    .option("resource_type", resourceType)
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {
                            onProgress?.invoke(0f)
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            if (totalBytes > 0) {
                                val progressRatio = (bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                onProgress?.invoke(progressRatio)
                            }
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as? String ?: resultData["url"] as? String ?: ""
                            val publicId = resultData["public_id"] as? String ?: ""
                            val width = (resultData["width"] as? Number)?.toInt() ?: 0
                            val height = (resultData["height"] as? Number)?.toInt() ?: 0
                            val uploadedAt = System.currentTimeMillis()

                            if (url.isNotBlank()) {
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.success(
                                            CloudinaryUploadResponse(
                                                secureUrl = url,
                                                publicId = publicId,
                                                width = width,
                                                height = height,
                                                uploadedAt = uploadedAt
                                            )
                                        )
                                    )
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Cloudinary upload succeeded but returned no URL")))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception(error.description ?: "Cloudinary upload failed")))
                            }
                        }

                        override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Cloudinary upload rescheduled: ${error.description}")))
                            }
                        }
                    })
                    .dispatch()

                continuation.invokeOnCancellation {
                    try {
                        com.cloudinary.android.MediaManager.get().cancelRequest(requestId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    override suspend fun uploadReelVideo(reelId: String, fileUri: Uri): Result<String> {
        return uploadToCloudinary(fileUri, "lootra_reels", "video")
    }

    override suspend fun uploadReelThumbnail(reelId: String, fileUri: Uri): Result<String> {
        return uploadToCloudinary(fileUri, "lootra_reels", "image")
    }

    override suspend fun uploadProductImage(
        fileUri: Uri,
        onProgress: ((Float) -> Unit)?
    ): Result<CloudinaryUploadResponse> {
        return uploadToCloudinaryWithDetails(fileUri, "lootra_reels", "image", onProgress)
    }
}
