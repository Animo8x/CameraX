package com.example.camerax.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.camerax.model.AiEnhanceConfig
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

    private val picturesDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "CameraX").apply {
            if (!exists()) mkdirs()
        }
    }

    private val moviesDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CameraX").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _latestMedia = MutableStateFlow<MediaItem?>(null)
    val latestMedia: StateFlow<MediaItem?> = _latestMedia.asStateFlow()

    suspend fun refreshMediaList() = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()

        // Read pictures
        picturesDir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".jpg", true) || it.name.endsWith(".png", true))
        }?.forEach { file ->
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
        }

        // Read videos
        moviesDir.listFiles()?.filter {
            it.isFile && it.name.endsWith(".mp4", true)
        }?.forEach { file ->
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

        val sorted = items.sortedByDescending { it.dateAdded }
        _mediaList.value = sorted
        _latestMedia.value = sorted.firstOrNull()
    }

    fun createNewImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(picturesDir, "IMG_$timeStamp.jpg")
    }

    fun createNewVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(moviesDir, "VID_$timeStamp.mp4")
    }

    suspend fun processAndSavePhoto(
        tempFile: File,
        studioConfig: ColorStudioConfig,
        filter: CameraFilter,
        aiConfig: AiEnhanceConfig,
        nightConfig: SuperNightConfig,
        isDocumentMode: Boolean = false,
        jpegQuality: Int = 95
    ): MediaItem = withContext(Dispatchers.IO) {
        val finalFile = createNewImageFile()

        val rawBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
        val processedBitmap = ImageProcessingPipeline.processBitmap(
            source = rawBitmap,
            studioConfig = studioConfig,
            filter = filter,
            aiConfig = aiConfig,
            nightConfig = nightConfig,
            isDocumentMode = isDocumentMode
        )

        FileOutputStream(finalFile).use { out ->
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            out.flush()
        }

        if (tempFile.exists() && tempFile != finalFile) {
            tempFile.delete()
        }

        val uri = getUriForFile(finalFile)
        val item = MediaItem(
            id = finalFile.name,
            file = finalFile,
            uri = uri,
            isVideo = false,
            dateAdded = finalFile.lastModified(),
            width = processedBitmap.width,
            height = processedBitmap.height,
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
        val newFile = File(picturesDir, "EDIT_${originalItem.file.nameWithoutExtension}_$timeStamp.jpg")

        FileOutputStream(newFile).use { out ->
            editedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            out.flush()
        }

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

    suspend fun deleteMediaItem(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val deleted = item.file.delete()
        if (deleted) {
            refreshMediaList()
        }
        deleted
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
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
}
