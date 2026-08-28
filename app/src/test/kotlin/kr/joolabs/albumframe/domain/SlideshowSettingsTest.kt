package kr.joolabs.albumframe.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideshowSettingsTest {
    @Test
    fun cameraSplitDefaultsToFrontCamera() {
        val settings = SlideshowSettings()

        assertTrue(settings.cameraEnabled)
        assertEquals(CameraLens.FRONT, settings.cameraLens)
    }
}
