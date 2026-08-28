package kr.joolabs.albumframe.domain

enum class SlideshowOrder {
    CHRONOLOGICAL,
    SHUFFLED,
}

enum class PhotoFit {
    CONTAIN,
    COVER,
}

enum class CameraLens {
    FRONT,
    BACK,
}

data class SlideshowSettings(
    val selectedAlbumId: String? = null,
    val intervalSeconds: Int = DEFAULT_INTERVAL_SECONDS,
    val order: SlideshowOrder = SlideshowOrder.CHRONOLOGICAL,
    val fit: PhotoFit = PhotoFit.CONTAIN,
    val cameraEnabled: Boolean = true,
    val cameraLens: CameraLens = CameraLens.FRONT,
) {
    companion object {
        const val DEFAULT_INTERVAL_SECONDS = 5
        val INTERVAL_CHOICES = listOf(3, 5, 10, 30)
    }
}
