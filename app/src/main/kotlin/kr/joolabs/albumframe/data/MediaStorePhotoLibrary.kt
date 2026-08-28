package kr.joolabs.albumframe.data

import android.content.ContentUris
import android.content.Context
import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kr.joolabs.albumframe.R
import kr.joolabs.albumframe.application.ImageMemoryPolicy
import kr.joolabs.albumframe.application.PhotoLibrary
import kr.joolabs.albumframe.domain.PhotoAlbum
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.max

class MediaStorePhotoLibrary(private val context: Context) : PhotoLibrary {
    private val resolver = context.contentResolver
    private val memoryPolicy = createMemoryPolicy(context)
    private val thumbnailCache = BitmapCountCache(memoryPolicy.thumbnailCacheEntries)
    private val displayCache = BitmapCountCache(memoryPolicy.displayCacheEntries)

    override fun listAlbums(): List<PhotoAlbum> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val albums = linkedMapOf<String, MutableAlbum>()
        var allCount = 0
        var allCover: String? = null
        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            )
            while (cursor.moveToNext()) {
                val photoId = ContentUris.withAppendedId(
                    collection,
                    cursor.getLong(idColumn),
                ).toString()
                allCount++
                if (allCover == null) allCover = photoId
                val bucketId = cursor.getString(bucketIdColumn) ?: UNKNOWN_ALBUM_ID
                val bucketName = cursor.getString(bucketNameColumn)
                    ?: context.getString(R.string.unnamed_album)
                albums.getOrPut(bucketId) {
                    MutableAlbum(bucketId, bucketName, photoId)
                }.photoCount++
            }
        }
        if (allCount == 0) return emptyList()
        return buildList {
            add(
                PhotoAlbum(
                    id = ALL_PHOTOS_ID,
                    name = context.getString(R.string.all_photos),
                    photoCount = allCount,
                    coverPhotoId = allCover,
                    isAggregate = true,
                ),
            )
            addAll(albums.values.map(MutableAlbum::toDomain))
        }
    }

    override fun listPhotoIds(albumId: String): List<String> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val isAllPhotos = albumId == ALL_PHOTOS_ID
        val selection = if (isAllPhotos) null else "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = if (isAllPhotos) null else arrayOf(albumId)
        return buildList {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    add(
                        ContentUris.withAppendedId(
                            collection,
                            cursor.getLong(idColumn),
                        ).toString(),
                    )
                }
            }
        }
    }

    override fun loadImage(photoId: String, maxDimension: Int): Bitmap? {
        val isThumbnail = maxDimension <= THUMBNAIL_DIMENSION
        val effectiveDimension = if (isThumbnail) {
            maxDimension.coerceIn(MIN_DECODE_DIMENSION, THUMBNAIL_DIMENSION)
        } else {
            maxDimension.coerceIn(MIN_DECODE_DIMENSION, memoryPolicy.maxDisplayDimension)
        }
        val cache = if (isThumbnail) {
            thumbnailCache
        } else {
            displayCache
        }
        val key = "$photoId@$effectiveDimension"
        cache.get(key)?.takeUnless(Bitmap::isRecycled)?.let { return it }
        val uri = photoId.toUri()
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri, effectiveDimension)
        } else {
            decodeWithBitmapFactory(uri, effectiveDimension)
        } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: android.net.Uri, maxDimension: Int): Bitmap? =
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(resolver, uri),
        ) { decoder, info, _ ->
            val sourceDimension = max(info.size.width, info.size.height)
            if (sourceDimension > maxDimension) {
                decoder.setTargetSampleSize(
                    ceil(sourceDimension.toDouble() / maxDimension)
                        .toInt()
                        .coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

    private fun decodeWithBitmapFactory(uri: android.net.Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).useSafely { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri).useSafely { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null
        return applyExifOrientation(uri, bitmap)
    }

    private fun applyExifOrientation(uri: android.net.Uri, bitmap: Bitmap): Bitmap {
        val transform = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                ExifTransform(
                    rotationDegrees = exif.rotationDegrees,
                    flipHorizontally = exif.isFlipped,
                )
            }
        }.getOrNull() ?: return bitmap
        if (transform.isIdentity) return bitmap

        val matrix = Matrix().apply {
            if (transform.flipHorizontally) postScale(-1f, 1f)
            if (transform.rotationDegrees != 0) {
                postRotate(transform.rotationDegrees.toFloat())
            }
        }
        val transformed = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
        if (transformed !== bitmap) bitmap.recycle()
        return transformed
    }

    private inline fun <T> InputStream?.useSafely(block: (InputStream) -> T): T? =
        this?.use(block)

    private data class MutableAlbum(
        val id: String,
        val name: String,
        val coverPhotoId: String,
        var photoCount: Int = 0,
    ) {
        fun toDomain() = PhotoAlbum(id, name, photoCount, coverPhotoId)
    }

    private data class ExifTransform(
        val rotationDegrees: Int,
        val flipHorizontally: Boolean,
    ) {
        val isIdentity: Boolean
            get() = rotationDegrees == 0 && !flipHorizontally
    }

    private class BitmapCountCache(maxEntries: Int) : LruCache<String, Bitmap>(maxEntries) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    companion object {
        const val ALL_PHOTOS_ID = "isAll"
        const val THUMBNAIL_DIMENSION = 512
        const val DISPLAY_DIMENSION = 2160
        private const val UNKNOWN_ALBUM_ID = "unknown"
        private const val MIN_DECODE_DIMENSION = 320

        private fun createMemoryPolicy(context: Context): ImageMemoryPolicy {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            return ImageMemoryPolicy.forDevice(
                memoryClassMb = activityManager.memoryClass,
                lowRamDevice = activityManager.isLowRamDevice,
                sdkInt = Build.VERSION.SDK_INT,
            )
        }
    }
}
