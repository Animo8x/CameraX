package com.example.camerax.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.camerax.model.AiEnhanceConfig
import com.example.camerax.model.AspectRatioOption
import com.example.camerax.model.CameraFilter
import com.example.camerax.model.ColorStudioConfig
import com.example.camerax.model.MediaItem
import com.example.camerax.model.SuperNightConfig
import com.example.camerax.processing.ImageProcessingPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaRepository(private val context: Context) {

    // Primary directory: Public DCIM/Camera (system Camera album across the entire device)
    val dcimCameraDir: File by lazy {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val cameraDir = File(dcim, "Camera")
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }
        cameraDir
    }

    // Secondary directory: App external files (fallback if DCIM is inaccessible)
    private val fallbackPicturesDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CameraX").apply {
            if (!exists()) mkdirs()
        }
    }

    private val fallbackMoviesDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CameraX").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _latestMedia = MutableStateFlow<MediaItem?>(null)
    val latestMedia: StateFlow<MediaItem?> = _latestMedia.asStateFlow()

    fun getPicturesOutputDirectory(): File {
        return try {
            if (dcimCameraDir.exists() || dcimCameraDir.mkdirs()) {
                dcimCameraDir
            } else {
                fallbackPicturesDir
            }
        } catch (e: Exception) {
            fallbackPicturesDir
        }
    }

    fun getMoviesOutputDirectory(): File {
        return try {
            if (dcimCameraDir.exists() || dcimCameraDir.mkdirs()) {
                dcimCameraDir
            } else {
                fallbackMoviesDir
            }
        } catch (e: Exception) {
            fallbackMoviesDir
        }
    }

    suspend fun refreshMediaList() = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val seenPaths = HashSet<String>()

        // Scan both public DCIM/Camera and app fallback directories
        val dirsToScan = listOf(dcimCameraDir, fallbackPicturesDir, fallbackMoviesDir)
        for (dir in dirsToScan) {
            try {
                if (!dir.exists() || !dir.isDirectory) continue
                dir.listFiles()?.forEach { file ->
                    if (!file.isFile || seenPaths.contains(file.absolutePath)) return@forEach
                    val name = file.name.lowercase(Locale.US)
                    val isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                    val isVideo = name.endsWith(".mp4")

                    if (isImage) {
                        seenPaths.add(file.absolutePath)
                        val uri = getUriForFile(file)
                        val bounds = getImageBounds(file)
                        items.add(
                            MediaItem(
                                id = file.name,
                                file = file,
                                uri = uri,
                                isVideo = false,
                                dateAdded = file.lastModified(),
                                width = bounds.first,
                                height = bounds.second,
                                sizeBytes = file.length()
                            )
                        )
                    } else if (isVideo) {
                        seenPaths.add(file.absolutePath)
                        val uri = getUriForFile(file)
                        val info = getVideoInfo(file)
                        items.add(
                            MediaItem(
                                id = file.name,
                                file = file,
                                uri = uri,
                                isVideo = true,
                                durationMs = info.first,
                                dateAdded = file.lastModified(),
                                width = info.second,
                                height = info.third,
                                sizeBytes = file.length()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error scanning dir $dir: ${e.message}")
            }
        }

        val sorted = items.sortedByDescending { it.dateAdded }
        _mediaList.value = sorted
        _latestMedia.value = sorted.firstOrNull()
    }

    fun createNewImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getPicturesOutputDirectory()
        return File(dir, "IMG_${timeStamp}.jpg")
    }

    fun createNewVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getMoviesOutputDirectory()
        return File(dir, "VID_${timeStamp}.mp4")
    }

    suspend fun processAndSavePhoto(
        tempFile: File,
        studioConfig: ColorStudioConfig,
        filter: CameraFilter,
        aiConfig: AiEnhanceConfig,
        nightConfig: SuperNightConfig,
        isDocumentMode: Boolean = false,
        aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
        digitalCropScale: Float = 1.0f,
        jpegQuality: Int = 95
    ): MediaItem = withContext(Dispatchers.IO) {
        var finalFile = createNewImageFile()

        val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
        val filteredBitmap = ImageProcessingPipeline.processBitmap(
            source = rawBitmap,
            studioConfig = studioConfig,
            filter = filter,
            aiConfig = aiConfig,
            nightConfig = nightConfig,
            isDocumentMode = isDocumentMode
        )

        // Apply digital zoom crop and aspect ratio crop
        val finalBitmap = applyCropAndZoom(filteredBitmap, aspectRatio, digitalCropScale)

        // 1. Write image to file in DCIM/Camera
        var writeSuccess = false
        try {
            FileOutputStream(finalFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.flush()
            }
            writeSuccess = true
        } catch (e: Exception) {
            Log.e("MediaRepository", "Direct save to DCIM/Camera failed, using fallback: ${e.message}")
            finalFile = File(fallbackPicturesDir, finalFile.name)
            FileOutputStream(finalFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.flush()
            }
            writeSuccess = true
        }

        // 2. Insert into Android MediaStore for system-wide Gallery visibility
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, finalFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.WIDTH, finalBitmap.width)
                put(MediaStore.Images.Media.HEIGHT, finalBitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w("MediaRepository", "MediaStore insertion exception: ${e.message}")
        }

        // 3. Request immediate MediaScanner indexing across the phone
        MediaScannerConnection.scanFile(
            context,
            arrayOf(finalFile.absolutePath),
            arrayOf("image/jpeg")
        ) { path, uri ->
            Log.i("MediaRepository", "MediaScanner indexed: $path -> $uri")
        }

        // Delete temporary cache file
        if (tempFile.exists() && tempFile != finalFile) {
            try { tempFile.delete() } catch (ignored: Exception) {}
        }

        val uri = getUriForFile(finalFile)
        val item = MediaItem(
            id = finalFile.name,
            file = finalFile,
            uri = uri,
            isVideo = false,
            dateAdded = finalFile.lastModified(),
            width = finalBitmap.width,
            height = finalBitmap.height,
            sizeBytes = finalFile.length()
        )

        refreshMediaList()
        item
    }

    suspend fun saveEditedCopy(
        originalItem: MediaItem,
        editedBitmap: Bitmap,
        jpegQuality: Int = 95
    ): MediaItem = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = getPicturesOutputDirectory()
        val newFile = File(dir, "EDIT_${originalItem.file.nameWithoutExtension}_$timeStamp.jpg")

        FileOutputStream(newFile).use { out ->
            editedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            out.flush()
        }

        // Insert into MediaStore and scan
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.WIDTH, editedBitmap.width)
                put(MediaStore.Images.Media.HEIGHT, editedBitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (ignored: Exception) {}

        MediaScannerConnection.scanFile(
            context,
            arrayOf(newFile.absolutePath),
            arrayOf("image/jpeg"),
            null
        )

        val uri = getUriForFile(newFile)
        val item = MediaItem(
            id = newFile.name,
            file = newFile,
            uri = uri,
            isVideo = false,
            dateAdded = newFile.lastModified(),
            width = editedBitmap.width,
            height = editedBitmap.height,
            sizeBytes = newFile.length()
        )

        refreshMediaList()
        item
    }

    fun notifyVideoRecorded(videoFile: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera")
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
            }
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (ignored: Exception) {}

        MediaScannerConnection.scanFile(
            context,
            arrayOf(videoFile.absolutePath),
            arrayOf("video/mp4"),
            null
        )
    }

    suspend fun deleteMediaItem(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val deleted = item.file.delete()
        if (deleted) {
            refreshMediaList()
        }
        deleted
    }

    fun getUriForFile(file: File): Uri {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    fun createShareIntent(item: MediaItem): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            val mime = if (item.isVideo) "video/mp4" else "image/jpeg"
            type = mime
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share via")
    }

    private fun getImageBounds(file: File): Pair<Int, Int> {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun getVideoInfo(file: File): Triple<Long, Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0
            Triple(duration, width, height)
        } catch (e: Exception) {
            Triple(0L, 0, 0)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    private fun applyCropAndZoom(bitmap: Bitmap, aspectRatio: AspectRatioOption, digitalScale: Float): Bitmap {
        var working = bitmap

        // 1. Digital 100X Zoom crop OR 0.5X Digital Wide FOV perspective
        if (digitalScale > 1.05f) {
            val cropW = (working.width / digitalScale).toInt().coerceIn(64, working.width)
            val cropH = (working.height / digitalScale).toInt().coerceIn(64, working.height)
            val startX = (working.width - cropW) / 2
            val startY = (working.height - cropH) / 2
            val zoomed = Bitmap.createBitmap(working, startX, startY, cropW, cropH)
            if (working != bitmap) working.recycle()
            working = zoomed
        } else if (digitalScale < 0.95f) {
            // Digital 0.5x Wide Mode perspective framing
            val targetW = (working.width * 1.12f).toInt()
            val targetH = (working.height * 1.12f).toInt()
            val wideCanvas = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(wideCanvas)
            canvas.drawColor(android.graphics.Color.BLACK)
            val startX = (targetW - working.width) / 2f
            val startY = (targetH - working.height) / 2f
            canvas.drawBitmap(working, startX, startY, null)
            if (working != bitmap) working.recycle()
            working = wideCanvas
        }

        // 2. Aspect Ratio Crop
        when (aspectRatio) {
            AspectRatioOption.RATIO_1_1 -> {
                val side = Math.min(working.width, working.height)
                val x = (working.width - side) / 2
                val y = (working.height - side) / 2
                val squared = Bitmap.createBitmap(working, x, y, side, side)
                if (working != bitmap) working.recycle()
                working = squared
            }
            AspectRatioOption.RATIO_16_9 -> {
                if (working.height > working.width) {
                    val targetH = (working.width * (16f / 9f)).toInt().coerceAtMost(working.height)
                    val y = (working.height - targetH) / 2
                    val cropped = Bitmap.createBitmap(working, 0, y, working.width, targetH)
                    if (working != bitmap) working.recycle()
                    working = cropped
                } else {
                    val targetW = (working.height * (16f / 9f)).toInt().coerceAtMost(working.width)
                    val x = (working.width - targetW) / 2
                    val cropped = Bitmap.createBitmap(working, x, 0, targetW, working.height)
                    if (working != bitmap) working.recycle()
                    working = cropped
                }
            }
            AspectRatioOption.RATIO_4_3 -> {
                if (working.height > working.width) {
                    val targetH = (working.width * (4f / 3f)).toInt().coerceAtMost(working.height)
                    val y = (working.height - targetH) / 2
                    val cropped = Bitmap.createBitmap(working, 0, y, working.width, targetH)
                    if (working != bitmap) working.recycle()
                    working = cropped
                } else {
                    val targetW = (working.height * (4f / 3f)).toInt().coerceAtMost(working.width)
                    val x = (working.width - targetW) / 2
                    val cropped = Bitmap.createBitmap(working, x, 0, targetW, working.height)
                    if (working != bitmap) working.recycle()
                    working = cropped
                }
            }
            AspectRatioOption.RATIO_FULL -> {
                // Keep full sensor capture
            }
        }
        return working
    }
}
