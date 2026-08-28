package kr.joolabs.albumframe.presentation

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kr.joolabs.albumframe.application.PhotoLibrary
import kr.joolabs.albumframe.application.SettingsRepository
import kr.joolabs.albumframe.domain.PhotoAlbum
import kr.joolabs.albumframe.domain.SlideshowSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aggregateAlbumTriesEveryPhotoBeforeReportingUnavailable() = runTest(dispatcher) {
        val album = PhotoAlbum(
            id = "all",
            name = "모든 사진",
            photoCount = 2,
            coverPhotoId = "content://bad-1",
            isAggregate = true,
        )
        val library = UnreadablePhotoLibrary(album)
        val viewModel = MainViewModel(
            photoLibrary = library,
            settingsRepository = InMemorySettingsRepository(
                SlideshowSettings(selectedAlbumId = album.id),
            ),
            ioDispatcher = dispatcher,
        )

        viewModel.refreshAlbums(canReadPhotos = true)
        runCurrent()
        viewModel.openSelectedAlbum()
        runCurrent()

        assertEquals(album.id, library.requestedAlbumId)
        assertEquals(listOf("content://bad-1", "content://bad-2"), library.loadAttempts)
        assertTrue(requireNotNull(viewModel.state.value.player).imageUnavailable)
        viewModel.setAppActive(false)
    }

    private class UnreadablePhotoLibrary(
        private val album: PhotoAlbum,
    ) : PhotoLibrary {
        var requestedAlbumId: String? = null
        val loadAttempts = mutableListOf<String>()

        override fun listAlbums(): List<PhotoAlbum> = listOf(album)

        override fun listPhotoIds(albumId: String): List<String> {
            requestedAlbumId = albumId
            return listOf("content://bad-1", "content://bad-2")
        }

        override fun loadImage(photoId: String, maxDimension: Int): Bitmap? {
            loadAttempts += photoId
            return null
        }
    }

    private class InMemorySettingsRepository(
        private var settings: SlideshowSettings,
    ) : SettingsRepository {
        override fun load(): SlideshowSettings = settings

        override fun save(settings: SlideshowSettings) {
            this.settings = settings
        }
    }
}
