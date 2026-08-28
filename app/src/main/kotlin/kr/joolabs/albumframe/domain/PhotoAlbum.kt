package kr.joolabs.albumframe.domain

data class PhotoAlbum(
    val id: String,
    val name: String,
    val photoCount: Int,
    val coverPhotoId: String?,
    val isAggregate: Boolean = false,
)

data class SlideshowSession(
    val album: PhotoAlbum,
    val photoIds: List<String>,
)
