package kr.joolabs.albumframe.application

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMemoryPolicyTest {
    @Test
    fun lowRamDeviceUsesSmallDecodeAndCacheBudget() {
        val policy = ImageMemoryPolicy.forDevice(
            memoryClassMb = 256,
            lowRamDevice = true,
            sdkInt = 28,
        )

        assertEquals(1_280, policy.maxDisplayDimension)
        assertEquals(12, policy.thumbnailCacheEntries)
        assertEquals(2, policy.displayCacheEntries)
    }

    @Test
    fun oldAndroidUsesBalancedBudgetEvenWithMoreMemory() {
        val policy = ImageMemoryPolicy.forDevice(
            memoryClassMb = 512,
            lowRamDevice = false,
            sdkInt = 28,
        )

        assertEquals(1_600, policy.maxDisplayDimension)
        assertEquals(2, policy.displayCacheEntries)
    }

    @Test
    fun modernDeviceKeepsFullQualityBudget() {
        val policy = ImageMemoryPolicy.forDevice(
            memoryClassMb = 512,
            lowRamDevice = false,
            sdkInt = 36,
        )

        assertEquals(2_160, policy.maxDisplayDimension)
        assertEquals(3, policy.displayCacheEntries)
    }
}
