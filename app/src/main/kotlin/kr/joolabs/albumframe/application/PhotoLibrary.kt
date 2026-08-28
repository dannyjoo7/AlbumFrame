package kr.joolabs.albumframe.application

import android.graphics.Bitmap
import kr.joolabs.albumframe.domain.PhotoAlbum

interface PhotoLibrary {
    fun listAlbums(): List<PhotoAlbum>

    fun listPhotoIds(albumId: String): List<String>

    fun loadImage(photoId: String, maxDimension: Int): Bitmap?
}
