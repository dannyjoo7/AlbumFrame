package kr.joolabs.albumframe.application

/** 기기 세대와 메모리에 맞춰 Bitmap 디코딩·캐시 상한을 정한다. */
data class ImageMemoryPolicy(
    val maxDisplayDimension: Int,
    val thumbnailCacheEntries: Int,
    val displayCacheEntries: Int,
) {
    companion object {
        fun forDevice(
            memoryClassMb: Int,
            lowRamDevice: Boolean,
            sdkInt: Int,
        ): ImageMemoryPolicy = when {
            lowRamDevice || memoryClassMb <= 128 -> ImageMemoryPolicy(
                maxDisplayDimension = 1_280,
                thumbnailCacheEntries = 12,
                displayCacheEntries = 2,
            )
            memoryClassMb <= 256 || sdkInt <= 28 -> ImageMemoryPolicy(
                maxDisplayDimension = 1_600,
                thumbnailCacheEntries = 20,
                displayCacheEntries = 2,
            )
            else -> ImageMemoryPolicy(
                maxDisplayDimension = 2_160,
                thumbnailCacheEntries = 32,
                displayCacheEntries = 3,
            )
        }
    }
}
